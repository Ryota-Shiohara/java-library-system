package library.model;

import library.exception.ValidationException;
import library.validation.InputRules;

public record Book(String id, String title, String genre, int totalCopies) {
    public Book {
        id = InputRules.normalizeId(id, "Book ID");
        title = InputRules.normalizeDisplayText(title, "Book title");
        genre = InputRules.normalizeDisplayText(genre, "Book genre");
        if (totalCopies < 1) throw new ValidationException("Total copies must be at least 1.");
    }
}
