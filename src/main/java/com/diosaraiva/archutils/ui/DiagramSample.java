package com.diosaraiva.archutils.ui;

/**
 * Available PlantUML diagram samples with their resource paths.
 */
public enum DiagramSample {

    ACTIVITY("Activity", "activity.puml"),
    ARCHIMATE_APPLICATION("Archimate Application", "archimate_application.puml"),
    ARCHIMATE_BUSINESS("Archimate Business", "archimate_business.puml"),
    ARCHIMATE_IMPLEMENTATION("Archimate Implementation", "archimate_implementation.puml"),
    ARCHIMATE_LAYERED("Archimate Layered", "archimate_layered.puml"),
    ARCHIMATE_MOTIVATION("Archimate Motivation", "archimate_motivation.puml"),
    ARCHIMATE_PHYSICAL("Archimate Physical", "archimate_physical.puml"),
    ARCHIMATE_STRATEGY("Archimate Strategy", "archimate_strategy.puml"),
    ARCHIMATE_TECHNOLOGY("Archimate Technology", "archimate_technology.puml"),
    C4_COMPONENT("C4 Component", "c4_component.puml"),
    C4_CONTAINER("C4 Container", "c4_container.puml"),
    C4_CONTEXT("C4 Context", "c4_context.puml"),
    C4_DEPLOYMENT("C4 Deployment", "c4_deployment.puml"),
    CLASS("Class", "class.puml"),
    COMPONENT("Component", "component.puml"),
    DEPLOYMENT("Deployment", "deployment.puml"),
    GANTT("Gantt", "gantt.puml"),
    MINDMAP("Mind Map", "mindmap.puml"),
    OBJECT("Object", "object.puml"),
    SEQUENCE("Sequence", "sequence.puml"),
    STATE("State", "state.puml"),
    TIMING("Timing", "timing.puml"),
    USE_CASE("Use Case", "usecase.puml"),
	CUSTOM_ARCHIMATE("Custom Archimate", "custom_archimate.puml"),
	CUSTOM_MODULAR("Custom Modular", "custom_modular.puml"),
	UTILS_COLORS("List Available [colors]", "util_colors.puml"),
	UTILS_OPENICONIC("List Available [icons]", "util_openiconic.puml"),
	UTILS_SKINPARAMS("List Available [skinparams]", "util_skinparams.puml"),
	UTILS_SPRITES("List Available [sprites]", "util_sprites.puml");

    private final String displayName;
    private final String fileName;

    DiagramSample(String displayName, String fileName) {
        this.displayName = displayName;
        this.fileName = fileName;
    }

    public String getFileName() { return fileName; }

    @Override
    public String toString() { return displayName; }
}
