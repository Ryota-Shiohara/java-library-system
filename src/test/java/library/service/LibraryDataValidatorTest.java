package library.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import library.exception.DataStoreException;
import library.model.Book;
import library.model.Loan;
import library.model.LoanHistory;
import library.model.Member;
import library.repository.BookRepository;
import library.repository.LoanRepository;
import library.repository.MemberRepository;
import org.junit.jupiter.api.Test;

class LibraryDataValidatorTest {
    @Test
    void acceptsConsistentStoredData() {
        assertDoesNotThrow(() -> LibraryDataValidator.validate(
                new FixedBookRepository(List.of(new Book("B1", "Title", "Genre", 1))),
                new FixedMemberRepository(List.of(new Member("M1", "Ada"))),
                new FixedLoanRepository(List.of(new Loan("loan-1", "B1", "M1",
                        java.time.LocalDate.of(2026, 7, 1), java.time.LocalDate.of(2026, 7, 15))))));
    }

    @Test
    void rejectsLoansThatExceedOwnedCopies() {
        assertThrows(DataStoreException.class, () -> LibraryDataValidator.validate(
                new FixedBookRepository(List.of(new Book("B1", "Title", "Genre", 1))),
                new FixedMemberRepository(List.of(new Member("M1", "Ada"), new Member("M2", "Grace"))),
                new FixedLoanRepository(List.of(
                        new Loan("loan-1", "B1", "M1", java.time.LocalDate.of(2026, 7, 1),
                                java.time.LocalDate.of(2026, 7, 15)),
                        new Loan("loan-2", "B1", "M2", java.time.LocalDate.of(2026, 7, 1),
                                java.time.LocalDate.of(2026, 7, 15))))));
    }

    private static final class FixedBookRepository implements BookRepository {
        private final List<Book> books;

        private FixedBookRepository(List<Book> books) {
            this.books = List.copyOf(books);
        }

        @Override
        public List<Book> findAll() {
            return books;
        }

        @Override
        public Optional<Book> findById(String id) {
            return books.stream().filter(book -> book.id().equals(id)).findFirst();
        }

        @Override
        public void save(Book book) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(String id) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FixedMemberRepository implements MemberRepository {
        private final List<Member> members;

        private FixedMemberRepository(List<Member> members) {
            this.members = List.copyOf(members);
        }

        @Override
        public List<Member> findAll() {
            return members;
        }

        @Override
        public Optional<Member> findById(String id) {
            return members.stream().filter(member -> member.id().equals(id)).findFirst();
        }

        @Override
        public void save(Member member) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(String id) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FixedLoanRepository implements LoanRepository {
        private final List<Loan> loans;

        private FixedLoanRepository(List<Loan> loans) {
            this.loans = List.copyOf(loans);
        }

        @Override
        public List<Loan> findAll() {
            return loans;
        }

        @Override
        public Optional<Loan> findById(String id) {
            return loans.stream().filter(loan -> loan.id().equals(id)).findFirst();
        }

        @Override
        public List<Loan> findByBookId(String bookId) {
            return loans.stream().filter(loan -> loan.bookId().equals(bookId)).toList();
        }

        @Override
        public List<Loan> findByMemberId(String memberId) {
            return loans.stream().filter(loan -> loan.memberId().equals(memberId)).toList();
        }

        @Override
        public boolean existsByBookIdAndMemberId(String bookId, String memberId) {
            return loans.stream().anyMatch(loan -> loan.bookId().equals(bookId) && loan.memberId().equals(memberId));
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
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<LoanHistory> findAllHistory() {
            return List.of();
        }

        @Override
        public List<LoanHistory> findHistoryByBookId(String bookId) {
            return List.of();
        }

        @Override
        public List<LoanHistory> findHistoryByMemberId(String memberId) {
            return List.of();
        }

        @Override
        public void completeReturn(String loanId, LoanHistory history) {
            throw new UnsupportedOperationException();
        }
    }
}
