package library.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import library.exception.DataStoreException;
import library.exception.EntityNotFoundException;
import library.exception.ValidationException;
import library.model.Book;
import library.validation.InputRules;

public final class FileBookRepository implements BookRepository {
    private final DataStore dataStore;
    private Map<String, Book> books;

    public FileBookRepository(DataStore dataStore) {
        if (dataStore == null) throw new ValidationException("Data store must not be null.");
        this.dataStore = dataStore;
        this.books = loadBooks(dataStore.read("books"));
    }

    @Override
    public List<Book> findAll() {
        return List.copyOf(sortedBooks(books));
    }

    @Override
    public Optional<Book> findById(String id) {
        return Optional.ofNullable(books.get(normalizeId(id)));
    }

    @Override
    public void save(Book book) {
        if (book == null) throw new ValidationException("Book must not be null.");
        Map<String, Book> candidate = new LinkedHashMap<>(books);
        candidate.put(book.id(), book);
        persist(candidate);
        books = candidate;
    }

    @Override
    public void deleteById(String id) {
        String normalizedId = normalizeId(id);
        if (!books.containsKey(normalizedId)) throw new EntityNotFoundException("Book not found: " + normalizedId + ".");
        Map<String, Book> candidate = new LinkedHashMap<>(books);
        candidate.remove(normalizedId);
        persist(candidate);
        books = candidate;
    }

    private Map<String, Book> loadBooks(List<List<String>> records) {
        Map<String, Book> loaded = new LinkedHashMap<>();
        for (List<String> record : records) {
            if (record.size() != 4 && record.size() != 5) {
                throw new DataStoreException("Book record must contain 4 or 5 fields.");
            }
            try {
                Book book = record.size() == 4
                        ? new Book(record.get(0), record.get(1), record.get(2), Integer.parseInt(record.get(3)))
                        : new Book(record.get(0), record.get(1), record.get(2), Integer.parseInt(record.get(3)), record.get(4));
                if (loaded.putIfAbsent(book.id(), book) != null) throw new DataStoreException("Duplicate book ID: " + book.id() + ".");
            } catch (NumberFormatException exception) {
                throw new DataStoreException("Book total copies must be an integer.", exception);
            }
        }
        return loaded;
    }

    private void persist(Map<String, Book> candidate) {
        List<List<String>> records = new ArrayList<>();
        candidate.values().stream().sorted(Comparator.comparing(Book::id)).forEach(book ->
                records.add(List.of(book.id(), book.title(), book.genre(), Integer.toString(book.totalCopies()), book.ndcCode())));
        dataStore.write("books", records);
    }

    private List<Book> sortedBooks(Map<String, Book> source) {
        return source.values().stream().sorted(Comparator.comparing(Book::id)).toList();
    }

    private String normalizeId(String id) {
        return InputRules.normalizeId(id, "Book ID");
    }
}
