package library.ui.book;

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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import library.exception.ValidationException;
import library.model.Book;
import library.model.NdcCategory;
import library.ui.UiStyles;

@SuppressWarnings("serial")
public final class BookDialog extends JDialog {
    private final JTextField idField = new JTextField(26);
    private final JTextField titleField = new JTextField(26);
    private final JTextField genreField = new JTextField(26);
    private final JTextField totalCopiesField = new JTextField(10);
    private final JComboBox<NdcCategory> ndcBox = new JComboBox<>(NdcCategory.values());
    private final JLabel errorLabel = new JLabel(" ");
    private boolean confirmed;

    public BookDialog(Window owner, Book book) {
        super(owner, book == null ? "Add Book" : "Edit Book", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildContent(book);
        setMinimumSize(new Dimension(560, 500));
        pack();
        setLocationRelativeTo(owner);
        UiStyles.bindEscape(this);
        UiStyles.requestInitialFocus(book == null ? idField : titleField);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String bookId() {
        return idField.getText();
    }

    public String bookTitle() {
        return titleField.getText();
    }

    public String bookGenre() {
        return genreField.getText();
    }

    public String ndcCode() {
        NdcCategory category = (NdcCategory) ndcBox.getSelectedItem();
        return category == null ? null : category.code();
    }

    public int totalCopies() {
        try {
            return Integer.parseInt(totalCopiesField.getText().strip());
        } catch (NumberFormatException exception) {
            throw new ValidationException("Total copies must be an integer.");
        }
    }

    private void buildContent(Book book) {
        UiStyles.configureTextField(idField);
        UiStyles.configureTextField(titleField);
        UiStyles.configureTextField(genreField);
        UiStyles.configureTextField(totalCopiesField);
        UiStyles.configureComboBox(ndcBox);
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(UiStyles.PAGE_BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(UiStyles.titleLabel(book == null ? "Add a Book" : "Edit Book Details"), BorderLayout.NORTH);
        header.add(UiStyles.mutedLabel(book == null
                ? "Register a new title and its available inventory."
                : "The book ID is permanent. Update the remaining details below."), BorderLayout.CENTER);
        content.add(header, BorderLayout.NORTH);

        JPanel fields = UiStyles.card();
        fields.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 4, 5, 4);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        addField(fields, constraints, 0, "Book ID", "1-32 letters, numbers, hyphens, or underscores", idField);
        addField(fields, constraints, 1, "Title", "Required", titleField);
        addField(fields, constraints, 2, "Genre", "Required free-text category", genreField);
        addField(fields, constraints, 3, "NDC category", "Primary classification from 0 to 9", ndcBox);
        addField(fields, constraints, 4, "Total copies", "Must be at least 1", totalCopiesField);
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 2;
        constraints.weighty = 1.0;
        fields.add(new JLabel(), constraints);
        content.add(fields, BorderLayout.CENTER);

        if (book != null) {
            idField.setText(book.id());
            UiStyles.configureReadOnlyTextField(idField);
            idField.setToolTipText("Book IDs cannot be changed after registration.");
            titleField.setText(book.title());
            genreField.setText(book.genre());
            totalCopiesField.setText(Integer.toString(book.totalCopies()));
            ndcBox.setSelectedItem(book.ndcCategory());
        }

        errorLabel.setForeground(UiStyles.DANGER);
        JButton saveButton = UiStyles.primaryButton(book == null ? "Add Book" : "Save Changes");
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
        constraints.weightx = 0.35;
        fields.add(labels, constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.65;
        fields.add(field, constraints);
    }

    private void confirm() {
        String id = idField.getText().strip();
        if (!id.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,31}")) {
            showValidationError("Enter a valid book ID.", idField);
            return;
        }
        if (titleField.getText().isBlank()) {
            showValidationError("Title is required.", titleField);
            return;
        }
        if (genreField.getText().isBlank()) {
            showValidationError("Genre is required.", genreField);
            return;
        }
        try {
            if (totalCopies() < 1) {
                showValidationError("Total copies must be at least 1.", totalCopiesField);
                return;
            }
        } catch (ValidationException exception) {
            showValidationError(exception.getMessage(), totalCopiesField);
            return;
        }
        confirmed = true;
        dispose();
    }

    private void showValidationError(String message, Component field) {
        errorLabel.setText(message);
        field.requestFocusInWindow();
        if (field instanceof JTextField textField) {
            textField.selectAll();
        }
    }
}
