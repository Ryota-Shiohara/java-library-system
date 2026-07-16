package library.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

public final class UiStyles {
    private static final Logger LOGGER = Logger.getLogger(UiStyles.class.getName());

    public static final Color PAGE_BACKGROUND = new Color(243, 246, 250);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color ALTERNATE_BACKGROUND = new Color(248, 250, 253);
    public static final Color APP_BAR_BACKGROUND = new Color(24, 43, 68);
    public static final Color APP_BAR_TEXT = new Color(250, 252, 255);
    public static final Color APP_BAR_MUTED_TEXT = new Color(176, 192, 212);
    public static final Color PRIMARY = new Color(43, 99, 166);
    public static final Color PRIMARY_DARK = new Color(29, 70, 119);
    public static final Color PRIMARY_TINT = new Color(228, 237, 248);
    public static final Color TEXT = new Color(31, 42, 56);
    public static final Color MUTED_TEXT = new Color(96, 112, 132);
    public static final Color BORDER = new Color(213, 221, 231);
    public static final Color DANGER = new Color(160, 62, 54);
    public static final Color DANGER_BACKGROUND = new Color(247, 235, 232);
    public static final Color SUCCESS = new Color(42, 108, 82);
    public static final Color SUCCESS_BACKGROUND = new Color(229, 240, 234);
    public static final Color WARNING = new Color(145, 98, 25);
    public static final Color WARNING_BACKGROUND = new Color(248, 239, 219);

    private UiStyles() { }

    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException exception) {
            LOGGER.log(Level.FINE, "System look and feel is unavailable. Using the Java default.", exception);
        }
        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("Table.showVerticalLines", Boolean.FALSE);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.selectionBackground", PRIMARY_TINT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
        UIManager.put("TextField.selectionBackground", PRIMARY_TINT);
        UIManager.put("TextField.selectionForeground", TEXT);
    }

    public static JPanel card() {
        JPanel panel = new SurfacePanel();
        panel.setBorder(cardBorder());
        return panel;
    }

    public static Border cardBorder() {
        return BorderFactory.createEmptyBorder(15, 17, 15, 17);
    }

    public static Border compactCardBorder() {
        return BorderFactory.createEmptyBorder(9, 13, 9, 13);
    }

    public static JLabel titleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 25f));
        return label;
    }

    public static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        return label;
    }

    public static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED_TEXT);
        return label;
    }

    public static JLabel toolbarLabel(String text) {
        JLabel label = mutedLabel(text.toUpperCase(java.util.Locale.ROOT));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 5));
        return label;
    }

    public static JPanel metricCard(String labelText, JLabel valueLabel) {
        JPanel panel = card();
        panel.setLayout(new java.awt.BorderLayout(0, 4));
        JLabel label = mutedLabel(labelText);
        valueLabel.setForeground(TEXT);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 20f));
        panel.add(label, java.awt.BorderLayout.NORTH);
        panel.add(valueLabel, java.awt.BorderLayout.CENTER);
        return panel;
    }

    public static JPanel metric(String labelText, JLabel valueLabel, boolean separated) {
        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 3));
        panel.setOpaque(false);
        Border spacing = BorderFactory.createEmptyBorder(2, separated ? 18 : 2, 2, 18);
        panel.setBorder(separated
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER), spacing)
                : spacing);
        JLabel label = mutedLabel(labelText.toUpperCase(java.util.Locale.ROOT));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        valueLabel.setForeground(TEXT);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 20f));
        panel.add(label, java.awt.BorderLayout.NORTH);
        panel.add(valueLabel, java.awt.BorderLayout.CENTER);
        return panel;
    }

    public static JButton primaryButton(String text) {
        return new StyledButton(text, ButtonStyle.PRIMARY);
    }

    public static JButton secondaryButton(String text) {
        return new StyledButton(text, ButtonStyle.SECONDARY);
    }

    public static JButton dangerButton(String text) {
        return new StyledButton(text, ButtonStyle.DANGER);
    }

    public static JButton quietButton(String text) {
        return new StyledButton(text, ButtonStyle.QUIET);
    }

    public static void configureTable(JTable table) {
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(35);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(PRIMARY_TINT);
        table.setSelectionForeground(TEXT);
        table.setBackground(CARD_BACKGROUND);
        table.setForeground(TEXT);
        table.setFont(table.getFont().deriveFont(13f));
        table.setDefaultRenderer(Object.class, new BaseTableRenderer(SwingConstants.LEADING));
        table.setDefaultRenderer(String.class, new BaseTableRenderer(SwingConstants.LEADING));
        table.setDefaultRenderer(Integer.class, new BaseTableRenderer(SwingConstants.CENTER));
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 37));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setForeground(MUTED_TEXT);
        header.setBackground(CARD_BACKGROUND);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
    }

    public static void configureTabs(JTabbedPane tabs) {
        tabs.setUI(new CleanTabbedPaneUi());
        tabs.setOpaque(false);
        tabs.setBackground(CARD_BACKGROUND);
        tabs.setForeground(TEXT);
        tabs.setFocusable(false);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    public static void configureTextField(JTextField field) {
        field.setBackground(CARD_BACKGROUND);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setFont(field.getFont().deriveFont(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(181, 193, 207)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    public static void configureReadOnlyTextField(JTextField field) {
        field.setEditable(false);
        field.setBackground(new Color(237, 241, 246));
        field.setForeground(MUTED_TEXT);
        field.setCaretColor(MUTED_TEXT);
    }

    public static void configureComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(CARD_BACKGROUND);
        comboBox.setForeground(TEXT);
        comboBox.setFont(comboBox.getFont().deriveFont(13f));
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(181, 193, 207)));
    }

    public static JScrollPane tableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(CARD_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        return scrollPane;
    }

    public static void setColumnWidths(JTable table, int... widths) {
        int count = Math.min(widths.length, table.getColumnModel().getColumnCount());
        for (int index = 0; index < count; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
    }

    public static TableCellRenderer statusRenderer() {
        return new BaseTableRenderer(SwingConstants.CENTER) {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                if (!selected) {
                    boolean overdue = "Overdue".equals(value);
                    label.setForeground(overdue ? DANGER : SUCCESS);
                }
                return label;
            }
        };
    }

    public static TableCellRenderer availabilityRenderer() {
        return new BaseTableRenderer(SwingConstants.CENTER) {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                if (!selected && value instanceof Number number) {
                    boolean available = number.intValue() > 0;
                    label.setForeground(available ? SUCCESS : DANGER);
                }
                return label;
            }
        };
    }

    public static void onTextChanged(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        });
    }

    public static void bindEscape(JDialog dialog) {
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-dialog");
        dialog.getRootPane().getActionMap().put("close-dialog", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                dialog.dispose();
            }
        });
    }

    public static void bindSearchShortcut(JComponent component, JTextField searchField) {
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcutMask), "focus-search");
        component.getActionMap().put("focus-search", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });
    }

    public static void requestInitialFocus(JComponent component) {
        javax.swing.SwingUtilities.invokeLater(component::requestFocusInWindow);
    }

    @SuppressWarnings("serial")
    private static final class SurfacePanel extends JPanel {
        private SurfacePanel() {
            setOpaque(false);
            setBackground(CARD_BACKGROUND);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            canvas.setColor(getBackground());
            canvas.fillRoundRect(0, 0, Math.max(0, width - 1), Math.max(0, height - 1), 8, 8);
            canvas.setColor(BORDER);
            canvas.drawRoundRect(0, 0, Math.max(0, width - 1), Math.max(0, height - 1), 8, 8);
            canvas.dispose();
            super.paintComponent(graphics);
        }
    }

    private enum ButtonStyle {
        PRIMARY,
        SECONDARY,
        DANGER,
        QUIET
    }

    private static final class CleanTabbedPaneUi extends BasicTabbedPaneUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(0, 22, 0, 22);
            tabInsets = new Insets(12, 17, 11, 17);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintTabBackground(
                Graphics graphics,
                int tabPlacement,
                int tabIndex,
                int x,
                int y,
                int width,
                int height,
                boolean selected) {
            graphics.setColor(selected ? PRIMARY_TINT : CARD_BACKGROUND);
            graphics.fillRect(x, y, width, height);
        }

        @Override
        protected void paintTabBorder(
                Graphics graphics,
                int tabPlacement,
                int tabIndex,
                int x,
                int y,
                int width,
                int height,
                boolean selected) {
            if (selected) {
                graphics.setColor(PRIMARY);
                graphics.fillRect(x + 8, y + height - 3, width - 16, 3);
            }
        }

        @Override
        protected void paintContentBorder(Graphics graphics, int tabPlacement, int selectedIndex) { }

        @Override
        protected void paintFocusIndicator(
                Graphics graphics,
                int tabPlacement,
                Rectangle[] rectangles,
                int tabIndex,
                Rectangle iconRectangle,
                Rectangle textRectangle,
                boolean selected) { }
    }

    @SuppressWarnings("serial")
    private static final class StyledButton extends JButton {
        private final ButtonStyle style;

        private StyledButton(String text, ButtonStyle style) {
            super(text);
            this.style = style;
            setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(8, 14, 8, 14));
            setFont(getFont().deriveFont(Font.BOLD, 12f));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = fillColor();
            Color outline = outlineColor();
            setForeground(foregroundColor());
            canvas.setColor(fill);
            canvas.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);
            if (outline != null) {
                canvas.setColor(outline);
                canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);
            }
            if (isFocusOwner()) {
                canvas.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 115));
                canvas.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 5, 5);
            }
            canvas.dispose();
            super.paintComponent(graphics);
        }

        private Color fillColor() {
            if (!isEnabled()) {
                return new Color(238, 242, 247);
            }
            boolean pressed = getModel().isPressed();
            boolean rollover = getModel().isRollover();
            if (style == ButtonStyle.PRIMARY) {
                if (pressed) {
                    return PRIMARY_DARK;
                }
                return rollover ? new Color(42, 108, 198) : PRIMARY;
            }
            if (style == ButtonStyle.DANGER && (pressed || rollover)) {
                return DANGER_BACKGROUND;
            }
            if (style == ButtonStyle.SECONDARY && (pressed || rollover)) {
                return PRIMARY_TINT;
            }
            if (style == ButtonStyle.QUIET && (pressed || rollover)) {
                return new Color(235, 240, 246);
            }
            return style == ButtonStyle.QUIET ? new Color(0, 0, 0, 0) : CARD_BACKGROUND;
        }

        private Color outlineColor() {
            if (!isEnabled()) {
                return BORDER;
            }
            return switch (style) {
                case PRIMARY -> null;
                case SECONDARY -> new Color(157, 178, 202);
                case DANGER -> new Color(215, 176, 171);
                case QUIET -> null;
            };
        }

        private Color foregroundColor() {
            if (!isEnabled()) {
                return new Color(139, 150, 165);
            }
            return switch (style) {
                case PRIMARY -> Color.WHITE;
                case SECONDARY -> PRIMARY_DARK;
                case DANGER -> DANGER;
                case QUIET -> TEXT;
            };
        }
    }

    @SuppressWarnings("serial")
    private static class BaseTableRenderer extends DefaultTableCellRenderer {
        private final int alignment;

        private BaseTableRenderer(int alignment) {
            this.alignment = alignment;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            label.setHorizontalAlignment(alignment);
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            if (selected) {
                label.setBackground(PRIMARY_TINT);
                label.setForeground(TEXT);
            } else {
                label.setBackground(row % 2 == 0 ? CARD_BACKGROUND : ALTERNATE_BACKGROUND);
                label.setForeground(TEXT);
            }
            return label;
        }
    }
}
