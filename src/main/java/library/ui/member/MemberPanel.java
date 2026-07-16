package library.ui.member;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import library.exception.LibraryException;
import library.model.Book;
import library.model.Member;
import library.service.LoanHistoryService;
import library.service.LoanService;
import library.service.MemberService;
import library.ui.InfoTableDialog;
import library.ui.ReadOnlyTableModel;
import library.ui.UiStyles;
import library.ui.loan.LoanHistoryDialog;

@SuppressWarnings("serial")
public final class MemberPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(MemberPanel.class.getName());

    private final MemberService memberService;
    private final LoanService loanService;
    private final LoanHistoryService historyService;
    private final Runnable dataChanged;
    private final ReadOnlyTableModel tableModel = new ReadOnlyTableModel(
            new String[] {"ID", "Name", "Borrowed"},
            new Class<?>[] {String.class, String.class, Integer.class});
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(24);
    private final JLabel resultLabel = UiStyles.mutedLabel("0 members");
    private final JLabel messageLabel = UiStyles.mutedLabel("Select a member to manage them.");
    private final JButton editButton = UiStyles.secondaryButton("Edit");
    private final JButton deleteButton = UiStyles.dangerButton("Delete");
    private final JButton borrowedBooksButton = UiStyles.secondaryButton("Borrowed Books");
    private final JButton historyButton = UiStyles.secondaryButton("History");
    private String currentQuery = "";

    public MemberPanel(
            MemberService memberService,
            LoanService loanService,
            LoanHistoryService historyService,
            Runnable dataChanged) {
        if (memberService == null || loanService == null || historyService == null || dataChanged == null) {
            throw new IllegalArgumentException("Panel dependencies must not be null.");
        }
        this.memberService = memberService;
        this.loanService = loanService;
        this.historyService = historyService;
        this.dataChanged = dataChanged;
        buildContent();
        refreshData();
    }

    public void refreshData() {
        String selectedId = selectedMemberId(false);
        List<Member> allMembers = memberService.listMembers();
        List<Member> visibleMembers = currentQuery.isBlank()
                ? allMembers
                : memberService.searchMembers(currentQuery);

        tableModel.setRowCount(0);
        for (Member member : visibleMembers) {
            int borrowedCount = loanService.findBorrowedBooksByMember(member.id()).size();
            tableModel.addRow(new Object[] {member.id(), member.name(), borrowedCount});
        }

        resultLabel.setText(visibleMembers.size() + (visibleMembers.size() == 1 ? " member" : " members"));
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
        text.add(UiStyles.titleLabel("Members"), BorderLayout.NORTH);
        text.add(UiStyles.mutedLabel("Maintain member records and review current borrowing activity."),
                BorderLayout.CENTER);
        heading.add(text, BorderLayout.CENTER);
        top.add(heading);
        top.add(Box.createVerticalStrut(10));
        top.add(createFilters());
        return top;
    }

    private JPanel createFilters() {
        UiStyles.configureTextField(searchField);
        searchField.setToolTipText("Search by member ID or name");
        searchField.addActionListener(event -> searchMembers());
        JButton searchButton = UiStyles.primaryButton("Search");
        searchButton.addActionListener(event -> searchMembers());
        JButton clearButton = UiStyles.secondaryButton("Clear");
        clearButton.addActionListener(event -> clearSearch());

        JPanel filters = UiStyles.card();
        filters.setLayout(new FlowLayout(FlowLayout.LEADING, 7, 0));
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setLabelFor(searchField);
        filters.add(searchLabel);
        filters.add(searchField);
        filters.add(searchButton);
        filters.add(clearButton);
        filters.add(UiStyles.mutedLabel("Search by ID or name. Use Ctrl+F or Command+F to focus."));
        return filters;
    }

    private JPanel createTableArea() {
        UiStyles.configureTable(table);
        UiStyles.setColumnWidths(table, 130, 420, 90);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editMember();
                }
            }
        });

        editButton.addActionListener(event -> editMember());
        deleteButton.addActionListener(event -> deleteMember());
        borrowedBooksButton.addActionListener(event -> showBorrowedBooks());
        historyButton.addActionListener(event -> showHistory());
        JButton addButton = UiStyles.primaryButton("Add Member");
        addButton.setToolTipText("Register a new library member");
        addButton.addActionListener(event -> addMember());

        JPanel tableCard = UiStyles.card();
        tableCard.setLayout(new BorderLayout(0, 10));
        JPanel tableHeading = new JPanel(new BorderLayout());
        tableHeading.setOpaque(false);
        tableHeading.add(UiStyles.sectionLabel("Member Directory"), BorderLayout.WEST);
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
        relatedActions.add(borrowedBooksButton);
        relatedActions.add(historyButton);
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

    private void addMember() {
        try {
            MemberDialog dialog = new MemberDialog(owner(), null);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) {
                return;
            }
            Member added = memberService.addMember(dialog.memberId(), dialog.memberName());
            dataChanged.run();
            showSuccess("Member " + added.id() + " was added.");
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void editMember() {
        try {
            Member selected = selectedMember();
            if (selected == null) {
                return;
            }
            MemberDialog dialog = new MemberDialog(owner(), selected);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) {
                return;
            }
            Member updated = memberService.updateMember(selected.id(), dialog.memberName());
            dataChanged.run();
            selectMember(updated.id());
            showSuccess("Member " + updated.id() + " was updated.");
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void deleteMember() {
        try {
            Member selected = selectedMember();
            if (selected == null) {
                return;
            }
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Delete " + selected.id() + " - " + selected.name() + "?\nThis action cannot be undone.",
                    "Delete Member",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            memberService.deleteMember(selected.id());
            dataChanged.run();
            showSuccess("Member " + selected.id() + " was deleted.");
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void searchMembers() {
        currentQuery = searchField.getText();
        refreshSafely();
        messageLabel.setForeground(UiStyles.MUTED_TEXT);
        messageLabel.setText("Search filters were applied.");
    }

    private void clearSearch() {
        currentQuery = "";
        searchField.setText("");
        refreshSafely();
        messageLabel.setForeground(UiStyles.MUTED_TEXT);
        messageLabel.setText("Showing all registered members.");
    }

    private void showBorrowedBooks() {
        try {
            Member selected = selectedMember();
            if (selected == null) {
                return;
            }
            List<Book> books = loanService.findBorrowedBooksByMember(selected.id());
            List<Object[]> rows = books.stream()
                    .map(book -> new Object[] {
                            book.id(), book.title(), book.genre(), book.ndcCode(), book.totalCopies()
                    })
                    .toList();
            InfoTableDialog dialog = new InfoTableDialog(
                    owner(),
                    "Borrowed Books",
                    selected.name(),
                    "Books currently borrowed by member " + selected.id() + ".",
                    new String[] {"Book ID", "Title", "Genre", "NDC", "Total"},
                    new Class<?>[] {String.class, String.class, String.class, String.class, Integer.class},
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
            Member selected = selectedMember();
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

    private Member selectedMember() {
        String id = selectedMemberId(true);
        return id == null ? null : memberService.findMemberById(id).orElse(null);
    }

    private String selectedMemberId(boolean showSelectionError) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            if (showSelectionError) {
                showError("Select a member first.");
            }
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return (String) tableModel.getValueAt(modelRow, 0);
    }

    private void restoreSelection(String id) {
        if (id != null) {
            selectMember(id);
        }
    }

    private void selectMember(String id) {
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
        borrowedBooksButton.setEnabled(selected);
        historyButton.setEnabled(selected);
        if (selected) {
            String id = selectedMemberId(false);
            messageLabel.setForeground(UiStyles.TEXT);
            messageLabel.setText("Selected member: " + id + "  |  Double-click to edit");
        } else {
            messageLabel.setForeground(UiStyles.MUTED_TEXT);
            messageLabel.setText(tableModel.getRowCount() == 0
                    ? "No members match the current search."
                    : "Select a member to manage them.");
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
        LOGGER.log(Level.SEVERE, "Unexpected error in member panel.", exception);
        showError("An unexpected error occurred.");
    }
}
