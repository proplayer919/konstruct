package dev.proplayer919.konstruct.instance;

import dev.proplayer919.konstruct.instance.gameplayer.GamePlayerData;
import dev.proplayer919.konstruct.instance.gameplayer.GamePlayerStatus;
import dev.proplayer919.konstruct.loot.ChestIdentifier;
import dev.proplayer919.konstruct.loot.ChestLootRegistry;
import dev.proplayer919.konstruct.match.MatchManager;
import dev.proplayer919.konstruct.match.MatchStatus;
import dev.proplayer919.konstruct.match.types.MatchType;
import dev.proplayer919.konstruct.messages.MatchMessages;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.util.BoundsHelper;
import dev.proplayer919.konstruct.util.PlayerHubHelper;
import dev.proplayer919.konstruct.util.PlayerInventoryBlockRegistry;
import io.github.togar2.pvp.events.EntityPreDeathEvent;
import io.github.togar2.pvp.events.FinalAttackEvent;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.Inventory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class GameInstanceData extends InstanceData {
    private final UUID hostUUID;
    private final String hostUsername;
    private final MatchType matchType;
    private final ChestLootRegistry chestLootRegistry;
    private final PlayerInventoryBlockRegistry inventoryBlockRegistry;
    private final Date startTime = new Date(System.currentTimeMillis() + 300000); // Default to 5 minutes from now
    
    @Setter
    private MatchStatus matchStatus = MatchStatus.WAITING;

    private final Collection<GamePlayerData> players = new HashSet<>();

    // Per-instance scheduler
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public GameInstanceData(String id, UUID hostUUID, MatchType matchType) {
        super(InstanceType.GAME, matchType.getInstance(), id);
        this.hostUUID = hostUUID;
        this.hostUsername = Objects.requireNonNull(MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(hostUUID)).getUsername();
        this.matchType = matchType;
        this.chestLootRegistry = new ChestLootRegistry();
        this.inventoryBlockRegistry = new PlayerInventoryBlockRegistry();

        // Setup schedules
        scheduler.scheduleAtFixedRate(() -> {
            // Don't advertise in the minute where the match starts
            long millisUntilStart = getStartTime().getTime() - System.currentTimeMillis();
            if (getMatchStatus() == MatchStatus.WAITING && isNotFull()
                    && millisUntilStart >= 60000) {
                Collection<Audience> playersInHubs = HubInstanceRegistry.getAllPlayersInHubs()
                        .stream()
                        .map(p -> (Audience) p)
                        .collect(Collectors.toList());
                MessagingHelper.sendMessage(playersInHubs, MatchMessages.createMatchAdvertiseMessage(id, hostUsername, matchType.getName(), startTime));

                // Send a message to people in the match as well
                sendMessageToAllPlayers(MatchMessages.createCountdownMessage(startTime, players.size(), matchType.getMinPlayers()));
            }
        }, 0, 60, TimeUnit.SECONDS);

        // Schedule a task that runs 5 seconds before the match starts using scheduler
        long delayMillis = startTime.getTime() - System.currentTimeMillis() - 5000;
        if (delayMillis < 0) delayMillis = 0;
        scheduler.schedule(this::startPreMatchCountdown, delayMillis, TimeUnit.MILLISECONDS);

        // Setup events
        this.getInstance().eventNode().addListener(PlayerDisconnectEvent.class, event -> {
            // Handle player disconnect
            MatchManager.playerLeaveMatch(this, event.getPlayer());
        });

        this.getInstance().eventNode().addListener(EntityPreDeathEvent.class, event -> {
            // Handle player death
            event.setCancelled(true);
        });

        this.getInstance().eventNode().addListener(FinalAttackEvent.class, event -> {
            // If the match is not in progress, cancel the attack
            if (this.matchStatus != MatchStatus.IN_PROGRESS) {
                event.setCancelled(true);
            }

            // Find if the attack is fatal
            Entity target = event.getTarget();
            if (target instanceof Player player) {
                GamePlayerData playerData = this.players.stream()
                        .filter(p -> p.getUuid().equals(player.getUuid()))
                        .findFirst()
                        .orElse(null);
                if (playerData != null && playerData.isAlive()) {
                    double finalDamage = event.getBaseDamage() + event.getEnchantsExtraDamage();
                    if (finalDamage >= player.getHealth()) {
                        Entity killer = event.getEntity();
                        if (killer instanceof Player killerPlayer) {
                            killPlayer(playerData);

                            // This attack would be fatal, so trigger the elimination message (but the actual death will be handled in EntityPreDeathEvent)
                            if (getAlivePlayers().size() == 1) {
                                // Find the killer's player data
                                this.players.stream()
                                        .filter(p -> p.getUuid().equals(killerPlayer.getUuid()))
                                        .findFirst().ifPresent(this::winMatch);
                            } else {
                                sendMessageToAllPlayers(MatchMessages.createPlayerEliminatedMessage(player.getUsername(), killerPlayer.getUsername(), getAlivePlayers().size() - 1));
                            }

                            Component killerMessage = MatchMessages.createKillerMessage(player.getUsername());
                            killerPlayer.sendActionBar(killerMessage);

                            Sound killerSound = Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.AMBIENT, 1.0f, 1.0f);
                            killerPlayer.playSound(killerSound);
                        }
                    }
                }
            }
        });

        this.getInstance().eventNode().addListener(PlayerMoveEvent.class, event -> {
            // If the match is in countdown, prevent movement
            if (this.matchStatus == MatchStatus.COUNTDOWN) {
                // TODO: fix this (it still allows movement)
                event.setCancelled(true);
            }

            // If the player is below Y=0 and are alive while the match is in progress, kill them
            if (this.matchStatus == MatchStatus.IN_PROGRESS) {
                Player player = event.getPlayer();
                GamePlayerData playerData = this.players.stream()
                        .filter(p -> p.getUuid().equals(player.getUuid()))
                        .findFirst()
                        .orElse(null);
                if (playerData != null && playerData.isAlive()) {
                    if (player.getPosition().y() < 0) {
                        killPlayer(playerData);

                        sendMessageToAllPlayers(MatchMessages.createPlayerVoidMessage(player.getUsername(), getAlivePlayers().size() - 1));

                        if (getAlivePlayers().size() == 1) {
                            GamePlayerData winnerData = getAlivePlayers().iterator().next();
                            winMatch(winnerData);
                        }
                    }
                }
            }
        });

        this.getInstance().eventNode().addListener(InventoryCloseEvent.class, event -> {
            // Update the chest loot registry when a chest inventory is closed
            Pos blockPos = inventoryBlockRegistry.getPlayerInventoryBlockPosition(event.getPlayer().getUuid());
            ChestIdentifier chestId = new ChestIdentifier(this.getInstance().getBlock(blockPos), blockPos);
            chestLootRegistry.setLoot(chestId, (Inventory) event.getInventory());
        });

        this.getInstance().eventNode().addListener(PlayerBlockInteractEvent.class, event -> {
            if (matchStatus != MatchStatus.IN_PROGRESS) {
                event.setCancelled(true);
                return;
            }

            // Check if it's a chest
            Block block = event.getBlock();
            if (block.name().equals("minecraft:chest") || block.name().equals("minecraft:waxed_copper_chest") || block.name().equals("minecraft:ender_chest")) {
                // Attempt to find the inventory in the loot registry
                ChestIdentifier chestId = new ChestIdentifier(block, event.getBlockPosition().asPos());
                Inventory chestInventory = chestLootRegistry.getLoot(chestId);

                // Register the player's interaction with this block
                inventoryBlockRegistry.setPlayerInventoryBlockPosition(event.getPlayer().getUuid(), event.getBlockPosition().asPos());

                // Open the inventory for the player
                event.getPlayer().openInventory(chestInventory);
            }
        });

        this.getInstance().eventNode().addListener(PlayerBlockPlaceEvent.class, event -> {
            if (matchStatus != MatchStatus.IN_PROGRESS) {
                event.setCancelled(true);
            }

            // Figure out what block the player is looking at (before the block place)
            Point targetedPosition = event.getPlayer().getTargetBlockPosition(20);
            if (targetedPosition != null) {
                Block block = this.getInstance().getBlock(targetedPosition);
                if (block.name().equals("minecraft:chest") || block.name().equals("minecraft:waxed_copper_chest") || block.name().equals("minecraft:ender_chest")) {
                    event.setCancelled(true);
                }
            }

            // Check if it is within the arena type's bounds
            boolean inBounds = BoundsHelper.isInBounds(event.getBlockPosition().asPos(), matchType.getBuildingBounds1(), matchType.getBuildingBounds2());

            if (!inBounds) {
                event.setCancelled(true);
                MessagingHelper.sendMessage(event.getPlayer(), MessageType.PROTECT, "You cannot modify the arena outside of the bounds!");
            }
        });

        this.getInstance().eventNode().addListener(PlayerBlockBreakEvent.class, event -> {
            if (matchStatus != MatchStatus.IN_PROGRESS) {
                event.setCancelled(true);
            }

            // Check if it is within the arena type's bounds
            boolean inBounds = BoundsHelper.isInBounds(event.getBlockPosition().asPos(), matchType.getBuildingBounds1(), matchType.getBuildingBounds2());

            if (!inBounds) {
                event.setCancelled(true);
                MessagingHelper.sendMessage(event.getPlayer(), MessageType.PROTECT, "You cannot modify the arena outside of the bounds!");
            }
        });
    }

    public void startPreMatchCountdown() {
        // Use a plain thread for the 5-second countdown to avoid tying up the scheduler
        new Thread(() -> {
            int countdown = 5;
            while (countdown > 0) {
                Component message = MatchMessages.createCountdownMessage(startTime, players.size(), matchType.getMinPlayers());
                sendMessageToAllPlayers(message);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                countdown--;
            }

            startMatch();
        }, "game-pre-match-countdown-" + getId()).start();
    }

    public void startMatchCountdown() {
        setMatchStatus(MatchStatus.COUNTDOWN);

        // Use a plain thread for the 10-second countdown
        new Thread(() -> {
            int countdown = 10;
            while (countdown > 0) {
                Component actionbarMessage = Component.text("Get ready to go in ", NamedTextColor.YELLOW)
                        .append(Component.text(countdown + " seconds!", NamedTextColor.GOLD));
                sendActionbarToAllPlayers(actionbarMessage);
                sendSoundToAllPlayers(Sound.sound(Key.key("minecraft:block.note_block.bell"), Sound.Source.AMBIENT, 1.0f, 1.0f));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                countdown--;
            }

            // Final message
            Component goMessage = Component.text("GO!", NamedTextColor.GREEN).decorate(TextDecoration.BOLD);
            sendActionbarToAllPlayers(goMessage);
            sendSoundToAllPlayers(Sound.sound(Key.key("minecraft:entity.firework_rocket.launch"), Sound.Source.AMBIENT, 1.0f, 1.0f));

            teleportPlayersToStartingLocations();

            setMatchStatus(MatchStatus.IN_PROGRESS);
        }, "game-start-countdown-" + getId()).start();
    }

    public void teleportPlayersToStartingLocations() {
        int playerIndex = 0;
        for (GamePlayerData gamePlayerData : players) {
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(gamePlayerData.getUuid());
            if (player != null) {
                Pos spawnPos = matchType.getSpawnPointForPlayer(playerIndex, players.size());
                player.teleport(spawnPos);
                player.setGameMode(GameMode.SURVIVAL);
                playerIndex++;
            }
        }
    }

    public void startMatch() {
        if (getMatchStatus() == MatchStatus.WAITING) {
            if (hasEnoughPlayers()) {
                // Teleport all players
                teleportPlayersToStartingLocations();

                // Start the match countdown
                startMatchCountdown();
            } else {
                // Not enough players, cancel the match
                tooLittlePlayers();
            }
        }
    }

    public void tooLittlePlayers() {
        sendMessageToAllPlayers(MatchMessages.createMatchTooLittlePlayersMessage(matchType.getMinPlayers()));

        matchOver();
    }

    public void killPlayer(GamePlayerData playerData) {
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerData.getUuid());
        if (player != null && playerData.isAlive()) {
            Pos deathPos = player.getPosition();

            Entity lightning = new Entity(EntityType.LIGHTNING_BOLT);
            lightning.setInstance(this.getInstance(), deathPos);

            player.setVelocity(Vec.ZERO);

            playerData.setStatus(GamePlayerStatus.DEAD);
            player.setGameMode(GameMode.SPECTATOR);

            player.teleport(matchType.getSpectatorSpawn());
        }
    }

    public void winMatch(GamePlayerData playerData) {
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerData.getUuid());
        if (player != null && playerData.isAlive()) {
            setMatchStatus(MatchStatus.ENDED);

            Component winMessage = MatchMessages.createWinnerMessage(player.getUsername());
            sendMessageToAllPlayers(winMessage);

            // Play victory sound to the winner
            Sound victorySound = Sound.sound(Key.key("minecraft:item.totem.use"), Sound.Source.AMBIENT, 1.0f, 1.0f);
            player.playSound(victorySound);

            // After a short delay, pack up the match using the scheduler
            scheduler.schedule(this::matchOver, 5, TimeUnit.SECONDS);
        }
    }

    public void matchOver() {
        for (GamePlayerData gamePlayerData : players) {
            Player p = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(gamePlayerData.getUuid());
            if (p != null) {
                PlayerHubHelper.returnPlayerToHub(p);

                MessagingHelper.sendMessage(p, MessageType.SERVER, "You have been returned to the hub.");
            }
        }

        // De-register this instance
        GameInstanceRegistry.removeInstanceById(getId());

        // Shutdown the per-instance scheduler to avoid leak
        try {
            scheduler.shutdownNow();
        } catch (Exception ignored) {
        }
    }

    public void sendMessageToAllPlayers(Component message) {
        for (GamePlayerData player : players) {
            Objects.requireNonNull(MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(player.getUuid())).sendMessage(message);
        }
    }

    public void sendActionbarToAllPlayers(Component message) {
        for (GamePlayerData player : players) {
            Objects.requireNonNull(MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(player.getUuid()))
                    .sendActionBar(message);
        }
    }

    public void sendSoundToAllPlayers(Sound sound) {
        for (GamePlayerData player : players) {
            Objects.requireNonNull(MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(player.getUuid()))
                    .playSound(sound);
        }
    }

    public boolean isNotFull() {
        return players.size() < matchType.getMaxPlayers();
    }

    public boolean isFull() {
        return players.size() == matchType.getMaxPlayers();
    }

    public boolean hasEnoughPlayers() {
        return players.size() >= matchType.getMinPlayers();
    }

    public void addPlayer(GamePlayerData player) {
        players.add(player);

        // Check if we have enough players to start the match early
        if (isFull() && getMatchStatus() == MatchStatus.WAITING) {
            // Start the pre-match countdown immediately
            startPreMatchCountdown();
        }
    }

    public Collection<GamePlayerData> getAlivePlayers() {
        Collection<GamePlayerData> alivePlayers = new HashSet<>();
        for (GamePlayerData player : players) {
            if (player.isAlive()) {
                alivePlayers.add(player);
            }
        }
        return alivePlayers;
    }

    public Collection<GamePlayerData> getDeadPlayers() {
        Collection<GamePlayerData> deadPlayers = new HashSet<>();
        for (GamePlayerData player : players) {
            if (player.isDead()) {
                deadPlayers.add(player);
            }
        }
        return deadPlayers;
    }

    public Collection<GamePlayerData> getSpectators() {
        Collection<GamePlayerData> spectators = new HashSet<>();
        for (GamePlayerData player : players) {
            if (player.isSpectating()) {
                spectators.add(player);
            }
        }
        return spectators;
    }

}
