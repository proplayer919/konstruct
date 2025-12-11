package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.instance.HubInstanceData;
import dev.proplayer919.konstruct.instance.HubInstanceRegistry;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import dev.proplayer919.konstruct.util.PlayerHubHelper;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

public class KickHubCommand extends Command {

    public KickHubCommand() {
        super("kickhub", "hubkick");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Usage: /kickhub hub-<id>"));

        var idArg = ArgumentType.String("id").setSuggestionCallback((sender, context, suggestion) -> {
            for (HubInstanceData hubInstance : HubInstanceRegistry.getAllInstances().values()) {
                suggestion.addEntry(new SuggestionEntry(hubInstance.getId()));
            }
        });

        addSyntax((sender, context) -> {
            final String id = context.get(idArg);
            if (sender instanceof Player player) {
                if (!PlayerPermissionRegistry.hasPermission(player, "command.kickhub")) {
                    MessagingHelper.sendMessage(sender, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }

                HubInstanceData hubInstance = HubInstanceRegistry.getInstanceById(id);
                if (hubInstance != null) {
                    // Kick all players from the hub
                    for (var p : hubInstance.getPlayers()) {
                        // Find another hub to send them to
                        HubInstanceData targetHub = HubInstanceRegistry.getInstanceWithLowestPlayersExcept(hubInstance);
                        if (targetHub != null) {
                            PlayerHubHelper.movePlayerToHub(player, targetHub);
                            MessagingHelper.sendMessage(p, MessageType.SERVER, "You have been kicked from " + id + " to " + targetHub.getId() + ".");
                        } else {
                            Component message = MessagingHelper.createMessage(MessageType.ERROR, "You have been kicked from " + id + " but no other hubs are available. Please try reconnecting later.");
                            p.kick(message);
                        }
                    }

                    MessagingHelper.sendMessage(sender, MessageType.ADMIN, "All players have been kicked from " + id + ".");
                } else {
                    MessagingHelper.sendMessage(sender, MessageType.ERROR, "Hub with ID '" + id + "' does not exist.");
                }
            }
        }, idArg);
    }
}