package library.model;

import java.time.LocalDate;
import library.exception.ValidationException;
import library.validation.InputRules;

public record Loan(String id, String bookId, String memberId, LocalDate checkoutDate, LocalDate dueDate) {
    public Loan {
        id = InputRules.normalizeDisplayText(id, "Loan ID");
        bookId = InputRules.normalizeId(bookId, "Book ID");
        memberId = InputRules.normalizeId(memberId, "Member ID");
        if (checkoutDate == null) {
            throw new ValidationException("Checkout date must not be null.");
        }
        if (dueDate == null) {
            throw new ValidationException("Due date must not be null.");
        }
        if (!dueDate.equals(checkoutDate.plusDays(14))) {
            throw new ValidationException("Due date must be 14 days after checkout date.");
        }
    }
}
