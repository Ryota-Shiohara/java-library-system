package library.ui.loan;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
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
import javax.swing.SwingUtilities;
import library.exception.LibraryException;
import library.model.Loan;
import library.service.BookService;
import library.service.LoanHistoryService;
import library.service.LoanService;
import library.service.MemberService;
import library.service.dto.LoanDetails;
import library.ui.ReadOnlyTableModel;
import library.ui.UiStyles;

@SuppressWarnings("serial")
public final class LoanPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(LoanPanel.class.getName());

    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;
    private final LoanHistoryService historyService;
    private final Runnable dataChanged;
    private final ReadOnlyTableModel tableModel = new ReadOnlyTableModel(
            new String[] {"Loan ID", "Book", "Member", "Checkout Date", "Due Date", "Status"},
            new Class<?>[] {String.class, String.class, String.class, String.class, String.class, String.class});
    private final JTable table = new JTable(tableModel);
    private final JLabel resultLabel = UiStyles.mutedLabel("0 active loans");
    private final JLabel messageLabel = UiStyles.mutedLabel("Select a loan to return it.");
    private final JButton returnButton = UiStyles.dangerButton("Return Selected");

    public LoanPanel(
            BookService bookService,
            MemberService memberService,
            LoanService loanService,
            LoanHistoryService historyService,
            Runnable dataChanged) {
        if (bookService == null || memberService == null || loanService == null
                || historyService == null || dataChanged == null) {
            throw new IllegalArgumentException("Panel dependencies must not be null.");
        }
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
        this.historyService = historyService;
        this.dataChanged = dataChanged;
        buildContent();
        refreshData();
    }

    public void refreshData() {
        String selectedId = selectedLoanId(false);
        List<LoanDetails> allLoans = loanService.listActiveLoans();

        tableModel.setRowCount(0);
        for (LoanDetails loan : allLoans) {
            tableModel.addRow(new Object[] {
                    loan.id(),
                    loan.book().id() + " - " + loan.book().title(),
                    loan.member().id() + " - " + loan.member().name(),
                    loan.checkoutDate().toString(),
                    loan.dueDate().toString(),
                    loan.overdue() ? "Overdue" : "On time"
            });
        }

        resultLabel.setText(allLoans.size() + (allLoans.size() == 1 ? " active loan" : " active loans"));
        restoreSelection(selectedId);
        updateSelectionActions();
    }

    private void buildContent() {
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        setBackground(UiStyles.PAGE_BACKGROUND);
        add(createTopArea(), BorderLayout.NORTH);
        add(createTableArea(), BorderLayout.CENTER);
    }

    private JPanel createTopArea() {
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);
        text.add(UiStyles.titleLabel("Loans"), BorderLayout.NORTH);
        text.add(UiStyles.mutedLabel("Check out available books, monitor due dates, and process returns."),
                BorderLayout.CENTER);
        heading.add(text, BorderLayout.CENTER);
        return heading;
    }

    private JPanel createTableArea() {
        UiStyles.configureTable(table);
        UiStyles.setColumnWidths(table, 175, 250, 210, 100, 100, 80);
        table.getColumnModel().getColumn(5).setCellRenderer(UiStyles.statusRenderer());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        returnButton.addActionListener(event -> returnLoan());
        JButton checkoutButton = UiStyles.primaryButton("New Checkout");
        checkoutButton.setToolTipText("Choose an available book and a registered member");
        checkoutButton.addActionListener(event -> checkout());
        JButton historyButton = UiStyles.secondaryButton("Loan History");
        historyButton.setToolTipText("Search completed loans");
        historyButton.addActionListener(event -> showHistory());

        JPanel tableCard = UiStyles.card();
        tableCard.setLayout(new BorderLayout(0, 10));
        JPanel tableHeading = new JPanel(new BorderLayout());
        tableHeading.setOpaque(false);
        tableHeading.add(UiStyles.sectionLabel("Active Circulation"), BorderLayout.WEST);
        tableHeading.add(resultLabel, BorderLayout.EAST);
        JPanel tableTop = new JPanel();
        tableTop.setLayout(new BoxLayout(tableTop, BoxLayout.Y_AXIS));
        tableTop.setOpaque(false);
        tableTop.add(tableHeading);
        tableTop.add(Box.createVerticalStrut(9));
        JPanel commandBar = new JPanel(new FlowLayout(FlowLayout.LEADING, 7, 0));
        commandBar.setOpaque(false);
        commandBar.add(checkoutButton);
        commandBar.add(returnButton);
        commandBar.add(historyButton);
        tableTop.add(commandBar);
        tableCard.add(tableTop, BorderLayout.NORTH);
        tableCard.add(UiStyles.tableScrollPane(table), BorderLayout.CENTER);

        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setOpaque(false);
        actionBar.add(messageLabel, BorderLayout.CENTER);
        tableCard.add(actionBar, BorderLayout.SOUTH);
        return tableCard;
    }

    private void checkout() {
        try {
            CheckoutDialog dialog = new CheckoutDialog(owner(), bookService, memberService);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) {
                return;
            }
            Loan loan = loanService.checkout(dialog.selectedBookId(), dialog.selectedMemberId());
            dataChanged.run();
            selectLoan(loan.id());
            showSuccess("Checkout completed. Due date: " + loan.dueDate());
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void returnLoan() {
        try {
            String loanId = selectedLoanId(true);
            if (loanId == null) {
                return;
            }
            LoanDetails loan = loanService.findActiveLoanById(loanId).orElse(null);
            if (loan == null) {
                showError("The selected loan is no longer active.");
                refreshData();
                return;
            }
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Return " + loan.book().title() + " from " + loan.member().name() + "?\n"
                            + "Loan ID: " + loan.id() + "\nDue date: " + loan.dueDate(),
                    "Return Book",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            loanService.returnLoan(loanId);
            dataChanged.run();
            showSuccess("Return completed for loan " + loanId + ".");
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private void showHistory() {
        try {
            LoanHistoryDialog dialog = new LoanHistoryDialog(owner(), historyService);
            dialog.setVisible(true);
        } catch (LibraryException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        }
    }

    private String selectedLoanId(boolean showSelectionError) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            if (showSelectionError) {
                showError("Select a loan first.");
            }
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return (String) tableModel.getValueAt(modelRow, 0);
    }

    private void restoreSelection(String id) {
        if (id != null) {
            selectLoan(id);
        }
    }

    private void selectLoan(String id) {
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
        returnButton.setEnabled(selected);
        if (selected) {
            String loanId = selectedLoanId(false);
            messageLabel.setForeground(UiStyles.TEXT);
            messageLabel.setText("Selected loan: " + loanId);
        } else {
            messageLabel.setForeground(UiStyles.MUTED_TEXT);
            messageLabel.setText(tableModel.getRowCount() == 0
                    ? "There are no active loans."
                    : "Select a loan to return it.");
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
        LOGGER.log(Level.SEVERE, "Unexpected error in loan panel.", exception);
        showError("An unexpected error occurred.");
    }

}
