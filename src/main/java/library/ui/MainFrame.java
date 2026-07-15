package library.ui;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import library.service.BookService;
import library.service.LoanHistoryService;
import library.service.LoanService;
import library.service.MemberService;
import library.ui.book.BookPanel;
import library.ui.loan.LoanPanel;
import library.ui.member.MemberPanel;

public final class MainFrame extends JFrame {
    private final BookPanel bookPanel;
    private final MemberPanel memberPanel;
    private final LoanPanel loanPanel;

    public MainFrame(
            BookService bookService,
            MemberService memberService,
            LoanService loanService,
            LoanHistoryService historyService) {
        if (bookService == null || memberService == null || loanService == null || historyService == null) {
            throw new IllegalArgumentException("Frame dependencies must not be null.");
        }
        setTitle("Library System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationByPlatform(true);

        Runnable dataChanged = this::refreshAll;
        bookPanel = new BookPanel(bookService, loanService, historyService, dataChanged);
        memberPanel = new MemberPanel(memberService, loanService, historyService, dataChanged);
        loanPanel = new LoanPanel(bookService, memberService, loanService, historyService, dataChanged);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books", bookPanel);
        tabs.addTab("Members", memberPanel);
        tabs.addTab("Loans", loanPanel);
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
    }

    private void refreshAll() {
        bookPanel.refreshData();
        memberPanel.refreshData();
        loanPanel.refreshData();
    }
}
