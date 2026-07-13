package library.model;

import library.validation.InputRules;

public record Member(String id, String name) {
    public Member {
        id = InputRules.normalizeId(id, "Member ID");
        name = InputRules.normalizeDisplayText(name, "Member name");
    }
}
