package library.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import library.exception.ValidationException;
import org.junit.jupiter.api.Test;

class NdcCategoryTest {
    @Test
    void resolvesAllTenPrimaryCategories() {
        assertEquals("General works", NdcCategory.fromCode(" 0 ").displayName());
        assertEquals("Literature", NdcCategory.fromCode("9").displayName());
        assertEquals(10, NdcCategory.values().length);
    }

    @Test
    void rejectsInvalidCodes() {
        assertThrows(ValidationException.class, () -> NdcCategory.fromCode("10"));
        assertThrows(ValidationException.class, () -> NdcCategory.fromCode("A"));
        assertThrows(ValidationException.class, () -> NdcCategory.fromCode(" "));
    }

    @Test
    void keepsLegacyBookConstructorCompatible() {
        assertEquals("0", new Book("B1", "Title", "Genre", 1).ndcCode());
        assertEquals("4", new Book("B2", "Science", "Science", 1, " 4 ").ndcCode());
    }
}
