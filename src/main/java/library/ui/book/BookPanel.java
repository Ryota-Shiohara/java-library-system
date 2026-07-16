package library.ui.book;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import library.exception.LibraryException;
import library.model.Book;
import library.model.Member;
import library.model.NdcCategory;
import library.service.BookService;
import library.service.LoanHistoryService;
import library.service.LoanService;
import library.service.dto.BookSummary;
import library.service.dto.ClassificationSummary;
import library.ui.InfoTableDialog;
import library.ui.ReadOnlyTableModel;
import library.ui.UiStyles;
import library.ui.loan.LoanHistoryDialog;

@SuppressWarnings("serial")
public final class BookPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(BookPanel.class.getName());

    private final BookService bookService;
    private final LoanService loanService;
    private final LoanHistoryService historyService;
    private final Runnable dataChanged;
    private final ReadOnlyTableModel tableModel = new ReadOnlyTableModel(
            new String[] {"ID", "Title", "Genre", "NDC", "Total", "Loaned", "Available"},
            new Class<?>[] {String.class, String.class, String.class, String.class,
                    Integer.class, Integer.class, Integer.class});
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<NdcFilter> ndcFilterBox = new JComboBox<>();
    private final JLabel copyCountLabel = new JLabel("0");
    private final JLabel loanedCountLabel = new JLabel("0");
    private final JLabel availableCountLabel = new JLabel("0");
    private final JLabel resultLabel = UiStyles.mutedLabel("0 books");
    private final JLabel messageLabel = UiStyles.mutedLabel("Select a book to manage it.");
    private final JButton editButton = UiStyles.secondaryButton("Edit");
    private final JButton deleteButton = UiStyles.dangerButton("Delete");
    private final JButton borrowersButton = UiStyles.secondaryButton("Borrowers");
    private final JButton historyButton = UiStyles.secondaryButton("History");
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
        String selectedId = selectedBookId(false);
        List<BookSummary> allBooks = bookService.listBooks();
        List<BookSummary> visibleBooks = bookService.searchBooks(currentQuery, currentNdcCode);

        tableModel.setRowCount(0);
        for (BookSummary book : visibleBooks) {
            tableModel.addRow(new Object[] {
                    book.id(), book.title(), book.genre(), book.ndcCode(), book.totalCopies(),
                    book.loanedCopies(), book.availableCopies()
            });
        }

        int totalCopies = allBooks.stream().mapToInt(BookSummary::totalCopies).sum();
        int loanedCopies = allBooks.stream().mapToInt(BookSummary::loanedCopies).sum();
        int availableCopies = allBooks.stream().mapToInt(BookSummary::availableCopies).sum();
        copyCountLabel.setText(Integer.toString(totalCopies));
        loanedCountLabel.setText(Integer.toString(loanedCopies));
        availableCountLabel.setText(Integer.toString(availableCopies));
        resultLabel.setText(visibleBooks.size() + (visibleBooks.size() == 1 ? " book" : " books"));

        restoreSelection(selectedId);
        updateSelectionActions();
    }

    private void buildContent() {
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        setBackground(UiStyles.PAGE_BACKGROUND);
        add(createTopArea(), BorderLayout.NORTH);
        add(createTableArea(), BorderLayout.CENTER);
        UiStyles.bindSearchShortcut(this, searchField);
    }

    private JPanel createTopArea() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);
        text.add(UiStyles.titleLabel("Books"), BorderLayout.NORTH);
        text.add(UiStyles.mutedLabel("Search the catalog, track inventory, and review circulation."),
                BorderLayout.CENTER);
        heading.add(text, BorderLayout.CENTER);

        top.add(heading);
        top.add(Box.createVerticalStrut(12));

        JPanel metrics = new JPanel(new GridLayout(1, 3, 10, 0));
        metrics.setOpaque(false);
        metrics.add(UiStyles.metricCard("Total copies", copyCountLabel));
        metrics.add(UiStyles.metricCard("On loan", loanedCountLabel));
        metrics.add(UiStyles.metricCard("Available", availableCountLabel));
        top.add(metrics);
        top.add(Box.createVerticalStrut(10));
        top.add(createFilters());
        return top;
    }

    private JPanel createFilters() {
        UiStyles.configureTextField(searchField);
        for (NdcCategory category : NdcCategory.values()) {
            ndcFilterBox.addItem(new NdcFilter(category.code(), category.toString()));
        }
        ndcFilterBox.insertItemAt(new NdcFilter("", "All categories"), 0);
        ndcFilterBox.setSelectedIndex(0);
        ndcFilterBox.setToolTipText("Filter books by NDC category");
        ndcFilterBox.addActionListener(event -> {
            NdcFilter filter = (NdcFilter) ndcFilterBox.getSelectedItem();
            currentNdcCode = filter == null ? "" : filter.code();
            refreshSafely();
        });

        searchField.setToolTipText("Search by ID, title, or genre");
        searchField.addActionListener(event -> searchBooks());
        JButton searchButton = UiStyles.primaryButton("Search");
        searchButton.addActionListener(event -> searchBooks());
        JButton clearButton = UiStyles.secondaryButton("Clear");
        clearButton.addActionListener(event -> clearSearch());

        JPanel filters = UiStyles.card();
        filters.setLayout(new BorderLayout(10, 8));
        JPanel searchGroup = new JPanel(new FlowLayout(FlowLayout.LEADING, 7, 0));
        searchGroup.setOpaque(false);
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setLabelFor(searchField);
        searchGroup.add(searchLabel);
        searchGroup.add(searchField);
        searchGroup.add(searchButton);
        searchGroup.add(clearButton);
        filters.add(searchGroup, BorderLayout.CENTER);

        JPanel categoryGroup = new JPanel(new FlowLayout(FlowLayout.TRAILING, 7, 0));
        categoryGroup.setOpaque(false);
        JLabel categoryLabel = new JLabel("NDC category");
        categoryLabel.setLabelFor(ndcFilterBox);
        categoryGroup.add(categoryLabel);
        categoryGroup.add(ndcFilterBox);
        filters.add(categoryGroup, BorderLayout.EAST);
        return filters;
    }

    private JPanel createTableArea() {
        UiStyles.configureTable(table);
        UiStyles.setColumnWidths(table, 90, 250, 145, 55, 60, 60, 75);
        table.getColumnModel().getColumn(6).setCellRenderer(UiStyles.availabilityRenderer());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editBook();
                }
            }
        });

        editButton.addActionListener(event -> editBook());
        deleteButton.addActionListener(event -> deleteBook());
        borrowersButton.addActionListener(event -> showBorrowers());
        historyButton.addActionListener(event -> showHistory());
        JButton addButton = UiStyles.primaryButton("Add Book");
        addButton.setToolTipText("Register a new catalog item");
        addButton.addActionListener(event -> addBook());
        JButton statisticsButton = UiStyles.secondaryButton("Statistics");
        statisticsButton.setToolTipText("View inventory and loan totals by NDC category");
        statisticsButton.addActionListener(event -> showStatistics());

        JPanel tableCard = UiStyles.card();
        tableCard.setLayout(new BorderLayout(0, 10));
        JPanel tableHeading = new JPanel(new BorderLayout());
        tableHeading.setOpaque(false);
        tableHeading.add(UiStyles.sectionLabel("Catalog"), BorderLayout.WEST);
        tableHeading.add(resultLabel, BorderLayout.EAST);
        JPanel tableTop = new JPanel();
        tableTop.setLayout(new BoxLayout(tableTop, BoxLayout.Y_AXIS));
        tableTop.setOpaque(false);
        tableTop.add(tableHeading);
        tableTop.add(Box.createVerticalStrut(9));

        JPanel commandBar = new JPanel(new BorderLayout(8, 0));
        commandBar.setOpaque(false);
        JPanel recordActions = new JPanel(new FlowLayout(FlowLayout.LEADING, 7, 0));
        recordActions.setOpaque(false);
        recordActions.add(addButton);
        recordActions.add(editButton);
        recordActions.add(deleteButton);
        commandBar.add(recordActions, BorderLayout.WEST);
        JPanel relatedActions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 7, 0));
        relatedActions.setOpaque(false);
        relatedActions.add(borrowersButton);
        relatedActions.add(historyButton);
        relatedActions.add(statisticsButton);
        commandBar.add(relatedActions, BorderLayout.EAST);
        tableTop.add(commandBar);
        tableCard.add(tableTop, BorderLayout.NORTH);
        tableCard.add(UiStyles.tableScrollPane(table), BorderLayout.CENTER);

        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setOpaque(false);
        actionBar.add(messageLabel, BorderLayout.CENTER);
        tableCard.add(actionBar, BorderLayout.SOUTH);
        return tableCard;
    }

    private void addBook() {
        try {
            BookDialog dialog = new BookDialog(owner(), null);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) {
                return;
            }
            Book added = bookService.addBook(
                    dialog.bookId(), dialog.bookTitle(), dialog.bookGenre(), dialog.totalCopies(), dialog.ndcCode());
            dataChanged.run();
            showSuccess("Book " + added.id() + " was added.");
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void editBook() {
        try {
            Book selected = selectedBook();
            if (selected == null) {
                return;
            }
            BookDialog dialog = new BookDialog(owner(), selected);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) {
                return;
            }
            Book updated = bookService.updateBook(
                    selected.id(), dialog.bookTitle(), dialog.bookGenre(), dialog.totalCopies(), dialog.ndcCode());
            dataChanged.run();
            selectBook(updated.id());
            showSuccess("Book " + updated.id() + " was updated.");
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void deleteBook() {
        try {
            Book selected = selectedBook();
            if (selected == null) {
                return;
            }
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Delete " + selected.id() + " - " + selected.title() + "?\nThis action cannot be undone.",
                    "Delete Book",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            bookService.deleteBook(selected.id());
            dataChanged.run();
            showSuccess("Book " + selected.id() + " was deleted.");
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void searchBooks() {
        currentQuery = searchField.getText();
        refreshSafely();
        messageLabel.setForeground(UiStyles.MUTED_TEXT);
        messageLabel.setText("Search filters were applied.");
    }

    private void clearSearch() {
        currentQuery = "";
        searchField.setText("");
        currentNdcCode = "";
        ndcFilterBox.setSelectedIndex(0);
        refreshSafely();
        messageLabel.setForeground(UiStyles.MUTED_TEXT);
        messageLabel.setText("Showing the complete catalog.");
    }

    private void showBorrowers() {
        try {
            Book selected = selectedBook();
            if (selected == null) {
                return;
            }
            List<Member> borrowers = loanService.findBorrowersByBook(selected.id());
            List<Object[]> rows = borrowers.stream()
                    .map(member -> new Object[] {member.id(), member.name()})
                    .toList();
            InfoTableDialog dialog = new InfoTableDialog(
                    owner(),
                    "Current Borrowers",
                    selected.title(),
                    "Members currently borrowing book " + selected.id() + ".",
                    new String[] {"Member ID", "Name"},
                    new Class<?>[] {String.class, String.class},
                    rows);
            dialog.setVisible(true);
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void showStatistics() {
        try {
            List<Object[]> rows = new ArrayList<>();
            for (ClassificationSummary summary : bookService.listClassificationSummaries()) {
                rows.add(new Object[] {
                        summary.ndcCode(), summary.ndcName(), summary.bookCount(), summary.totalCopies(),
                        summary.loanedCopies(), summary.availableCopies(), summary.historicalLoanCount()
                });
            }
            InfoTableDialog dialog = new InfoTableDialog(
                    owner(),
                    "NDC Statistics",
                    "NDC Classification Statistics",
                    "Inventory and completed-loan totals for every primary NDC category.",
                    new String[] {"NDC", "Category", "Books", "Total", "Loaned", "Available", "History"},
                    new Class<?>[] {String.class, String.class, Integer.class, Integer.class,
                            Integer.class, Integer.class, Integer.class},
                    rows);
            dialog.setVisible(true);
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void showHistory() {
        try {
            Book selected = selectedBook();
            if (selected == null) {
                return;
            }
            LoanHistoryDialog dialog = new LoanHistoryDialog(owner(), historyService, selected.id());
            dialog.setVisible(true);
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private Book selectedBook() {
        String id = selectedBookId(true);
        return id == null ? null : bookService.findBookById(id).orElse(null);
    }

    private String selectedBookId(boolean showSelectionError) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            if (showSelectionError) {
                showError("Select a book first.");
            }
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return (String) tableModel.getValueAt(modelRow, 0);
    }

    private void restoreSelection(String id) {
        if (id == null) {
            return;
        }
        selectBook(id);
    }

    private void selectBook(String id) {
        for (int modelRow = 0; modelRow < tableModel.getRowCount(); modelRow++) {
            if (id.equals(tableModel.getValueAt(modelRow, 0))) {
                int viewRow = table.convertRowIndexToView(modelRow);
                table.setRowSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                return;
            }
        }
    }

    private void updateSelectionActions() {
        boolean selected = table.getSelectedRow() >= 0;
        editButton.setEnabled(selected);
        deleteButton.setEnabled(selected);
        borrowersButton.setEnabled(selected);
        historyButton.setEnabled(selected);
        if (selected) {
            String id = selectedBookId(false);
            messageLabel.setForeground(UiStyles.TEXT);
            messageLabel.setText("Selected book: " + id + "  |  Double-click to edit");
        } else {
            messageLabel.setForeground(UiStyles.MUTED_TEXT);
            messageLabel.setText(tableModel.getRowCount() == 0
                    ? "No books match the current filters."
                    : "Select a book to manage it.");
        }
    }

    private void refreshSafely() {
        try {
            refreshData();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void showSuccess(String message) {
        messageLabel.setForeground(UiStyles.SUCCESS);
        messageLabel.setText(message);
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

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
