package library.model;

import java.util.Arrays;
import java.util.Locale;
import library.exception.ValidationException;

public enum NdcCategory {
    GENERAL_WORKS("0", "General works"),
    PHILOSOPHY("1", "Philosophy"),
    HISTORY("2", "History"),
    SOCIAL_SCIENCES("3", "Social sciences"),
    NATURAL_SCIENCES("4", "Natural sciences"),
    TECHNOLOGY("5", "Technology"),
    INDUSTRY("6", "Industry"),
    ARTS("7", "Arts"),
    LANGUAGE("8", "Language"),
    LITERATURE("9", "Literature");

    private final String code;
    private final String displayName;

    NdcCategory(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public static NdcCategory fromCode(String code) {
        if (code == null) {
            throw new ValidationException("NDC code must not be null.");
        }
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(category -> category.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ValidationException("NDC code must be a digit from 0 to 9."));
    }

    public static String normalizeCode(String code) {
        return fromCode(code).code;
    }

    public static String normalizeOptionalCode(String code) {
        if (code == null || code.strip().isEmpty()) {
            return "";
        }
        return normalizeCode(code);
    }

    @Override
    public String toString() {
        return code + " " + displayName;
    }
}
