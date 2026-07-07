package com.diosaraiva.archutils.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Single entry point for all UI-facing strings.
 *
 * <p>UI classes call {@link #get(String)} / {@link #get(String, Object...)}
 * instead of hard-coding text or instantiating {@link ResourceBundle} ad hoc.
 * The active {@link Locale} is switchable at runtime via {@link #setLocale}.
 */
public final class I18n {

    private static final String BUNDLE = "i18n.messages";

    // Locales offered in Settings; order drives the menu.
    public static final Locale EN_US = Locale.of("en", "US");
    public static final Locale PT_BR = Locale.of("pt", "BR");
    public static final Locale ES_ES = Locale.of("es", "ES");

    // Reads .properties strictly as UTF-8 so accented text is deterministic and
    // never falls back to ISO-8859-1 on a single malformed byte.
    private static final ResourceBundle.Control UTF8_CONTROL = new ResourceBundle.Control() {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                ClassLoader loader, boolean reload) throws IOException {
            var resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            try (InputStream in = loader.getResourceAsStream(resourceName)) {
                if (in == null) {
                    return null;
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
        }
    };

    private static Locale current = EN_US;
    private static ResourceBundle bundle = load(current);

    private I18n() { }

    public static Locale getLocale() { return current; }

    // Switches the active locale and reloads the bundle. Callers must re-render
    // UI text afterwards (menus/labels) since bundles are read eagerly per lookup.
    public static void setLocale(Locale locale) {
        current = locale == null ? EN_US : locale;
        bundle = load(current);
    }

    // Resolves a key; returns the key itself (wrapped) when missing so gaps are
    // visible in the UI instead of throwing.
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            return "!" + key + "!";
        }
    }

    // Resolves a key and substitutes {0}, {1}... arguments.
    public static String get(String key, Object... args) {
        var pattern = get(key);
        return java.text.MessageFormat.format(pattern, args);
    }

    private static ResourceBundle load(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE, locale, UTF8_CONTROL);
    }
}
