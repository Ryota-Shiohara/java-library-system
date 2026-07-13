package library.ui.loan;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import library.exception.LibraryException;
import library.service.BookService;
import library.service.LoanService;
import library.service.MemberService;
import library.service.dto.LoanDetails;

public final class LoanPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(LoanPanel.class.getName());
    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;
    private final Runnable dataChanged;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {"Loan ID", "Book", "Member", "Checkout Date", "Due Date", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public LoanPanel(
            BookService bookService,
            MemberService memberService,
            LoanService loanService,
            Runnable dataChanged) {
        if (bookService == null || memberService == null || loanService == null || dataChanged == null) {
            throw new IllegalArgumentException("Panel dependencies must not be null.");
        }
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
        this.dataChanged = dataChanged;
        buildContent();
        refreshData();
    }

    public void refreshData() {
        tableModel.setRowCount(0);
        for (LoanDetails loan : loanService.listActiveLoans()) {
            tableModel.addRow(new Object[] {
                    loan.id(),
                    loan.book().id() + " - " + loan.book().title(),
                    loan.member().id() + " - " + loan.member().name(),
                    loan.checkoutDate().toString(),
                    loan.dueDate().toString(),
                    loan.overdue() ? "Overdue" : "On time"
            });
        }
    }

    private void buildContent() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        JButton checkoutButton = new JButton("Checkout");
        checkoutButton.addActionListener(event -> checkout());
        JButton returnButton = new JButton("Return");
        returnButton.addActionListener(event -> returnLoan());
        toolbar.add(checkoutButton);
        toolbar.add(returnButton);
        add(toolbar, BorderLayout.NORTH);
    }

    private void checkout() {
        try {
            CheckoutDialog dialog = new CheckoutDialog(owner(), bookService, memberService);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) {
                return;
            }
            loanService.checkout(dialog.selectedBookId(), dialog.selectedMemberId());
            dataChanged.run();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void returnLoan() {
        try {
            String loanId = selectedLoanId();
            if (loanId == null) {
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Return the selected loan?", "Return Loan", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            loanService.returnLoan(loanId);
            dataChanged.run();
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private String selectedLoanId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Select a loan first.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return (String) tableModel.getValueAt(modelRow, 0);
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Library System", JOptionPane.ERROR_MESSAGE);
    }

    private void handleUnexpectedError(Exception exception) {
        LOGGER.log(Level.SEVERE, "Unexpected error in loan panel.", exception);
        showError("An unexpected error occurred.");
    }
}
