package library.ui.loan;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import library.model.Member;
import library.service.BookService;
import library.service.MemberService;
import library.service.dto.BookSummary;
import library.ui.ReadOnlyTableModel;
import library.ui.UiStyles;

@SuppressWarnings("serial")
public final class CheckoutDialog extends JDialog {
    private final ReadOnlyTableModel bookModel = new ReadOnlyTableModel(
            new String[] {"Book ID", "Title", "Available"},
            new Class<?>[] {String.class, String.class, Integer.class});
    private final ReadOnlyTableModel memberModel = new ReadOnlyTableModel(
            new String[] {"Member ID", "Name"},
            new Class<?>[] {String.class, String.class});
    private final JTable bookTable = new JTable(bookModel);
    private final JTable memberTable = new JTable(memberModel);
    private final TableRowSorter<ReadOnlyTableModel> bookSorter = new TableRowSorter<>(bookModel);
    private final TableRowSorter<ReadOnlyTableModel> memberSorter = new TableRowSorter<>(memberModel);
    private final JTextField bookSearchField = new JTextField(18);
    private final JTextField memberSearchField = new JTextField(18);
    private final JLabel selectionLabel = UiStyles.mutedLabel("Select one available book and one member.");
    private final JButton checkoutButton = UiStyles.primaryButton("Complete Checkout");
    private boolean confirmed;

    public CheckoutDialog(Window owner, BookService bookService, MemberService memberService) {
        super(owner, "New Checkout", Dialog.ModalityType.APPLICATION_MODAL);
        if (bookService == null || memberService == null) {
            throw new IllegalArgumentException("Checkout dependencies must not be null.");
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        populateOptions(bookService, memberService);
        buildContent();
        setMinimumSize(new Dimension(800, 520));
        setSize(920, 580);
        setLocationRelativeTo(owner);
        UiStyles.bindEscape(this);
        UiStyles.requestInitialFocus(bookSearchField);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String selectedBookId() {
        int viewRow = bookTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = bookTable.convertRowIndexToModel(viewRow);
        return (String) bookModel.getValueAt(modelRow, 0);
    }

    public String selectedMemberId() {
        int viewRow = memberTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = memberTable.convertRowIndexToModel(viewRow);
        return (String) memberModel.getValueAt(modelRow, 0);
    }

    private void populateOptions(BookService bookService, MemberService memberService) {
        for (BookSummary book : bookService.listBooks()) {
            if (book.availableCopies() > 0) {
                bookModel.addRow(new Object[] {book.id(), book.title(), book.availableCopies()});
            }
        }
        for (Member member : memberService.listMembers()) {
            memberModel.addRow(new Object[] {member.id(), member.name()});
        }
    }

    private void buildContent() {
        UiStyles.configureTextField(bookSearchField);
        UiStyles.configureTextField(memberSearchField);
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(UiStyles.PAGE_BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(UiStyles.titleLabel("New Checkout"), BorderLayout.NORTH);
        header.add(UiStyles.mutedLabel("Choose an available book and the member receiving it."),
                BorderLayout.CENTER);
        content.add(header, BorderLayout.NORTH);

        JPanel selectors = new JPanel(new GridLayout(1, 2, 12, 0));
        selectors.setOpaque(false);
        selectors.add(createBookSelector());
        selectors.add(createMemberSelector());
        content.add(selectors, BorderLayout.CENTER);

        checkoutButton.setEnabled(false);
        checkoutButton.addActionListener(event -> confirm());
        JButton cancelButton = UiStyles.secondaryButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.add(selectionLabel, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);
        actions.add(cancelButton);
        actions.add(checkoutButton);
        footer.add(actions, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);

        bookTable.getSelectionModel().addListSelectionListener(event -> updateSelectionState());
        memberTable.getSelectionModel().addListSelectionListener(event -> updateSelectionState());
        UiStyles.onTextChanged(bookSearchField,
                () -> applyFilter(bookSorter, bookModel, bookSearchField.getText()));
        UiStyles.onTextChanged(memberSearchField,
                () -> applyFilter(memberSorter, memberModel, memberSearchField.getText()));

        setContentPane(content);
        getRootPane().setDefaultButton(checkoutButton);
        updateSelectionState();
    }

    private JPanel createBookSelector() {
        UiStyles.configureTable(bookTable);
        bookTable.setRowSorter(bookSorter);
        UiStyles.setColumnWidths(bookTable, 90, 250, 75);
        bookTable.getColumnModel().getColumn(2).setCellRenderer(UiStyles.availabilityRenderer());
        bookSearchField.setToolTipText("Filter available books by ID or title");

        JPanel panel = UiStyles.card();
        panel.setLayout(new BorderLayout(0, 9));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(UiStyles.sectionLabel("Available Books"), BorderLayout.WEST);
        heading.add(UiStyles.mutedLabel(bookModel.getRowCount() + " available"), BorderLayout.EAST);
        panel.add(heading, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        JPanel search = new JPanel(new BorderLayout(7, 0));
        search.setOpaque(false);
        JLabel label = new JLabel("Search");
        label.setLabelFor(bookSearchField);
        search.add(label, BorderLayout.WEST);
        search.add(bookSearchField, BorderLayout.CENTER);
        body.add(search, BorderLayout.NORTH);
        body.add(UiStyles.tableScrollPane(bookTable), BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMemberSelector() {
        UiStyles.configureTable(memberTable);
        memberTable.setRowSorter(memberSorter);
        UiStyles.setColumnWidths(memberTable, 100, 260);
        memberSearchField.setToolTipText("Filter members by ID or name");

        JPanel panel = UiStyles.card();
        panel.setLayout(new BorderLayout(0, 9));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(UiStyles.sectionLabel("Members"), BorderLayout.WEST);
        heading.add(UiStyles.mutedLabel(memberModel.getRowCount() + " registered"), BorderLayout.EAST);
        panel.add(heading, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        JPanel search = new JPanel(new BorderLayout(7, 0));
        search.setOpaque(false);
        JLabel label = new JLabel("Search");
        label.setLabelFor(memberSearchField);
        search.add(label, BorderLayout.WEST);
        search.add(memberSearchField, BorderLayout.CENTER);
        body.add(search, BorderLayout.NORTH);
        body.add(UiStyles.tableScrollPane(memberTable), BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private void applyFilter(
            TableRowSorter<ReadOnlyTableModel> sorter,
            ReadOnlyTableModel model,
            String value) {
        String query = value.strip().toUpperCase(Locale.ROOT);
        if (query.isEmpty()) {
            sorter.setRowFilter(null);
            updateSelectionState();
            return;
        }
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends ReadOnlyTableModel, ? extends Integer> entry) {
                for (int column = 0; column < model.getColumnCount(); column++) {
                    Object cell = entry.getValue(column);
                    if (cell != null && cell.toString().toUpperCase(Locale.ROOT).contains(query)) {
                        return true;
                    }
                }
                return false;
            }
        });
        updateSelectionState();
    }

    private void updateSelectionState() {
        boolean hasBooks = bookModel.getRowCount() > 0;
        boolean hasMembers = memberModel.getRowCount() > 0;
        boolean hasVisibleBooks = bookTable.getRowCount() > 0;
        boolean hasVisibleMembers = memberTable.getRowCount() > 0;
        boolean hasBookSelection = selectedBookId() != null;
        boolean hasMemberSelection = selectedMemberId() != null;
        checkoutButton.setEnabled(hasBookSelection && hasMemberSelection);

        selectionLabel.setForeground(UiStyles.MUTED_TEXT);
        if (!hasBooks) {
            selectionLabel.setForeground(UiStyles.DANGER);
            selectionLabel.setText("No books are available for checkout.");
        } else if (!hasMembers) {
            selectionLabel.setForeground(UiStyles.DANGER);
            selectionLabel.setText("No members are available for checkout.");
        } else if (!hasVisibleBooks) {
            selectionLabel.setText("No available books match the current search.");
        } else if (!hasVisibleMembers) {
            selectionLabel.setText("No members match the current search.");
        } else if (!hasBookSelection && !hasMemberSelection) {
            selectionLabel.setText("Select one available book and one member.");
        } else if (!hasBookSelection) {
            selectionLabel.setText("Select a book to continue.");
        } else if (!hasMemberSelection) {
            selectionLabel.setText("Select a member to continue.");
        } else {
            selectionLabel.setForeground(UiStyles.SUCCESS);
            selectionLabel.setText("Ready: " + selectedBookId() + " to " + selectedMemberId());
        }
    }

    private void confirm() {
        if (selectedBookId() == null || selectedMemberId() == null) {
            updateSelectionState();
            return;
        }
        confirmed = true;
        dispose();
    }
}
