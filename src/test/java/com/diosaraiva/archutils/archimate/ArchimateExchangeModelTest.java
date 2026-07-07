package com.diosaraiva.archutils.archimate;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal, dependency-free unit tests for {@link ArchimateExchangeModel} and
 * {@link PlantUmlArchimateConverter}.
 *
 * <p>The project has no build tool / JUnit on the classpath, so this test is a
 * self-verifying program: run its {@link #main(String[])} method (e.g. from
 * Eclipse "Run As &gt; Java Application"). It prints one line per check and
 * exits with a non-zero status if any assertion fails.
 */
public final class ArchimateExchangeModelTest {

    private static int failures;

    public static void main(String[] args) throws Exception {
        producesValidRootAndFolders();
        usesCorrectXsiTypes();
        generatesUniqueIds();
        writesFileToDisk();
        convertsPlantUmlArchimateSource();
        reportsWarningsForNonArchimateSource();

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** Root element is {@code archimate:model} and standard folders exist. */
    private static void producesValidRootAndFolders() {
        var model = new ArchimateExchangeModel("MyModel");
        String xml = model.toXmlString();
        check("root is archimate:model", xml.contains("<archimate:model"));
        check("declares archimate namespace",
                xml.contains("http://www.archimatetool.com/archimate"));
        check("declares xsi namespace",
                xml.contains("http://www.w3.org/2001/XMLSchema-instance"));
        check("has business folder", xml.contains("type=\"business\""));
        check("has application folder", xml.contains("type=\"application\""));
        check("has technology folder", xml.contains("type=\"technology\""));
        check("has relations folder", xml.contains("type=\"relations\""));
        check("has views folder", xml.contains("type=\"diagrams\""));
        check("well-formed XML", isWellFormed(xml));
    }

    /** Elements and relationships carry the correct {@code xsi:type}. */
    private static void usesCorrectXsiTypes() {
        var model = new ArchimateExchangeModel("Types");
        model.addElement("BusinessActor", "id-actor-1", "Customer", "business");
        model.addRelationship("AssignmentRelationship", "id-rel-1", "id-actor-1", "id-proc-1");
        String xml = model.toXmlString();
        check("element xsi:type is archimate:BusinessActor",
                xml.contains("xsi:type=\"archimate:BusinessActor\""));
        check("relationship xsi:type is archimate:AssignmentRelationship",
                xml.contains("xsi:type=\"archimate:AssignmentRelationship\""));
        check("relationship has source", xml.contains("source=\"id-actor-1\""));
        check("relationship has target", xml.contains("target=\"id-proc-1\""));
        check("element retains id", xml.contains("id=\"id-actor-1\""));
        check("still well-formed with concepts", isWellFormed(xml));
    }

    /** Generated ids are unique across a large batch. */
    private static void generatesUniqueIds() {
        var seen = new java.util.HashSet<String>();
        boolean unique = true;
        for (int i = 0; i < 10_000; i++) {
            if (!seen.add(ArchimateExchangeModel.newId())) {
                unique = false;
                break;
            }
        }
        check("10k generated ids are unique", unique);
        check("id has expected prefix", ArchimateExchangeModel.newId().startsWith("id-"));
    }

    /** {@link ArchimateExchangeModel#writeTo(File)} produces a readable file. */
    private static void writesFileToDisk() throws Exception {
        var model = new ArchimateExchangeModel("Disk");
        model.addElement("ApplicationComponent", "id-app-1", "Shop", "application");
        File tmp = File.createTempFile("archimate-test", ".xml");
        tmp.deleteOnExit();
        model.writeTo(tmp);
        String content = Files.readString(tmp.toPath());
        check("file is non-empty", content.length() > 0);
        check("file content is well-formed", isWellFormed(content));
        check("file contains element", content.contains("xsi:type=\"archimate:ApplicationComponent\""));
    }

    /** The PlantUML converter maps macros to ArchiMate concepts. */
    private static void convertsPlantUmlArchimateSource() {
        String puml = """
                @startuml
                !include <archimate/Archimate>
                Business_Actor(customer, "Customer")
                Business_Process(onboarding, "Customer Onboarding")
                Rel_Assignment(customer, onboarding, "performs")
                @enduml
                """;
        var result = PlantUmlArchimateConverter.convert(puml, "Sample");
        check("converted 2 elements", result.model().getElementCount() == 2);
        check("converted 1 relationship", result.model().getRelationshipCount() == 1);
        String xml = result.model().toXmlString();
        check("has BusinessActor", xml.contains("xsi:type=\"archimate:BusinessActor\""));
        check("has BusinessProcess", xml.contains("xsi:type=\"archimate:BusinessProcess\""));
        check("has AssignmentRelationship",
                xml.contains("xsi:type=\"archimate:AssignmentRelationship\""));
        check("no warnings for valid archimate source", result.warnings().isEmpty());
    }

    /** Non-ArchiMate source yields warnings, not silent guesses. */
    private static void reportsWarningsForNonArchimateSource() {
        String puml = """
                @startuml
                Alice -> Bob : Hello
                @enduml
                """;
        var result = PlantUmlArchimateConverter.convert(puml, "NotArchimate");
        check("no elements for non-archimate source", result.model().getElementCount() == 0);
        check("emits warnings for non-archimate source", !result.warnings().isEmpty());
    }

    // -------------------- helpers --------------------

    private static final Pattern XSI_TYPE = Pattern.compile("xsi:type=\"([^\"]+)\"");

    private static boolean isWellFormed(String xml) {
        try {
            var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.newDocumentBuilder().parse(
                    new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            // sanity: every xsi:type should be namespaced with archimate:
            Matcher m = XSI_TYPE.matcher(xml);
            while (m.find()) {
                if (!m.group(1).startsWith("archimate:")) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            System.out.println("  XML parse error: " + ex.getMessage());
            return false;
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

    private ArchimateExchangeModelTest() { }
}
