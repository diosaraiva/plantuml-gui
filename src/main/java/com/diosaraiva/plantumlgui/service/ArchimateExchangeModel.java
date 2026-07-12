package com.diosaraiva.plantumlgui.service;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class ArchimateExchangeModel {

    private static final String ARCHIMATE_NS = "http://www.archimatetool.com/archimate";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    private record FolderDef(String type, String displayName) { }

    private static final Map<String, FolderDef> LAYER_FOLDERS = new LinkedHashMap<>();
    static {
        LAYER_FOLDERS.put("strategy", new FolderDef("strategy", "Strategy"));
        LAYER_FOLDERS.put("business", new FolderDef("business", "Business"));
        LAYER_FOLDERS.put("application", new FolderDef("application", "Application"));
        LAYER_FOLDERS.put("technology", new FolderDef("technology", "Technology"));
        LAYER_FOLDERS.put("motivation", new FolderDef("motivation", "Motivation"));
        LAYER_FOLDERS.put("implementation",
                new FolderDef("implementation_migration", "Implementation & Migration"));
        LAYER_FOLDERS.put("other", new FolderDef("other", "Other"));
    }

    private final Document document;
    private final Element root;
    private final Map<String, Element> foldersByLayer = new LinkedHashMap<>();
    private final Element relationsFolder;

    private int elementCount;
    private int relationshipCount;

    public ArchimateExchangeModel(String modelName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            this.document = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException("Unable to create XML document builder", ex);
        }
        this.document.setXmlStandalone(true);

        this.root = document.createElementNS(ARCHIMATE_NS, "archimate:model");
        root.setAttribute("xmlns:xsi", XSI_NS);
        root.setAttribute("xmlns:archimate", ARCHIMATE_NS);
        root.setAttribute("name", modelName == null || modelName.isBlank() ? "Model" : modelName);
        root.setAttribute("id", newId());
        root.setAttribute("version", "5.0.0");
        document.appendChild(root);

        for (var entry : LAYER_FOLDERS.entrySet()) {
            foldersByLayer.put(entry.getKey(), createFolder(entry.getValue()));
        }
        this.relationsFolder = createFolder(new FolderDef("relations", "Relations"));
        createFolder(new FolderDef("diagrams", "Views"));
    }

    public void addElement(String type, String id, String name, String layer) {
        Element parent = foldersByLayer.getOrDefault(normalizeLayer(layer),
                foldersByLayer.get("other"));
        Element element = document.createElement("element");
        element.setAttributeNS(XSI_NS, "xsi:type", "archimate:" + type);
        element.setAttribute("name", name == null ? "" : name);
        element.setAttribute("id", id);
        parent.appendChild(element);
        elementCount++;
    }

    public void addRelationship(String type, String id, String source, String target) {
        addRelationship(type, id, source, target, null);
    }

    public void addRelationship(String type, String id, String source, String target, String name) {
        Element rel = document.createElement("element");
        rel.setAttributeNS(XSI_NS, "xsi:type", "archimate:" + type);
        if (name != null && !name.isBlank()) {
            rel.setAttribute("name", name);
        }
        rel.setAttribute("id", id);
        rel.setAttribute("source", source);
        rel.setAttribute("target", target);
        relationsFolder.appendChild(rel);
        relationshipCount++;
    }

    public int getElementCount() { return elementCount; }

    public int getRelationshipCount() { return relationshipCount; }

    public void writeTo(File outputFile) throws IOException {
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            Files.createDirectories(outputFile.getParentFile().toPath());
        }
        try (Writer writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            writeTo(writer);
        }
    }

    public void writeTo(Writer writer) throws IOException {
        try {
            newTransformer().transform(new DOMSource(document), new StreamResult(writer));
        } catch (TransformerException ex) {
            throw new IOException("Failed to serialise ArchiMate model", ex);
        }
    }

    public String toXmlString() {
        StringWriter sw = new StringWriter();
        try {
            writeTo(sw);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        return sw.toString();
    }

    private Element createFolder(FolderDef def) {
        Element folder = document.createElement("folder");
        folder.setAttribute("name", def.displayName());
        folder.setAttribute("id", newId());
        folder.setAttribute("type", def.type());
        root.appendChild(folder);
        return folder;
    }

    private static String normalizeLayer(String layer) {
        if (layer == null) return "other";
        String key = layer.trim().toLowerCase();
        return LAYER_FOLDERS.containsKey(key) ? key : "other";
    }

    public static String newId() {
        return "id-" + UUID.randomUUID().toString().replace("-", "");
    }

    private static Transformer newTransformer() throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        return transformer;
    }
}
