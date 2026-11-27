package domain;

import lombok.Getter;
// enum 클래스입니다~

public enum UserRole {
    SUPPORTER("supporter", "서포터"),
    MAKER("maker", "메이커"),
    ADMIN("admin", "관리자");

    private final String dbValue;
    @Getter
    private final String displayName;

    UserRole(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public static UserRole fromDbValue(String value) {
        for (UserRole role : values()) {
            if (role.dbValue.equals(value)) {
                return role;
            }
        }

        throw new IllegalArgumentException("잘못된 사용자 유형: " + value);
    }
}