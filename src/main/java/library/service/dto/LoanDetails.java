package library.service.dto;

import java.time.LocalDate;
import library.model.Book;
import library.model.Member;

public record LoanDetails(
        String id,
        Book book,
        Member member,
        LocalDate checkoutDate,
        LocalDate dueDate,
        boolean overdue) { }
