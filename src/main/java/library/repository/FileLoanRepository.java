package library.repository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import library.exception.DataStoreException;
import library.exception.DuplicateIdException;
import library.exception.EntityNotFoundException;
import library.exception.ValidationException;
import library.model.Book;
import library.model.Loan;
import library.model.LoanHistory;
import library.model.Member;
import library.validation.InputRules;

public final class FileLoanRepository implements LoanRepository {
    private static final String ACTIVE_MARKER = "ACTIVE";
    private static final String HISTORY_MARKER = "HISTORY";
    private final DataStore dataStore;
    private Map<String, Loan> loans;
    private Map<String, LoanHistory> histories;

    public FileLoanRepository(DataStore dataStore) {
        if (dataStore == null) {
            throw new ValidationException("Data store must not be null.");
        }
        this.dataStore = dataStore;
        LoadedRecords loaded = loadRecords(dataStore.read("loans"));
        this.loans = loaded.loans();
        this.histories = loaded.histories();
    }

    @Override
    public List<Loan> findAll() {
        return List.copyOf(sortedLoans(loans));
    }

    @Override
    public Optional<Loan> findById(String id) {
        return Optional.ofNullable(loans.get(normalizeLoanId(id)));
    }

    @Override
    public List<Loan> findByBookId(String bookId) {
        String normalizedBookId = InputRules.normalizeId(bookId, "Book ID");
        return loans.values().stream()
                .filter(loan -> loan.bookId().equals(normalizedBookId))
                .sorted(Comparator.comparing(Loan::id))
                .toList();
    }

    @Override
    public List<Loan> findByMemberId(String memberId) {
        String normalizedMemberId = InputRules.normalizeId(memberId, "Member ID");
        return loans.values().stream()
                .filter(loan -> loan.memberId().equals(normalizedMemberId))
                .sorted(Comparator.comparing(Loan::id))
                .toList();
    }

    @Override
    public List<LoanHistory> findAllHistory() {
        return List.copyOf(sortedHistories(histories));
    }

    @Override
    public List<LoanHistory> findHistoryByBookId(String bookId) {
        String normalizedBookId = InputRules.normalizeId(bookId, "Book ID");
        return histories.values().stream()
                .filter(history -> history.book().id().equals(normalizedBookId))
                .sorted(historyComparator())
                .toList();
    }

    @Override
    public List<LoanHistory> findHistoryByMemberId(String memberId) {
        String normalizedMemberId = InputRules.normalizeId(memberId, "Member ID");
        return histories.values().stream()
                .filter(history -> history.member().id().equals(normalizedMemberId))
                .sorted(historyComparator())
                .toList();
    }

    @Override
    public boolean existsByBookIdAndMemberId(String bookId, String memberId) {
        String normalizedBookId = InputRules.normalizeId(bookId, "Book ID");
        String normalizedMemberId = InputRules.normalizeId(memberId, "Member ID");
        return loans.values().stream().anyMatch(loan -> loan.bookId().equals(normalizedBookId)
                && loan.memberId().equals(normalizedMemberId));
    }

    @Override
    public int countActiveLoansForBook(String bookId) {
        return findByBookId(bookId).size();
    }

    @Override
    public boolean hasActiveLoanForMember(String memberId) {
        return !findByMemberId(memberId).isEmpty();
    }

    @Override
    public void save(Loan loan) {
        if (loan == null) {
            throw new ValidationException("Loan must not be null.");
        }
        if (histories.containsKey(loan.id())) {
            throw new DuplicateIdException("Loan ID already exists: " + loan.id() + ".");
        }
        Map<String, Loan> candidate = new LinkedHashMap<>(loans);
        candidate.put(loan.id(), loan);
        persist(candidate, histories);
        loans = candidate;
    }

    @Override
    public void deleteById(String id) {
        String normalizedId = normalizeLoanId(id);
        if (!loans.containsKey(normalizedId)) {
            throw new EntityNotFoundException("Loan not found: " + normalizedId + ".");
        }
        Map<String, Loan> candidate = new LinkedHashMap<>(loans);
        candidate.remove(normalizedId);
        persist(candidate, histories);
        loans = candidate;
    }

    @Override
    public void completeReturn(String loanId, LoanHistory history) {
        String normalizedLoanId = normalizeLoanId(loanId);
        if (history == null) {
            throw new ValidationException("Loan history must not be null.");
        }
        Loan loan = loans.get(normalizedLoanId);
        if (loan == null) {
            throw new EntityNotFoundException("Loan not found: " + normalizedLoanId + ".");
        }
        validateHistoryMatchesLoan(normalizedLoanId, loan, history);
        if (histories.containsKey(normalizedLoanId)) {
            throw new DuplicateIdException("Loan history ID already exists: " + normalizedLoanId + ".");
        }
        Map<String, Loan> candidateLoans = new LinkedHashMap<>(loans);
        Map<String, LoanHistory> candidateHistories = new LinkedHashMap<>(histories);
        candidateLoans.remove(normalizedLoanId);
        candidateHistories.put(normalizedLoanId, history);
        persist(candidateLoans, candidateHistories);
        loans = candidateLoans;
        histories = candidateHistories;
    }

    private LoadedRecords loadRecords(List<List<String>> records) {
        Map<String, Loan> loaded = new LinkedHashMap<>();
        Map<String, LoanHistory> loadedHistories = new LinkedHashMap<>();
        for (List<String> record : records) {
            if (record.size() == 5) {
                addActiveLoan(loaded, loadedHistories, record, 0);
            } else if (record.size() == 6 && ACTIVE_MARKER.equals(record.get(0))) {
                addActiveLoan(loaded, loadedHistories, record, 1);
            } else if (record.size() == 12 && HISTORY_MARKER.equals(record.get(0))) {
                addHistory(loaded, loadedHistories, record);
            } else {
                throw new DataStoreException("Loan record has an invalid format.");
            }
        }
        return new LoadedRecords(loaded, loadedHistories);
    }

    private void addActiveLoan(
            Map<String, Loan> loaded,
            Map<String, LoanHistory> loadedHistories,
            List<String> record,
            int offset) {
        try {
            Loan loan = new Loan(record.get(offset), record.get(offset + 1), record.get(offset + 2),
                    LocalDate.parse(record.get(offset + 3)), LocalDate.parse(record.get(offset + 4)));
            ensureUniqueId(loaded, loadedHistories, loan.id());
            loaded.put(loan.id(), loan);
        } catch (DateTimeParseException exception) {
            throw new DataStoreException("Loan dates must use ISO format.", exception);
        } catch (ValidationException exception) {
            throw new DataStoreException("Invalid loan record.", exception);
        }
    }

    private void addHistory(
            Map<String, Loan> loaded,
            Map<String, LoanHistory> loadedHistories,
            List<String> record) {
        try {
            Book book = new Book(record.get(2), record.get(3), record.get(4), Integer.parseInt(record.get(5)), record.get(6));
            Member member = new Member(record.get(7), record.get(8));
            LoanHistory history = new LoanHistory(
                    record.get(1),
                    book,
                    member,
                    LocalDate.parse(record.get(9)),
                    LocalDate.parse(record.get(10)),
                    LocalDate.parse(record.get(11)));
            ensureUniqueId(loaded, loadedHistories, history.id());
            loadedHistories.put(history.id(), history);
        } catch (DateTimeParseException exception) {
            throw new DataStoreException("History dates must use ISO format.", exception);
        } catch (NumberFormatException exception) {
            throw new DataStoreException("History total copies must be an integer.", exception);
        } catch (ValidationException exception) {
            throw new DataStoreException("Invalid loan history record.", exception);
        }
    }

    private void ensureUniqueId(
            Map<String, Loan> loaded,
            Map<String, LoanHistory> loadedHistories,
            String id) {
        if (loaded.containsKey(id) || loadedHistories.containsKey(id)) {
            throw new DataStoreException("Duplicate loan ID: " + id + ".");
        }
    }

    private void persist(Map<String, Loan> candidateLoans, Map<String, LoanHistory> candidateHistories) {
        List<List<String>> records = new ArrayList<>();
        candidateLoans.values().stream()
                .sorted(Comparator.comparing(Loan::id))
                .forEach(loan -> records.add(List.of(
                        ACTIVE_MARKER,
                        loan.id(),
                        loan.bookId(),
                        loan.memberId(),
                        loan.checkoutDate().toString(),
                        loan.dueDate().toString())));
        candidateHistories.values().stream()
                .sorted(Comparator.comparing(LoanHistory::id))
                .forEach(history -> records.add(List.of(
                        HISTORY_MARKER,
                        history.id(),
                        history.book().id(),
                        history.book().title(),
                        history.book().genre(),
                        Integer.toString(history.book().totalCopies()),
                        history.book().ndcCode(),
                        history.member().id(),
                        history.member().name(),
                        history.checkoutDate().toString(),
                        history.dueDate().toString(),
                        history.returnDate().toString())));
        dataStore.write("loans", records);
    }

    private List<Loan> sortedLoans(Map<String, Loan> source) {
        return source.values().stream().sorted(Comparator.comparing(Loan::id)).toList();
    }

    private List<LoanHistory> sortedHistories(Map<String, LoanHistory> source) {
        return source.values().stream().sorted(historyComparator()).toList();
    }

    private Comparator<LoanHistory> historyComparator() {
        return Comparator.comparing(LoanHistory::returnDate).thenComparing(LoanHistory::id);
    }

    private void validateHistoryMatchesLoan(String loanId, Loan loan, LoanHistory history) {
        if (!history.id().equals(loanId)
                || !history.book().id().equals(loan.bookId())
                || !history.member().id().equals(loan.memberId())
                || !history.checkoutDate().equals(loan.checkoutDate())
                || !history.dueDate().equals(loan.dueDate())) {
            throw new ValidationException("Loan history does not match the active loan.");
        }
    }

    private String normalizeLoanId(String id) {
        return InputRules.normalizeDisplayText(id, "Loan ID");
    }

    private record LoadedRecords(Map<String, Loan> loans, Map<String, LoanHistory> histories) { }
}
