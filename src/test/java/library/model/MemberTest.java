package library.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import library.exception.ValidationException;
import org.junit.jupiter.api.Test;

class MemberTest {
    @Test
    void normalizesIdentifierAndName() {
        Member member = new Member(" m-1 ", "  Ada Lovelace  ");

        assertEquals("M-1", member.id());
        assertEquals("Ada Lovelace", member.name());
    }

    @Test
    void rejectsBlankRequiredValues() {
        assertThrows(ValidationException.class, () -> new Member(" ", "Name"));
        assertThrows(ValidationException.class, () -> new Member("M1", " \n "));
        assertThrows(ValidationException.class, () -> new Member("bad id", "Name"));
    }
}
