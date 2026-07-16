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
        List<List<String>> records = List.of(
                List.of("B1", "Title\twith\nUnicode café", "Genre|One", "2"),
                List.of("B2", "Second Title", "Reference", "1"));
        store.write("books", records);
        assertEquals(records, store.read("books"));
        assertThrows(UnsupportedOperationException.class,
                () -> store.read("books").add(List.of("B3", "Title", "Genre", "1")));
        assertThrows(UnsupportedOperationException.class,
                () -> store.read("books").get(0).add("extra"));
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

    @Test
    void rejectsInvalidBase64AndEmptyRecords() throws Exception {
        Files.writeString(temporaryDirectory.resolve("members.data"), "LIBRARY-DATA-V1\n%%%\n");
        assertThrows(DataStoreException.class, () -> new FileDataStore(temporaryDirectory).read("members"));

        Files.writeString(temporaryDirectory.resolve("members.data"), "LIBRARY-DATA-V1\n\n");
        assertThrows(DataStoreException.class, () -> new FileDataStore(temporaryDirectory).read("members"));
    }
}
