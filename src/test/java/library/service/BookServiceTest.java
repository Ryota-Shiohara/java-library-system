package library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import library.exception.DuplicateIdException;
import library.exception.OperationNotAllowedException;
import library.model.Book;
import library.model.LoanHistory;
import library.model.Member;
import library.repository.BookRepository;
import library.repository.LoanHistoryRepository;
import library.repository.LoanQuery;
import org.junit.jupiter.api.Test;

class BookServiceTest {
    @Test
    void addsSearchesAndSortsBooks() {
        InMemoryBookRepository repository = new InMemoryBookRepository();
        BookService service = new BookService(repository, new StubLoanQuery(0));
        service.addBook("b2", "Java Basics", "Programming", 2);
        service.addBook("B1", "Clean Code", "Programming", 1);
        assertEquals(List.of("B1", "B2"), service.listBooks().stream().map(summary -> summary.id()).toList());
        assertEquals(List.of("B2"), service.searchBooks("JAVA").stream().map(summary -> summary.id()).toList());
    }

    @Test
    void rejectsDuplicateAndUnsafeInventoryChanges() {
        InMemoryBookRepository repository = new InMemoryBookRepository();
        BookService service = new BookService(repository, new StubLoanQuery(2));
        service.addBook("B1", "Title", "Genre", 2);
        assertThrows(DuplicateIdException.class, () -> service.addBook(" b1 ", "Other", "Genre", 1));
        assertThrows(OperationNotAllowedException.class, () -> service.updateBook("B1", "Title", "Genre", 1));
        assertEquals(2, service.findBookById("b1").orElseThrow().totalCopies());
    }

    @Test
    void filtersBooksAndSummarizesCopiesByNdcCategory() {
        InMemoryBookRepository repository = new InMemoryBookRepository();
        BookService service = new BookService(repository, new StubLoanQuery(0));
        service.addBook("B1", "Algorithms", "Science", 3, "4");
        service.addBook("B2", "Poems", "Literature", 2, "9");

        assertEquals(List.of("B1"), service.searchBooks("", "4").stream()
                .map(summary -> summary.id()).toList());
        assertEquals(1, service.listClassificationSummaries().stream()
                .filter(summary -> summary.ndcCode().equals("4"))
                .findFirst().orElseThrow().bookCount());
        assertEquals(3, service.listClassificationSummaries().stream()
                .filter(summary -> summary.ndcCode().equals("4"))
                .findFirst().orElseThrow().totalCopies());
    }

    @Test
    void includesHistoricalLoansInNdcSummary() {
        InMemoryBookRepository repository = new InMemoryBookRepository();
        repository.save(new Book("B1", "Algorithms", "Science", 1, "4"));
        LoanHistory history = new LoanHistory(
                "loan-1",
                new Book("B1", "Algorithms", "Science", 1, "4"),
                new Member("M1", "Ada"),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 15),
                java.time.LocalDate.of(2026, 7, 10));
        BookService service = new BookService(
                repository,
                new StubLoanQuery(0),
                new FixedHistoryRepository(List.of(history)));

        assertEquals(1, service.listClassificationSummaries().stream()
                .filter(summary -> summary.ndcCode().equals("4"))
                .findFirst().orElseThrow().historicalLoanCount());
    }

    private static final class StubLoanQuery implements LoanQuery {
        private final int count;
        private StubLoanQuery(int count) { this.count = count; }
        @Override public int countActiveLoansForBook(String bookId) { return count; }
        @Override public boolean hasActiveLoanForMember(String memberId) { return false; }
    }

    private static final class InMemoryBookRepository implements BookRepository {
        private final List<Book> books = new ArrayList<>();
        @Override public List<Book> findAll() { return List.copyOf(books); }
        @Override public Optional<Book> findById(String id) {
            return books.stream().filter(book -> book.id().equals(id.strip().toUpperCase())).findFirst();
        }
        @Override public void save(Book book) {
            books.removeIf(existing -> existing.id().equals(book.id()));
            books.add(book);
        }
        @Override public void deleteById(String id) { books.removeIf(book -> book.id().equals(id)); }
    }

    private static final class FixedHistoryRepository implements LoanHistoryRepository {
        private final List<LoanHistory> histories;

        private FixedHistoryRepository(List<LoanHistory> histories) {
            this.histories = List.copyOf(histories);
        }

        @Override public List<LoanHistory> findAllHistory() { return histories; }
        @Override public List<LoanHistory> findHistoryByBookId(String bookId) { return histories; }
        @Override public List<LoanHistory> findHistoryByMemberId(String memberId) { return histories; }
    }
}
