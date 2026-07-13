package library.validation;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import library.exception.ValidationException;

public final class InputRules {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9_-]{0,31}");

    private InputRules() { }

    public static String normalizeId(String value, String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName");
        if (value == null) throw new ValidationException(fieldName + " must not be null.");
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!ID_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException(fieldName + " must match [A-Z0-9][A-Z0-9_-]{0,31}.");
        }
        return normalized;
    }

    public static String normalizeDisplayText(String value, String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName");
        if (value == null) throw new ValidationException(fieldName + " must not be null.");
        String normalized = value.strip();
        if (normalized.isBlank()) throw new ValidationException(fieldName + " must not be blank.");
        return normalized;
    }

    public static String normalizeSearchQuery(String value) {
        if (value == null) throw new ValidationException("Search query must not be null.");
        return value.strip().toUpperCase(Locale.ROOT);
    }
}
