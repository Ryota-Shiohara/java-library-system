package library.ui.book;

import java.awt.BorderLayout;
import java.awt.Dialog;
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

public final class BookDialog extends JDialog {
    private final JTextField idField = new JTextField(24);
    private final JTextField titleField = new JTextField(24);
    private final JTextField genreField = new JTextField(24);
    private final JTextField totalCopiesField = new JTextField(8);
    private final JComboBox<NdcCategory> ndcBox = new JComboBox<>(NdcCategory.values());
    private boolean confirmed;

    public BookDialog(Window owner, Book book) {
        super(owner, book == null ? "Add Book" : "Edit Book", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildContent(book);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() { return confirmed; }
    public String bookId() { return idField.getText(); }
    public String bookTitle() { return titleField.getText(); }
    public String bookGenre() { return genreField.getText(); }
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
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        addField(fields, constraints, 0, "ID:", idField);
        addField(fields, constraints, 1, "Title:", titleField);
        addField(fields, constraints, 2, "Genre:", genreField);
        addField(fields, constraints, 3, "NDC:", ndcBox);
        addField(fields, constraints, 4, "Total copies:", totalCopiesField);
        if (book != null) {
            idField.setText(book.id());
            idField.setEditable(false);
            titleField.setText(book.title());
            genreField.setText(book.genre());
            totalCopiesField.setText(Integer.toString(book.totalCopies()));
            ndcBox.setSelectedItem(book.ndcCategory());
        }
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(event -> { confirmed = true; dispose(); });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveButton);
        buttons.add(cancelButton);
        setLayout(new BorderLayout());
        add(fields, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
    }

    private void addField(JPanel fields, GridBagConstraints constraints, int row, String label, java.awt.Component field) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        fields.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        fields.add(field, constraints);
    }
}
