package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.PunishmentMessages;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public class KickCommand extends Command {

    public KickCommand() {
        super("kick");

        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Usage: /kick <player> [message]"));

        var playerArg = ArgumentType.String("player").setSuggestionCallback((sender, context, suggestion) -> {
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
                suggestion.addEntry(new SuggestionEntry(player.getUsername()));
            });
        });
        var messageArg = ArgumentType.StringArray("message").setDefaultValue(new String[0]);

        addSyntax((sender, context) -> {
            // Only check permissions for actual player senders. Allow console/non-player to run the command.
            if (sender instanceof Player) {
                if (!PlayerPermissionRegistry.hasPermission((Player) sender, "command.kick")) {
                    MessagingHelper.sendMessage(sender, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }
            }

            String targetName = context.get(playerArg);
            String[] msgParts = context.get(messageArg);
            String message = msgParts.length > 0 ? String.join(" ", msgParts) : "You have been kicked from the server.";

            Player target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(targetName);
            if (target == null) {
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Player '" + targetName + "' not found.");
                return;
            }

            Component comp = PunishmentMessages.buildKickComponent(message);
            target.kick(comp);
            MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Kicked " + targetName + ".");
        }, playerArg, messageArg);
    }
}
