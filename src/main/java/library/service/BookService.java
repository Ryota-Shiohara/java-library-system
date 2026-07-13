package library.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import library.exception.DuplicateIdException;
import library.exception.EntityNotFoundException;
import library.exception.OperationNotAllowedException;
import library.model.Book;
import library.repository.BookRepository;
import library.repository.LoanQuery;
import library.service.dto.BookSummary;
import library.validation.InputRules;

public final class BookService {
    private final BookRepository bookRepository;
    private final LoanQuery loanQuery;

    public BookService(BookRepository bookRepository, LoanQuery loanQuery) {
        if (bookRepository == null) throw new IllegalArgumentException("Book repository must not be null.");
        if (loanQuery == null) throw new IllegalArgumentException("Loan query must not be null.");
        this.bookRepository = bookRepository;
        this.loanQuery = loanQuery;
    }

    public Book addBook(String id, String title, String genre, int totalCopies) {
        Book book = new Book(id, title, genre, totalCopies);
        if (bookRepository.findById(book.id()).isPresent()) {
            throw new DuplicateIdException("Book ID already exists: " + book.id() + ".");
        }
        bookRepository.save(book);
        return book;
    }

    public Book updateBook(String id, String title, String genre, int totalCopies) {
        String normalizedId = InputRules.normalizeId(id, "Book ID");
        requireBook(normalizedId);
        Book updated = new Book(normalizedId, title, genre, totalCopies);
        int loanedCopies = loanQuery.countActiveLoansForBook(normalizedId);
        if (updated.totalCopies() < loanedCopies) {
            throw new OperationNotAllowedException("Total copies cannot be less than active loans.");
        }
        bookRepository.save(updated);
        return updated;
    }

    public void deleteBook(String id) {
        String normalizedId = InputRules.normalizeId(id, "Book ID");
        requireBook(normalizedId);
        if (loanQuery.countActiveLoansForBook(normalizedId) > 0) {
            throw new OperationNotAllowedException("A book with active loans cannot be deleted.");
        }
        bookRepository.deleteById(normalizedId);
    }

    public Optional<Book> findBookById(String id) {
        return bookRepository.findById(InputRules.normalizeId(id, "Book ID"));
    }

    public List<BookSummary> listBooks() {
        return summarize(bookRepository.findAll());
    }

    public List<BookSummary> searchBooks(String query) {
        String normalizedQuery = InputRules.normalizeSearchQuery(query);
        return summarize(bookRepository.findAll().stream()
                .filter(book -> normalizedQuery.isEmpty()
                        || book.title().toUpperCase(Locale.ROOT).contains(normalizedQuery)
                        || book.genre().toUpperCase(Locale.ROOT).contains(normalizedQuery))
                .toList());
    }

    public int availableCopies(String bookId) {
        String normalizedId = InputRules.normalizeId(bookId, "Book ID");
        Book book = requireBook(normalizedId);
        return book.totalCopies() - loanQuery.countActiveLoansForBook(normalizedId);
    }

    private Book requireBook(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: " + id + "."));
    }

    private List<BookSummary> summarize(List<Book> books) {
        return books.stream()
                .map(book -> {
                    int loanedCopies = loanQuery.countActiveLoansForBook(book.id());
                    return new BookSummary(book.id(), book.title(), book.genre(), book.totalCopies(),
                            loanedCopies, book.totalCopies() - loanedCopies);
                })
                .sorted(Comparator.comparing(BookSummary::id))
                .toList();
    }
}
