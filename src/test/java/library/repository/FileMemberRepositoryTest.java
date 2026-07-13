package library.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import library.exception.DataStoreException;
import library.model.Member;
import org.junit.jupiter.api.Test;

class FileMemberRepositoryTest {
    @Test
    void keepsPreviousStateWhenPersistenceFails() {
        DataStore failingStore = new DataStore() {
            @Override
            public List<List<String>> read(String collectionName) {
                return List.of();
            }

            @Override
            public void write(String collectionName, List<List<String>> records) {
                throw new DataStoreException("Write failed.");
            }
        };
        FileMemberRepository repository = new FileMemberRepository(failingStore);

        assertThrows(DataStoreException.class, () -> repository.save(new Member("M1", "Ada")));
        assertEquals(List.of(), repository.findAll());
    }
}
