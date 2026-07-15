package library.ui.book;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import library.exception.LibraryException;
import library.model.Book;
import library.model.Member;
import library.model.NdcCategory;
import library.service.BookService;
import library.service.LoanHistoryService;
import library.service.LoanService;
import library.service.dto.BookSummary;
import library.service.dto.ClassificationSummary;
import library.ui.loan.LoanHistoryDialog;

public final class BookPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(BookPanel.class.getName());
    private final BookService bookService;
    private final LoanService loanService;
    private final LoanHistoryService historyService;
    private final Runnable dataChanged;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {"ID", "Title", "Genre", "NDC", "Total", "Loaned", "Available"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private final JComboBox<NdcFilter> ndcFilterBox = new JComboBox<>();
    private String currentQuery = "";
    private String currentNdcCode = "";

    public BookPanel(
            BookService bookService,
            LoanService loanService,
            LoanHistoryService historyService,
            Runnable dataChanged) {
        if (bookService == null || loanService == null || historyService == null || dataChanged == null) {
            throw new IllegalArgumentException("Panel dependencies must not be null.");
        }
        this.bookService = bookService;
        this.loanService = loanService;
        this.historyService = historyService;
        this.dataChanged = dataChanged;
        buildContent();
        refreshData();
    }

    public void refreshData() {
        List<BookSummary> books = bookService.searchBooks(currentQuery, currentNdcCode);
        tableModel.setRowCount(0);
        for (BookSummary book : books) {
            tableModel.addRow(new Object[] {
                    book.id(), book.title(), book.genre(), book.ndcCode(), book.totalCopies(),
                    book.loanedCopies(), book.availableCopies()
            });
        }
    }

    private void buildContent() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(createToolbar(), BorderLayout.NORTH);
    }

    private JPanel createToolbar() {
        JButton addButton = new JButton("Add");
        addButton.addActionListener(event -> addBook());
        JButton editButton = new JButton("Edit");
        editButton.addActionListener(event -> editBook());
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(event -> deleteBook());
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(event -> searchBooks());
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(event -> clearSearch());
        JButton borrowersButton = new JButton("Borrowers");
        borrowersButton.addActionListener(event -> showBorrowers());
        JButton historyButton = new JButton("History");
        historyButton.addActionListener(event -> showHistory());
        JButton statisticsButton = new JButton("Statistics");
        statisticsButton.addActionListener(event -> showStatistics());
        ndcFilterBox.addItem(new NdcFilter("", "All"));
        for (NdcCategory category : NdcCategory.values()) {
            ndcFilterBox.addItem(new NdcFilter(category.code(), category.toString()));
        }
        ndcFilterBox.addActionListener(event -> {
            NdcFilter filter = (NdcFilter) ndcFilterBox.getSelectedItem();
            currentNdcCode = filter == null ? "" : filter.code();
            refreshData();
        });
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(new JLabel("Search:"));
        toolbar.add(searchField);
        toolbar.add(searchButton);
        toolbar.add(clearButton);
        toolbar.add(borrowersButton);
        toolbar.add(historyButton);
        toolbar.add(new JLabel("NDC:"));
        toolbar.add(ndcFilterBox);
        toolbar.add(statisticsButton);
        return toolbar;
    }

    private void addBook() {
        try {
            BookDialog dialog = new BookDialog(owner(), null);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) return;
            bookService.addBook(dialog.bookId(), dialog.bookTitle(), dialog.bookGenre(), dialog.totalCopies(), dialog.ndcCode());
            dataChanged.run();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void editBook() {
        try {
            Book selected = selectedBook();
            if (selected == null) return;
            BookDialog dialog = new BookDialog(owner(), selected);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) return;
            bookService.updateBook(selected.id(), dialog.bookTitle(), dialog.bookGenre(), dialog.totalCopies(), dialog.ndcCode());
            dataChanged.run();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void deleteBook() {
        try {
            Book selected = selectedBook();
            if (selected == null) return;
            int choice = JOptionPane.showConfirmDialog(this,
                    "Delete book " + selected.id() + "?", "Delete Book", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
            bookService.deleteBook(selected.id());
            dataChanged.run();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void searchBooks() {
        try {
            currentQuery = searchField.getText();
            refreshData();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void clearSearch() {
        try {
            currentQuery = "";
            searchField.setText("");
            currentNdcCode = "";
            ndcFilterBox.setSelectedIndex(0);
            refreshData();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void showBorrowers() {
        try {
            Book selected = selectedBook();
            if (selected == null) return;
            List<Member> borrowers = loanService.findBorrowersByBook(selected.id());
            String message = borrowers.isEmpty()
                    ? selected.title() + " has no active borrowers."
                    : borrowers.stream().map(member -> member.id() + " - " + member.name())
                            .reduce((left, right) -> left + "\n" + right).orElseThrow();
            JOptionPane.showMessageDialog(this, message, "Borrowers", JOptionPane.INFORMATION_MESSAGE);
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void showStatistics() {
        try {
            StringBuilder message = new StringBuilder(
                    "NDC | Category | Books | Total | Loaned | Available | History\n");
            for (ClassificationSummary summary : bookService.listClassificationSummaries()) {
                message.append(String.format(Locale.ROOT, "%s | %s | %d | %d | %d | %d | %d%n",
                        summary.ndcCode(), summary.ndcName(), summary.bookCount(), summary.totalCopies(),
                        summary.loanedCopies(), summary.availableCopies(), summary.historicalLoanCount()));
            }
            JOptionPane.showMessageDialog(this, message.toString(), "NDC Statistics", JOptionPane.INFORMATION_MESSAGE);
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void showHistory() {
        try {
            Book selected = selectedBook();
            if (selected == null) return;
            LoanHistoryDialog dialog = new LoanHistoryDialog(owner(), historyService, selected.id());
            dialog.setVisible(true);
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private Book selectedBook() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Select a book first.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String id = (String) tableModel.getValueAt(modelRow, 0);
        return bookService.findBookById(id).orElse(null);
    }

    private Window owner() { return SwingUtilities.getWindowAncestor(this); }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Library System", JOptionPane.ERROR_MESSAGE);
    }

    private void handleUnexpectedError(Exception exception) {
        LOGGER.log(Level.SEVERE, "Unexpected error in book panel.", exception);
        showError("An unexpected error occurred.");
    }

    private record NdcFilter(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
