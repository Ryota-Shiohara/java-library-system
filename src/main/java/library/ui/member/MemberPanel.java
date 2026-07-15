package library.ui.member;

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
import javax.swing.table.TableRowSorter;
import library.exception.LibraryException;
import library.model.Book;
import library.model.Member;
import library.service.LoanService;
import library.service.LoanHistoryService;
import library.service.MemberService;
import library.ui.loan.LoanHistoryDialog;

public final class MemberPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(MemberPanel.class.getName());
    private final MemberService memberService;
    private final LoanService loanService;
    private final LoanHistoryService historyService;
    private final Runnable dataChanged;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {"ID", "Name", "Borrowed"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
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
        List<Member> members = currentQuery.isEmpty()
                ? memberService.listMembers()
                : memberService.searchMembers(currentQuery);
        tableModel.setRowCount(0);
        for (Member member : members) {
            int borrowedCount = loanService.findBorrowedBooksByMember(member.id()).size();
            tableModel.addRow(new Object[] {member.id(), member.name(), borrowedCount});
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
        addButton.addActionListener(event -> addMember());
        JButton editButton = new JButton("Edit");
        editButton.addActionListener(event -> editMember());
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(event -> deleteMember());
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(event -> searchMembers());
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(event -> clearSearch());
        JButton borrowedBooksButton = new JButton("Borrowed Books");
        borrowedBooksButton.addActionListener(event -> showBorrowedBooks());
        JButton historyButton = new JButton("History");
        historyButton.addActionListener(event -> showHistory());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(new JLabel("Search:"));
        toolbar.add(searchField);
        toolbar.add(searchButton);
        toolbar.add(clearButton);
        toolbar.add(borrowedBooksButton);
        toolbar.add(historyButton);
        return toolbar;
    }

    private void addMember() {
        try {
            MemberDialog dialog = new MemberDialog(owner(), null);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) {
                return;
            }
            memberService.addMember(dialog.memberId(), dialog.memberName());
            dataChanged.run();
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
            memberService.updateMember(selected.id(), dialog.memberName());
            dataChanged.run();
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
            int choice = JOptionPane.showConfirmDialog(this,
                    "Delete member " + selected.id() + "?", "Delete Member", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            memberService.deleteMember(selected.id());
            dataChanged.run();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void searchMembers() {
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

    private void showBorrowedBooks() {
        try {
            Member selected = selectedMember();
            if (selected == null) {
                return;
            }
            List<Book> books = loanService.findBorrowedBooksByMember(selected.id());
            String message = books.isEmpty()
                    ? selected.name() + " has no active loans."
                    : books.stream().map(book -> book.id() + " - " + book.title()).reduce((left, right) -> left + "\n" + right)
                            .orElseThrow();
            JOptionPane.showMessageDialog(this, message, "Borrowed Books", JOptionPane.INFORMATION_MESSAGE);
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void showHistory() {
        try {
            Member selected = selectedMember();
            if (selected == null) return;
            LoanHistoryDialog dialog = new LoanHistoryDialog(owner(), historyService, selected.id());
            dialog.setVisible(true);
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private Member selectedMember() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Select a member first.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String id = (String) tableModel.getValueAt(modelRow, 0);
        return memberService.findMemberById(id).orElse(null);
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
