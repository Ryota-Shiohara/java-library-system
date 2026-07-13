package library.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import library.exception.DataStoreException;
import library.model.Loan;
import org.junit.jupiter.api.Test;

class FileLoanRepositoryTest {
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
}
