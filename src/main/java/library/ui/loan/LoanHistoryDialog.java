package library.ui.loan;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import library.exception.LibraryException;
import library.exception.ValidationException;
import library.model.LoanHistory;
import library.model.NdcCategory;
import library.service.LoanHistoryService;
import library.ui.ReadOnlyTableModel;
import library.ui.UiStyles;

@SuppressWarnings("serial")
public final class LoanHistoryDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(LoanHistoryDialog.class.getName());

    private final LoanHistoryService historyService;
    private final JTextField queryField = new JTextField(22);
    private final JTextField fromDateField = new JTextField(10);
    private final JTextField toDateField = new JTextField(10);
    private final JComboBox<NdcFilter> ndcFilterBox = new JComboBox<>();
    private final ReadOnlyTableModel tableModel = new ReadOnlyTableModel(
            new String[] {"Loan ID", "Book", "Member", "NDC", "Checkout Date", "Due Date", "Return Date", "Status"},
            new Class<?>[] {String.class, String.class, String.class, String.class,
                    String.class, String.class, String.class, String.class});
    private final JTable table = new JTable(tableModel);
    private final JLabel resultLabel = UiStyles.mutedLabel("0 completed loans");

    public LoanHistoryDialog(Window owner, LoanHistoryService historyService) {
        this(owner, historyService, "");
    }

    public LoanHistoryDialog(Window owner, LoanHistoryService historyService, String initialQuery) {
        super(owner, "Loan History", Dialog.ModalityType.APPLICATION_MODAL);
        if (historyService == null) {
            throw new IllegalArgumentException("Loan history service must not be null.");
        }
        this.historyService = historyService;
        queryField.setText(initialQuery == null ? "" : initialQuery);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildContent();
        setMinimumSize(new Dimension(800, 480));
        setSize(1000, 580);
        setLocationRelativeTo(owner);
        UiStyles.bindEscape(this);
        UiStyles.bindSearchShortcut(getRootPane(), queryField);
        refreshSafely();
    }

    private void buildContent() {
        UiStyles.configureTextField(queryField);
        UiStyles.configureTextField(fromDateField);
        UiStyles.configureTextField(toDateField);
        UiStyles.configureComboBox(ndcFilterBox);
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(UiStyles.PAGE_BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(UiStyles.titleLabel("Loan History"), BorderLayout.NORTH);
        header.add(UiStyles.mutedLabel(
                "Search returned loans by book, member, return date, or NDC category."), BorderLayout.CENTER);
        top.add(header, BorderLayout.NORTH);
        top.add(createFilters(), BorderLayout.CENTER);
        content.add(top, BorderLayout.NORTH);

        UiStyles.configureTable(table);
        UiStyles.setColumnWidths(table, 160, 230, 200, 50, 95, 95, 95, 80);
        table.getColumnModel().getColumn(7).setCellRenderer(UiStyles.statusRenderer());
        JPanel tableCard = UiStyles.card();
        tableCard.setLayout(new BorderLayout(0, 10));
        JPanel tableHeading = new JPanel(new BorderLayout());
        tableHeading.setOpaque(false);
        tableHeading.add(UiStyles.sectionLabel("Completed Loans"), BorderLayout.WEST);
        tableHeading.add(resultLabel, BorderLayout.EAST);
        tableCard.add(tableHeading, BorderLayout.NORTH);
        tableCard.add(UiStyles.tableScrollPane(table), BorderLayout.CENTER);
        content.add(tableCard, BorderLayout.CENTER);

        JButton closeButton = UiStyles.primaryButton("Close");
        closeButton.addActionListener(event -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        footer.setOpaque(false);
        footer.add(closeButton);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private JPanel createFilters() {
        ndcFilterBox.addItem(new NdcFilter("", "All categories"));
        for (NdcCategory category : NdcCategory.values()) {
            ndcFilterBox.addItem(new NdcFilter(category.code(), category.toString()));
        }
        queryField.setToolTipText("Search loan, book, member, genre, or NDC details");
        fromDateField.setToolTipText("Earliest return date in yyyy-MM-dd format");
        toDateField.setToolTipText("Latest return date in yyyy-MM-dd format");
        queryField.addActionListener(event -> refreshSafely());
        toDateField.addActionListener(event -> refreshSafely());

        JButton searchButton = UiStyles.primaryButton("Search");
        searchButton.addActionListener(event -> refreshSafely());
        JButton clearButton = UiStyles.quietButton("Clear");
        clearButton.addActionListener(event -> clearFilters());

        JPanel filters = UiStyles.card();
        filters.setBorder(UiStyles.compactCardBorder());
        filters.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addFilter(filters, constraints, 0, "Search", queryField, 1.0);
        addFilter(filters, constraints, 2, "NDC category", ndcFilterBox, 0.4);
        addFilter(filters, constraints, 4, "Returned from", fromDateField, 0.3);
        addFilter(filters, constraints, 6, "Returned to", toDateField, 0.3);

        constraints.gridx = 8;
        constraints.weightx = 0.0;
        filters.add(searchButton, constraints);
        constraints.gridx = 9;
        filters.add(clearButton, constraints);
        return filters;
    }

    private void addFilter(
            JPanel panel,
            GridBagConstraints constraints,
            int column,
            String labelText,
            java.awt.Component field,
            double weight) {
        JLabel label = new JLabel(labelText);
        label.setLabelFor(field);
        constraints.gridx = column;
        constraints.weightx = 0.0;
        panel.add(label, constraints);
        constraints.gridx = column + 1;
        constraints.weightx = weight;
        panel.add(field, constraints);
    }

    private void refreshSafely() {
        try {
            refreshData();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Unexpected error in loan history dialog.", exception);
            showError("An unexpected error occurred.");
        }
    }

    private void refreshData() {
        NdcFilter filter = (NdcFilter) ndcFilterBox.getSelectedItem();
        LocalDate fromDate = parseDate(fromDateField.getText(), "From date");
        LocalDate toDate = parseDate(toDateField.getText(), "To date");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException("From date must not be after to date.");
        }
        List<LoanHistory> histories = historyService.searchHistory(
                queryField.getText(),
                filter == null ? "" : filter.code(),
                fromDate,
                toDate);
        tableModel.setRowCount(0);
        for (LoanHistory history : histories) {
            tableModel.addRow(new Object[] {
                    history.id(),
                    history.book().id() + " - " + history.book().title(),
                    history.member().id() + " - " + history.member().name(),
                    history.book().ndcCode(),
                    history.checkoutDate().toString(),
                    history.dueDate().toString(),
                    history.returnDate().toString(),
                    history.overdue() ? "Overdue" : "On time"
            });
        }
        resultLabel.setText(histories.size()
                + (histories.size() == 1 ? " completed loan" : " completed loans"));
    }

    private void clearFilters() {
        queryField.setText("");
        fromDateField.setText("");
        toDateField.setText("");
        ndcFilterBox.setSelectedIndex(0);
        refreshSafely();
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.strip().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException exception) {
            throw new ValidationException(fieldName + " must use yyyy-MM-dd.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Library System", JOptionPane.ERROR_MESSAGE);
    }

    private record NdcFilter(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
