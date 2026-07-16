package library.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import library.exception.DataStoreException;
import library.model.Book;
import library.model.Loan;
import library.model.LoanHistory;
import library.model.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileLoanRepositoryTest {
    @TempDir
    Path dataDirectory;

    @Test
    void keepsPreviousStateWhenPersistenceFails() {
        DataStore failingStore = new DataStore() {
            @Override
            public List<List<String>> read(String collectionName) {
                return List.of();
            }

            @Override
            public void write(String collectionName, List<List<String>> records) {
                throw new DataStoreException("Write failed.");
            }
        };
        FileLoanRepository repository = new FileLoanRepository(failingStore);
        LocalDate checkoutDate = LocalDate.of(2026, 7, 1);

        assertThrows(DataStoreException.class,
                () -> repository.save(new Loan("loan-1", "B1", "M1", checkoutDate, checkoutDate.plusDays(14))));
        assertEquals(List.of(), repository.findAll());
    }

    @Test
    void roundTripsMultipleLoansAndSupportsQueries() {
        FileLoanRepository repository = new FileLoanRepository(new FileDataStore(dataDirectory));
        LocalDate firstDate = LocalDate.of(2026, 7, 1);
        LocalDate secondDate = LocalDate.of(2026, 7, 2);
        repository.save(new Loan("loan-2", "B1", "M2", secondDate, secondDate.plusDays(14)));
        repository.save(new Loan("loan-1", "B1", "M1", firstDate, firstDate.plusDays(14)));

        FileLoanRepository restored = new FileLoanRepository(new FileDataStore(dataDirectory));

        assertEquals(List.of("loan-1", "loan-2"), restored.findAll().stream().map(Loan::id).toList());
        assertEquals(2, restored.countActiveLoansForBook("b1"));
        assertEquals(List.of("loan-1"), restored.findByMemberId("m1").stream().map(Loan::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> restored.findAll().add(
                new Loan("loan-3", "B1", "M3", firstDate, firstDate.plusDays(14))));
    }

    @Test
    void rejectsMalformedDatesAndDuplicateStoredLoanIds() {
        DataStore malformedStore = fixedStore(List.of(
                List.of("loan-1", "B1", "M1", "not-a-date", "2026-07-15")));
        DataStore duplicateStore = fixedStore(List.of(
                List.of("loan-1", "B1", "M1", "2026-07-01", "2026-07-15"),
                List.of("loan-1", "B2", "M2", "2026-07-02", "2026-07-16")));

        assertThrows(DataStoreException.class, () -> new FileLoanRepository(malformedStore));
        assertThrows(DataStoreException.class, () -> new FileLoanRepository(duplicateStore));
    }

    @Test
    void movesAnActiveLoanToHistoryAndRestoresIt() {
        FileDataStore store = new FileDataStore(dataDirectory);
        FileLoanRepository repository = new FileLoanRepository(store);
        LocalDate checkoutDate = LocalDate.of(2026, 7, 1);
        Loan loan = new Loan("loan-1", "B1", "M1", checkoutDate, checkoutDate.plusDays(14));
        repository.save(loan);

        LoanHistory history = new LoanHistory(
                loan.id(),
                new Book("B1", "Algorithms", "Science", 1, "4"),
                new Member("M1", "Ada"),
                checkoutDate,
                checkoutDate.plusDays(14),
                LocalDate.of(2026, 7, 20));
        repository.completeReturn(loan.id(), history);

        assertEquals(List.of(), repository.findAll());
        assertEquals(List.of("loan-1"), repository.findAllHistory().stream().map(LoanHistory::id).toList());

        FileLoanRepository restored = new FileLoanRepository(new FileDataStore(dataDirectory));
        assertEquals("4", restored.findAllHistory().get(0).book().ndcCode());
        assertEquals(LocalDate.of(2026, 7, 20), restored.findAllHistory().get(0).returnDate());
    }

    private DataStore fixedStore(List<List<String>> records) {
        return new DataStore() {
            @Override
            public List<List<String>> read(String collectionName) {
                return records;
            }

            @Override
            public void write(String collectionName, List<List<String>> newRecords) { }
        };
    }
}
