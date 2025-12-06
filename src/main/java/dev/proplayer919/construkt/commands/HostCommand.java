package dev.proplayer919.construkt.commands;

import dev.proplayer919.construkt.instance.match.MatchType;
import dev.proplayer919.construkt.instance.match.MatchTypeRegistry;
import dev.proplayer919.construkt.messages.MessagingHelper;
import dev.proplayer919.construkt.messages.Namespace;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

public class HostCommand extends Command {

    public HostCommand() {
        super("host", "hostgame");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, Namespace.SERVER, "Usage: /host <id>"));

        var idArg = ArgumentType.String("id");

        addSyntax((sender, context) -> {
            final String id = context.get(idArg);
            if (sender instanceof Player player) {
                // Check if the MatchType exists
                MatchType matchType = MatchTypeRegistry.getMatchTypeById(id);
                if (matchType != null) {
                    // Host the game (implementation not shown)
                    MessagingHelper.sendMessage(player, Namespace.SERVER, "Hosting a " + matchType.getName() + " game!");
                    // Additional logic to create and start the match would go here
                } else {
                    MessagingHelper.sendMessage(player, Namespace.ERROR, "Match type with ID '" + id + "' does not exist.");
                }
            } else {
                MessagingHelper.sendMessage(sender, Namespace.ERROR, "Only players can use this command.");
            }
        }, idArg);
    }
}