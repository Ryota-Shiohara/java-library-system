package library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import library.exception.ValidationException;
import library.model.Book;
import library.model.LoanHistory;
import library.model.Member;
import library.repository.LoanHistoryRepository;
import org.junit.jupiter.api.Test;

class LoanHistoryServiceTest {
    @Test
    void searchesHistoryByTextNdcAndReturnDateRange() {
        InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
        repository.histories.add(history("loan-1", "B1", "Algorithms", "4", "M1", "Ada",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 20)));
        repository.histories.add(history("loan-2", "B2", "Poems", "9", "M2", "Grace",
                LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 10)));
        LoanHistoryService service = new LoanHistoryService(repository);

        assertEquals(List.of("loan-1"), service.searchHistory(
                "science", "4", LocalDate.of(2026, 7, 19), LocalDate.of(2026, 7, 21))
                .stream().map(LoanHistory::id).toList());
        assertEquals(List.of("loan-2"), service.searchHistory(
                "grace", "", null, null).stream().map(LoanHistory::id).toList());
    }

    @Test
    void rejectsReversedDateRange() {
        LoanHistoryService service = new LoanHistoryService(new InMemoryHistoryRepository());
        assertThrows(ValidationException.class, () -> service.searchHistory(
                "", "", LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 1)));
    }

    private static LoanHistory history(
            String loanId,
            String bookId,
            String title,
            String ndcCode,
            String memberId,
            String memberName,
            LocalDate checkoutDate,
            LocalDate returnDate) {
        Book book = new Book(bookId, title, "Science", 1, ndcCode);
        Member member = new Member(memberId, memberName);
        return new LoanHistory(loanId, book, member, checkoutDate, checkoutDate.plusDays(14), returnDate);
    }

    private static final class InMemoryHistoryRepository implements LoanHistoryRepository {
        private final List<LoanHistory> histories = new ArrayList<>();

        @Override
        public List<LoanHistory> findAllHistory() {
            return List.copyOf(histories);
        }

        @Override
        public List<LoanHistory> findHistoryByBookId(String bookId) {
            return histories.stream().filter(history -> history.book().id().equals(bookId)).toList();
        }

        @Override
        public List<LoanHistory> findHistoryByMemberId(String memberId) {
            return histories.stream().filter(history -> history.member().id().equals(memberId)).toList();
        }
    }
}
