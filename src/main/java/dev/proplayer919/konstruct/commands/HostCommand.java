package dev.proplayer919.konstruct.commands;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.instance.InstanceLoader;
import dev.proplayer919.konstruct.matches.MatchData;
import dev.proplayer919.konstruct.matches.MatchManager;
import dev.proplayer919.konstruct.matches.MatchesRegistry;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.messages.MessageType;
import net.minestom.server.command.builder.Command;
import net.minestom.server.instance.Instance;

public class HostCommand extends Command {

    public HostCommand() {
        super("host", "hostmatch", "hostgame");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> {
            if (sender instanceof CustomPlayer player) {
                Instance lobbyInstance = InstanceLoader.loadAnvilInstance("data/maps/lobby", false);
                Instance matchInstance = InstanceLoader.loadAnvilInstance("data/maps/arenas/deathmatch1", true);
                MatchData matchData = new MatchData(player, lobbyInstance, matchInstance);
                MatchesRegistry.registerMatch(matchData);
                MatchManager.setupMatch(matchData);
                MatchManager.spawnPlayerIntoMatch(matchData, player);
            } else {
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Only players can use this command.");
            }
        });
    }
}