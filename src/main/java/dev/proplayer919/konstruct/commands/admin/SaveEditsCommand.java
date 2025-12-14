package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.editor.EditorSession;
import dev.proplayer919.konstruct.editor.EditorSessionRegistry;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import dev.proplayer919.konstruct.util.PlayerHubHelper;
import net.minestom.server.command.builder.Command;

public class SaveEditsCommand extends Command {

    public SaveEditsCommand() {
        super("saveedits");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> {
            if (sender instanceof CustomPlayer player) {
                if (!PlayerPermissionRegistry.hasPermission(player, "command.editor")) {
                    MessagingHelper.sendMessage(sender, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }

                // Check if the player has an active editor session
                EditorSession session = EditorSessionRegistry.getSession(player.getUuid());
                if (session == null) {
                    MessagingHelper.sendMessage(sender, MessageType.ADMIN, "You do not have an active editor session.");
                    return;
                }

                // Save the edits
                session.save();

                // Exit the editor session
                EditorSessionRegistry.removeSession(player.getUuid());

                PlayerHubHelper.returnPlayerToHub(player);

                MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Your edits have been saved successfully.");
            } else {
                MessagingHelper.sendMessage(sender, MessageType.ADMIN, "This command can only be used by players.");
            }
        });
    }
}