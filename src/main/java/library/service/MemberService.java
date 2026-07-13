package library.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import library.exception.DuplicateIdException;
import library.exception.EntityNotFoundException;
import library.exception.OperationNotAllowedException;
import library.exception.ValidationException;
import library.model.Member;
import library.repository.LoanQuery;
import library.repository.MemberRepository;
import library.validation.InputRules;

public final class MemberService {
    private final MemberRepository memberRepository;
    private final LoanQuery loanQuery;

    public MemberService(MemberRepository memberRepository, LoanQuery loanQuery) {
        if (memberRepository == null) {
            throw new ValidationException("Member repository must not be null.");
        }
        if (loanQuery == null) {
            throw new ValidationException("Loan query must not be null.");
        }
        this.memberRepository = memberRepository;
        this.loanQuery = loanQuery;
    }

    public Member addMember(String id, String name) {
        Member member = new Member(id, name);
        if (memberRepository.findById(member.id()).isPresent()) {
            throw new DuplicateIdException("Member ID already exists: " + member.id() + ".");
        }
        memberRepository.save(member);
        return member;
    }

    public Member updateMember(String id, String name) {
        String normalizedId = InputRules.normalizeId(id, "Member ID");
        requireMember(normalizedId);
        Member updated = new Member(normalizedId, name);
        memberRepository.save(updated);
        return updated;
    }

    public void deleteMember(String id) {
        String normalizedId = InputRules.normalizeId(id, "Member ID");
        requireMember(normalizedId);
        if (loanQuery.hasActiveLoanForMember(normalizedId)) {
            throw new OperationNotAllowedException("A member with active loans cannot be deleted.");
        }
        memberRepository.deleteById(normalizedId);
    }

    public Optional<Member> findMemberById(String id) {
        return memberRepository.findById(InputRules.normalizeId(id, "Member ID"));
    }

    public List<Member> listMembers() {
        return memberRepository.findAll().stream().sorted(Comparator.comparing(Member::id)).toList();
    }

    public List<Member> searchMembers(String query) {
        String normalizedQuery = InputRules.normalizeSearchQuery(query);
        return memberRepository.findAll().stream()
                .filter(member -> normalizedQuery.isEmpty()
                        || member.id().toUpperCase(Locale.ROOT).contains(normalizedQuery)
                        || member.name().toUpperCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(Member::id))
                .toList();
    }

    private Member requireMember(String id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found: " + id + "."));
    }
}
