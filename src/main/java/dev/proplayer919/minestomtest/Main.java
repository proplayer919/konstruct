package dev.proplayer919.minestomtest;

import dev.proplayer919.minestomtest.commands.GameModeCommand;
import dev.proplayer919.minestomtest.commands.PermissionCommand;
import dev.proplayer919.minestomtest.permissions.Permission;
import dev.proplayer919.minestomtest.permissions.PermissionRegistry;
import dev.proplayer919.minestomtest.permissions.PlayerPermissionRegistry;
import dev.proplayer919.minestomtest.generators.InstanceCreator;
import io.github.togar2.pvp.MinestomPvP;
import io.github.togar2.pvp.feature.CombatFeatureSet;
import io.github.togar2.pvp.feature.CombatFeatures;
import net.bridgesplash.sidebar.SidebarAPI;
import net.bridgesplash.sidebar.sidebar.CustomSidebar;
import net.bridgesplash.sidebar.state.State;
import net.kyori.adventure.text.Component;
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
import dev.proplayer919.minestomtest.commands.ServerCommand;
import dev.proplayer919.minestomtest.commands.GiveCommand;
import dev.proplayer919.minestomtest.helpers.MessagingHelper;
import dev.proplayer919.minestomtest.storage.SqliteDatabase;
import net.minestom.server.command.ConsoleSender;
import org.jetbrains.annotations.NotNull;
import org.everbuild.blocksandstuff.blocks.BlockBehaviorRuleRegistrations;
import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations;

import java.util.Map;
import java.util.Hashtable;
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

        // Register permissions
        PermissionRegistry.registerPermission(buildPermission);
        PermissionRegistry.registerPermission(breakPermission);
        PermissionRegistry.registerPermission(giveCommandPermission);
        PermissionRegistry.registerPermission(gamemodeCommandPermission);
        PermissionRegistry.registerPermission(permissionCommandPermission);

        // Initialize SQLite database for persistent player permissions
        SqliteDatabase sqliteDb = new SqliteDatabase(Path.of("data", "permissions.db"));
        try {
            sqliteDb.connect();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Ensure DB is closed on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sqliteDb.close();
        }, "Sqlite-Shutdown-Close"));

        // Create instances
        InstanceContainer hubInstance = InstanceCreator.createSimpleInstanceContainer(Block.GRASS_BLOCK, Block.GOLD_BLOCK, false);

        // Map of server IDs to instances
        Map<String, Instance> instanceMap = new Hashtable<>();
        instanceMap.put("hub", hubInstance);

        // Init commands
        ServerCommand serverCommand = new ServerCommand(instanceMap);
        GiveCommand giveCommand = new GiveCommand();
        GameModeCommand gameModeCommand = new GameModeCommand();
        PermissionCommand permissionCommand = new PermissionCommand();

        MinecraftServer.getCommandManager().register(giveCommand);
        MinecraftServer.getCommandManager().register(gameModeCommand);
        MinecraftServer.getCommandManager().register(serverCommand);
        MinecraftServer.getCommandManager().register(permissionCommand);

        // Create a sidebar
        CustomSidebar sidebar = new CustomSidebar(Component.text("Player Sidebar"));

        // Create some reactive state
        State<Boolean> isSneaking = new State<>(false);
        State<Integer> sneaks = new State<>(0);

        // Register states with keys
        sidebar.addState("is_sneaking", isSneaking);
        sidebar.addState("sneaks", sneaks);

        // Define lines using MiniMessage tags
        sidebar.setLine("status_line", "<ifstate:is_sneaking:true:'<green>Sneaking</green>':'<red>Standing</red>'/>");
        sidebar.setLine("count_line", "Sneak count: <state:sneaks/>");

        // Player spawning
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            // Set player instance and position
            event.setSpawningInstance(hubInstance);
            player.setRespawnPoint(new Pos(0.5, 40, 0.5));
            player.setGameMode(GameMode.SURVIVAL);
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();

            // Setup sidebar for the player
            SidebarAPI.getSidebarManager().addSidebar(player, sidebar);
        });

        globalEventHandler.addListener(PlayerBlockBreakEvent.class, event -> {
            final Player player = event.getPlayer();

            Instance playerInstance = player.getInstance();
            if (playerInstance == null) {
                return;
            }

            if (instanceMap.containsValue(playerInstance)) {
                if (playerInstance == hubInstance) {
                    if (!PlayerPermissionRegistry.hasPermission(player, breakPermission)) {
                        MessagingHelper.sendProtectMessage(player, "You cannot break blocks here");
                        event.setCancelled(true);
                    }
                }
            }
        });

        globalEventHandler.addListener(PlayerBlockPlaceEvent.class, event -> {
            final Player player = event.getPlayer();

            Instance playerInstance = player.getInstance();
            if (playerInstance == null) {
                return;
            }

            if (instanceMap.containsValue(playerInstance)) {
                if (playerInstance == hubInstance) {
                    if (!PlayerPermissionRegistry.hasPermission(player, buildPermission)) {
                        MessagingHelper.sendProtectMessage(player, "You cannot place blocks here");
                        event.setCancelled(true);
                    }
                }
            }
        });

        globalEventHandler.addListener(PlayerStartSneakingEvent.class, event -> {
            isSneaking.set(true);
        });

        globalEventHandler.addListener(PlayerStopSneakingEvent.class, event -> {
            isSneaking.set(false);
            sneaks.setPrev(prev -> prev + 1);
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