package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.matches.MatchData;
import dev.proplayer919.konstruct.matches.MatchesRegistry;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;

public class UnfreezeCommand extends Command {

    public UnfreezeCommand() {
        super("unfreeze", "unfreezeplayer");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Usage: /unfreeze <username>"));

        var usernameArg = ArgumentType.String("username").setSuggestionCallback((sender, context, suggestion) -> {
            if (sender instanceof CustomPlayer player) {
                MatchData matchData = MatchesRegistry.getMatchWithPlayer(player);
                if (matchData != null) {
                    for (CustomPlayer customPlayer : matchData.getPlayers()) {
                        if (customPlayer.isFrozen()) {
                            suggestion.addEntry(new SuggestionEntry(customPlayer.getUsername()));
                        }
                    }
                }
            }
        });

        addSyntax((sender, context) -> {
            final String username = context.get(usernameArg);
            if (sender instanceof CustomPlayer player) {
                if (!PlayerPermissionRegistry.hasPermission(player, "command.freeze")) {
                    MessagingHelper.sendMessage(player, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }

                MatchData matchData = MatchesRegistry.getMatchWithPlayer(player);
                if (matchData == null) {
                    MessagingHelper.sendMessage(player, MessageType.ERROR, "You are not currently in a match.");
                    return;
                }

                CustomPlayer targetPlayer = (CustomPlayer) MinecraftServer.getConnectionManager().findOnlinePlayer(username);
                if (targetPlayer == null) {
                    MessagingHelper.sendMessage(player, MessageType.ERROR, "Player with username '" + username + "' is not online.");
                    return;
                }

                if (!matchData.isPlayerAlive(targetPlayer)) {
                    MessagingHelper.sendMessage(player, MessageType.ERROR, "Player '" + username + "' is not in your match or is already dead.");
                    return;
                }

                if (!targetPlayer.isFrozen()) {
                    MessagingHelper.sendMessage(player, MessageType.ERROR, "Player '" + username + "' isn't frozen.");
                }

                targetPlayer.setFrozen(false);

                MessagingHelper.sendMessage(player, MessageType.ADMIN, "Player '" + username + "' has been unfrozen.");
            } else {
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Only players can use this command.");
            }
        }, usernameArg);
    }
}