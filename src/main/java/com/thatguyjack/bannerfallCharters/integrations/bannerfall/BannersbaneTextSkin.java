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

        replaced = replaceJsonColorNames(replaced);
        replaced = replaceLegacyColorCodes(replaced);
        replaced = replaceAmpersandColorCodes(replaced);

        replaced = replaceBannerfallName(replaced);
        replaced = replaceKingdomNames(replaced);
        replaced = replaceKingdomPhrases(replaced);

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

        replaced = replaceIgnoreCase(replaced, "Blue Kingdom", DUSK);
        replaced = replaceIgnoreCase(replaced, "Red Kingdom", DAWN);

        replaced = replaceIgnoreCase(replaced, "Blue kingdom", DUSK);
        replaced = replaceIgnoreCase(replaced, "Red kingdom", DAWN);

        replaced = replaceStandaloneKingdomWord(replaced, "Blue", DUSK);
        replaced = replaceStandaloneKingdomWord(replaced, "Red", DAWN);
        replaced = replaceStandaloneKingdomWord(replaced, "blue", "dusk");
        replaced = replaceStandaloneKingdomWord(replaced, "red", "dawn");
        replaced = replaceStandaloneKingdomWord(replaced, "BLUE", "DUSK");
        replaced = replaceStandaloneKingdomWord(replaced, "RED", "DAWN");

        return replaced;
    }

    private static String replaceKingdomPhrases(String text) {
        String replaced = text;

        replaced = replaceIgnoreCase(replaced, "Entering the Dawn", "Entering the Kingdom of Dawn");
        replaced = replaceIgnoreCase(replaced, "Entering the Dusk", "Entering the Kingdom of Dusk");

        replaced = replaceIgnoreCase(replaced, "entering the Dawn", "entering the Kingdom of Dawn");
        replaced = replaceIgnoreCase(replaced, "entering the Dusk", "entering the Kingdom of Dusk");

        replaced = replaceIgnoreCase(replaced, "Entering the §eDawn", "Entering the §eKingdom of Dawn");
        replaced = replaceIgnoreCase(replaced, "Entering the §9Dusk", "Entering the §9Kingdom of Dusk");

        replaced = replaceIgnoreCase(replaced, "entering the §eDawn", "entering the §eKingdom of Dawn");
        replaced = replaceIgnoreCase(replaced, "entering the §9Dusk", "entering the §9Kingdom of Dusk");

        replaced = replaceIgnoreCase(replaced, "Entering the \\u00a7eDawn", "Entering the \\u00a7eKingdom of Dawn");
        replaced = replaceIgnoreCase(replaced, "Entering the \\u00a79Dusk", "Entering the \\u00a79Kingdom of Dusk");

        replaced = replaceIgnoreCase(replaced, "entering the \\u00a7eDawn", "entering the \\u00a7eKingdom of Dawn");
        replaced = replaceIgnoreCase(replaced, "entering the \\u00a79Dusk", "entering the \\u00a79Kingdom of Dusk");

        return replaced;
    }

    private static String replaceLegacyColorCodes(String text) {
        return text
                .replace("§b", "§9")
                .replace("§c", "§e")

                .replace("\\u00a7b", "\\u00a79")
                .replace("\\u00a7c", "\\u00a7e");
    }

    private static String replaceAmpersandColorCodes(String text) {
        return text
                .replace("&b", "&9")
                .replace("&c", "&e");
    }

    private static String replaceJsonColorNames(String text) {
        return text
                .replace("\"color\":\"aqua\"", "\"color\":\"blue\"")
                .replace("\"color\":\"dark_aqua\"", "\"color\":\"blue\"")
                .replace("\"color\":\"blue\"", "\"color\":\"blue\"")
                .replace("\"color\":\"dark_blue\"", "\"color\":\"blue\"")

                .replace("\"color\":\"red\"", "\"color\":\"yellow\"")
                .replace("\"color\":\"dark_red\"", "\"color\":\"yellow\"");
    }

    private static String replaceIgnoreCase(String text, String target, String replacement) {
        return Pattern.compile(Pattern.quote(target), Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .replaceAll(replacement);
    }

    private static String replaceStandaloneKingdomWord(String text, String target, String replacement) {
        return Pattern.compile("(?<!\"color\":\")\\b" + Pattern.quote(target) + "\\b")
                .matcher(text)
                .replaceAll(replacement);
    }
}