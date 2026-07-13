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
}
