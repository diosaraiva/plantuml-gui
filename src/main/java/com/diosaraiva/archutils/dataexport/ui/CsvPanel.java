package com.diosaraiva.archutils.dataexport.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.diosaraiva.archutils.dataexport.CsvExportService;
import com.diosaraiva.archutils.dataexport.JsonExportService;
import com.diosaraiva.archutils.dataexport.MarkdownExportService;
import com.diosaraiva.archutils.dataexport.TableData;
import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.ui.SwingUtils;
import com.diosaraiva.archutils.ui.TextLineNumber;

public class CsvPanel extends JPanel {

    private static final String SAMPLE_CSV = """
            name,age,city
            Alice,30,New York
            Bob,25,London""";

    private static final CsvExportService CSV = new CsvExportService();
    private static final JsonExportService JSON = new JsonExportService();
    private static final MarkdownExportService MARKDOWN = new MarkdownExportService();

    private final JTextArea csvArea;
    private final JTextArea jsonArea;
    private final JTextArea markdownArea;

    private final JLabel csvHeader = createHeader(I18n.get("csv.col.csv"));
    private final JLabel jsonHeader = createHeader(I18n.get("csv.col.json"));
    private final JLabel markdownHeader = createHeader(I18n.get("csv.col.markdown"));

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
        add(createColumn(csvHeader, csvArea));
        add(createColumn(jsonHeader, jsonArea));
        add(createColumn(markdownHeader, markdownArea));
        markdownArea.setEditable(false);
        csvArea.getDocument().addDocumentListener(SwingUtils.onDocumentChange(() -> sync(true)));
        jsonArea.getDocument().addDocumentListener(SwingUtils.onDocumentChange(() -> sync(false)));
    }

    public void applyLanguage() {
        csvHeader.setText(I18n.get("csv.col.csv"));
        jsonHeader.setText(I18n.get("csv.col.json"));
        markdownHeader.setText(I18n.get("csv.col.markdown"));
        repaint();
    }

    private static JLabel createHeader(String title) {
        var header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        return header;
    }

    private JPanel createColumn(JLabel header, JTextArea area) {
        var col = new JPanel(new BorderLayout(0, 4));
        col.add(header, BorderLayout.NORTH);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setLineWrap(false);
        var scrollPane = new JScrollPane(area);

        scrollPane.setRowHeaderView(new TextLineNumber(area));
        col.add(scrollPane, BorderLayout.CENTER);
        return col;
    }

    private void sync(boolean fromCsv) {
        if (updating) {
            return;
        }
        updating = true;
        try {
            if (fromCsv) {
                var data = TableData.fromCsv(csvArea.getText());
                jsonArea.setText(JSON.format(data));
            } else {
                var data = TableData.fromJson(jsonArea.getText());
                csvArea.setText(CSV.format(data));
            }
            markdownArea.setText(MARKDOWN.format(TableData.fromCsv(csvArea.getText())));
        } catch (Exception ignored) {

        } finally {
            updating = false;
        }
    }
}
