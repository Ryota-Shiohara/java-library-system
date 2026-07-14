package library.ui.book;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import library.service.BookService;
import library.service.LoanService;
import library.service.dto.BookSummary;

public final class BookPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(BookPanel.class.getName());
    private final BookService bookService;
    private final LoanService loanService;
    private final Runnable dataChanged;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {"ID", "Title", "Genre", "Total", "Loaned", "Available"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private String currentQuery = "";

    public BookPanel(BookService bookService, LoanService loanService, Runnable dataChanged) {
        if (bookService == null || loanService == null || dataChanged == null) {
            throw new IllegalArgumentException("Panel dependencies must not be null.");
        }
        this.bookService = bookService;
        this.loanService = loanService;
        this.dataChanged = dataChanged;
        buildContent();
        refreshData();
    }

    public void refreshData() {
        List<BookSummary> books = currentQuery.isEmpty()
                ? bookService.listBooks()
                : bookService.searchBooks(currentQuery);
        tableModel.setRowCount(0);
        for (BookSummary book : books) {
            tableModel.addRow(new Object[] {
                    book.id(), book.title(), book.genre(), book.totalCopies(),
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
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(new JLabel("Search:"));
        toolbar.add(searchField);
        toolbar.add(searchButton);
        toolbar.add(clearButton);
        toolbar.add(borrowersButton);
        return toolbar;
    }

    private void addBook() {
        try {
            BookDialog dialog = new BookDialog(owner(), null);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) return;
            bookService.addBook(dialog.bookId(), dialog.bookTitle(), dialog.bookGenre(), dialog.totalCopies());
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
            bookService.updateBook(selected.id(), dialog.bookTitle(), dialog.bookGenre(), dialog.totalCopies());
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
}
