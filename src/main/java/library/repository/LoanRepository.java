package library.repository;

import java.util.List;
import java.util.Optional;
import library.model.Loan;
import library.model.LoanHistory;

public interface LoanRepository extends LoanQuery, LoanHistoryRepository {
    List<Loan> findAll();
    Optional<Loan> findById(String id);
    List<Loan> findByBookId(String bookId);
    List<Loan> findByMemberId(String memberId);
    boolean existsByBookIdAndMemberId(String bookId, String memberId);
    void save(Loan loan);
    void deleteById(String id);
    void completeReturn(String loanId, LoanHistory history);
}
