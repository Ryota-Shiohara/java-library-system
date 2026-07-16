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
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 14f));
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        tabs.addTab("Books", bookPanel);
        tabs.addTab("Members", memberPanel);
        tabs.addTab("Loans", loanPanel);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(UiStyles.CARD_BACKGROUND);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiStyles.BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        JLabel mark = new JLabel("LS", JLabel.CENTER);
        mark.setOpaque(true);
        mark.setBackground(UiStyles.PRIMARY);
        mark.setForeground(java.awt.Color.WHITE);
        mark.setFont(mark.getFont().deriveFont(Font.BOLD, 16f));
        mark.setPreferredSize(new Dimension(42, 42));
        header.add(mark, BorderLayout.WEST);

        JPanel heading = new JPanel(new BorderLayout(0, 2));
        heading.setOpaque(false);
        JLabel title = new JLabel("Library System");
        title.setForeground(UiStyles.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));
        heading.add(title, BorderLayout.NORTH);
        heading.add(UiStyles.mutedLabel("Manage the catalog, members, and circulation from one workspace."),
                BorderLayout.CENTER);
        header.add(heading, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UiStyles.CARD_BACKGROUND);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiStyles.BORDER),
                BorderFactory.createEmptyBorder(7, 18, 7, 18)));
        JLabel localDataLabel = UiStyles.mutedLabel("Ready  |  Data is stored locally");
        localDataLabel.setFont(localDataLabel.getFont().deriveFont(12f));
        footer.add(localDataLabel, BorderLayout.WEST);

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
