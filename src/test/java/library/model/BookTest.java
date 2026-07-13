package library.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import library.exception.ValidationException;
import org.junit.jupiter.api.Test;

class BookTest {
    @Test
    void normalizesIdentifiersAndDisplayText() {
        Book book = new Book(" b-1 ", "  Clean Code  ", "  Software  ", 2);
        assertEquals("B-1", book.id());
        assertEquals("Clean Code", book.title());
        assertEquals("Software", book.genre());
    }

    @Test
    void rejectsBlankRequiredValues() {
        assertThrows(ValidationException.class, () -> new Book("B1", " ", "Genre", 1));
        assertThrows(ValidationException.class, () -> new Book("B1", "Title", "\n", 1));
    }

    @Test
    void rejectsInvalidIdentifierAndCopyCount() {
        assertThrows(ValidationException.class, () -> new Book("bad id", "Title", "Genre", 1));
        assertThrows(ValidationException.class, () -> new Book("B1", "Title", "Genre", 0));
    }
}
