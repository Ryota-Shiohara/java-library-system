package library.service.dto;

public record ClassificationSummary(
        String ndcCode,
        String ndcName,
        int bookCount,
        int totalCopies,
        int loanedCopies,
        int availableCopies,
        int historicalLoanCount) { }
