package com.diosaraiva.plantumlgui.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Best-effort translation of PlantUML ArchiMate macros into an ArchimateExchangeModel.
public final class ArchimatePlantUmlConverter {

    // Layer_Type(alias, "Name", ...)
    private static final Pattern ELEMENT = Pattern.compile(
            "^\\s*([A-Z][A-Za-z]+)_([A-Za-z]+)\\s*\\(\\s*([A-Za-z0-9_]+)\\s*,\\s*\"([^\"]*)\"");

    // Rel_Type(source, target[, "Label"])
    private static final Pattern RELATION = Pattern.compile(
            "^\\s*Rel_([A-Za-z]+)\\s*\\(\\s*([A-Za-z0-9_]+)\\s*,\\s*([A-Za-z0-9_]+)\\s*"
                    + "(?:,\\s*\"([^\"]*)\")?\\s*\\)");

    private record Mapping(String archiType, String layer) { }

    // PlantUML macro -> ArchiMate type and owning layer.
    private static final Map<String, Mapping> MACROS = new HashMap<>();
    static {
        // Macros whose ArchiMate type is the layer prefix plus the suffix.
        prefixed("Business", "business", "Actor", "Role", "Collaboration", "Interface", "Process",
                "Function", "Interaction", "Event", "Service");
        prefixed("Application", "application", "Component", "Collaboration", "Interface", "Function",
                "Interaction", "Process", "Event", "Service");
        prefixed("Technology", "technology", "Collaboration", "Interface", "Function", "Process",
                "Interaction", "Event", "Service");

        // Macros whose ArchiMate type is just the suffix.
        plain("business", "Business_Object:BusinessObject", "Business_Contract:Contract",
                "Business_Representation:Representation", "Business_Product:Product");
        plain("application", "Application_DataObject:DataObject");
        plain("technology", "Technology_Node:Node", "Technology_Device:Device",
                "Technology_SystemSoftware:SystemSoftware", "Technology_Path:Path",
                "Technology_CommunicationNetwork:CommunicationNetwork", "Technology_Artifact:Artifact",
                "Physical_Equipment:Equipment", "Physical_Facility:Facility",
                "Physical_DistributionNetwork:DistributionNetwork", "Physical_Material:Material");
        plain("motivation", "Motivation_Stakeholder:Stakeholder", "Motivation_Driver:Driver",
                "Motivation_Assessment:Assessment", "Motivation_Goal:Goal", "Motivation_Outcome:Outcome",
                "Motivation_Principle:Principle", "Motivation_Requirement:Requirement",
                "Motivation_Constraint:Constraint", "Motivation_Meaning:Meaning", "Motivation_Value:Value");
        plain("strategy", "Strategy_Resource:Resource", "Strategy_Capability:Capability",
                "Strategy_CourseOfAction:CourseOfAction", "Strategy_ValueStream:ValueStream");
        plain("implementation", "Implementation_WorkPackage:WorkPackage",
                "Implementation_Deliverable:Deliverable", "Implementation_Event:ImplementationEvent",
                "Implementation_Plateau:Plateau", "Implementation_Gap:Gap");
        plain(ArchimateExchangeModel.OTHER_LAYER, "Other_Location:Location", "Other_Grouping:Grouping", "Other_Junction:Junction");
    }

    private ArchimatePlantUmlConverter() { }

    public record Result(ArchimateExchangeModel model, List<String> warnings) { }

    public static Result convert(String plantUmlSource, String modelName) {
        var model = new ArchimateExchangeModel(modelName);
        var warnings = new ArrayList<String>();
        var idByAlias = new HashMap<String, String>();
        boolean sawArchimateInclude = false;

        for (String raw : plantUmlSource.split("\\R")) {
            String line = raw.strip();
            if (isDirective(line)) {
                sawArchimateInclude |= line.contains("archimate/Archimate");
                continue;
            }

            Matcher rel = RELATION.matcher(line);
            if (rel.find()) {
                addRelationship(model, idByAlias, warnings, rel);
                continue;
            }

            Matcher element = ELEMENT.matcher(line);
            if (element.find()) {
                addElement(model, idByAlias, warnings, element, line);
                continue;
            }

            if (line.matches("^[A-Za-z].*\\(.*\\).*")) {
                warnings.add("Unrecognised declaration - skipped: " + line);
            }
        }

        if (!sawArchimateInclude) {
            warnings.add("Source does not '!include <archimate/Archimate>'; "
                    + "the diagram may not be ArchiMate-aware, results are best-effort.");
        }
        if (model.getElementCount() == 0) {
            warnings.add("No ArchiMate elements were recognised in the PlantUML source.");
        }
        return new Result(model, warnings);
    }

    private static boolean isDirective(String line) {
        return line.isEmpty() || line.startsWith("'") || line.startsWith("@")
                || line.startsWith("!") || line.startsWith("title");
    }

    private static void addElement(ArchimateExchangeModel model, Map<String, String> idByAlias,
            List<String> warnings, Matcher matcher, String line) {
        String macro = matcher.group(1) + "_" + matcher.group(2);
        Mapping mapping = MACROS.get(macro);
        if (mapping == null) {
            warnings.add("Unmapped element macro '" + macro + "' - skipped: " + line);
            return;
        }
        String id = ArchimateExchangeModel.newId();
        idByAlias.put(matcher.group(3), id);
        model.addElement(mapping.archiType(), id, matcher.group(4), mapping.layer());
    }

    private static void addRelationship(ArchimateExchangeModel model, Map<String, String> idByAlias,
            List<String> warnings, Matcher matcher) {
        String sourceAlias = matcher.group(2);
        String targetAlias = matcher.group(3);
        String source = idByAlias.get(sourceAlias);
        String target = idByAlias.get(targetAlias);
        if (source == null || target == null) {
            warnings.add("Relationship references unknown element(s) '"
                    + (source == null ? sourceAlias : targetAlias) + "' - skipped.");
            return;
        }
        model.addRelationship(matcher.group(1) + "Relationship",
                ArchimateExchangeModel.newId(), source, target, matcher.group(4));
    }

    private static void prefixed(String prefix, String layer, String... suffixes) {
        for (String suffix : suffixes) {
            MACROS.put(prefix + "_" + suffix, new Mapping(prefix + suffix, layer));
        }
    }

    private static void plain(String layer, String... macroToType) {
        for (String entry : macroToType) {
            String[] parts = entry.split(":", 2);
            MACROS.put(parts[0], new Mapping(parts[1], layer));
        }
    }
}