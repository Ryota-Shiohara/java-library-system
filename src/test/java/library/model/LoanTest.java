package library.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import library.exception.ValidationException;
import org.junit.jupiter.api.Test;

class LoanTest {
    @Test
    void normalizesReferencedIdentifiers() {
        LocalDate checkoutDate = LocalDate.of(2026, 7, 1);
        Loan loan = new Loan(" loan-1 ", " b-1 ", " m-1 ", checkoutDate, checkoutDate.plusDays(14));

        assertEquals("loan-1", loan.id());
        assertEquals("B-1", loan.bookId());
        assertEquals("M-1", loan.memberId());
    }

    @Test
    void rejectsUnexpectedDueDate() {
        LocalDate checkoutDate = LocalDate.of(2026, 7, 1);

        assertThrows(ValidationException.class,
                () -> new Loan("loan-1", "B1", "M1", checkoutDate, checkoutDate.plusDays(13)));
    }
}
