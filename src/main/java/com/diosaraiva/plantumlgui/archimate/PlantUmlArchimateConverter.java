package com.diosaraiva.plantumlgui.archimate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlantUmlArchimateConverter {

    private static final Pattern ELEMENT = Pattern.compile(
            "^\\s*([A-Z][A-Za-z]+)_([A-Za-z]+)\\s*\\(\\s*([A-Za-z0-9_]+)\\s*,\\s*\"([^\"]*)\"");

    private static final Pattern RELATION = Pattern.compile(
            "^\\s*Rel_([A-Za-z]+)\\s*\\(\\s*([A-Za-z0-9_]+)\\s*,\\s*([A-Za-z0-9_]+)\\s*"
                    + "(?:,\\s*\"([^\"]*)\")?\\s*\\)");

    private static final Map<String, String> ELEMENT_TYPES = new HashMap<>();

    private static final Map<String, String> LAYER_OF = new HashMap<>();

    static {

        putBusiness("Actor", "Role", "Collaboration", "Interface", "Process", "Function",
                "Interaction", "Event", "Service");
        map("Business_Object", "BusinessObject", "business");
        map("Business_Contract", "Contract", "business");
        map("Business_Representation", "Representation", "business");
        map("Business_Product", "Product", "business");

        putApplication("Component", "Collaboration", "Interface", "Function", "Interaction",
                "Process", "Event", "Service");
        map("Application_DataObject", "DataObject", "application");

        map("Technology_Node", "Node", "technology");
        map("Technology_Device", "Device", "technology");
        map("Technology_SystemSoftware", "SystemSoftware", "technology");
        map("Technology_Collaboration", "TechnologyCollaboration", "technology");
        map("Technology_Interface", "TechnologyInterface", "technology");
        map("Technology_Path", "Path", "technology");
        map("Technology_CommunicationNetwork", "CommunicationNetwork", "technology");
        map("Technology_Function", "TechnologyFunction", "technology");
        map("Technology_Process", "TechnologyProcess", "technology");
        map("Technology_Interaction", "TechnologyInteraction", "technology");
        map("Technology_Event", "TechnologyEvent", "technology");
        map("Technology_Service", "TechnologyService", "technology");
        map("Technology_Artifact", "Artifact", "technology");

        map("Physical_Equipment", "Equipment", "technology");
        map("Physical_Facility", "Facility", "technology");
        map("Physical_DistributionNetwork", "DistributionNetwork", "technology");
        map("Physical_Material", "Material", "technology");

        for (String t : new String[]{"Stakeholder", "Driver", "Assessment", "Goal", "Outcome",
                "Principle", "Requirement", "Constraint", "Meaning", "Value"}) {
            map("Motivation_" + t, t, "motivation");
        }

        map("Strategy_Resource", "Resource", "strategy");
        map("Strategy_Capability", "Capability", "strategy");
        map("Strategy_CourseOfAction", "CourseOfAction", "strategy");
        map("Strategy_ValueStream", "ValueStream", "strategy");

        map("Implementation_WorkPackage", "WorkPackage", "implementation");
        map("Implementation_Deliverable", "Deliverable", "implementation");
        map("Implementation_Event", "ImplementationEvent", "implementation");
        map("Implementation_Plateau", "Plateau", "implementation");
        map("Implementation_Gap", "Gap", "implementation");

        map("Other_Location", "Location", "other");
        map("Other_Grouping", "Grouping", "other");
        map("Other_Junction", "Junction", "other");
    }

    private static void putBusiness(String... names) {
        for (String n : names) map("Business_" + n, "Business" + n, "business");
    }

    private static void putApplication(String... names) {
        for (String n : names) map("Application_" + n, "Application" + n, "application");
    }

    private static void map(String macro, String archiType, String layer) {
        ELEMENT_TYPES.put(macro, archiType);
        LAYER_OF.put(macro, layer);
    }

    private PlantUmlArchimateConverter() { }

    public record Result(ArchimateExchangeModel model, List<String> warnings) { }

    public static Result convert(String plantUmlSource, String modelName) {
        ArchimateExchangeModel model = new ArchimateExchangeModel(modelName);
        List<String> warnings = new ArrayList<>();
        Map<String, String> idByAlias = new HashMap<>();

        boolean sawArchimateInclude = false;

        for (String raw : plantUmlSource.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("'") || line.startsWith("@")
                    || line.startsWith("title") || line.startsWith("!")) {
                if (line.contains("archimate/Archimate")) {
                    sawArchimateInclude = true;
                }
                continue;
            }

            Matcher rel = RELATION.matcher(line);
            if (rel.find()) {

                relationBuffer(warnings, idByAlias, model, rel);
                continue;
            }

            Matcher el = ELEMENT.matcher(line);
            if (el.find()) {
                String macro = el.group(1) + "_" + el.group(2);
                String alias = el.group(3);
                String name = el.group(4);
                String archiType = ELEMENT_TYPES.get(macro);
                if (archiType == null) {
                    warnings.add("Unmapped element macro '" + macro + "' - skipped: " + line);
                    continue;
                }
                String id = ArchimateExchangeModel.newId();
                idByAlias.put(alias, id);
                model.addElement(archiType, id, name, LAYER_OF.getOrDefault(macro, "other"));
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

    private static void relationBuffer(List<String> warnings, Map<String, String> idByAlias,
            ArchimateExchangeModel model, Matcher rel) {
        String type = rel.group(1) + "Relationship";
        String srcAlias = rel.group(2);
        String tgtAlias = rel.group(3);
        String label = rel.group(4);

        String src = idByAlias.get(srcAlias);
        String tgt = idByAlias.get(tgtAlias);
        if (src == null || tgt == null) {
            warnings.add("Relationship references unknown element(s) '"
                    + (src == null ? srcAlias : tgtAlias) + "' - skipped.");
            return;
        }
        model.addRelationship(type, ArchimateExchangeModel.newId(), src, tgt, label);
    }
}
