package dev.proplayer919.konstruct.messages;

import dev.proplayer919.konstruct.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum MessageType {
    PROTECT("PROTECT", Emojis.LOCK),
    ERROR("ERROR", Emojis.CROSS_MARK),
    SERVER("SERVER", Emojis.LIGHTNING),
    ADMIN("ADMIN", Emojis.STAR),
    PERMISSION("PERMISSION", Emojis.WARNING),
    BROADCAST("BROADCAST", Emojis.SPEAKER),
    SUCCESS("SUCCESS", Emojis.CHECK_MARK),
    ANTICHEAT("ANTICHEAT", Emojis.WARNING);

    private final String label;
    private final String emoji;

    MessageType(String label, String emoji) {
        this.label = label;
        this.emoji = emoji;
    }

    public String label() {
        return label;
    }

    public String emoji() {
        return emoji;
    }

    public Component labelComponent() {
        String text = (emoji != null && !emoji.isEmpty()) ? emoji + " " + label : label;
        return Component.text(text).color(Constants.BRAND_COLOUR_PRIMARY_2);
    }
}