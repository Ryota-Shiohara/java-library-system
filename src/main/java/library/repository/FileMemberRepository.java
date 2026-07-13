package library.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import library.exception.DataStoreException;
import library.exception.EntityNotFoundException;
import library.exception.ValidationException;
import library.model.Member;
import library.validation.InputRules;

public final class FileMemberRepository implements MemberRepository {
    private final DataStore dataStore;
    private Map<String, Member> members;

    public FileMemberRepository(DataStore dataStore) {
        if (dataStore == null) {
            throw new ValidationException("Data store must not be null.");
        }
        this.dataStore = dataStore;
        this.members = loadMembers(dataStore.read("members"));
    }

    @Override
    public List<Member> findAll() {
        return List.copyOf(sortedMembers(members));
    }

    @Override
    public Optional<Member> findById(String id) {
        return Optional.ofNullable(members.get(normalizeId(id)));
    }

    @Override
    public void save(Member member) {
        if (member == null) {
            throw new ValidationException("Member must not be null.");
        }
        Map<String, Member> candidate = new LinkedHashMap<>(members);
        candidate.put(member.id(), member);
        persist(candidate);
        members = candidate;
    }

    @Override
    public void deleteById(String id) {
        String normalizedId = normalizeId(id);
        if (!members.containsKey(normalizedId)) {
            throw new EntityNotFoundException("Member not found: " + normalizedId + ".");
        }
        Map<String, Member> candidate = new LinkedHashMap<>(members);
        candidate.remove(normalizedId);
        persist(candidate);
        members = candidate;
    }

    private Map<String, Member> loadMembers(List<List<String>> records) {
        Map<String, Member> loaded = new LinkedHashMap<>();
        for (List<String> record : records) {
            if (record.size() != 2) {
                throw new DataStoreException("Member record must contain 2 fields.");
            }
            Member member;
            try {
                member = new Member(record.get(0), record.get(1));
            } catch (ValidationException exception) {
                throw new DataStoreException("Invalid member record.", exception);
            }
            if (loaded.putIfAbsent(member.id(), member) != null) {
                throw new DataStoreException("Duplicate member ID: " + member.id() + ".");
            }
        }
        return loaded;
    }

    private void persist(Map<String, Member> candidate) {
        List<List<String>> records = new ArrayList<>();
        candidate.values().stream()
                .sorted(Comparator.comparing(Member::id))
                .forEach(member -> records.add(List.of(member.id(), member.name())));
        dataStore.write("members", records);
    }

    private List<Member> sortedMembers(Map<String, Member> source) {
        return source.values().stream().sorted(Comparator.comparing(Member::id)).toList();
    }

    private String normalizeId(String id) {
        return InputRules.normalizeId(id, "Member ID");
    }
}
