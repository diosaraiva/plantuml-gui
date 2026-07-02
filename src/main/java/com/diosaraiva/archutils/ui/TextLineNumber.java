package com.diosaraiva.archutils.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * A lightweight line-number gutter that is painted alongside a
 * {@link JTextComponent} (typically inside a {@link javax.swing.JScrollPane}'s
 * row header). Line numbers stay in sync when text is typed, pasted, deleted or
 * scrolled, and the component mirrors the editor's font so numbers align with
 * their lines.
 */
public class TextLineNumber extends JComponent
        implements CaretListener, DocumentListener {

    private static final int MARGIN = 6;

    private final JTextComponent editor;
    private int lastDigits;

    /**
     * Creates a line-number gutter bound to the given text component.
     *
     * @param editor the text component whose lines are numbered
     */
    public TextLineNumber(JTextComponent editor) {
        this.editor = editor;
        setFont(editor.getFont());
        setForeground(new Color(120, 120, 120));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)));

        editor.getDocument().addDocumentListener(this);
        editor.addCaretListener(this);

        // Keep the gutter font in sync if the editor font changes.
        editor.addPropertyChangeListener("font", evt -> {
            setFont(editor.getFont());
            documentChanged();
        });

        updatePreferredWidth();
    }

    private int getLineCount() {
        Element root = editor.getDocument().getDefaultRootElement();
        return root.getElementCount();
    }

    private void updatePreferredWidth() {
        int lines = getLineCount();
        int digits = Math.max(String.valueOf(lines).length(), 2);
        if (digits == lastDigits) {
            return;
        }
        lastDigits = digits;
        FontMetrics fm = getFontMetrics(getFont());
        int width = fm.charWidth('0') * digits + MARGIN * 2;
        Dimension size = new Dimension(width, Integer.MAX_VALUE - 1000000);
        setPreferredSize(size);
        setSize(size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        FontMetrics fm = editor.getFontMetrics(editor.getFont());
        g.setFont(editor.getFont());

        Rectangle clip = g.getClipBounds();
        Insets insets = getInsets();
        int availableWidth = getSize().width - insets.right - MARGIN;

        // Determine the range of offsets currently visible in the clip.
        int startOffset = editor.viewToModel2D(new java.awt.Point(0, clip.y));
        int endOffset = editor.viewToModel2D(new java.awt.Point(0, clip.y + clip.height));

        Element root = editor.getDocument().getDefaultRootElement();
        int startLine = root.getElementIndex(startOffset);
        int endLine = root.getElementIndex(endOffset);

        for (int line = startLine; line <= endLine; line++) {
            try {
                Element lineElement = root.getElement(line);
                int lineStart = lineElement.getStartOffset();
                Rectangle r = editor.modelToView2D(lineStart).getBounds();
                if (r == null) {
                    continue;
                }
                String number = String.valueOf(line + 1);
                int stringWidth = fm.stringWidth(number);
                int x = availableWidth - stringWidth;
                int y = r.y + fm.getAscent();
                g.setColor(getForeground());
                g.drawString(number, x, y);
            } catch (BadLocationException ex) {
                // Line no longer valid – skip it.
            }
        }
    }

    // -------------------- listeners --------------------

    private void documentChanged() {
        SwingUtilities.invokeLater(() -> {
            updatePreferredWidth();
            revalidate();
            repaint();
        });
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        repaint();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        documentChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        documentChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        documentChanged();
    }
}
