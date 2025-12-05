package dev.proplayer919.minestomtest.commands;

import dev.proplayer919.minestomtest.helpers.MessagingHelper;
import dev.proplayer919.minestomtest.permissions.Permission;
import dev.proplayer919.minestomtest.permissions.PermissionRegistry;
import dev.proplayer919.minestomtest.permissions.PlayerPermissionRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("PatternValidation")
public class PermissionCommand extends Command {

    public PermissionCommand() {
        super("permission", "perm");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendAdminMessage(sender, "Usage: /permission <username/UUID> <permission> <true/false>"));

        var usernameArg = ArgumentType.String("username");
        var permissionArg = ArgumentType.String("permission");
        var valueArg = ArgumentType.Boolean("value");

        addSyntax((sender, context) -> {
            final String username = context.get(usernameArg);
            final String permissionNode = context.get(permissionArg);
            final boolean value = context.get(valueArg);

            if (sender instanceof Player player) {
                if (!PlayerPermissionRegistry.hasPermission(player, PermissionRegistry.getPermissionByNode("command.permission"))) {
                    MessagingHelper.sendPermissionMessage(sender, "You do not have permission to use this command.");
                    return;
                }
            }

            Permission permission = PermissionRegistry.getPermissionByNode(permissionNode);
            if (permission == null) {
                MessagingHelper.sendErrorMessage(sender, "Permission node " + permissionNode + " does not exist.");
                return;
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
                    PlayerPermissionRegistry.grantPermission(targetUUID, permission);
                    MessagingHelper.sendAdminMessage(sender, "Granted permission " + permissionNode + " to UUID " + username);
                } else {
                    PlayerPermissionRegistry.revokePermission(targetUUID, permission);
                    MessagingHelper.sendAdminMessage(sender, "Revoked permission " + permissionNode + " from UUID " + username);
                }
            } else {
                Player targetPlayer = MinecraftServer.getConnectionManager().findOnlinePlayer(username);
                if (targetPlayer == null) {
                    MessagingHelper.sendErrorMessage(sender, "Player " + username + " is not online.");
                    return;
                }

                if (value) {
                    PlayerPermissionRegistry.grantPermission(targetPlayer, permission);
                    MessagingHelper.sendAdminMessage(sender, "Granted permission " + permissionNode + " to " + username);
                } else {
                    PlayerPermissionRegistry.revokePermission(targetPlayer, permission);
                    MessagingHelper.sendAdminMessage(sender, "Revoked permission " + permissionNode + " from " + username);
                }
            }
        }, usernameArg, permissionArg, valueArg);
    }
}