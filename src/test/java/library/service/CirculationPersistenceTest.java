package library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import library.exception.OperationNotAllowedException;
import library.repository.FileBookRepository;
import library.repository.FileDataStore;
import library.repository.FileLoanRepository;
import library.repository.FileMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CirculationPersistenceTest {
    @TempDir
    Path dataDirectory;

    @Test
    void restoresActiveLoansAndAllowsReturnAfterReload() {
        FileDataStore store = new FileDataStore(dataDirectory);
        FileBookRepository books = new FileBookRepository(store);
        FileMemberRepository members = new FileMemberRepository(store);
        FileLoanRepository loans = new FileLoanRepository(store);
        BookService bookService = new BookService(books, loans);
        MemberService memberService = new MemberService(members, loans);
        LoanService loanService = new LoanService(books, members, loans, fixedClock(), () -> "loan-1");
        bookService.addBook("B1", "Data Structures", "Programming", 2);
        memberService.addMember("M1", "Ada Lovelace");
        loanService.checkout("B1", "M1");

        FileBookRepository restoredBooks = new FileBookRepository(new FileDataStore(dataDirectory));
        FileMemberRepository restoredMembers = new FileMemberRepository(new FileDataStore(dataDirectory));
        FileLoanRepository restoredLoans = new FileLoanRepository(new FileDataStore(dataDirectory));
        LibraryDataValidator.validate(restoredBooks, restoredMembers, restoredLoans);
        LoanService restoredLoanService = new LoanService(restoredBooks, restoredMembers, restoredLoans,
                fixedClock(), () -> "unused");

        assertEquals(List.of("M1"), restoredLoanService.findBorrowersByBook("B1").stream()
                .map(member -> member.id()).toList());
        assertEquals(1, restoredLoanService.findBorrowedBooksByMember("M1").size());
        restoredLoanService.returnLoan("loan-1");
        assertTrue(restoredLoans.findAll().isEmpty());
        assertEquals(1, restoredLoans.findAllHistory().size());
        assertEquals("B1", restoredLoans.findAllHistory().get(0).book().id());

        FileLoanRepository restoredHistoryRepository = new FileLoanRepository(new FileDataStore(dataDirectory));
        assertEquals(1, restoredHistoryRepository.findAllHistory().size());
    }

    @Test
    void restoresLoansForDifferentBookIdsWithTheSameTitle() {
        FileDataStore store = new FileDataStore(dataDirectory);
        FileBookRepository books = new FileBookRepository(store);
        FileMemberRepository members = new FileMemberRepository(store);
        FileLoanRepository loans = new FileLoanRepository(store);
        BookService bookService = new BookService(books, loans);
        MemberService memberService = new MemberService(members, loans);
        java.util.concurrent.atomic.AtomicInteger nextId = new java.util.concurrent.atomic.AtomicInteger(1);
        LoanService loanService = new LoanService(books, members, loans, fixedClock(),
                () -> "loan-" + nextId.getAndIncrement());
        bookService.addBook("B1", "Shared Title", "Reference", 1);
        bookService.addBook("B2", "Shared Title", "Reference", 1);
        memberService.addMember("M1", "Ada");
        memberService.addMember("M2", "Grace");
        loanService.checkout("B1", "M1");
        loanService.checkout("B2", "M2");

        FileBookRepository restoredBooks = new FileBookRepository(new FileDataStore(dataDirectory));
        FileMemberRepository restoredMembers = new FileMemberRepository(new FileDataStore(dataDirectory));
        FileLoanRepository restoredLoans = new FileLoanRepository(new FileDataStore(dataDirectory));
        LibraryDataValidator.validate(restoredBooks, restoredMembers, restoredLoans);

        assertEquals(2, restoredLoans.findAll().size());
        assertEquals(List.of("B1", "B2"), restoredLoans.findAll().stream().map(loan -> loan.bookId()).toList());
    }

    @Test
    void persistsAllLoansAndRejectsOnlyTheCheckoutBeyondCapacity() {
        FileDataStore store = new FileDataStore(dataDirectory);
        FileBookRepository books = new FileBookRepository(store);
        FileMemberRepository members = new FileMemberRepository(store);
        FileLoanRepository loans = new FileLoanRepository(store);
        BookService bookService = new BookService(books, loans);
        MemberService memberService = new MemberService(members, loans);
        java.util.concurrent.atomic.AtomicInteger nextId = new java.util.concurrent.atomic.AtomicInteger(1);
        LoanService loanService = new LoanService(books, members, loans, fixedClock(),
                () -> "loan-" + nextId.getAndIncrement());
        bookService.addBook("B1", "Two Copies", "Reference", 2);
        memberService.addMember("M1", "Ada");
        memberService.addMember("M2", "Grace");
        memberService.addMember("M3", "Katherine");
        loanService.checkout("B1", "M1");
        loanService.checkout("B1", "M2");

        assertThrows(OperationNotAllowedException.class, () -> loanService.checkout("B1", "M3"));
        assertEquals(0, bookService.availableCopies("B1"));
        assertEquals(2, new FileLoanRepository(new FileDataStore(dataDirectory)).findAll().size());
    }

    private Clock fixedClock() {
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        Instant instant = LocalDate.of(2026, 7, 1).atStartOfDay(zone).toInstant();
        return Clock.fixed(instant, zone);
    }
}
