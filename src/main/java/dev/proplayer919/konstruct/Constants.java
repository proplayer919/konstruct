package dev.proplayer919.konstruct;

import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

public abstract class Constants {
    /// The default locale for messages and formatting.
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /// Used for primary highlights and accents.
    public static final TextColor BRAND_COLOUR_PRIMARY_1 = TextColor.color(0xFFB647);

    /// Used for message body text
    public static final TextColor BRAND_COLOUR_PRIMARY_2 = TextColor.color(0xFF9B00);

    /// Dark colour, used for backgrounds and accents.
    public static final TextColor BRAND_COLOUR_PRIMARY_3 = TextColor.color(0xC98C2F);

    /// Used for player names
    public static final TextColor BRAND_COLOUR_ALT_1 = TextColor.color(0xFF5100);

    /// Used for secondary highlights
    public static final TextColor BRAND_COLOUR_ALT_2 = TextColor.color(0xFF006A);
}
