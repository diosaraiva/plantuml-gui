package com.diosaraiva.archutils.i18n;

// Self-verifying tests for I18n key resolution across the three locales,
// argument substitution, and missing-key handling.
public final class I18nTest {

    private static int failures;

    public static void main(String[] args) {
        resolvesPerLocale();
        substitutesArguments();
        missingKeyIsVisible();
        allLocalesCoverCoreKeys();

        // Restore default so test order never leaks locale state.
        I18n.setLocale(I18n.EN_US);

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void resolvesPerLocale() {
        I18n.setLocale(I18n.EN_US);
        check("en menu.file", I18n.get("menu.file").equals("File"));
        I18n.setLocale(I18n.PT_BR);
        check("pt menu.file", I18n.get("menu.file").equals("Arquivo"));
        I18n.setLocale(I18n.ES_ES);
        check("es menu.file", I18n.get("menu.file").equals("Archivo"));
    }

    private static void substitutesArguments() {
        I18n.setLocale(I18n.EN_US);
        check("en args", I18n.get("console.compile.ok", 0).contains("exit 0"));
        I18n.setLocale(I18n.PT_BR);
        check("pt args", I18n.get("console.compile.ok", 0).contains("saída 0"));
        I18n.setLocale(I18n.ES_ES);
        check("es args", I18n.get("console.compile.ok", 0).contains("salida 0"));
    }

    private static void missingKeyIsVisible() {
        I18n.setLocale(I18n.EN_US);
        check("missing key wrapped", I18n.get("no.such.key").equals("!no.such.key!"));
    }

    // Every locale must resolve the core keys (no !key! placeholders leaking).
    private static void allLocalesCoverCoreKeys() {
        var keys = new String[]{
                "app.title", "menu.settings.language", "menu.settings.console",
                "export.button", "tab.preview", "tab.console",
                "plantuml.code.empty", "archimate.export.finished", "copy.none"
        };
        for (var locale : new java.util.Locale[]{I18n.EN_US, I18n.PT_BR, I18n.ES_ES}) {
            I18n.setLocale(locale);
            for (var key : keys) {
                var value = I18n.get(key);
                check(locale.toLanguageTag() + " has " + key,
                        !value.startsWith("!") && !value.isBlank());
            }
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("PASS: " + name);
        } else {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private I18nTest() { }
}
