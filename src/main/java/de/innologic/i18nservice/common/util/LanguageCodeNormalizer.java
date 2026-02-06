package de.innologic.i18nservice.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LanguageCodeNormalizer {

    private LanguageCodeNormalizer() {}

    /**
     * Normalisiert einen Locale/Language Code grob nach BCP-47-Konvention:
     * - Trenner: '_' -> '-'
     * - Sprache: lowercase (de, en, zh)
     * - Script: TitleCase (Hant, Latn)
     * - Region: UPPERCASE (DE, US) oder digits (419)
     * - Rest (variants/private-use): unverändert (oder leicht bereinigt)
     * <p/>
     * Beispiele:
     *  - "de-de" -> "de-DE"
     *  - "zh-hant-tw" -> "zh-Hant-TW"
     *  - "sr_Cyrl_rs" -> "sr-Cyrl-RS"
     */
    public static String normalize(String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isEmpty()) return null;

        s = s.replace('_', '-');

        // Mehrfach '-' bereinigen
        while (s.contains("--")) s = s.replace("--", "-");
        if (s.startsWith("-") || s.endsWith("-")) {
            s = trimDashes(s);
        }

        String[] parts = s.split("-");
        if (parts.length == 0 || parts[0].isBlank()) return null;

        List<String> out = new ArrayList<>(parts.length);

        // 1) language
        out.add(parts[0].toLowerCase(Locale.ROOT));

        // 2..n) script/region/variants/private-use
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p == null || p.isBlank()) continue;

            if (isScript(p)) {
                out.add(toTitleCase(p));
            } else if (isRegionAlpha2(p)) {
                out.add(p.toUpperCase(Locale.ROOT));
            } else if (isRegionNumeric3(p)) {
                out.add(p); // z.B. 419
            } else {
                // variants/private-use: keine harte Veränderung, aber trim
                out.add(p.trim());
            }
        }

        return String.join("-", out);
    }

    /**
     * Sehr einfache Plausibilitätsprüfung (nicht vollständiges BCP-47!).
     * Reicht für P0: "de", "de-DE", "zh-Hant-TW", "es-419"
     */
    public static boolean isPlausible(String raw) {
        String n = normalize(raw);
        if (n == null) return false;

        String[] parts = n.split("-");
        if (parts.length == 0) return false;

        // language 2-3 letters (P0)
        String lang = parts[0];
        if (!lang.matches("^[a-z]{2,3}$")) return false;

        // weitere Tags sehr tolerant
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.isBlank()) return false;
            if (!p.matches("^[A-Za-z0-9]{2,8}$")) return false;
        }
        return true;
    }

    private static boolean isScript(String p) {
        return p.length() == 4 && p.matches("^[A-Za-z]{4}$");
    }

    private static boolean isRegionAlpha2(String p) {
        return p.length() == 2 && p.matches("^[A-Za-z]{2}$");
    }

    private static boolean isRegionNumeric3(String p) {
        return p.length() == 3 && p.matches("^[0-9]{3}$");
    }

    private static String toTitleCase(String p) {
        String lower = p.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String trimDashes(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '-') start++;
        while (end > start && s.charAt(end - 1) == '-') end--;
        return s.substring(start, end);
    }
}
