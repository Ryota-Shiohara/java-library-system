package library.repository;

public interface LoanQuery {
    int countActiveLoansForBook(String bookId);
    boolean hasActiveLoanForMember(String memberId);
}
