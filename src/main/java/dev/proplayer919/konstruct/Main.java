package dev.proplayer919.konstruct;

import dev.proplayer919.konstruct.commands.HostCommand;
import dev.proplayer919.konstruct.commands.JoinMatchCommand;
import dev.proplayer919.konstruct.commands.LeaveMatchCommand;
import dev.proplayer919.konstruct.commands.admin.*;
import dev.proplayer919.konstruct.commands.HubCommand;
import dev.proplayer919.konstruct.match.types.DeathmatchMatchType;
import dev.proplayer919.konstruct.match.types.MatchTypeRegistry;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.PunishmentMessages;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import dev.proplayer919.konstruct.sidebar.SidebarData;
import dev.proplayer919.konstruct.instance.HubInstanceData;
import dev.proplayer919.konstruct.instance.HubInstanceRegistry;
import dev.proplayer919.konstruct.generators.InstanceCreator;
import dev.proplayer919.konstruct.sidebar.SidebarRegistry;
import io.github.togar2.pvp.MinestomPvP;
import io.github.togar2.pvp.feature.CombatFeatureSet;
import io.github.togar2.pvp.feature.CombatFeatures;
import net.bridgesplash.sidebar.SidebarAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.anticheat.MangoAC;
import net.mangolise.anticheat.events.PlayerFlagEvent;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.instance.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.coordinate.Pos;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.storage.SqliteDatabase;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.ping.Status;
import net.minestom.server.utils.identity.NamedAndIdentified;
import org.jetbrains.annotations.NotNull;
import org.everbuild.blocksandstuff.blocks.BlockBehaviorRuleRegistrations;
import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
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
        SqliteDatabase sqliteDb = new SqliteDatabase(Path.of("data", "konstruct-data.db"));
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

        // Server commands
        HubCommand hubCommand = new HubCommand();
        MinecraftServer.getCommandManager().register(hubCommand);

        HostCommand hostCommand = new HostCommand();
        MinecraftServer.getCommandManager().register(hostCommand);

        JoinMatchCommand joinMatchCommand = new JoinMatchCommand();
        MinecraftServer.getCommandManager().register(joinMatchCommand);

        LeaveMatchCommand leaveMatchCommand = new LeaveMatchCommand();
        MinecraftServer.getCommandManager().register(leaveMatchCommand);

        // Admin commands
        GiveCommand giveCommand = new GiveCommand();
        MinecraftServer.getCommandManager().register(giveCommand);

        GameModeCommand gameModeCommand = new GameModeCommand();
        MinecraftServer.getCommandManager().register(gameModeCommand);

        PermissionCommand permissionCommand = new PermissionCommand();
        MinecraftServer.getCommandManager().register(permissionCommand);

        KickHubCommand kickHubCommand = new KickHubCommand();
        MinecraftServer.getCommandManager().register(kickHubCommand);

        KickCommand kickCommand = new KickCommand();
        MinecraftServer.getCommandManager().register(kickCommand);

        BanCommand banCommand = new BanCommand();
        MinecraftServer.getCommandManager().register(banCommand);

        UnbanCommand unbanCommand = new UnbanCommand();
        MinecraftServer.getCommandManager().register(unbanCommand);

        // Admin abuse commands
        WinCommand winCommand = new WinCommand();
        MinecraftServer.getCommandManager().register(winCommand);

        KillCommand killCommand = new KillCommand();
        MinecraftServer.getCommandManager().register(killCommand);

        // Admin funny commands
        SizeCommand sizeCommand = new SizeCommand();
        MinecraftServer.getCommandManager().register(sizeCommand);

        // Register game types
        MatchTypeRegistry.registerMatchType(new DeathmatchMatchType());

        // Player spawning
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(ServerListPingEvent.class, event -> {
            byte[] favicon = null;
            try {
                Path faviconPath = Path.of("data", "icon.png");
                if (Files.exists(faviconPath)) {
                    favicon = Files.readAllBytes(faviconPath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            int onlinePlayers = MinecraftServer.getConnectionManager().getOnlinePlayerCount();

            event.setStatus(Status.builder()
                    .description(
                            Component.text("§l §e                   -§k=§e-§6 §lKonstruct§e -§k=§e-§r")
                                    .appendNewline()
                                    .append(Component.text("                 §dv0.0 §aBETA §7-§b Whitelist Only!")))
                    .favicon(favicon)
                    .playerInfo(Status.PlayerInfo.builder()
                            .onlinePlayers(onlinePlayers)
                            .maxPlayers(500)
                            .sample(NamedAndIdentified.named(Component.text("Herobrine")))
                            .build())
                    .versionInfo(new Status.VersionInfo("1.21.10", 773)) // set some fake version info
                    .build());
        });

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
                    net.kyori.adventure.text.Component comp = PunishmentMessages.buildBanComponent(reason, expires);
                    player.kick(comp);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

            // Make it so players with permission "*" get permission level 4
            if (PlayerPermissionRegistry.hasPermission(player, "*")) {
                player.setPermissionLevel(4);
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
                MessagingHelper.sendMessage(player, MessageType.PROTECT, "You cannot break blocks in a hub");
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
                MessagingHelper.sendMessage(player, MessageType.PROTECT, "You cannot break blocks in a hub");
                event.setCancelled(true);
            }
        });

        globalEventHandler.addListener(PlayerFlagEvent.class, event -> {
            final Player player = event.player();

            // Get all players with anticheat.notify permission
            for (var staffId : PlayerPermissionRegistry.getPlayersWithPermission("anticheat.notify")) {
                Player staffPlayer = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(staffId);
                if (staffPlayer != null) {
                    MessagingHelper.sendMessage(staffPlayer, MessageType.ANTICHEAT, "Player " + player.getUsername() + " flagged for " + event.checkName() + " (Certainty: " + (event.certainty() * 100) + "%)");
                }
            }
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
