package library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import library.exception.DuplicateIdException;
import library.exception.OperationNotAllowedException;
import library.model.Member;
import library.repository.LoanQuery;
import library.repository.MemberRepository;
import org.junit.jupiter.api.Test;

class MemberServiceTest {
    @Test
    void addsSearchesAndUpdatesMembers() {
        InMemoryMemberRepository repository = new InMemoryMemberRepository();
        MemberService service = new MemberService(repository, new MutableLoanQuery());

        service.addMember("m2", "Grace Hopper");
        service.addMember("M1", "Ada Lovelace");

        assertEquals(List.of("M1", "M2"), service.listMembers().stream().map(Member::id).toList());
        assertEquals(List.of("M2"), service.searchMembers("hopper").stream().map(Member::id).toList());
        assertEquals("Ada Byron", service.updateMember("m1", "Ada Byron").name());
        assertThrows(DuplicateIdException.class, () -> service.addMember(" M1 ", "Another Ada"));
    }

    @Test
    void rejectsDeletionForMembersWithActiveLoans() {
        InMemoryMemberRepository repository = new InMemoryMemberRepository();
        MutableLoanQuery loanQuery = new MutableLoanQuery();
        MemberService service = new MemberService(repository, loanQuery);
        service.addMember("M1", "Ada");

        loanQuery.hasActiveLoan = true;
        assertThrows(OperationNotAllowedException.class, () -> service.deleteMember("M1"));
        assertEquals("Ada", service.findMemberById("M1").orElseThrow().name());

        loanQuery.hasActiveLoan = false;
        service.deleteMember("M1");
        assertEquals(List.of(), service.listMembers());
    }

    private static final class MutableLoanQuery implements LoanQuery {
        private boolean hasActiveLoan;

        @Override
        public int countActiveLoansForBook(String bookId) {
            return 0;
        }

        @Override
        public boolean hasActiveLoanForMember(String memberId) {
            return hasActiveLoan;
        }
    }

    private static final class InMemoryMemberRepository implements MemberRepository {
        private final List<Member> members = new ArrayList<>();

        @Override
        public List<Member> findAll() {
            return List.copyOf(members);
        }

        @Override
        public Optional<Member> findById(String id) {
            return members.stream().filter(member -> member.id().equals(id.strip().toUpperCase())).findFirst();
        }

        @Override
        public void save(Member member) {
            members.removeIf(existing -> existing.id().equals(member.id()));
            members.add(member);
        }

        @Override
        public void deleteById(String id) {
            members.removeIf(member -> member.id().equals(id));
        }
    }
}
