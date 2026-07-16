package library.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import library.exception.DataStoreException;
import library.model.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileMemberRepositoryTest {
    @TempDir
    Path temporaryDirectory;

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

    @Test
    void roundTripsMultipleMembersAndReturnsImmutableSortedList() {
        FileMemberRepository repository = new FileMemberRepository(new FileDataStore(temporaryDirectory));
        repository.save(new Member("M2", "Grace\tHopper"));
        repository.save(new Member("M1", "Ada Lovelace"));

        FileMemberRepository restored = new FileMemberRepository(new FileDataStore(temporaryDirectory));

        assertEquals(List.of("M1", "M2"), restored.findAll().stream().map(Member::id).toList());
        assertEquals("Grace\tHopper", restored.findById("m2").orElseThrow().name());
        assertThrows(UnsupportedOperationException.class,
                () -> restored.findAll().add(new Member("M3", "Katherine Johnson")));
    }

    @Test
    void rejectsMalformedAndDuplicateStoredMembers() {
        DataStore malformedStore = fixedStore(List.of(List.of("bad id", "Ada")));
        DataStore duplicateStore = fixedStore(List.of(
                List.of("M1", "Ada"),
                List.of("m1", "Grace")));

        assertThrows(DataStoreException.class, () -> new FileMemberRepository(malformedStore));
        assertThrows(DataStoreException.class, () -> new FileMemberRepository(duplicateStore));
    }

    private DataStore fixedStore(List<List<String>> records) {
        return new DataStore() {
            @Override
            public List<List<String>> read(String collectionName) {
                return records;
            }

            @Override
            public void write(String collectionName, List<List<String>> newRecords) { }
        };
    }
}
