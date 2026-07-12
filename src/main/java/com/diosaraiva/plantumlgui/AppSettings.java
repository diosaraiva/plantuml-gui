package com.diosaraiva.plantumlgui;

import java.util.Locale;
import java.util.prefs.Preferences;

import com.diosaraiva.plantumlgui.util.I18n;

public final class AppSettings {

    private static final Preferences PREFS =
            Preferences.userRoot().node("com/diosaraiva/plantumlgui");

    private static final String KEY_LANGUAGE = "language";

    private AppSettings() { }

    public static Locale getLanguage() {
        var tag = PREFS.get(KEY_LANGUAGE, "en-US");
        var locale = Locale.forLanguageTag(tag);
        return locale.getLanguage().isEmpty() ? I18n.EN_US : locale;
    }

    public static void setLanguage(Locale locale) {
        PREFS.put(KEY_LANGUAGE, locale.toLanguageTag());
    }
}
