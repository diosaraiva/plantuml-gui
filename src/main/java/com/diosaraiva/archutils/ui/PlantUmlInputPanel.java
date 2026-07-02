package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.util.SampleLoader;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Input section: sample selector and code editor.
 * <p>
 * Provides a line-number gutter, undo/redo support (via {@link UndoManager})
 * and clipboard access for the PlantUML source text.
 */
public class PlantUmlInputPanel extends JPanel {

    private final JComboBox<DiagramSample> sampleCombo;
    private final JTextArea codeTextArea;
    private final UndoManager undoManager = new UndoManager();
    private final List<Runnable> undoStateListeners = new ArrayList<>();

    public PlantUmlInputPanel() {
        sampleCombo = new JComboBox<>(DiagramSample.values());
        sampleCombo.setSelectedItem(DiagramSample.SEQUENCE);
        codeTextArea = new JTextArea(10, 20);
        codeTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("PlantUML Input"));
        GridBagConstraints gbc = createGbc();

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel("Samples:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        sampleCombo.addActionListener(e -> loadSample());
        add(sampleCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        codeTextArea.setLineWrap(true);
        codeTextArea.setWrapStyleWord(false);
        JScrollPane scrollPane = new JScrollPane(codeTextArea);
        // Line-number gutter kept in sync with the editor.
        scrollPane.setRowHeaderView(new TextLineNumber(codeTextArea));
        add(scrollPane, gbc);

        initUndo();
        loadSample();
    }

    /** Wires the undo manager, keyboard shortcuts and state notifications. */
    private void initUndo() {
        codeTextArea.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
                fireUndoStateChanged();
            }
        });

        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        codeTextArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut), "archutils-undo");
        codeTextArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcut), "archutils-redo");
        codeTextArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut | InputEvent.SHIFT_DOWN_MASK),
                "archutils-redo");
        codeTextArea.getActionMap().put("archutils-undo", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { undo(); }
        });
        codeTextArea.getActionMap().put("archutils-redo", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { redo(); }
        });
    }

    private void loadSample() {
        DiagramSample sample = (DiagramSample) sampleCombo.getSelectedItem();
        if (sample == null) return;
        try {
            codeTextArea.setText(SampleLoader.load(sample.getFileName()));
            codeTextArea.setCaretPosition(0);
        } catch (Exception ex) {
            codeTextArea.setText("Error loading sample: " + ex.getMessage());
        }
        // A freshly loaded sample is a clean starting point, not an undoable edit.
        undoManager.discardAllEdits();
        fireUndoStateChanged();
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        return gbc;
    }

    public String getCode() { return codeTextArea.getText().trim(); }

    public void setCode(String code) {
        codeTextArea.setText(code);
        codeTextArea.setCaretPosition(0);
        undoManager.discardAllEdits();
        fireUndoStateChanged();
    }

    /** Allows external listeners to observe code changes for live preview. */
    public void addCodeDocumentListener(DocumentListener listener) {
        codeTextArea.getDocument().addDocumentListener(listener);
    }

    // -------------------- undo / redo / clipboard --------------------

    /** @return {@code true} if an undo operation is currently available. */
    public boolean canUndo() { return undoManager.canUndo(); }

    /** @return {@code true} if a redo operation is currently available. */
    public boolean canRedo() { return undoManager.canRedo(); }

    /** Performs an undo on the editor if possible. */
    public void undo() {
        try {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
        } catch (CannotUndoException ignored) {
            // Nothing to undo.
        }
        fireUndoStateChanged();
    }

    /** Performs a redo on the editor if possible. */
    public void redo() {
        try {
            if (undoManager.canRedo()) {
                undoManager.redo();
            }
        } catch (CannotRedoException ignored) {
            // Nothing to redo.
        }
        fireUndoStateChanged();
    }

    /**
     * Copies the current text selection (if any) or the full editor contents
     * to the system clipboard.
     */
    public void copyToClipboard() {
        String selected = codeTextArea.getSelectedText();
        String text = (selected != null && !selected.isEmpty())
                ? selected : codeTextArea.getText();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    /**
     * Registers a listener that is notified whenever the undo/redo availability
     * may have changed, allowing UI (e.g. menu items) to update its state.
     *
     * @param listener callback invoked on the EDT after a state change
     */
    public void addUndoStateListener(Runnable listener) {
        undoStateListeners.add(listener);
    }

    private void fireUndoStateChanged() {
        for (Runnable r : undoStateListeners) {
            r.run();
        }
    }
}

