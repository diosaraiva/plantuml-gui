package com.diosaraiva.archutils.i18n;

import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * Persistent application settings backed by {@link Preferences}.
 *
 * <p>Currently stores the UI language tag. Kept tiny and dependency-free so any
 * class can read/write a setting without wiring a config file.
 */
public final class AppSettings {

    private static final Preferences PREFS =
            Preferences.userRoot().node("com/diosaraiva/archutils");

    private static final String KEY_LANGUAGE = "language";

    private AppSettings() { }

    // Reads the stored locale, defaulting to English (US) on first run.
    public static Locale getLanguage() {
        var tag = PREFS.get(KEY_LANGUAGE, "en-US");
        var locale = Locale.forLanguageTag(tag);
        return locale.getLanguage().isEmpty() ? I18n.EN_US : locale;
    }

    // Persists the selected locale as a BCP-47 language tag.
    public static void setLanguage(Locale locale) {
        PREFS.put(KEY_LANGUAGE, locale.toLanguageTag());
    }
}
