package dev.proplayer919.konstruct.matches;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.hubs.HubData;
import dev.proplayer919.konstruct.hubs.HubRegistry;
import dev.proplayer919.konstruct.loot.ChestIdentifier;
import dev.proplayer919.konstruct.messages.MatchMessages;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.sidebar.SidebarData;
import dev.proplayer919.konstruct.sidebar.SidebarRegistry;
import dev.proplayer919.konstruct.util.BoundsHelper;
import dev.proplayer919.konstruct.util.PlayerHubHelper;
import dev.proplayer919.konstruct.util.PlayerSpawnHelper;
import io.github.togar2.pvp.events.EntityPreDeathEvent;
import io.github.togar2.pvp.events.FinalAttackEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.Inventory;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class MatchManager {
    public static void setupMatch(MatchData matchData) {
        // Setup schedules
        matchData.getScheduler().scheduleAtFixedRate(() -> {
            // Don't advertise in the minute where the match starts
            long millisUntilStart = matchData.getStartTime().getTime() - System.currentTimeMillis();
            if (matchData.getStatus() == MatchStatus.WAITING && !matchData.isFull()
                    && millisUntilStart >= 60000) {
                Collection<CustomPlayer> playersInHubs = HubRegistry.getAllPlayersInHubs();
                MessagingHelper.sendMessage(playersInHubs, MatchMessages.createMatchAdvertiseMessage(matchData.getMatchUUID(), matchData.getHostUsername(), matchData.getStartTime()));

                // Send a message to people in the match as well
                MessagingHelper.sendMessage(matchData.getPlayers(), MatchMessages.createCountdownMessage(matchData.getStartTime(), matchData.getPlayers().size(), matchData.getMinPlayers()));
            }
        }, 0, 60, TimeUnit.SECONDS);

        // Schedule a task that runs 5 seconds before the match starts using scheduler
        long delayMillis = matchData.getStartTime().getTime() - System.currentTimeMillis() - 5000;
        if (delayMillis < 0) delayMillis = 0;
        matchData.getScheduler().schedule(() -> {
            startPreMatchCountdown(matchData);
        }, delayMillis, TimeUnit.MILLISECONDS);

        // Setup events
        matchData.getLobbyInstance().eventNode().addListener(PlayerDisconnectEvent.class, event -> {
            // Handle player disconnect
            playerLeaveMatch(matchData, (CustomPlayer) event.getPlayer());
        });

        matchData.getMatchInstance().eventNode().addListener(PlayerDisconnectEvent.class, event -> {
            // Handle player disconnect
            playerLeaveMatch(matchData, (CustomPlayer) event.getPlayer());
        });

        matchData.getLobbyInstance().eventNode().addListener(EntityPreDeathEvent.class, event -> {
            // Handle player death
            event.setCancelled(true);
        });

        matchData.getMatchInstance().eventNode().addListener(EntityPreDeathEvent.class, event -> {
            // Handle player death
            event.setCancelled(true);
        });

        matchData.getMatchInstance().eventNode().addListener(FinalAttackEvent.class, event -> {
            // If the match is not in progress, cancel the attack
            if (matchData.getStatus() != MatchStatus.IN_PROGRESS) {
                event.setCancelled(true);
            }

            // Find if the attack is fatal
            Entity target = event.getTarget();
            if (target instanceof CustomPlayer player) {
                if (player.isAlive()) {
                    double finalDamage = event.getBaseDamage() + event.getEnchantsExtraDamage();
                    if (finalDamage >= player.getHealth()) {
                        Entity killer = event.getEntity();
                        if (killer instanceof CustomPlayer killerPlayer) {
                            killPlayer(matchData, player);

                            if (matchData.getAlivePlayerCount() == 1) {
                                winMatch(matchData, matchData.getAlivePlayers().iterator().next());
                            } else {
                                MessagingHelper.sendMessage(matchData.getPlayers(), MatchMessages.createPlayerEliminatedMessage(player.getUsername(), killerPlayer.getUsername(), matchData.getAlivePlayerCount() - 1));
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

        matchData.getMatchInstance().eventNode().addListener(PlayerMoveEvent.class, event -> {
            // If the match is in countdown, prevent movement
            if (matchData.getStatus() == MatchStatus.COUNTDOWN) {
                event.setCancelled(true);
            }

            // If the player is below Y=0 and are alive while the match is in progress, kill them
            if (matchData.getStatus() == MatchStatus.IN_PROGRESS) {
                CustomPlayer player = (CustomPlayer) event.getPlayer();
                if (player.isAlive()) {
                    if (player.getPosition().y() < 0) {
                        killPlayer(matchData, player);

                        MessagingHelper.sendMessage(matchData.getPlayers(), MatchMessages.createPlayerVoidMessage(player.getUsername(), matchData.getAlivePlayerCount() - 1));

                        if (matchData.getAlivePlayerCount() == 1) {
                            winMatch(matchData, matchData.getAlivePlayers().iterator().next());
                        }
                    }
                }
            }
        });

        matchData.getMatchInstance().eventNode().addListener(InventoryCloseEvent.class, event -> {
            // Update the chest loot registry when a chest inventory is closed
            Pos blockPos = matchData.getInventoryBlockRegistry().getPlayerInventoryBlockPosition(event.getPlayer().getUuid());
            ChestIdentifier chestId = new ChestIdentifier(matchData.getMatchInstance().getBlock(blockPos), blockPos);
            matchData.getChestLootRegistry().setLoot(chestId, (Inventory) event.getInventory());
        });

        matchData.getMatchInstance().eventNode().addListener(PlayerBlockInteractEvent.class, event -> {
            if (matchData.getStatus() != MatchStatus.IN_PROGRESS) {
                event.setCancelled(true);
                return;
            }

            // Check if it's a chest
            Block block = event.getBlock();
            if (block.name().equals("minecraft:chest") || block.name().equals("minecraft:waxed_copper_chest") || block.name().equals("minecraft:ender_chest")) {
                // Attempt to find the inventory in the loot registry
                ChestIdentifier chestId = new ChestIdentifier(block, event.getBlockPosition().asPos());
                Inventory chestInventory = matchData.getChestLootRegistry().getLoot(chestId);

                // Register the player's interaction with this block
                matchData.getInventoryBlockRegistry().setPlayerInventoryBlockPosition(event.getPlayer().getUuid(), event.getBlockPosition().asPos());

                // Open the inventory for the player
                event.getPlayer().openInventory(chestInventory);
            }
        });

        matchData.getMatchInstance().eventNode().addListener(PlayerBlockPlaceEvent.class, event -> {
            if (matchData.getStatus() != MatchStatus.IN_PROGRESS) {
                event.setCancelled(true);
            }

            // Figure out what block the player is looking at (before the block place)
            Point targetedPosition = event.getPlayer().getTargetBlockPosition(20);
            if (targetedPosition != null) {
                Block block = matchData.getMatchInstance().getBlock(targetedPosition);
                if (block.name().equals("minecraft:chest") || block.name().equals("minecraft:waxed_copper_chest") || block.name().equals("minecraft:ender_chest")) {
                    event.setCancelled(true);
                }
            }

            // Check if it is within the arena type's bounds
            boolean inBounds = BoundsHelper.isInBounds(event.getBlockPosition().asPos(), matchData.getBuildingBounds1(), matchData.getBuildingBounds2());

            if (!inBounds) {
                event.setCancelled(true);
                MessagingHelper.sendMessage(event.getPlayer(), MessageType.PROTECT, "You cannot modify the arena outside of the bounds!");
            }
        });

        matchData.getMatchInstance().eventNode().addListener(PlayerBlockBreakEvent.class, event -> {
            if (matchData.getStatus() != MatchStatus.IN_PROGRESS) {
                event.setCancelled(true);
            }

            // Check if it is within the arena type's bounds
            boolean inBounds = BoundsHelper.isInBounds(event.getBlockPosition().asPos(), matchData.getBuildingBounds1(), matchData.getBuildingBounds2());

            if (!inBounds) {
                event.setCancelled(true);
                MessagingHelper.sendMessage(event.getPlayer(), MessageType.PROTECT, "You cannot modify the arena outside of the bounds!");
            }
        });

        matchData.getMatchInstance().eventNode().addListener(ItemDropEvent.class, event -> {
            ItemEntity itemEntity = new ItemEntity(event.getItemStack());
            itemEntity.setPickupDelay(300, ChronoUnit.MILLIS);
            itemEntity.setVelocity(event.getPlayer().getPosition().direction().mul(0.5));
            itemEntity.setInstance(event.getPlayer().getInstance(), event.getPlayer().getPosition().add(0, 0.5, 0));
        });

        matchData.getMatchInstance().eventNode().addListener(PickupItemEvent.class, event -> {
            if (event.getEntity() instanceof CustomPlayer player) {
                // Prevent picking up items if the player is not alive
                if (!player.isAlive()) {
                    event.setCancelled(true);
                    return;
                }

                player.getInventory().addItemStack(event.getItemStack());
            }
        });
    }

    public static void startPreMatchCountdown(MatchData matchData) {
        // Use a plain thread for the 5-second countdown to avoid tying up the scheduler
        new Thread(() -> {
            int countdown = 5;
            while (countdown > 0) {
                Component message = MatchMessages.createPreMatchCountdownMessage(countdown);
                MessagingHelper.sendMessage(matchData.getPlayers(), message);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                countdown--;
            }

            startMatch(matchData);
        }, "game-pre-match-countdown-" + matchData.getMatchUUID()).start();
    }

    public static void startMatchCountdown(MatchData matchData) {
        matchData.setStatus(MatchStatus.COUNTDOWN);

        // Use a plain thread for the 10-second countdown to avoid tying up the scheduler
        new Thread(() -> {
            int countdown = 10;
            while (countdown > 0) {
                Component actionbarMessage = Component.text("Get ready to go in ", NamedTextColor.YELLOW)
                        .append(Component.text(countdown + " seconds!", NamedTextColor.GOLD));
                MessagingHelper.sendActionBar(matchData.getPlayers(), actionbarMessage);
                MessagingHelper.sendSound(matchData.getPlayers(), Sound.sound(Key.key("minecraft:block.note_block.bell"), Sound.Source.AMBIENT, 1.0f, 1.0f));

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
            MessagingHelper.sendActionBar(matchData.getPlayers(), goMessage);
            MessagingHelper.sendSound(matchData.getPlayers(), Sound.sound(Key.key("minecraft:block.note_block.pling"), Sound.Source.AMBIENT, 1.0f, 1.0f));

            matchData.setStatus(MatchStatus.IN_PROGRESS);
        }, "game-start-countdown-" + matchData.getMatchUUID()).start();
    }

    public static void teleportPlayersToStartingLocations(MatchData matchData) {
        int playerIndex = 0;
        for (CustomPlayer player : matchData.getPlayers()) {
            Pos spawnPos = PlayerSpawnHelper.getSpawnPointForPlayer(playerIndex, matchData.getPlayerCount());
            player.setInstance(matchData.getMatchInstance());
            player.teleport(spawnPos);
            player.setGameMode(GameMode.SURVIVAL);
            player.setPlayerStatus(PlayerStatus.ALIVE);
            playerIndex++;
        }
    }

    public static void startMatch(MatchData matchData) {
        if (matchData.getStatus() == MatchStatus.WAITING) {
            if (matchData.hasEnoughPlayers()) {
                // Teleport all players
                teleportPlayersToStartingLocations(matchData);

                // Start the match countdown
                startMatchCountdown(matchData);
            } else {
                // Not enough players, cancel the match
                tooLittlePlayers(matchData);
            }
        }
    }

    public static void tooLittlePlayers(MatchData matchData) {
        MessagingHelper.sendMessage(matchData.getPlayers(), MatchMessages.createMatchTooLittlePlayersMessage(matchData.getMinPlayers()));

        matchOver(matchData);
    }

    public static void killPlayer(MatchData matchData, CustomPlayer player) {
        if (player.isAlive()) {
            Pos deathPos = player.getPosition();

            Entity lightning = new Entity(EntityType.LIGHTNING_BOLT);
            lightning.setInstance(matchData.getMatchInstance(), deathPos);

            player.setVelocity(Vec.ZERO);

            player.setPlayerStatus(PlayerStatus.SPECTATOR);
            player.setGameMode(GameMode.SPECTATOR);

            player.teleport(matchData.getSpectatorSpawn());
        }
    }

    public static void winMatch(MatchData matchData, CustomPlayer player) {
        if (player.isAlive()) {
            matchData.setStatus(MatchStatus.ENDED);

            Component winMessage = MatchMessages.createWinnerMessage(player.getUsername());
            MessagingHelper.sendMessage(matchData.getPlayers(), winMessage);

            // Create a title bar
            Component title = Component.text("VICTORY", NamedTextColor.GREEN).decorate(TextDecoration.BOLD);
            player.sendTitlePart(TitlePart.TITLE, title);

            // Play victory sound to the winner
            Sound victorySound = Sound.sound(Key.key("minecraft:item.totem.use"), Sound.Source.AMBIENT, 1.0f, 1.0f);
            player.playSound(victorySound);

            // After a short delay, pack up the match using the scheduler
            matchData.getScheduler().schedule(() -> matchOver(matchData), 5, TimeUnit.SECONDS);
        }
    }

    public static void matchOver(MatchData matchData) {
        for (CustomPlayer player : matchData.getPlayers()) {
            PlayerHubHelper.returnPlayerToHub(player);

            MessagingHelper.sendMessage(player, MessageType.SERVER, "You have been returned to the hub.");
        }

        // De-register this instance
        MatchesRegistry.unregisterMatch(matchData.getMatchUUID());

        // Shutdown the per-instance scheduler to avoid leak
        try {
            matchData.getScheduler().shutdownNow();
        } catch (Exception ignored) {
        }
    }

    public static void spawnPlayerIntoMatch(MatchData matchData, CustomPlayer player) {
        HubData hubData = HubRegistry.getInstanceWithPlayer(player.getUuid());
        if (hubData != null) {
            hubData.getPlayers().remove(player);
        } else {
            throw new IllegalStateException("Player is not in any hub instance");
        }

        matchData.addPlayer(player);

        Instance lobbyInstance = matchData.getLobbyInstance();

        player.setInstance(lobbyInstance);
        player.setEnableRespawnScreen(false);
        player.setRespawnPoint(matchData.getSpectatorSpawn());
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(matchData.getLobbySpawn());
        PlayerHubHelper.resetPlayerAttributes(player);

        // Update sidebar
        SidebarData sidebarData = SidebarRegistry.getSidebarByPlayerId(player.getUuid());
        if (sidebarData != null) {
            sidebarData.setInstanceId("In Match");
        }

        // Send a message to all players in the match that a new player has joined
        MessagingHelper.sendMessage(matchData.getPlayers(), MatchMessages.createPlayerJoinedMessage(player.getUsername(), matchData.getPlayers().size(), matchData.getMaxPlayers()));

        // If the match is full after this player joined, start the pre-match countdown
        if (matchData.isFull() && matchData.getStatus() == MatchStatus.WAITING) {
            startPreMatchCountdown(matchData);
        }
    }

    public static void playerLeaveMatch(MatchData matchData, CustomPlayer player) {
        if (!player.isAlive()) {
            return;
        }

        switch (matchData.getStatus()) {
            case WAITING -> {
                matchData.getPlayers().remove(player);
                MessagingHelper.sendMessage(matchData.getPlayers(), MatchMessages.createPlayerLeftMessage(player.getUsername(), matchData.getPlayers().size(), matchData.getMaxPlayers()));
            }
            case IN_PROGRESS -> {
                player.setPlayerStatus(PlayerStatus.SPECTATOR);
                MessagingHelper.sendMessage(matchData.getPlayers(), MatchMessages.createPlayerDisconnectMessage(player.getUsername(), matchData.getAlivePlayerCount()));

                if (matchData.getAlivePlayers().size() == 1) {
                    winMatch(matchData, matchData.getAlivePlayers().iterator().next());
                }
            }
        }
    }
}
