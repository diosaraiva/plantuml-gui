package com.diosaraiva.archutils.archimate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builder for the <strong>ArchiMate Model Exchange</strong> XML consumed by
 * <a href="https://www.archimatetool.com/">Archi</a> (its native
 * {@code .archimate} format, aligned with the ArchiMate 3.x metamodel).
 *
 * <p>The output uses the {@code archimate:model} root element, groups concepts
 * into {@code <folder>} elements (business / application / technology /
 * relations / views &hellip;) and tags every concept with an
 * {@code xsi:type="archimate:&lt;Type&gt;"} attribute and a unique {@code id}.
 *
 * <p>No external dependency (nor JAXB, which is not shipped with the JDK) is
 * required: the document is assembled with the built-in DOM API from the
 * {@code java.xml} module. Example:
 * <pre>{@code
 * ArchimateExchangeModel model = new ArchimateExchangeModel("MyModel");
 * model.addElement("BusinessActor", "id-actor-1", "Customer", "business");
 * model.addRelationship("AssignmentRelationship", "id-rel-1", "id-actor-1", "id-process-1");
 * model.writeTo(outputFile);
 * }</pre>
 */
public final class ArchimateExchangeModel {

    /** Archi native model namespace (EMF/Ecore based). */
    private static final String ARCHIMATE_NS = "http://www.archimatetool.com/archimate";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Canonical folder layout of an Archi model. The key is the layer name
     * accepted by {@link #addElement}; the value is the {@code type} attribute
     * / display name Archi expects.
     */
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

        // Pre-create the standard layer folders so an importing tool always
        // finds the expected structure, even when a layer has no elements.
        for (var entry : LAYER_FOLDERS.entrySet()) {
            foldersByLayer.put(entry.getKey(), createFolder(entry.getValue()));
        }
        this.relationsFolder = createFolder(new FolderDef("relations", "Relations"));
        createFolder(new FolderDef("diagrams", "Views"));
    }

    /**
     * Adds an ArchiMate element to the folder matching {@code layer}.
     *
     * @param type  ArchiMate 3.x type name without prefix, e.g. {@code BusinessActor}
     * @param id    unique identifier for the element
     * @param name  human-readable label
     * @param layer target layer: business / application / technology / motivation /
     *              strategy / implementation / other (unknown values fall back to "other")
     */
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

    /**
     * Adds an ArchiMate relationship to the {@code relations} folder.
     *
     * @param type   relationship type ending in {@code Relationship},
     *               e.g. {@code AssignmentRelationship}
     * @param id     unique identifier for the relationship
     * @param source identifier of the source element
     * @param target identifier of the target element
     */
    public void addRelationship(String type, String id, String source, String target) {
        addRelationship(type, id, source, target, null);
    }

    /** Same as {@link #addRelationship(String, String, String, String)} with an optional name. */
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

    /** @return number of elements added so far. */
    public int getElementCount() { return elementCount; }

    /** @return number of relationships added so far. */
    public int getRelationshipCount() { return relationshipCount; }

    /** Serialises the model to {@code outputFile} as indented UTF-8 XML. */
    public void writeTo(File outputFile) throws IOException {
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            Files.createDirectories(outputFile.getParentFile().toPath());
        }
        try (Writer writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            writeTo(writer);
        }
    }

    /** Serialises the model to the given writer. */
    public void writeTo(Writer writer) throws IOException {
        try {
            newTransformer().transform(new DOMSource(document), new StreamResult(writer));
        } catch (TransformerException ex) {
            throw new IOException("Failed to serialise ArchiMate model", ex);
        }
    }

    /** @return the model serialised to a String (useful for tests). */
    public String toXmlString() {
        StringWriter sw = new StringWriter();
        try {
            writeTo(sw);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        return sw.toString();
    }

    // -------------------- internals --------------------

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

    /** Generates an Archi-style identifier, e.g. {@code id-1f3c...}. */
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
