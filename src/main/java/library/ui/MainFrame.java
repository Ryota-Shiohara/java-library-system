package library.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import library.service.BookService;
import library.service.LoanHistoryService;
import library.service.LoanService;
import library.service.MemberService;
import library.ui.book.BookPanel;
import library.ui.loan.LoanPanel;
import library.ui.member.MemberPanel;

@SuppressWarnings("serial")
public final class MainFrame extends JFrame {
    static {
        UiStyles.installLookAndFeel();
    }

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
        setMinimumSize(new Dimension(900, 600));
        setLocationByPlatform(true);

        Runnable dataChanged = this::refreshAll;
        bookPanel = new BookPanel(bookService, loanService, historyService, dataChanged);
        memberPanel = new MemberPanel(memberService, loanService, historyService, dataChanged);
        loanPanel = new LoanPanel(bookService, memberService, loanService, historyService, dataChanged);

        JTabbedPane tabs = new JTabbedPane();
        UiStyles.configureTabs(tabs);
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 13f));
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        tabs.addTab("Books", bookPanel);
        tabs.addTab("Members", memberPanel);
        tabs.addTab("Loans", loanPanel);
        tabs.setToolTipTextAt(0, "Manage books, inventory, and borrowers");
        tabs.setToolTipTextAt(1, "Manage members and borrowed books");
        tabs.setToolTipTextAt(2, "Check out, return, and review loans");

        JPanel header = new JPanel(new BorderLayout(28, 0));
        header.setBackground(UiStyles.APP_BAR_BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 15, 24));

        JPanel heading = new JPanel(new BorderLayout(0, 1));
        heading.setOpaque(false);
        JLabel title = new JLabel("Library System");
        title.setForeground(UiStyles.APP_BAR_TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        heading.add(title, BorderLayout.NORTH);
        JLabel context = new JLabel("CATALOG AND CIRCULATION");
        context.setForeground(UiStyles.APP_BAR_MUTED_TEXT);
        context.setFont(context.getFont().deriveFont(Font.BOLD, 10f));
        heading.add(context, BorderLayout.SOUTH);
        header.add(heading, BorderLayout.WEST);

        JPanel storage = new JPanel(new BorderLayout(0, 2));
        storage.setOpaque(false);
        JLabel storageTitle = new JLabel("LOCAL DATA", JLabel.RIGHT);
        storageTitle.setForeground(UiStyles.APP_BAR_TEXT);
        storageTitle.setFont(storageTitle.getFont().deriveFont(Font.BOLD, 11f));
        JLabel storageDetail = new JLabel("Stored on this device", JLabel.RIGHT);
        storageDetail.setForeground(UiStyles.APP_BAR_MUTED_TEXT);
        storageDetail.setFont(storageDetail.getFont().deriveFont(11f));
        storage.add(storageTitle, BorderLayout.NORTH);
        storage.add(storageDetail, BorderLayout.SOUTH);
        header.add(storage, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UiStyles.PAGE_BACKGROUND);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiStyles.BORDER),
                BorderFactory.createEmptyBorder(7, 24, 7, 24)));
        JLabel localDataLabel = UiStyles.mutedLabel("Data is stored on this device.");
        localDataLabel.setFont(localDataLabel.getFont().deriveFont(11f));
        footer.add(localDataLabel, BorderLayout.WEST);
        JLabel readyLabel = new JLabel("OFFLINE READY");
        readyLabel.setForeground(UiStyles.PRIMARY);
        readyLabel.setFont(readyLabel.getFont().deriveFont(Font.BOLD, 10f));
        footer.add(readyLabel, BorderLayout.EAST);

        setLayout(new BorderLayout());
        getContentPane().setBackground(UiStyles.PAGE_BACKGROUND);
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private void refreshAll() {
        bookPanel.refreshData();
        memberPanel.refreshData();
        loanPanel.refreshData();
    }
}
