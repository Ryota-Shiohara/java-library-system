package library;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import library.exception.LibraryException;
import library.repository.BookRepository;
import library.repository.DataStore;
import library.repository.FileBookRepository;
import library.repository.FileDataStore;
import library.repository.FileLoanRepository;
import library.repository.FileMemberRepository;
import library.repository.LoanRepository;
import library.repository.MemberRepository;
import library.service.BookService;
import library.service.LibraryDataValidator;
import library.service.LoanService;
import library.service.MemberService;
import library.ui.MainFrame;

public final class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::startApplication);
    }

    private static void startApplication() {
        try {
            DataStore dataStore = new FileDataStore(Path.of("data"));
            BookRepository bookRepository = new FileBookRepository(dataStore);
            MemberRepository memberRepository = new FileMemberRepository(dataStore);
            LoanRepository loanRepository = new FileLoanRepository(dataStore);
            LibraryDataValidator.validate(bookRepository, memberRepository, loanRepository);

            BookService bookService = new BookService(bookRepository, loanRepository);
            MemberService memberService = new MemberService(memberRepository, loanRepository);
            LoanService loanService = new LoanService(bookRepository, memberRepository, loanRepository);
            MainFrame frame = new MainFrame(bookService, memberService, loanService);
            frame.setVisible(true);
        } catch (LibraryException exception) {
            showStartupError(exception.getMessage());
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Unexpected startup error.", exception);
            showStartupError("An unexpected error occurred.");
        }
    }

    private static void showStartupError(String message) {
        JOptionPane.showMessageDialog(null, message, "Library System", JOptionPane.ERROR_MESSAGE);
    }
}
