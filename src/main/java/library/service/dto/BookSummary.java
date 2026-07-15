package library.service.dto;

public record BookSummary(
        String id,
        String title,
        String genre,
        int totalCopies,
        int loanedCopies,
        int availableCopies,
        String ndcCode,
        String ndcName) { }
