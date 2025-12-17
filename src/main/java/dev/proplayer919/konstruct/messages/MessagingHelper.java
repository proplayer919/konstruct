package dev.proplayer919.konstruct.messages;

import dev.proplayer919.konstruct.Constants;
import dev.proplayer919.konstruct.CustomPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;

public final class MessagingHelper {
    public static void sendMessage(Audience audience, MessageType messageType, String message) {
        audience.sendMessage(createMessage(messageType, message));
    }

    public static void sendMessage(Collection<Audience> audiences, MessageType messageType, String message) {
        Component component = createMessage(messageType, message);
        for (Audience audience : audiences) {
            audience.sendMessage(component);
        }
    }

    public static void sendMessage(Collection<CustomPlayer> audiences, Component message) {
        for (Audience audience : audiences) {
            audience.sendMessage(message);
        }
    }

    public static void sendActionBar(Audience audience, Component message) {
        audience.sendActionBar(message);
    }

    public static void sendActionBar(Collection<CustomPlayer> audience, Component message) {
        for (Audience a : audience) {
            a.sendActionBar(message);
        }
    }

    public static void sendSound(Audience audience, Sound sound) {
        audience.playSound(sound);
    }

    public static void sendSound(Collection<CustomPlayer> audience, Sound sound) {
        for (Audience a : audience) {
            a.playSound(sound);
        }
    }

    // Helper to build the Component for a namespace + message
    public static Component createMessage(MessageType messageType, String message) {
        return messageType.labelComponent()
                .append(Component.text(" | ").color(NamedTextColor.GRAY))
                .append(Component.text(message).color(Constants.BRAND_COLOUR_PRIMARY_1));
    }
}
