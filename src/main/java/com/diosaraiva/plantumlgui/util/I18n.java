package com.diosaraiva.plantumlgui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class I18n {

    private static final String BUNDLE = "i18n.messages";

    public static final Locale EN_US = Locale.of("en", "US");
    public static final Locale PT_BR = Locale.of("pt", "BR");
    public static final Locale ES_ES = Locale.of("es", "ES");

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

    public static void setLocale(Locale locale) {
        current = locale == null ? EN_US : locale;
        bundle = load(current);
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            return "!" + key + "!";
        }
    }

    public static String get(String key, Object... args) {
        var pattern = get(key);
        return java.text.MessageFormat.format(pattern, args);
    }

    private static ResourceBundle load(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE, locale, UTF8_CONTROL);
    }
}
