package library.repository;

import java.util.List;
import library.model.LoanHistory;

public interface LoanHistoryRepository {
    List<LoanHistory> findAllHistory();
    List<LoanHistory> findHistoryByBookId(String bookId);
    List<LoanHistory> findHistoryByMemberId(String memberId);
}
