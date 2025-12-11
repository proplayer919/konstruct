package dev.proplayer919.konstruct.commands;

import dev.proplayer919.konstruct.instance.GameInstanceData;
import dev.proplayer919.konstruct.instance.GameInstanceRegistry;
import dev.proplayer919.konstruct.match.MatchManager;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.messages.MessageType;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public class JoinMatchCommand extends Command {

    public JoinMatchCommand() {
        super("join", "joinmatch", "joingame");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.SERVER, "Usage: /join <id>"));

        var idArg = ArgumentType.String("id").setSuggestionCallback((sender, context, suggestion) -> {
            for (GameInstanceData gameInstance : GameInstanceRegistry.getAllInstances().values()) {
                suggestion.addEntry(new SuggestionEntry(gameInstance.getId()));
            }
        });

        addSyntax((sender, context) -> {
            final String id = context.get(idArg);
            if (sender instanceof Player player) {
                // Prevent joining another match while already in a game
                if (GameInstanceRegistry.getInstanceWithPlayer(player.getUuid()) != null) {
                    MessagingHelper.sendMessage(player, MessageType.ERROR, "You are already in a game and cannot join another.");
                    return;
                }

                // Check if the GameInstanceData exists
                GameInstanceData gameInstanceData = GameInstanceRegistry.getInstanceById(id);
                if (gameInstanceData != null) {
                    // Check if the match is already full
                    if (gameInstanceData.isNotFull()) {
                        MatchManager.spawnPlayerIntoMatch(gameInstanceData, player);
                    } else {
                        MessagingHelper.sendMessage(player, MessageType.ERROR, "The match in instance '" + id + "' is already full. (" + gameInstanceData.getPlayers().size() + "/" + gameInstanceData.getMatchType().getMaxPlayers() + ")");
                    }
                } else {
                    MessagingHelper.sendMessage(player, MessageType.ERROR, "Game instance with ID '" + id + "' does not exist.");
                }
            } else {
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Only players can use this command.");
            }
        }, idArg);
    }
}