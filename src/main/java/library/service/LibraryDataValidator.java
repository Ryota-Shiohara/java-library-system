package library.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import library.exception.DataStoreException;
import library.exception.ValidationException;
import library.model.Loan;
import library.repository.BookRepository;
import library.repository.LoanRepository;
import library.repository.MemberRepository;

public final class LibraryDataValidator {
    private LibraryDataValidator() { }

    public static void validate(
            BookRepository bookRepository,
            MemberRepository memberRepository,
            LoanRepository loanRepository) {
        if (bookRepository == null || memberRepository == null || loanRepository == null) {
            throw new ValidationException("Repositories must not be null.");
        }
        Map<String, Integer> loanCounts = new HashMap<>();
        Set<String> bookMemberPairs = new HashSet<>();
        for (Loan loan : loanRepository.findAll()) {
            if (bookRepository.findById(loan.bookId()).isEmpty()) {
                throw new DataStoreException("Loan references an unknown book: " + loan.bookId() + ".");
            }
            if (memberRepository.findById(loan.memberId()).isEmpty()) {
                throw new DataStoreException("Loan references an unknown member: " + loan.memberId() + ".");
            }
            String pair = loan.bookId() + "\u0000" + loan.memberId();
            if (!bookMemberPairs.add(pair)) {
                throw new DataStoreException("Duplicate active loan for book and member.");
            }
            loanCounts.merge(loan.bookId(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : loanCounts.entrySet()) {
            int totalCopies = bookRepository.findById(entry.getKey()).orElseThrow().totalCopies();
            if (entry.getValue() > totalCopies) {
                throw new DataStoreException("Active loans exceed total copies for book: " + entry.getKey() + ".");
            }
        }
    }
}
