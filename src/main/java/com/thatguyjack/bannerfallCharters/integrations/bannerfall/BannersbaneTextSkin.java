package com.thatguyjack.bannerfallCharters.integrations.bannerfall;

import java.util.regex.Pattern;

public final class BannersbaneTextSkin {
    private BannersbaneTextSkin() {}

    private static final String DAWN = "Dawn";
    private static final String DUSK = "Dusk";
    private static final String BANNERSBANE = "Bannersbane";

    public static String apply(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String replaced = text;

        // Do colors first so JSON color values become valid Minecraft colors
        // before we replace plain words like "Blue" / "Red".
        replaced = replaceJsonColorNames(replaced);
        replaced = replaceLegacyColorCodes(replaced);
        replaced = replaceAmpersandColorCodes(replaced);

        replaced = replaceBannerfallName(replaced);
        replaced = replaceKingdomNames(replaced);

        return replaced;
    }

    private static String replaceBannerfallName(String text) {
        return text
                .replace("Bannerfall", BANNERSBANE)
                .replace("bannerfall", "bannersbane")
                .replace("BANNERFALL", "BANNERSBANE");
    }

    private static String replaceKingdomNames(String text) {
        String replaced = text;

        replaced = replaceIgnoreCase(replaced, "Blue Kingdom", DAWN);
        replaced = replaceIgnoreCase(replaced, "Red Kingdom", DUSK);

        replaced = replaceIgnoreCase(replaced, "Blue kingdom", DAWN);
        replaced = replaceIgnoreCase(replaced, "Red kingdom", DUSK);

        /*
         * Replace standalone Blue/Red, but DO NOT touch JSON color fields like:
         * "color":"blue"
         * "color":"red"
         *
         * Otherwise you get invalid JSON like:
         * "color":"Dawn"
         * "color":"Dusk"
         */
        replaced = replaceStandaloneKingdomWord(replaced, "Blue", DAWN);
        replaced = replaceStandaloneKingdomWord(replaced, "Red", DUSK);
        replaced = replaceStandaloneKingdomWord(replaced, "blue", "dawn");
        replaced = replaceStandaloneKingdomWord(replaced, "red", "dusk");
        replaced = replaceStandaloneKingdomWord(replaced, "BLUE", "DAWN");
        replaced = replaceStandaloneKingdomWord(replaced, "RED", "DUSK");

        return replaced;
    }

    private static String replaceLegacyColorCodes(String text) {
        return text
                // Normal section sign codes.
                .replace("§b", "§e") // aqua -> yellow
                .replace("§c", "§9") // red -> blue

                // JSON-escaped section sign codes.
                .replace("\\u00a7b", "\\u00a7e")
                .replace("\\u00a7c", "\\u00a79");
    }

    private static String replaceAmpersandColorCodes(String text) {
        return text
                .replace("&b", "&e") // aqua -> yellow
                .replace("&c", "&9"); // red -> blue
    }

    private static String replaceJsonColorNames(String text) {
        return text
                // Blue Kingdom / Dawn colors.
                .replace("\"color\":\"aqua\"", "\"color\":\"yellow\"")
                .replace("\"color\":\"dark_aqua\"", "\"color\":\"yellow\"")
                .replace("\"color\":\"blue\"", "\"color\":\"yellow\"")
                .replace("\"color\":\"dark_blue\"", "\"color\":\"yellow\"")

                // Red Kingdom / Dusk colors.
                .replace("\"color\":\"red\"", "\"color\":\"blue\"")
                .replace("\"color\":\"dark_red\"", "\"color\":\"blue\"");
    }

    private static String replaceIgnoreCase(String text, String target, String replacement) {
        return Pattern.compile(Pattern.quote(target), Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .replaceAll(replacement);
    }

    private static String replaceStandaloneKingdomWord(String text, String target, String replacement) {
        /*
         * This negative lookbehind prevents replacing JSON color values:
         *
         * "color":"blue"
         *          ^ do not replace this
         *
         * But it still replaces:
         *
         * "text":"1 Blue"
         * Randomly assigned players: 1 Blue, 0 Red.
         */
        return Pattern.compile("(?<!\"color\":\")\\b" + Pattern.quote(target) + "\\b")
                .matcher(text)
                .replaceAll(replacement);
    }
}