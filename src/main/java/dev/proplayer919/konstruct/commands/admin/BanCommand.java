package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.messages.PunishmentMessages;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import dev.proplayer919.konstruct.storage.SqliteDatabase;
import dev.proplayer919.konstruct.util.UsernameUuidResolver;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.nio.file.Path;
import java.util.UUID;

public class BanCommand extends Command {
    private static final SqliteDatabase db = new SqliteDatabase(Path.of("data", "konstruct-data.db"));

    static {
        try {
            db.connect();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(db::close, "BanCommand-DB-Close"));
    }

    public BanCommand() {
        super("ban");

        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Usage: /ban <player> [[duration] <message>]"));

        var playerArg = ArgumentType.String("player").setSuggestionCallback((sender, context, suggestion) -> {
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
                suggestion.addEntry(new SuggestionEntry(player.getUsername()));
            });
        });
        var argsArg = ArgumentType.StringArray("args").setDefaultValue(new String[0]);

        addSyntax((sender, context) -> {
            // Only check permissions for actual player senders. Allow console/non-player to run the command.
            if (sender instanceof Player) {
                if (!PlayerPermissionRegistry.hasPermission((Player) sender, "command.ban")) {
                    MessagingHelper.sendMessage(sender, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }
            }

            String targetName = context.get(playerArg);
            String[] extra = context.get(argsArg);

            // parse duration if first arg looks like a time (e.g., "7d", "24h", "30m"). We'll support s/m/h/d/w/y. If not present, ban is permanent.
            Long expiresAt = null;
            String reason;
            if (extra.length > 0 && extra[0].matches("^\\d+[smhd]")) {
                // parse
                String dur = extra[0];
                long mult = 1000L; // seconds
                char unit = dur.charAt(dur.length() - 1);
                long val = Long.parseLong(dur.substring(0, dur.length() - 1));
                switch (unit) {
                    case 's' -> mult = 1000L;
                    case 'm' -> mult = 60_000L;
                    case 'h' -> mult = 3_600_000L;
                    case 'd' -> mult = 86_400_000L;
                    case 'w' -> mult = 604_800_000L;
                    case 'y' -> mult = 31_536_000_000L;
                }
                expiresAt = System.currentTimeMillis() + val * mult;
                if (extra.length > 1) {
                    reason = String.join(" ", java.util.Arrays.copyOfRange(extra, 1, extra.length));
                } else {
                    reason = "Banned by an operator.";
                }
            } else {
                reason = extra.length > 0 ? String.join(" ", extra) : "Banned by an operator.";
            }

            Player target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(targetName);
            UUID targetUuid = null;
            if (target != null) targetUuid = target.getUuid();

            try {
                if (target != null) {
                    // Online: ban by UUID
                    db.insertBanSync(targetUuid, sender instanceof Player ? ((Player) sender).getUuid().toString() : null, expiresAt, reason);
                } else {
                    // Offline: require resolving username -> uuid via Mojang API
                    UUID offlineUuid = UsernameUuidResolver.resolveUuid(targetName);
                    if (offlineUuid == null) {
                        MessagingHelper.sendMessage(sender, MessageType.ERROR, "Failed to resolve username to UUID; cannot ban offline player. Ensure the username is correct and Mojang API is reachable.");
                        return;
                    }
                    db.insertBanSync(offlineUuid, sender instanceof Player ? ((Player) sender).getUuid().toString() : null, expiresAt, reason);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Failed to write ban to database.");
                return;
            }

            if (target != null) {
                Component comp = PunishmentMessages.buildBanComponent(reason, expiresAt);
                target.kick(comp);
            }

            MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Banned " + targetName + (expiresAt != null ? " until " + new java.util.Date(expiresAt).toString() : " permanently") + ". Reason: " + reason);
        }, playerArg, argsArg);
    }
}
