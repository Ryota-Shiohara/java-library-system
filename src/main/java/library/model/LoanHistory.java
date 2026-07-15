package library.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import library.exception.ValidationException;
import library.validation.InputRules;

public record LoanHistory(
        String id,
        Book book,
        Member member,
        LocalDate checkoutDate,
        LocalDate dueDate,
        LocalDate returnDate) {
    public LoanHistory {
        id = InputRules.normalizeDisplayText(id, "Loan ID");
        if (book == null) {
            throw new ValidationException("History book must not be null.");
        }
        if (member == null) {
            throw new ValidationException("History member must not be null.");
        }
        if (checkoutDate == null) {
            throw new ValidationException("Checkout date must not be null.");
        }
        if (dueDate == null) {
            throw new ValidationException("Due date must not be null.");
        }
        if (!dueDate.equals(checkoutDate.plusDays(14))) {
            throw new ValidationException("Due date must be 14 days after checkout date.");
        }
        if (returnDate == null) {
            throw new ValidationException("Return date must not be null.");
        }
        if (returnDate.isBefore(checkoutDate)) {
            throw new ValidationException("Return date cannot be before checkout date.");
        }
    }

    public boolean overdue() {
        return returnDate.isAfter(dueDate);
    }

    public long overdueDays() {
        return overdue() ? ChronoUnit.DAYS.between(dueDate, returnDate) : 0;
    }
}
