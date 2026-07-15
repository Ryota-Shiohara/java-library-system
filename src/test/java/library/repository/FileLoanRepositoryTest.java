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
}
