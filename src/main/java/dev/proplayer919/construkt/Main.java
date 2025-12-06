package dev.proplayer919.construkt;

import dev.proplayer919.construkt.commands.HostCommand;
import dev.proplayer919.construkt.commands.admin.*;
import dev.proplayer919.construkt.commands.HubCommand;
import dev.proplayer919.construkt.instance.match.DeathmatchMatchType;
import dev.proplayer919.construkt.instance.match.MatchTypeRegistry;
import dev.proplayer919.construkt.messages.Namespace;
import dev.proplayer919.construkt.permissions.PlayerPermissionRegistry;
import dev.proplayer919.construkt.sidebar.SidebarData;
import dev.proplayer919.construkt.instance.HubInstanceData;
import dev.proplayer919.construkt.instance.HubInstanceRegistry;
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
import dev.proplayer919.construkt.messages.MessagingHelper;
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
    public final static CombatFeatureSet modernVanilla = CombatFeatures.modernVanilla();

    static void main(String[] args) {
        // Initialization
        MinecraftServer minecraftServer = MinecraftServer.init(new Auth.Online());

        MinestomPvP.init();

        BlockPlacementRuleRegistrations.registerDefault();
        BlockBehaviorRuleRegistrations.registerDefault();

        // Initialize SQLite database for persistent data
        SqliteDatabase sqliteDb = new SqliteDatabase(Path.of("data", "construkt-data.db"));
        try {
            sqliteDb.connect();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Ensure DB is closed on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(sqliteDb::close, "Sqlite-Shutdown-Close"));

        // Create hub instances
        int hubs = 5; // Number of hub instances to create
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
        KickHubCommand kickHubCommand = new KickHubCommand();
        KickCommand kickCommand = new KickCommand();
        BanCommand banCommand = new BanCommand();
        UnbanCommand unbanCommand = new UnbanCommand();
        HostCommand hostCommand = new HostCommand();

        MinecraftServer.getCommandManager().register(giveCommand);
        MinecraftServer.getCommandManager().register(gameModeCommand);
        MinecraftServer.getCommandManager().register(permissionCommand);
        MinecraftServer.getCommandManager().register(hubCommand);
        MinecraftServer.getCommandManager().register(kickHubCommand);
        MinecraftServer.getCommandManager().register(kickCommand);
        MinecraftServer.getCommandManager().register(banCommand);
        MinecraftServer.getCommandManager().register(unbanCommand);
        MinecraftServer.getCommandManager().register(hostCommand);

        // Register game types
        MatchTypeRegistry.registerMatchType(new DeathmatchMatchType());

        // Player spawning
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            // Pick a hub instance with the least players
            HubInstanceData hubInstanceData = HubInstanceRegistry.getInstanceWithLowestPlayers();
            hubInstanceData.getPlayers().add(player);
            Instance hubInstance = hubInstanceData.getInstance();
            event.setSpawningInstance(hubInstance);
            player.setRespawnPoint(new Pos(0.5, 40, 0.5));
            player.setGameMode(GameMode.SURVIVAL);
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();

            // Check if player is banned
            try {
                java.util.Map<String, Object> banInfo = sqliteDb.getBanInfoSync(player.getUuid());
                if (banInfo != null) {
                    String reason = (String) banInfo.get("reason");
                    Object expiresObj = banInfo.get("expires_at");
                    Long expires = expiresObj == null ? null : ((Number) expiresObj).longValue();
                    net.kyori.adventure.text.Component comp = dev.proplayer919.construkt.messages.BanMessage.buildBanComponent(reason, expires);
                    player.kick(comp);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

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

            if (PlayerPermissionRegistry.hasPermission(player, "server.break")) {
                return;
            }

            if (HubInstanceRegistry.getInstanceWithPlayer(player.getUuid()) != null) {
                MessagingHelper.sendMessage(player, Namespace.PROTECT, "You cannot break blocks in a hub");
                event.setCancelled(true);
            }
        });

        globalEventHandler.addListener(PlayerBlockPlaceEvent.class, event -> {
            final Player player = event.getPlayer();

            Instance playerInstance = player.getInstance();
            if (playerInstance == null) {
                return;
            }

            if (PlayerPermissionRegistry.hasPermission(player, "server.build")) {
                return;
            }

            if (HubInstanceRegistry.getInstanceWithPlayer(player.getUuid()) != null) {
                MessagingHelper.sendMessage(player, Namespace.PROTECT, "You cannot break blocks in a hub");
                event.setCancelled(true);
            }
        });

        globalEventHandler.addListener(PlayerFlagEvent.class, event -> {
            final Player player = event.player();

            MessagingHelper.sendMessage(player, Namespace.ANTICHEAT, "You have been flagged for " + event.checkName() + " (Certainty: " + (event.certainty() * 100) + "%)");
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
