package library.ui;

import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public final class ReadOnlyTableModel extends DefaultTableModel {
    private final Class<?>[] columnTypes;

    public ReadOnlyTableModel(String[] columnNames, Class<?>[] columnTypes) {
        super(columnNames, 0);
        if (columnNames.length != columnTypes.length) {
            throw new IllegalArgumentException("Column names and types must have the same length.");
        }
        this.columnTypes = columnTypes.clone();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }
}
