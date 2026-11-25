package domain;

import lombok.Getter;

public enum ApprovalStatus {
    AWAITING("awaiting", "심사대기"),
    APPROVED("approved", "승인"),
    UNAPPROVED("unapproved", "반려");

    private final String dbValue;

    @Getter
    private final String displayName;

    ApprovalStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public static ApprovalStatus fromDbValue(String value) {
        for (ApprovalStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("잘못된 심사 상태: " + value);
    }
}