package dev.proplayer919.construkt;

import dev.proplayer919.construkt.commands.GameModeCommand;
import dev.proplayer919.construkt.commands.HubCommand;
import dev.proplayer919.construkt.commands.PermissionCommand;
import dev.proplayer919.construkt.sidebar.SidebarData;
import dev.proplayer919.construkt.instance.HubInstanceData;
import dev.proplayer919.construkt.instance.HubInstanceRegistry;
import dev.proplayer919.construkt.permissions.Permission;
import dev.proplayer919.construkt.permissions.PermissionRegistry;
import dev.proplayer919.construkt.generators.InstanceCreator;
import dev.proplayer919.construkt.sidebar.SidebarRegistry;
import io.github.togar2.pvp.MinestomPvP;
import io.github.togar2.pvp.feature.CombatFeatureSet;
import io.github.togar2.pvp.feature.CombatFeatures;
import net.bridgesplash.sidebar.SidebarAPI;
import net.mangolise.anticheat.MangoAC;
import net.mangolise.anticheat.events.PlayerFlagEvent;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.coordinate.Pos;
import dev.proplayer919.construkt.commands.GiveCommand;
import dev.proplayer919.construkt.helpers.MessagingHelper;
import dev.proplayer919.construkt.storage.SqliteDatabase;
import net.minestom.server.command.ConsoleSender;
import org.jetbrains.annotations.NotNull;
import org.everbuild.blocksandstuff.blocks.BlockBehaviorRuleRegistrations;
import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static CombatFeatureSet modernVanilla = CombatFeatures.modernVanilla();

    static void main(String[] args) {
        // Initialization
        MinecraftServer minecraftServer = MinecraftServer.init(new Auth.Online());

        MinestomPvP.init();

        BlockPlacementRuleRegistrations.registerDefault();
        BlockBehaviorRuleRegistrations.registerDefault();

        // Create permissions
        Permission buildPermission = new Permission("server.build");
        Permission breakPermission = new Permission("server.break");
        Permission giveCommandPermission = new Permission("command.give");
        Permission gamemodeCommandPermission = new Permission("command.gamemode");
        Permission permissionCommandPermission = new Permission("command.permission");
        Permission hostCommandPermission = new Permission("command.host");

        // Register permissions
        PermissionRegistry.registerPermission(buildPermission);
        PermissionRegistry.registerPermission(breakPermission);
        PermissionRegistry.registerPermission(giveCommandPermission);
        PermissionRegistry.registerPermission(gamemodeCommandPermission);
        PermissionRegistry.registerPermission(permissionCommandPermission);
        PermissionRegistry.registerPermission(hostCommandPermission);

        // Initialize SQLite database for persistent data
        SqliteDatabase sqliteDb = new SqliteDatabase(Path.of("data", "construkt-data.db"));
        try {
            sqliteDb.connect();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Ensure DB is closed on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sqliteDb.close();
        }, "Sqlite-Shutdown-Close"));

        // Create hub instances
        int hubs = 2; // Number of hub instances to create
        for (int i = 0; i < hubs; i++) {
            InstanceContainer hubInstance = InstanceCreator.createSimpleInstanceContainer(Block.GRASS_BLOCK, Block.GOLD_BLOCK, false);
            HubInstanceData hubData = new HubInstanceData(hubInstance, "hub-" + (i + 1));
            HubInstanceRegistry.registerInstance(hubData);
        }

        // Init commands
        GiveCommand giveCommand = new GiveCommand();
        GameModeCommand gameModeCommand = new GameModeCommand();
        PermissionCommand permissionCommand = new PermissionCommand();
        HubCommand hubCommand = new HubCommand();

        MinecraftServer.getCommandManager().register(giveCommand);
        MinecraftServer.getCommandManager().register(gameModeCommand);
        MinecraftServer.getCommandManager().register(permissionCommand);
        MinecraftServer.getCommandManager().register(hubCommand);

        // Player spawning
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            // Pick a hub instance with the least players
            HubInstanceData hubInstanceData = HubInstanceRegistry.getInstanceWithLowestPlayers();
            Instance hubInstance = hubInstanceData.getInstance();
            event.setSpawningInstance(hubInstance);
            player.setRespawnPoint(new Pos(0.5, 40, 0.5));
            player.setGameMode(GameMode.SURVIVAL);
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();

            // Find the instance the player is in
            Instance playerInstance = player.getInstance();
            String playerInstanceId = "unknown";
            if (HubInstanceRegistry.getInstanceByInstance(playerInstance) != null) {
                playerInstanceId = HubInstanceRegistry.getInstanceByInstance(playerInstance).getId();
            }

            // Setup sidebar for the player
            SidebarData sidebarData = new SidebarData(player.getUuid());
            sidebarData.setInstanceId(playerInstanceId);

            SidebarRegistry.registerSidebar(sidebarData);

            SidebarAPI.getSidebarManager().addSidebar(player, sidebarData.getSidebar());
        });

        globalEventHandler.addListener(PlayerBlockBreakEvent.class, event -> {
            final Player player = event.getPlayer();

            Instance playerInstance = player.getInstance();
            if (playerInstance == null) {
                return;
            }

            if (HubInstanceRegistry.getInstanceWithPlayer(player.getUuid()) != null) {
                MessagingHelper.sendProtectMessage(player, "You cannot break blocks in a hub");
                event.setCancelled(true);
            }
        });

        globalEventHandler.addListener(PlayerBlockPlaceEvent.class, event -> {
            final Player player = event.getPlayer();

            Instance playerInstance = player.getInstance();
            if (playerInstance == null) {
                return;
            }

            if (HubInstanceRegistry.getInstanceWithPlayer(player.getUuid()) != null) {
                MessagingHelper.sendProtectMessage(player, "You cannot place blocks in a hub");
                event.setCancelled(true);
            }
        });

        globalEventHandler.addListener(PlayerFlagEvent.class, event -> {
            final Player player = event.player();

            MessagingHelper.sendAnticheatMessage(player, "You have been flagged for " + event.checkName() + " (Certainty: " + (event.certainty() * 100) + "%)");
        });

        // Setup anticheat
        MangoAC.Config config = new MangoAC.Config();
        MangoAC ac = new MangoAC(config);
        ac.start();

        // Start the server on port 25565
        minecraftServer.start("0.0.0.0", 25565);

        // Start a console input thread that executes typed commands as the console sender
        Thread consoleThread = getConsoleThread();
        consoleThread.start();
    }

    private static @NotNull Thread getConsoleThread() {
        Thread consoleThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                var commandManager = MinecraftServer.getCommandManager();
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    try {
                        // Execute the command as console
                        commandManager.execute(new ConsoleSender(), line);
                    } catch (Throwable t) {
                        // Print stack trace but keep the console loop running
                        t.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "Console-Input-Thread");
        consoleThread.setDaemon(true);
        return consoleThread;
    }
}