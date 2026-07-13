package library.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import library.exception.DataStoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDataStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsSpecialCharacters() {
        FileDataStore store = new FileDataStore(temporaryDirectory);
        List<List<String>> records = List.of(List.of("B1", "Title\twith\nUnicode café", "Genre|One", "2"));
        store.write("books", records);
        assertEquals(records, store.read("books"));
    }

    @Test
    void missingFileReturnsEmptyList() {
        assertEquals(List.of(), new FileDataStore(temporaryDirectory).read("books"));
    }

    @Test
    void rejectsCorruptedHeader() throws Exception {
        Files.writeString(temporaryDirectory.resolve("books.data"), "BROKEN\n");
        assertThrows(DataStoreException.class, () -> new FileDataStore(temporaryDirectory).read("books"));
    }
}

