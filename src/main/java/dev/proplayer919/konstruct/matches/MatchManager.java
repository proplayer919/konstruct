package dev.proplayer919.konstruct.matches;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.bot.BotPlayer;
import dev.proplayer919.konstruct.bot.UsernameGenerator;
import dev.proplayer919.konstruct.hubs.HubData;
import dev.proplayer919.konstruct.hubs.HubRegistry;
import dev.proplayer919.konstruct.loot.ChestIdentifier;
import dev.proplayer919.konstruct.matches.events.MatchEndEvent;
import dev.proplayer919.konstruct.matches.events.MatchStartEvent;
import dev.proplayer919.konstruct.matches.modifiers.ThunderModifier;
import dev.proplayer919.konstruct.messages.MatchMessages;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.sidebar.SidebarData;
import dev.proplayer919.konstruct.sidebar.SidebarRegistry;
import dev.proplayer919.konstruct.util.BoundsHelper;
import dev.proplayer919.konstruct.util.ItemDropper;
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
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MatchManager {
    private static final Map<Block, Material> blockDrops = Map.of(
            Block.GRASS_BLOCK, Material.DIRT,
            Block.DIRT, Material.DIRT,
            Block.STONE, Material.COBBLESTONE,
            Block.OAK_LOG, Material.OAK_LOG,
            Block.OAK_PLANKS, Material.OAK_PLANKS,
            Block.COBBLESTONE, Material.COBBLESTONE
    );

    public static void setupMatch(MatchData matchData) {
        // Setup schedules
        matchData.getScheduler().scheduleAtFixedRate(() -> {
            // Don't advertise in the minute where the match starts
            long millisUntilStart = matchData.getStartTime().getTime() - System.currentTimeMillis();
            if (matchData.getStatus() == MatchStatus.WAITING && !matchData.isFull()
                    && millisUntilStart >= 60000) {
                Collection<CustomPlayer> playersInHubs = HubRegistry.getAllPlayersInHubs();
                Collection<MatchPlayer> matchPlayersInHubs = new ArrayList<>(playersInHubs);
                MessagingHelper.sendMessage(matchPlayersInHubs, MatchMessages.createMatchAdvertiseMessage(matchData.getMatchUUID(), matchData.getHostUsername(), matchData.getStartTime()));

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
        matchData.getMatchInstance().eventNode().addListener(PlayerMoveEvent.class, event -> {
            // If the match is in countdown, prevent movement
            if (matchData.getStatus() == MatchStatus.COUNTDOWN) {
                // If the event didn't change position, don't cancel
                if (!event.getNewPosition().samePoint(event.getPlayer().getPosition())) {
                    event.setCancelled(true);
                }
                return;
            }

            // If the player is frozen, prevent movement
            CustomPlayer player = (CustomPlayer) event.getPlayer();
            if (player.isFrozen()) {
                // If the event didn't change position, don't cancel
                if (!event.getNewPosition().samePoint(player.getPosition())) {
                    event.setCancelled(true);
                }
                return;
            }

            // If the player is below Y=0 and are alive while the match is in progress, kill them
            if (player.getPosition().y() < 0) {
                if (matchData.getStatus() == MatchStatus.IN_PROGRESS && player.isAlive()) {
                    killPlayer(matchData, player);

                    MessagingHelper.sendMessage(matchData.getPlayers(), MatchMessages.createPlayerVoidMessage(player.getUsername(), matchData.getAlivePlayerCount() - 1));

                    if (matchData.getAlivePlayerCount() == 1) {
                        winMatch(matchData, matchData.getAlivePlayers().iterator().next());
                    }
                } else {
                    // Teleport the player back to the spectator spawn
                    player.teleport(matchData.getLobbySpawn());
                }
            }
        });

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

            if (event.getEntity() instanceof MatchPlayer player) {
                if (matchData.getPlayerAttackers().containsKey(player)) {
                    MatchPlayer killer = matchData.getPlayerAttackers().get(player);

                    killPlayer(matchData, player);

                    MessagingHelper.sendMessage(matchData.getPlayers(), MatchMessages.createPlayerEliminatedMessage(player.getUsername(), killer.getUsername(), matchData.getAlivePlayerCount()));

                    if (matchData.getAlivePlayerCount() == 1) {
                        winMatch(matchData, matchData.getAlivePlayers().iterator().next());
                    }

                    Component killerMessage = MatchMessages.createKillerMessage(player.getUsername());
                    killer.sendActionBar(killerMessage);

                    Sound killerSound = Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.AMBIENT, 1.0f, 1.0f);
                    killer.playSound(killerSound);
                }
            }
        });

        matchData.getMatchInstance().eventNode().addListener(FinalAttackEvent.class, event -> {
            // If the match is not in progress, cancel the attack
            if (matchData.getStatus() != MatchStatus.IN_PROGRESS) {
                event.setCancelled(true);
            }

            // Find if the attack is fatal
            Entity target = event.getTarget();
            if (target instanceof MatchPlayer player) {
                Entity attacker = event.getEntity();
                if (attacker instanceof MatchPlayer attackerPlayer) {
                    matchData.getPlayerAttackers().put(player, attackerPlayer);
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
                event.setBlockingItemUse(true);

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
                return;
            }

            // Check if it is within the arena type's bounds
            boolean inBounds = BoundsHelper.isInBounds(event.getBlockPosition().asPos(), matchData.getBuildingBounds1(), matchData.getBuildingBounds2());

            if (!inBounds) {
                event.setCancelled(true);
                MessagingHelper.sendMessage(event.getPlayer(), MessageType.PROTECT, "You cannot modify the arena outside of the bounds!");
                return;
            }

            Block block = event.getBlock();
            if (!blockDrops.containsKey(block)) {
                // Drop the block as an item
                ItemStack droppedItem = ItemStack.of(blockDrops.get(block));
                ItemDropper.dropItem(droppedItem, matchData.getMatchInstance(), event.getBlockPosition().asPos());
            }
        });

        matchData.getMatchInstance().eventNode().addListener(ItemDropEvent.class, event -> {
            ItemDropper.dropItemFromPlayer(event.getItemStack(), (CustomPlayer) event.getPlayer(), false);
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

        matchData.addModifier(new ThunderModifier());
    }

    public static void startPreMatchCountdown(MatchData matchData) {
        if (matchData.getStatus() != MatchStatus.WAITING) {
            return;
        }

        // Use a plain thread for the 5-second countdown to avoid tying up the scheduler
        new Thread(() -> {
            matchData.setStatus(MatchStatus.PRE_COUNTDOWN);

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
        for (MatchPlayer player : matchData.getPlayers()) {
            Pos spawnPos = PlayerSpawnHelper.getSpawnPointForPlayer(playerIndex, matchData.getPlayerCount());
            player.setInstance(matchData.getMatchInstance());
            player.teleport(spawnPos);
            player.setGameMode(GameMode.SURVIVAL);
            player.setPlayerStatus(PlayerStatus.ALIVE);
            playerIndex++;
        }
    }

    public static void startMatch(MatchData matchData) {
        if (matchData.getStatus() == MatchStatus.PRE_COUNTDOWN) {
            // Spawn up to 4 extra bots if the match isn't full
            int botsToSpawn = matchData.getMaxPlayers() - matchData.getPlayers().size();
            for (int i = 0; i < botsToSpawn && i < 4; i++) {
                Collection<String> existingUsernames = new ArrayList<>();
                for (MatchPlayer matchPlayer : matchData.getPlayers()) {
                    existingUsernames.add(matchPlayer.getUsername());
                }

                String username = UsernameGenerator.generateUniqueUsername(existingUsernames);
                PlayerSkin skin = new PlayerSkin("ewogICJ0aW1lc3RhbXAiIDogMTY0MDI2OTY5OTg0OSwKICAicHJvZmlsZUlkIiA6ICIwMmIwZTg2ZGM4NmE0YWU3YmM0MTAxNWQyMWY4MGMxYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJab21iaWUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzgzYWFhZWUyMjg2OGNhZmRhYTFmNmY0YTBlNTZiMGZkYjY0Y2QwYWVhYWJkNmU4MzgxOGMzMTJlYmU2NjQzNyIKICAgIH0KICB9Cn0=", "DKIiqztp2XXi973AJeJ5jSGaLVIFAM+XyQGhRwmYOSlEo2Scc2YcaKi6gQCAmTtNeWlnf9wagZ8sezJzePANn0Yi3xMETd5OojATXKamNoQB7VsRRhXNl47WmOz5/DpZPk5yxVIPWo6jJCb7RwDkX/CIaYJPErA0tQOB8UyR2g37oZYgkHLqj80080scReh4KiZYs3ymfF/5vRUdkyBbaiVpeB87V4t4HFscoZt8iJlaa3fD8ZR0wbkMe7VGC5iafrXTGBbBMDlBYBkRtuR4Mqg2IRZpXFIh3FlNitW8x3hUsRHPDPBSGLgErjOnFtVafytt3Q2t3zc0jCmL8/wGzlppghVzK0IrAoAHCL7FCe1uGwFf8lRgTk7Vq2ZLFg0qxZN4dbO51vj7MT+MIUkP+7Zs2k2yxlmFdMGQiIsF37HVjfc6QdBVAfAFr2+1PcJ0ffRkgUTjyqL0UJv5qPqEJ9MBXhKwn4JlPigljvQIIYj/JxIWjJT1EgBKuv4M3g59jQL0vB4K9jeasI8vvXGAvTJFqa7KkumXKUoiZwSU4mVYrzxYlvQ2Ku14Q3pLl3BTsoeRkZq0YWabt+xHjMdK5srJZcV9AuGeIALMMxWGQA5riNAHQ5ZFpDq5vTYwfZn/+DsyG3MB5ftNTb2Dnsf5zbzpACW6uAbu/5csdKlrZMU=");
                BotPlayer botPlayer = new BotPlayer(UUID.randomUUID(), username, skin, matchData.getPlayers().size() + 1, matchData);
                MatchManager.spawnPlayerIntoMatch(matchData, botPlayer);
            }

            if (matchData.hasEnoughPlayers()) {
                // Call the match start event
                MatchStartEvent startEvent = new MatchStartEvent(matchData);
                EventDispatcher.call(startEvent);

                // Show bots to all players
                showBotsToPlayers(matchData);

                // Teleport all players
                teleportPlayersToStartingLocations(matchData);

                // Start the match countdown
                startMatchCountdown(matchData);

                // Send the MOTD message to all players
                Component motdMessage = MatchMessages.createMOTDMessage(matchData.getHostUsername(), "Meadows", matchData.getPlayerCount(), matchData.getMaxPlayers(), matchData.getActiveModifiers());
                MessagingHelper.sendMessage(matchData.getPlayers(), motdMessage);
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

    public static void killPlayer(MatchData matchData, MatchPlayer player) {
        if (player.isAlive()) {
            if (player instanceof CustomPlayer customPlayer) {
                // Drop items from the player's inventory
                for (ItemStack itemStack : customPlayer.getInventory().getItemStacks()) {
                    if (itemStack.material() != Material.AIR) {
                        ItemDropper.dropItemFromPlayer(itemStack, customPlayer, true);
                    }
                }
            }

            // Death effects
            Pos deathPos = player.getPosition();

            Entity lightning = new Entity(EntityType.LIGHTNING_BOLT);
            lightning.setInstance(matchData.getMatchInstance(), deathPos);

            // Make the player a spectator
            player.setVelocity(Vec.ZERO);

            player.setPlayerStatus(PlayerStatus.SPECTATOR);
            player.setGameMode(GameMode.SPECTATOR);

            player.teleport(matchData.getSpectatorSpawn());

            if (player instanceof BotPlayer botPlayer) {
                removeBotFromPlayers(matchData, botPlayer);
                botPlayer.remove();
            }
        }
    }

    public static void winMatch(MatchData matchData, MatchPlayer player) {
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
        // Call the match end event
        MatchEndEvent endEvent = new MatchEndEvent(matchData);
        EventDispatcher.call(endEvent);

        for (MatchPlayer matchPlayer : matchData.getPlayers()) {
            if (matchPlayer instanceof CustomPlayer player) {
                PlayerHubHelper.returnPlayerToHub(player);

                MessagingHelper.sendMessage(player, MessageType.SERVER, "You have been returned to the hub.");
            }
        }

        removeBotsFromPlayers(matchData);

        // De-register this instance
        MatchesRegistry.unregisterMatch(matchData.getMatchUUID());

        // Shutdown the per-instance scheduler to avoid leak
        try {
            matchData.getScheduler().shutdownNow();
        } catch (Exception ignored) {
        }
    }

    public static void spawnPlayerIntoMatch(MatchData matchData, MatchPlayer player) {
        HubData hubData = HubRegistry.getInstanceWithPlayer(player.getUuid());
        if (hubData != null) {
            hubData.getPlayers().remove(player);
        }

        matchData.addPlayer(player);

        Instance lobbyInstance = matchData.getLobbyInstance();

        player.setInstance(lobbyInstance, matchData.getLobbySpawn());
        player.setEnableRespawnScreen(false);
        player.setRespawnPoint(matchData.getSpectatorSpawn());
        player.setGameMode(GameMode.ADVENTURE);

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

    public static void showBotsToPlayers(MatchData matchData) {
        Collection<BotPlayer> botPlayers = new ArrayList<>();
        Collection<CustomPlayer> players = new ArrayList<>();
        for (MatchPlayer matchPlayer : matchData.getPlayers()) {
            if (matchPlayer instanceof BotPlayer botPlayer) {
                botPlayers.add(botPlayer);
            } else if (matchPlayer instanceof CustomPlayer customPlayer) {
                players.add(customPlayer);
            }
        }

        for (CustomPlayer player : players) {
            for (BotPlayer botPlayer : botPlayers) {
                botPlayer.addPlayerViewer(player);
            }
        }
    }

    public static void removeBotsFromPlayers(MatchData matchData) {
        Collection<BotPlayer> botPlayers = new ArrayList<>();
        Collection<CustomPlayer> players = new ArrayList<>();
        for (MatchPlayer matchPlayer : matchData.getPlayers()) {
            if (matchPlayer instanceof BotPlayer botPlayer) {
                botPlayers.add(botPlayer);
            } else if (matchPlayer instanceof CustomPlayer customPlayer) {
                players.add(customPlayer);
            }
        }

        for (CustomPlayer player : players) {
            for (BotPlayer botPlayer : botPlayers) {
                botPlayer.removePlayerViewer(player);
            }
        }
    }

    public static void removeBotFromPlayers(MatchData matchData, BotPlayer botToRemove) {
        Collection<CustomPlayer> players = new ArrayList<>();
        for (MatchPlayer matchPlayer : matchData.getPlayers()) {
            if (matchPlayer instanceof CustomPlayer customPlayer) {
                players.add(customPlayer);
            }
        }

        for (CustomPlayer player : players) {
            botToRemove.removePlayerViewer(player);
        }
    }
}
