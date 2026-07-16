package library.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import library.exception.DataStoreException;
import library.model.Book;
import org.junit.jupiter.api.Test;

class FileBookRepositoryTest {
    @Test
    void keepsPreviousStateWhenPersistenceFails() {
        DataStore failingStore = new DataStore() {
            @Override public List<List<String>> read(String collectionName) { return List.of(); }
            @Override public void write(String collectionName, List<List<String>> records) {
                throw new DataStoreException("Write failed.");
            }
        };
        FileBookRepository repository = new FileBookRepository(failingStore);
        assertThrows(DataStoreException.class, () -> repository.save(new Book("B1", "Title", "Genre", 1)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void convertsInvalidStoredBookToDataStoreException() {
        DataStore invalidStore = new DataStore() {
            @Override public List<List<String>> read(String collectionName) {
                return List.of(List.of("bad id", "Title", "Genre", "1"));
            }
            @Override public void write(String collectionName, List<List<String>> records) { }
        };

        assertThrows(DataStoreException.class, () -> new FileBookRepository(invalidStore));
    }

    @Test
    void rejectsDuplicateStoredBookIdsAfterNormalization() {
        DataStore duplicateStore = new DataStore() {
            @Override public List<List<String>> read(String collectionName) {
                return List.of(
                        List.of("B1", "First", "Genre", "1"),
                        List.of("b1", "Second", "Genre", "1"));
            }
            @Override public void write(String collectionName, List<List<String>> records) { }
        };

        assertThrows(DataStoreException.class, () -> new FileBookRepository(duplicateStore));
    }
}
