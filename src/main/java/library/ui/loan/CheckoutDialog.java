package library.ui.loan;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import library.model.Member;
import library.service.BookService;
import library.service.MemberService;
import library.service.dto.BookSummary;

public final class CheckoutDialog extends JDialog {
    private final JComboBox<BookOption> bookBox = new JComboBox<>();
    private final JComboBox<MemberOption> memberBox = new JComboBox<>();
    private boolean confirmed;

    public CheckoutDialog(Window owner, BookService bookService, MemberService memberService) {
        super(owner, "Checkout", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        populateOptions(bookService, memberService);
        buildContent();
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String selectedBookId() {
        BookOption option = (BookOption) bookBox.getSelectedItem();
        return option == null ? null : option.id();
    }

    public String selectedMemberId() {
        MemberOption option = (MemberOption) memberBox.getSelectedItem();
        return option == null ? null : option.id();
    }

    private void populateOptions(BookService bookService, MemberService memberService) {
        List<BookSummary> availableBooks = bookService.listBooks().stream()
                .filter(summary -> summary.availableCopies() > 0)
                .toList();
        for (BookSummary book : availableBooks) {
            bookBox.addItem(new BookOption(book.id(), book.title(), book.availableCopies()));
        }
        for (Member member : memberService.listMembers()) {
            memberBox.addItem(new MemberOption(member.id(), member.name()));
        }
    }

    private void buildContent() {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        fields.add(new JLabel("Book:"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        fields.add(bookBox, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        fields.add(new JLabel("Member:"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        fields.add(memberBox, constraints);

        boolean canCheckout = bookBox.getItemCount() > 0 && memberBox.getItemCount() > 0;
        JLabel availabilityLabel = new JLabel();
        if (bookBox.getItemCount() == 0) {
            availabilityLabel.setText("No books are available for checkout.");
        } else if (memberBox.getItemCount() == 0) {
            availabilityLabel.setText("No members are available for checkout.");
        }
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        fields.add(availabilityLabel, constraints);

        JButton checkoutButton = new JButton("Checkout");
        checkoutButton.setEnabled(canCheckout);
        checkoutButton.addActionListener(event -> {
            confirmed = true;
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(checkoutButton);
        buttons.add(cancelButton);

        setLayout(new BorderLayout());
        add(fields, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(checkoutButton);
    }

    private record BookOption(String id, String title, int availableCopies) {
        @Override
        public String toString() {
            return id + " - " + title + " (" + availableCopies + " available)";
        }
    }

    private record MemberOption(String id, String name) {
        @Override
        public String toString() {
            return id + " - " + name;
        }
    }
}
