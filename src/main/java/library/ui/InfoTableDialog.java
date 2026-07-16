package library.ui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

@SuppressWarnings("serial")
public final class InfoTableDialog extends JDialog {
    public InfoTableDialog(
            Window owner,
            String title,
            String heading,
            String description,
            String[] columns,
            Class<?>[] columnTypes,
            List<Object[]> rows) {
        super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        if (columns == null || columnTypes == null || rows == null) {
            throw new IllegalArgumentException("Dialog table data must not be null.");
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildContent(heading, description, columns, columnTypes, rows);
        setMinimumSize(new Dimension(620, 360));
        setSize(760, 440);
        setLocationRelativeTo(owner);
        UiStyles.bindEscape(this);
    }

    private void buildContent(
            String heading,
            String description,
            String[] columns,
            Class<?>[] columnTypes,
            List<Object[]> rows) {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(UiStyles.PAGE_BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(UiStyles.titleLabel(heading), BorderLayout.NORTH);
        header.add(UiStyles.mutedLabel(description), BorderLayout.CENTER);
        content.add(header, BorderLayout.NORTH);

        ReadOnlyTableModel model = new ReadOnlyTableModel(columns, columnTypes);
        for (Object[] row : rows) {
            model.addRow(row);
        }
        JTable table = new JTable(model);
        UiStyles.configureTable(table);

        JPanel tableCard = UiStyles.card();
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.add(UiStyles.tableScrollPane(table), BorderLayout.CENTER);
        String resultText = rows.isEmpty()
                ? "No matching records."
                : rows.size() + (rows.size() == 1 ? " record" : " records");
        tableCard.add(UiStyles.mutedLabel(resultText), BorderLayout.SOUTH);
        content.add(tableCard, BorderLayout.CENTER);

        JButton closeButton = UiStyles.primaryButton("Close");
        closeButton.addActionListener(event -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        actions.setOpaque(false);
        actions.add(closeButton);
        content.add(actions, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(closeButton);
    }
}
