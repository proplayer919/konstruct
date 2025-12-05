package dev.proplayer919.construkt.helpers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.Hashtable;

@SuppressWarnings("unused")
public class MessagingHelper {
    public static void sendMessageToAudience(Audience audience, NamedTextColor namespaceColor, String namespace, String message) {
        Map<String, String> emojiMap = new Hashtable<>();
        emojiMap.put("PROTECT", Emojis.LOCK);
        emojiMap.put("ERROR", Emojis.CROSS_MARK);
        emojiMap.put("SERVER", Emojis.LIGHTNING);
        emojiMap.put("ADMIN", Emojis.STAR);
        emojiMap.put("PERMISSION", Emojis.WARNING);
        emojiMap.put("ELIMINATION", Emojis.SWORD);
        emojiMap.put("BROADCAST", Emojis.SPEAKER);
        emojiMap.put("SUCCESS", Emojis.CHECK_MARK);
        emojiMap.put("ANTICHEAT", Emojis.WARNING);

        // Prepend emoji if namespace matches
        if (emojiMap.containsKey(namespace)) {
            namespace = emojiMap.get(namespace) + " " + namespace;
        }

        Component formattedMessage = Component.text(namespace).color(namespaceColor)
                .append(Component.text(" | ").color(NamedTextColor.GRAY))
                .append(Component.text(message).color(NamedTextColor.GRAY));
        audience.sendMessage(formattedMessage);
    }

    public static void sendProtectMessage(Audience audience, String message) {
        sendMessageToAudience(audience, NamedTextColor.RED, "PROTECT", message);
    }

    public static void sendServerMessage(Audience audience, String message) {
        sendMessageToAudience(audience, NamedTextColor.AQUA, "SERVER", message);
    }

    public static void sendAdminMessage(Audience audience, String message) {
        sendMessageToAudience(audience, NamedTextColor.GOLD, "ADMIN", message);
    }

    public static void sendPermissionMessage(Audience audience, String message) {
        sendMessageToAudience(audience, NamedTextColor.DARK_PURPLE, "PERMISSION", message);
    }

    public static void sendErrorMessage(Audience audience, String message) {
        sendMessageToAudience(audience, NamedTextColor.RED, "ERROR", message);
    }

    public static void sendAnticheatMessage(Audience audience, String message) {
        sendMessageToAudience(audience, NamedTextColor.RED, "ANTICHEAT", message);
    }

    public static void sendEliminationMessage(Audience audience, String message) {
        sendMessageToAudience(audience, NamedTextColor.DARK_RED, "ELIMINATION", message);
    }

    public static void sendBroadcastMessage(Audience audience, String message) {
        sendMessageToAudience(audience, NamedTextColor.LIGHT_PURPLE, "BROADCAST", message);
    }

    public static void sendSuccessMessage(Audience audience, String message) {
        sendMessageToAudience(audience, NamedTextColor.GREEN, "SUCCESS", message);
    }
}
