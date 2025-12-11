package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.UUID;

public class PermissionCommand extends Command {

    public PermissionCommand() {
        super("permission", "perm");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Usage: /permission <username/UUID> <permission> <true/false>"));

        var usernameArg = ArgumentType.String("username").setSuggestionCallback((sender, context, suggestion) -> {
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
                suggestion.addEntry(new SuggestionEntry(player.getUsername()));
            });
        });
        var permissionArg = ArgumentType.String("permission");
        var valueArg = ArgumentType.Boolean("value");

        addSyntax((sender, context) -> {
            final String username = context.get(usernameArg);
            final String permissionNode = context.get(permissionArg);
            final boolean value = context.get(valueArg);

            if (sender instanceof Player player) {
                if (!PlayerPermissionRegistry.hasPermission(player, "command.permission")) {
                    MessagingHelper.sendMessage(sender, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }
            }


            // Detect if username is a UUID or a player name
            boolean isUUID = false;
            if (username.length() == 36) {
                // Likely a UUID
                try {
                    UUID.fromString(username);
                    isUUID = true;
                } catch (IllegalArgumentException e) {
                    // Not a valid UUID, proceed to treat as username
                }
            }

            if (isUUID) {
                UUID targetUUID = UUID.fromString(username);
                if (value) {
                    PlayerPermissionRegistry.grantPermission(targetUUID, permissionNode);
                    MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Granted permission " + permissionNode + " to UUID " + username);
                } else {
                    PlayerPermissionRegistry.revokePermission(targetUUID, permissionNode);
                    MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Revoked permission " + permissionNode + " from UUID " + username);
                }
            } else {
                Player targetPlayer = MinecraftServer.getConnectionManager().findOnlinePlayer(username);
                if (targetPlayer == null) {
                    MessagingHelper.sendMessage(sender, MessageType.ERROR, "Player " + username + " is not online.");
                    return;
                }

                if (value) {
                    PlayerPermissionRegistry.grantPermission(targetPlayer, permissionNode);
                    MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Granted permission " + permissionNode + " to " + username);
                } else {
                    PlayerPermissionRegistry.revokePermission(targetPlayer, permissionNode);
                    MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Revoked permission " + permissionNode + " from " + username);
                }
            }
        }, usernameArg, permissionArg, valueArg);
    }
}