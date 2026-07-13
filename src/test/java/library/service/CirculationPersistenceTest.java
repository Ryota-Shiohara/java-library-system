package library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
    }

    private Clock fixedClock() {
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        Instant instant = LocalDate.of(2026, 7, 1).atStartOfDay(zone).toInstant();
        return Clock.fixed(instant, zone);
    }
}
