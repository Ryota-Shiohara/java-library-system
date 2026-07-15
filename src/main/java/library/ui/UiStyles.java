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

    public static final Color PAGE_BACKGROUND = new Color(244, 247, 251);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color PRIMARY = new Color(30, 94, 184);
    public static final Color PRIMARY_DARK = new Color(22, 70, 137);
    public static final Color TEXT = new Color(28, 38, 52);
    public static final Color MUTED_TEXT = new Color(92, 106, 124);
    public static final Color BORDER = new Color(216, 224, 234);
    public static final Color DANGER = new Color(177, 48, 48);
    public static final Color DANGER_BACKGROUND = new Color(253, 235, 235);
    public static final Color SUCCESS = new Color(35, 117, 73);
    public static final Color SUCCESS_BACKGROUND = new Color(231, 247, 238);
    public static final Color WARNING = new Color(154, 92, 12);
    public static final Color WARNING_BACKGROUND = new Color(255, 245, 218);

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
        UIManager.put("Table.selectionBackground", new Color(218, 232, 252));
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
    }

    public static JPanel card() {
        JPanel panel = new SurfacePanel();
        panel.setBorder(cardBorder());
        return panel;
    }

    public static Border cardBorder() {
        return BorderFactory.createEmptyBorder(14, 16, 14, 16);
    }

    public static JLabel titleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 23f));
        return label;
    }

    public static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        return label;
    }

    public static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED_TEXT);
        return label;
    }

    public static JPanel metricCard(String labelText, JLabel valueLabel) {
        JPanel panel = card();
        panel.setLayout(new java.awt.BorderLayout(0, 4));
        JLabel label = mutedLabel(labelText);
        valueLabel.setForeground(TEXT);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 21f));
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

    public static void configureTable(JTable table) {
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(218, 232, 252));
        table.setSelectionForeground(TEXT);
        table.setBackground(CARD_BACKGROUND);
        table.setForeground(TEXT);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setForeground(TEXT);
        header.setBackground(new Color(247, 249, 252));
        header.setOpaque(true);
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
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 202, 218)),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
    }

    public static JScrollPane tableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(CARD_BACKGROUND);
        return scrollPane;
    }

    public static void setColumnWidths(JTable table, int... widths) {
        int count = Math.min(widths.length, table.getColumnModel().getColumnCount());
        for (int index = 0; index < count; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
    }

    public static TableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                if (!selected) {
                    boolean overdue = "Overdue".equals(value);
                    label.setForeground(overdue ? DANGER : SUCCESS);
                    label.setBackground(overdue ? DANGER_BACKGROUND : SUCCESS_BACKGROUND);
                }
                return label;
            }
        };
    }

    public static TableCellRenderer availabilityRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                if (!selected && value instanceof Number number) {
                    boolean available = number.intValue() > 0;
                    label.setForeground(available ? SUCCESS : DANGER);
                    label.setBackground(available ? SUCCESS_BACKGROUND : DANGER_BACKGROUND);
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
            canvas.setColor(new Color(27, 43, 65, 18));
            canvas.fillRoundRect(2, 3, Math.max(0, width - 4), Math.max(0, height - 5), 18, 18);
            canvas.setColor(getBackground());
            canvas.fillRoundRect(1, 1, Math.max(0, width - 3), Math.max(0, height - 4), 18, 18);
            canvas.setColor(BORDER);
            canvas.drawRoundRect(1, 1, Math.max(0, width - 3), Math.max(0, height - 4), 18, 18);
            canvas.dispose();
            super.paintComponent(graphics);
        }
    }

    private enum ButtonStyle {
        PRIMARY,
        SECONDARY,
        DANGER
    }

    private static final class CleanTabbedPaneUi extends BasicTabbedPaneUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(5, 18, 0, 18);
            tabInsets = new Insets(10, 18, 10, 18);
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
            graphics.setColor(selected ? CARD_BACKGROUND : PAGE_BACKGROUND);
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
                graphics.fillRoundRect(x + 10, y + height - 3, width - 20, 3, 3, 3);
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
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(7, 13, 7, 13));
            setFont(getFont().deriveFont(Font.BOLD));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = fillColor();
            Color outline = outlineColor();
            setForeground(foregroundColor());
            canvas.setColor(fill);
            canvas.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            if (outline != null) {
                canvas.setColor(outline);
                canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
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
                return new Color(237, 244, 254);
            }
            return CARD_BACKGROUND;
        }

        private Color outlineColor() {
            if (!isEnabled()) {
                return BORDER;
            }
            return switch (style) {
                case PRIMARY -> null;
                case SECONDARY -> new Color(184, 204, 230);
                case DANGER -> new Color(226, 180, 180);
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
            };
        }
    }
}
