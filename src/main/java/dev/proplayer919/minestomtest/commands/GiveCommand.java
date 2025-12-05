package dev.proplayer919.minestomtest.commands;

import dev.proplayer919.minestomtest.helpers.MessagingHelper;
import dev.proplayer919.minestomtest.permissions.PermissionRegistry;
import dev.proplayer919.minestomtest.permissions.PlayerPermissionRegistry;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.Objects;

@SuppressWarnings("PatternValidation")
public class GiveCommand extends Command {

    public GiveCommand() {
        super("give", "giveme", "getitem", "get", "i", "item", "giveitem");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendAdminMessage(sender, "Usage: /give <item> [amount]"));

        var itemIdArg = ArgumentType.String("item-id");
        var amountArg = ArgumentType.Integer("amount").setDefaultValue(1);

        addSyntax((sender, context) -> {
            final String itemId = context.get(itemIdArg);
            final int amount = context.get(amountArg);
            if (sender instanceof Player player) {
                if (!PlayerPermissionRegistry.hasPermission(player, PermissionRegistry.getPermissionByNode("command.gamemode"))) {
                    MessagingHelper.sendPermissionMessage(sender, "You do not have permission to use this command.");
                    return;
                }

                try {
                    ItemStack itemStack = ItemStack.of(Objects.requireNonNull(Material.fromKey(itemId)), amount);
                    player.getInventory().addItemStack(itemStack);
                    MessagingHelper.sendAdminMessage(sender, "Gave you " + amount + " of item " + itemId);
                } catch (Exception e) {
                    MessagingHelper.sendErrorMessage(sender, "Invalid item ID: " + itemId);
                }
            } else {
                MessagingHelper.sendErrorMessage(sender, "Only players can use this command.");
            }
        }, itemIdArg, amountArg);
    }
}