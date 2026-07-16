package library.ui.member;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import library.model.Member;
import library.ui.UiStyles;

@SuppressWarnings("serial")
public final class MemberDialog extends JDialog {
    private final JTextField idField = new JTextField(26);
    private final JTextField nameField = new JTextField(26);
    private final JLabel errorLabel = new JLabel(" ");
    private boolean confirmed;

    public MemberDialog(Window owner, Member member) {
        super(owner, member == null ? "Add Member" : "Edit Member", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildContent(member);
        setMinimumSize(new Dimension(540, 370));
        pack();
        setLocationRelativeTo(owner);
        UiStyles.bindEscape(this);
        UiStyles.requestInitialFocus(member == null ? idField : nameField);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String memberId() {
        return idField.getText();
    }

    public String memberName() {
        return nameField.getText();
    }

    private void buildContent(Member member) {
        UiStyles.configureTextField(idField);
        UiStyles.configureTextField(nameField);
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(UiStyles.PAGE_BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(UiStyles.titleLabel(member == null ? "Add a Member" : "Edit Member Details"),
                BorderLayout.NORTH);
        header.add(UiStyles.mutedLabel(member == null
                ? "Create a member record for library checkout."
                : "The member ID is permanent. Update the member name below."), BorderLayout.CENTER);
        content.add(header, BorderLayout.NORTH);

        JPanel fields = UiStyles.card();
        fields.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(7, 4, 7, 4);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        addField(fields, constraints, 0, "Member ID", "1-32 letters, numbers, hyphens, or underscores", idField);
        addField(fields, constraints, 1, "Name", "Required", nameField);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weighty = 1.0;
        fields.add(new JLabel(), constraints);
        content.add(fields, BorderLayout.CENTER);

        if (member != null) {
            idField.setText(member.id());
            UiStyles.configureReadOnlyTextField(idField);
            idField.setToolTipText("Member IDs cannot be changed after registration.");
            nameField.setText(member.name());
        }

        errorLabel.setForeground(UiStyles.DANGER);
        JButton saveButton = UiStyles.primaryButton(member == null ? "Add Member" : "Save Changes");
        saveButton.addActionListener(event -> confirm());
        JButton cancelButton = UiStyles.secondaryButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        JPanel buttons = new JPanel(new BorderLayout(10, 0));
        buttons.setOpaque(false);
        buttons.add(errorLabel, BorderLayout.CENTER);
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttonGroup.setOpaque(false);
        buttonGroup.add(cancelButton);
        buttonGroup.add(saveButton);
        buttons.add(buttonGroup, BorderLayout.EAST);
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(saveButton);
    }

    private void addField(
            JPanel fields,
            GridBagConstraints constraints,
            int row,
            String labelText,
            String helpText,
            Component field) {
        JPanel labels = new JPanel(new BorderLayout(0, 2));
        labels.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(UiStyles.TEXT);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        label.setLabelFor(field);
        JLabel help = UiStyles.mutedLabel(helpText);
        help.setFont(help.getFont().deriveFont(11f));
        labels.add(label, BorderLayout.NORTH);
        labels.add(help, BorderLayout.CENTER);

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.weightx = 0.4;
        fields.add(labels, constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.6;
        fields.add(field, constraints);
    }

    private void confirm() {
        String id = idField.getText().strip();
        if (!id.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,31}")) {
            showValidationError("Enter a valid member ID.", idField);
            return;
        }
        if (nameField.getText().isBlank()) {
            showValidationError("Name is required.", nameField);
            return;
        }
        confirmed = true;
        dispose();
    }

    private void showValidationError(String message, JTextField field) {
        errorLabel.setText(message);
        field.requestFocusInWindow();
        field.selectAll();
    }
}
