package library.service;

import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import library.exception.DuplicateIdException;
import library.exception.EntityNotFoundException;
import library.exception.OperationNotAllowedException;
import library.model.Book;
import library.model.LoanHistory;
import library.model.NdcCategory;
import library.repository.BookRepository;
import library.repository.LoanHistoryRepository;
import library.repository.LoanQuery;
import library.service.dto.BookSummary;
import library.service.dto.ClassificationSummary;
import library.validation.InputRules;

public final class BookService {
    private final BookRepository bookRepository;
    private final LoanQuery loanQuery;
    private final LoanHistoryRepository historyRepository;

    private static final LoanHistoryRepository EMPTY_HISTORY_REPOSITORY = new LoanHistoryRepository() {
        @Override public List<LoanHistory> findAllHistory() { return List.of(); }
        @Override public List<LoanHistory> findHistoryByBookId(String bookId) { return List.of(); }
        @Override public List<LoanHistory> findHistoryByMemberId(String memberId) { return List.of(); }
    };

    public BookService(BookRepository bookRepository, LoanQuery loanQuery) {
        this(bookRepository, loanQuery, EMPTY_HISTORY_REPOSITORY);
    }

    public BookService(
            BookRepository bookRepository,
            LoanQuery loanQuery,
            LoanHistoryRepository historyRepository) {
        if (bookRepository == null) throw new IllegalArgumentException("Book repository must not be null.");
        if (loanQuery == null) throw new IllegalArgumentException("Loan query must not be null.");
        if (historyRepository == null) throw new IllegalArgumentException("Loan history repository must not be null.");
        this.bookRepository = bookRepository;
        this.loanQuery = loanQuery;
        this.historyRepository = historyRepository;
    }

    public Book addBook(String id, String title, String genre, int totalCopies) {
        return addBook(id, title, genre, totalCopies, NdcCategory.GENERAL_WORKS.code());
    }

    public Book addBook(String id, String title, String genre, int totalCopies, String ndcCode) {
        Book book = new Book(id, title, genre, totalCopies, ndcCode);
        if (bookRepository.findById(book.id()).isPresent()) {
            throw new DuplicateIdException("Book ID already exists: " + book.id() + ".");
        }
        bookRepository.save(book);
        return book;
    }

    public Book updateBook(String id, String title, String genre, int totalCopies) {
        String normalizedId = InputRules.normalizeId(id, "Book ID");
        return updateBook(normalizedId, title, genre, totalCopies, requireBook(normalizedId).ndcCode());
    }

    public Book updateBook(String id, String title, String genre, int totalCopies, String ndcCode) {
        String normalizedId = InputRules.normalizeId(id, "Book ID");
        requireBook(normalizedId);
        Book updated = new Book(normalizedId, title, genre, totalCopies, ndcCode);
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
        return searchBooks(query, "");
    }

    public List<BookSummary> searchBooks(String query, String ndcCode) {
        String normalizedQuery = InputRules.normalizeSearchQuery(query);
        String normalizedNdcCode = NdcCategory.normalizeOptionalCode(ndcCode);
        return summarize(bookRepository.findAll().stream()
                .filter(book -> normalizedNdcCode.isEmpty() || book.ndcCode().equals(normalizedNdcCode))
                .filter(book -> normalizedQuery.isEmpty()
                        || book.id().toUpperCase(Locale.ROOT).contains(normalizedQuery)
                        || book.title().toUpperCase(Locale.ROOT).contains(normalizedQuery)
                        || book.genre().toUpperCase(Locale.ROOT).contains(normalizedQuery)
                        || book.ndcCode().contains(normalizedQuery)
                        || book.ndcCategory().displayName().toUpperCase(Locale.ROOT).contains(normalizedQuery))
                .toList());
    }

    public List<ClassificationSummary> listClassificationSummaries() {
        List<Book> books = bookRepository.findAll();
        List<LoanHistory> history = historyRepository.findAllHistory();
        return Arrays.stream(NdcCategory.values())
                .map(category -> {
                    List<Book> classifiedBooks = books.stream()
                            .filter(book -> book.ndcCode().equals(category.code()))
                            .toList();
                    int totalCopies = classifiedBooks.stream().mapToInt(Book::totalCopies).sum();
                    int loanedCopies = classifiedBooks.stream()
                            .mapToInt(book -> loanQuery.countActiveLoansForBook(book.id()))
                            .sum();
                    int historicalLoanCount = (int) history.stream()
                            .filter(item -> item.book().ndcCode().equals(category.code()))
                            .count();
                    return new ClassificationSummary(
                            category.code(),
                            category.displayName(),
                            classifiedBooks.size(),
                            totalCopies,
                            loanedCopies,
                            totalCopies - loanedCopies,
                            historicalLoanCount);
                })
                .toList();
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
                            loanedCopies, book.totalCopies() - loanedCopies,
                            book.ndcCode(), book.ndcCategory().displayName());
                })
                .sorted(Comparator.comparing(BookSummary::id))
                .toList();
    }
}
