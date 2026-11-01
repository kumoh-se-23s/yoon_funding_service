package persistence.dto;

import lombok.*;

import java.time.LocalDateTime;

// project_approval 테이블의 한 행 (심사 이력)
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectApprovalDTO {
    private Long approvalId;
    private Long projectId;
    private Integer projectStatus;     // 0=심사대기, 1=승인, 2=반려
    private String rejectReason;
    private LocalDateTime evalAt;
    private LocalDateTime submissionAt;
}
