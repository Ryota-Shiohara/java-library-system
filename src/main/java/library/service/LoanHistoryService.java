package library.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import library.exception.ValidationException;
import library.model.LoanHistory;
import library.model.NdcCategory;
import library.repository.LoanHistoryRepository;
import library.validation.InputRules;

public final class LoanHistoryService {
    private final LoanHistoryRepository historyRepository;

    public LoanHistoryService(LoanHistoryRepository historyRepository) {
        if (historyRepository == null) {
            throw new ValidationException("Loan history repository must not be null.");
        }
        this.historyRepository = historyRepository;
    }

    public List<LoanHistory> listHistory() {
        return sort(historyRepository.findAllHistory());
    }

    public List<LoanHistory> searchHistory(
            String query,
            String ndcCode,
            LocalDate fromDate,
            LocalDate toDate) {
        String normalizedQuery = InputRules.normalizeSearchQuery(query);
        String normalizedNdcCode = NdcCategory.normalizeOptionalCode(ndcCode);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException("From date cannot be after to date.");
        }
        return sort(historyRepository.findAllHistory().stream()
                .filter(history -> normalizedNdcCode.isEmpty()
                        || history.book().ndcCode().equals(normalizedNdcCode))
                .filter(history -> fromDate == null || !history.returnDate().isBefore(fromDate))
                .filter(history -> toDate == null || !history.returnDate().isAfter(toDate))
                .filter(history -> normalizedQuery.isEmpty() || matches(history, normalizedQuery))
                .toList());
    }

    private boolean matches(LoanHistory history, String query) {
        return contains(history.id(), query)
                || contains(history.book().id(), query)
                || contains(history.book().title(), query)
                || contains(history.book().genre(), query)
                || contains(history.book().ndcCode(), query)
                || contains(history.book().ndcCategory().displayName(), query)
                || contains(history.member().id(), query)
                || contains(history.member().name(), query);
    }

    private boolean contains(String value, String query) {
        return value.toUpperCase(Locale.ROOT).contains(query);
    }

    private List<LoanHistory> sort(List<LoanHistory> histories) {
        return histories.stream()
                .sorted(Comparator.comparing(LoanHistory::returnDate).thenComparing(LoanHistory::id))
                .toList();
    }
}
