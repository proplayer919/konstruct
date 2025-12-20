package dev.proplayer919.konstruct.commands;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.matches.MatchData;
import dev.proplayer919.konstruct.matches.MatchManager;
import dev.proplayer919.konstruct.matches.MatchesRegistry;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.util.PlayerHubHelper;
import net.minestom.server.command.builder.Command;

public class QuickStartCommand extends Command {

    public QuickStartCommand() {
        super("quickstart", "qstart");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> {
            // Find the match the player is currently in
            if (sender instanceof CustomPlayer player) {
                MatchData matchData = MatchesRegistry.getMatchWithPlayer(player);
                if (matchData != null) {
                    // Find if the player is the host of the match
                    if (matchData.getHostUUID().equals(player.getUuid())) {
                        if ((matchData.getPlayerCount() + 4) >= matchData.getMinPlayers()) {
                            MatchManager.startPreMatchCountdown(matchData);
                        } else {
                            MessagingHelper.sendMessage(player, MessageType.ERROR, "Not enough slots to quickstart the match.");
                        }
                    } else {
                        MessagingHelper.sendMessage(player, MessageType.ERROR, "Only the host can quickstart the match.");
                    }
                } else {
                    MessagingHelper.sendMessage(player, MessageType.ERROR, "You are not currently in a game.");
                }
            } else {
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Only players can use this command.");
            }
        });
    }
}