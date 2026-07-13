package library.repository;

import java.util.List;
import java.util.Optional;
import library.model.Book;

public interface BookRepository {
    List<Book> findAll();
    Optional<Book> findById(String id);
    void save(Book book);
    void deleteById(String id);
}
