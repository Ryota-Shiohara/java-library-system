package library.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import library.exception.DuplicateIdException;
import library.exception.EntityNotFoundException;
import library.exception.OperationNotAllowedException;
import library.exception.ValidationException;
import library.model.Book;
import library.model.Loan;
import library.model.LoanHistory;
import library.model.Member;
import library.repository.BookRepository;
import library.repository.LoanRepository;
import library.repository.MemberRepository;
import library.service.dto.LoanDetails;
import library.validation.InputRules;

public final class LoanService {
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final Clock clock;
    private final Supplier<String> loanIdSupplier;

    public LoanService(
            BookRepository bookRepository,
            MemberRepository memberRepository,
            LoanRepository loanRepository) {
        this(bookRepository, memberRepository, loanRepository, Clock.systemDefaultZone(),
                () -> UUID.randomUUID().toString());
    }

    public LoanService(
            BookRepository bookRepository,
            MemberRepository memberRepository,
            LoanRepository loanRepository,
            Clock clock,
            Supplier<String> loanIdSupplier) {
        if (bookRepository == null) {
            throw new ValidationException("Book repository must not be null.");
        }
        if (memberRepository == null) {
            throw new ValidationException("Member repository must not be null.");
        }
        if (loanRepository == null) {
            throw new ValidationException("Loan repository must not be null.");
        }
        if (clock == null) {
            throw new ValidationException("Clock must not be null.");
        }
        if (loanIdSupplier == null) {
            throw new ValidationException("Loan ID supplier must not be null.");
        }
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
        this.clock = clock;
        this.loanIdSupplier = loanIdSupplier;
    }

    public Loan checkout(String bookId, String memberId) {
        String normalizedBookId = InputRules.normalizeId(bookId, "Book ID");
        String normalizedMemberId = InputRules.normalizeId(memberId, "Member ID");
        Book book = requireBook(normalizedBookId);
        requireMember(normalizedMemberId);
        if (book.totalCopies() - loanRepository.countActiveLoansForBook(normalizedBookId) < 1) {
            throw new OperationNotAllowedException("No copies are available for checkout.");
        }
        if (loanRepository.existsByBookIdAndMemberId(normalizedBookId, normalizedMemberId)) {
            throw new OperationNotAllowedException("This member already has an active loan for this book.");
        }
        String loanId = InputRules.normalizeDisplayText(loanIdSupplier.get(), "Loan ID");
        if (loanRepository.findById(loanId).isPresent()) {
            throw new DuplicateIdException("Loan ID already exists: " + loanId + ".");
        }
        LocalDate checkoutDate = LocalDate.now(clock);
        Loan loan = new Loan(loanId, normalizedBookId, normalizedMemberId, checkoutDate, checkoutDate.plusDays(14));
        loanRepository.save(loan);
        return loan;
    }

    public void returnLoan(String loanId) {
        String normalizedLoanId = InputRules.normalizeDisplayText(loanId, "Loan ID");
        Loan loan = loanRepository.findById(normalizedLoanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found: " + normalizedLoanId + "."));
        Book book = requireBook(loan.bookId());
        Member member = requireMember(loan.memberId());
        LoanHistory history = new LoanHistory(
                loan.id(),
                book,
                member,
                loan.checkoutDate(),
                loan.dueDate(),
                LocalDate.now(clock));
        loanRepository.completeReturn(normalizedLoanId, history);
    }

    public Optional<LoanDetails> findActiveLoanById(String loanId) {
        return loanRepository.findById(InputRules.normalizeDisplayText(loanId, "Loan ID"))
                .map(this::toDetails);
    }

    public List<LoanDetails> listActiveLoans() {
        return toDetails(loanRepository.findAll());
    }

    public List<LoanDetails> findActiveLoansByBook(String bookId) {
        String normalizedBookId = InputRules.normalizeId(bookId, "Book ID");
        requireBook(normalizedBookId);
        return toDetails(loanRepository.findByBookId(normalizedBookId));
    }

    public List<LoanDetails> findActiveLoansByMember(String memberId) {
        String normalizedMemberId = InputRules.normalizeId(memberId, "Member ID");
        requireMember(normalizedMemberId);
        return toDetails(loanRepository.findByMemberId(normalizedMemberId));
    }

    public List<Member> findBorrowersByBook(String bookId) {
        String normalizedBookId = InputRules.normalizeId(bookId, "Book ID");
        requireBook(normalizedBookId);
        return loanRepository.findByBookId(normalizedBookId).stream()
                .map(loan -> requireMember(loan.memberId()))
                .distinct()
                .sorted(Comparator.comparing(Member::id))
                .toList();
    }

    public List<Book> findBorrowedBooksByMember(String memberId) {
        String normalizedMemberId = InputRules.normalizeId(memberId, "Member ID");
        requireMember(normalizedMemberId);
        return loanRepository.findByMemberId(normalizedMemberId).stream()
                .map(loan -> requireBook(loan.bookId()))
                .distinct()
                .sorted(Comparator.comparing(Book::id))
                .toList();
    }

    private List<LoanDetails> toDetails(List<Loan> loans) {
        return loans.stream()
                .map(this::toDetails)
                .sorted(Comparator.comparing(LoanDetails::checkoutDate).thenComparing(LoanDetails::id))
                .toList();
    }

    private LoanDetails toDetails(Loan loan) {
        Book book = requireBook(loan.bookId());
        Member member = requireMember(loan.memberId());
        return new LoanDetails(
                loan.id(),
                book,
                member,
                loan.checkoutDate(),
                loan.dueDate(),
                LocalDate.now(clock).isAfter(loan.dueDate()));
    }

    private Book requireBook(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: " + id + "."));
    }

    private Member requireMember(String id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found: " + id + "."));
    }
}
