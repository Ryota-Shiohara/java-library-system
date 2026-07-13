package library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import library.exception.DuplicateIdException;
import library.exception.OperationNotAllowedException;
import library.model.Book;
import library.repository.BookRepository;
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
}
