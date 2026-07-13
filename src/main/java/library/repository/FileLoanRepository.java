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
import library.exception.EntityNotFoundException;
import library.exception.ValidationException;
import library.model.Loan;
import library.validation.InputRules;

public final class FileLoanRepository implements LoanRepository {
    private final DataStore dataStore;
    private Map<String, Loan> loans;

    public FileLoanRepository(DataStore dataStore) {
        if (dataStore == null) {
            throw new ValidationException("Data store must not be null.");
        }
        this.dataStore = dataStore;
        this.loans = loadLoans(dataStore.read("loans"));
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
        Map<String, Loan> candidate = new LinkedHashMap<>(loans);
        candidate.put(loan.id(), loan);
        persist(candidate);
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
        persist(candidate);
        loans = candidate;
    }

    private Map<String, Loan> loadLoans(List<List<String>> records) {
        Map<String, Loan> loaded = new LinkedHashMap<>();
        for (List<String> record : records) {
            if (record.size() != 5) {
                throw new DataStoreException("Loan record must contain 5 fields.");
            }
            try {
                Loan loan = new Loan(record.get(0), record.get(1), record.get(2),
                        LocalDate.parse(record.get(3)), LocalDate.parse(record.get(4)));
                if (loaded.putIfAbsent(loan.id(), loan) != null) {
                    throw new DataStoreException("Duplicate loan ID: " + loan.id() + ".");
                }
            } catch (DateTimeParseException exception) {
                throw new DataStoreException("Loan dates must use ISO format.", exception);
            } catch (ValidationException exception) {
                throw new DataStoreException("Invalid loan record.", exception);
            }
        }
        return loaded;
    }

    private void persist(Map<String, Loan> candidate) {
        List<List<String>> records = new ArrayList<>();
        candidate.values().stream()
                .sorted(Comparator.comparing(Loan::id))
                .forEach(loan -> records.add(List.of(
                        loan.id(),
                        loan.bookId(),
                        loan.memberId(),
                        loan.checkoutDate().toString(),
                        loan.dueDate().toString())));
        dataStore.write("loans", records);
    }

    private List<Loan> sortedLoans(Map<String, Loan> source) {
        return source.values().stream().sorted(Comparator.comparing(Loan::id)).toList();
    }

    private String normalizeLoanId(String id) {
        return InputRules.normalizeDisplayText(id, "Loan ID");
    }
}
