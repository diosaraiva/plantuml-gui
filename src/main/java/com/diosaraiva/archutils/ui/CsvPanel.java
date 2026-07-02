package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.service.CsvJsonConverter;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Panel for live CSV / JSON / Markdown editing with a three-column layout.
 * Editing CSV or JSON automatically updates the other columns.
 */
public class CsvPanel extends JPanel {

    private static final String SAMPLE_CSV =
            "name,age,city\nAlice,30,New York\nBob,25,London";

    private final JTextArea csvArea;
    private final JTextArea jsonArea;
    private final JTextArea markdownArea;
    private boolean updating;

    public CsvPanel() {
        csvArea = new JTextArea();
        jsonArea = new JTextArea();
        markdownArea = new JTextArea();
        initComponents();
        csvArea.setText(SAMPLE_CSV);
    }

    private void initComponents() {
        setLayout(new GridLayout(1, 3, 12, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(createColumn("CSV", csvArea));
        add(createColumn("JSON", jsonArea));
        add(createColumn("Markdown", markdownArea));
        markdownArea.setEditable(false);
        csvArea.getDocument().addDocumentListener(SwingUtils.onDocumentChange(() -> sync(true)));
        jsonArea.getDocument().addDocumentListener(SwingUtils.onDocumentChange(() -> sync(false)));
    }

    private JPanel createColumn(String title, JTextArea area) {
        JPanel col = new JPanel(new BorderLayout(0, 4));
        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        col.add(header, BorderLayout.NORTH);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setLineWrap(false);
        col.add(new JScrollPane(area), BorderLayout.CENTER);
        return col;
    }

    /** Regenerates the derived columns from whichever source was edited. */
    private void sync(boolean fromCsv) {
        if (updating) {
            return;
        }
        updating = true;
        try {
            if (fromCsv) {
                jsonArea.setText(CsvJsonConverter.csvToJson(csvArea.getText()));
            } else {
                csvArea.setText(CsvJsonConverter.jsonToCsv(jsonArea.getText()));
            }
            markdownArea.setText(CsvJsonConverter.csvToMarkdown(csvArea.getText()));
        } catch (Exception ignored) {
            // Malformed intermediate input while typing – ignore until valid.
        } finally {
            updating = false;
        }
    }
}
