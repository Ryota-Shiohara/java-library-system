package library.repository;

import java.util.List;
import java.util.Optional;
import library.model.Member;

public interface MemberRepository {
    List<Member> findAll();
    Optional<Member> findById(String id);
    void save(Member member);
    void deleteById(String id);
}
