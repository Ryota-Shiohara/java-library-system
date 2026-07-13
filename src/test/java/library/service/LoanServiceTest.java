package library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import library.exception.EntityNotFoundException;
import library.exception.OperationNotAllowedException;
import library.model.Book;
import library.model.Loan;
import library.model.Member;
import library.repository.BookRepository;
import library.repository.LoanRepository;
import library.repository.MemberRepository;
import library.service.dto.LoanDetails;
import org.junit.jupiter.api.Test;

class LoanServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

    @Test
    void checksOutAndFindsBothSidesOfAnActiveLoan() {
        Fixture fixture = new Fixture(LocalDate.of(2026, 7, 1));
        fixture.addBook("B1", "Clean Code", 2);
        fixture.addMember("M1", "Ada");

        Loan loan = fixture.service.checkout("b1", "m1");

        assertEquals(LocalDate.of(2026, 7, 15), loan.dueDate());
        assertEquals(1, fixture.loans.countActiveLoansForBook("B1"));
        assertEquals(List.of("M1"), fixture.service.findBorrowersByBook("B1").stream().map(Member::id).toList());
        assertEquals(List.of("B1"), fixture.service.findBorrowedBooksByMember("M1").stream().map(Book::id).toList());
        assertEquals("loan-1", fixture.service.findActiveLoanById("loan-1").orElseThrow().id());
    }

    @Test
    void rejectsNoInventoryDuplicateLoansAndUnknownReferencesWithoutChangingState() {
        Fixture fixture = new Fixture(LocalDate.of(2026, 7, 1));
        fixture.addBook("B1", "Single Copy", 1);
        fixture.addMember("M1", "Ada");
        fixture.addMember("M2", "Grace");
        fixture.service.checkout("B1", "M1");

        assertThrows(OperationNotAllowedException.class, () -> fixture.service.checkout("B1", "M1"));
        assertThrows(OperationNotAllowedException.class, () -> fixture.service.checkout("B1", "M2"));
        assertThrows(EntityNotFoundException.class, () -> fixture.service.checkout("UNKNOWN", "M2"));
        assertEquals(1, fixture.service.listActiveLoans().size());
        assertEquals("M1", fixture.service.listActiveLoans().get(0).member().id());
    }

    @Test
    void returnsOnlyExistingLoans() {
        Fixture fixture = new Fixture(LocalDate.of(2026, 7, 1));
        fixture.addBook("B1", "Title", 1);
        fixture.addMember("M1", "Ada");
        Loan loan = fixture.service.checkout("B1", "M1");

        fixture.service.returnLoan(loan.id());
        assertEquals(List.of(), fixture.service.listActiveLoans());
        assertThrows(EntityNotFoundException.class, () -> fixture.service.returnLoan(loan.id()));
        assertEquals(0, fixture.loans.countActiveLoansForBook("B1"));
    }

    @Test
    void marksLoansOverdueOnlyAfterTheDueDate() {
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryMemberRepository members = new InMemoryMemberRepository();
        InMemoryLoanRepository loans = new InMemoryLoanRepository();
        books.save(new Book("B1", "Title", "Genre", 1));
        members.save(new Member("M1", "Ada"));
        LoanService checkoutService = new LoanService(books, members, loans,
                fixedClock(LocalDate.of(2026, 7, 1)), () -> "loan-1");
        checkoutService.checkout("B1", "M1");

        LoanService dueDateService = new LoanService(books, members, loans,
                fixedClock(LocalDate.of(2026, 7, 15)), () -> "unused");
        LoanService overdueService = new LoanService(books, members, loans,
                fixedClock(LocalDate.of(2026, 7, 16)), () -> "unused");

        assertFalse(dueDateService.listActiveLoans().get(0).overdue());
        assertTrue(overdueService.listActiveLoans().get(0).overdue());
    }

    private static Clock fixedClock(LocalDate date) {
        return Clock.fixed(Instant.from(date.atStartOfDay(ZONE)), ZONE);
    }

    private static final class Fixture {
        private final InMemoryBookRepository books = new InMemoryBookRepository();
        private final InMemoryMemberRepository members = new InMemoryMemberRepository();
        private final InMemoryLoanRepository loans = new InMemoryLoanRepository();
        private final LoanService service;

        private Fixture(LocalDate date) {
            AtomicInteger nextId = new AtomicInteger(1);
            Supplier<String> ids = () -> "loan-" + nextId.getAndIncrement();
            service = new LoanService(books, members, loans, fixedClock(date), ids);
        }

        private void addBook(String id, String title, int totalCopies) {
            books.save(new Book(id, title, "Genre", totalCopies));
        }

        private void addMember(String id, String name) {
            members.save(new Member(id, name));
        }
    }

    private static final class InMemoryBookRepository implements BookRepository {
        private final List<Book> books = new ArrayList<>();

        @Override
        public List<Book> findAll() {
            return List.copyOf(books);
        }

        @Override
        public Optional<Book> findById(String id) {
            return books.stream().filter(book -> book.id().equals(id.strip().toUpperCase())).findFirst();
        }

        @Override
        public void save(Book book) {
            books.removeIf(existing -> existing.id().equals(book.id()));
            books.add(book);
        }

        @Override
        public void deleteById(String id) {
            books.removeIf(book -> book.id().equals(id));
        }
    }

    private static final class InMemoryMemberRepository implements MemberRepository {
        private final List<Member> members = new ArrayList<>();

        @Override
        public List<Member> findAll() {
            return List.copyOf(members);
        }

        @Override
        public Optional<Member> findById(String id) {
            return members.stream().filter(member -> member.id().equals(id.strip().toUpperCase())).findFirst();
        }

        @Override
        public void save(Member member) {
            members.removeIf(existing -> existing.id().equals(member.id()));
            members.add(member);
        }

        @Override
        public void deleteById(String id) {
            members.removeIf(member -> member.id().equals(id));
        }
    }

    private static final class InMemoryLoanRepository implements LoanRepository {
        private final List<Loan> loans = new ArrayList<>();

        @Override
        public List<Loan> findAll() {
            return List.copyOf(loans);
        }

        @Override
        public Optional<Loan> findById(String id) {
            return loans.stream().filter(loan -> loan.id().equals(id.strip())).findFirst();
        }

        @Override
        public List<Loan> findByBookId(String bookId) {
            return loans.stream().filter(loan -> loan.bookId().equals(bookId.strip().toUpperCase())).toList();
        }

        @Override
        public List<Loan> findByMemberId(String memberId) {
            return loans.stream().filter(loan -> loan.memberId().equals(memberId.strip().toUpperCase())).toList();
        }

        @Override
        public boolean existsByBookIdAndMemberId(String bookId, String memberId) {
            return loans.stream().anyMatch(loan -> loan.bookId().equals(bookId.strip().toUpperCase())
                    && loan.memberId().equals(memberId.strip().toUpperCase()));
        }

        @Override
        public int countActiveLoansForBook(String bookId) {
            return findByBookId(bookId).size();
        }

        @Override
        public boolean hasActiveLoanForMember(String memberId) {
            return !findByMemberId(memberId).isEmpty();
        }

        @Override
        public void save(Loan loan) {
            loans.removeIf(existing -> existing.id().equals(loan.id()));
            loans.add(loan);
        }

        @Override
        public void deleteById(String id) {
            loans.removeIf(loan -> loan.id().equals(id.strip()));
        }
    }
}
