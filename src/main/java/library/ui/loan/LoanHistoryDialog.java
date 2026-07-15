package library.ui.loan;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import library.exception.LibraryException;
import library.exception.ValidationException;
import library.model.LoanHistory;
import library.model.NdcCategory;
import library.service.LoanHistoryService;

public final class LoanHistoryDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(LoanHistoryDialog.class.getName());
    private final LoanHistoryService historyService;
    private final JTextField queryField = new JTextField(16);
    private final JTextField fromDateField = new JTextField(10);
    private final JTextField toDateField = new JTextField(10);
    private final JComboBox<NdcFilter> ndcFilterBox = new JComboBox<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {"Loan ID", "Book", "Member", "NDC", "Checkout Date", "Due Date", "Return Date", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

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
        setSize(1000, 500);
        setLocationRelativeTo(owner);
        refreshData();
    }

    private void buildContent() {
        setLayout(new BorderLayout(8, 8));
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        ndcFilterBox.addItem(new NdcFilter("", "All"));
        for (NdcCategory category : NdcCategory.values()) {
            ndcFilterBox.addItem(new NdcFilter(category.code(), category.toString()));
        }

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(event -> refreshSafely());
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(event -> clearFilters());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        toolbar.add(new JLabel("Search:"));
        toolbar.add(queryField);
        toolbar.add(new JLabel("NDC:"));
        toolbar.add(ndcFilterBox);
        toolbar.add(new JLabel("From:"));
        toolbar.add(fromDateField);
        toolbar.add(new JLabel("To:"));
        toolbar.add(toDateField);
        toolbar.add(searchButton);
        toolbar.add(clearButton);
        add(toolbar, BorderLayout.NORTH);
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
