package domain;

import lombok.Getter;

public enum ProgressStatus {
    READY("ready", "준비 중"),
    ONGOING("ongoing", "진행 중"),
    SUCCESS("success", "성공"),
    FAIL("fail", "실패");

    private final String dbValue;

    @Getter
    private final String displayName;

    ProgressStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public static ProgressStatus fromDbValue(String value) {
        for (ProgressStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("잘못된 진행 상태: " + value);
    }
}