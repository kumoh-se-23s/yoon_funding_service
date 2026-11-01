package persistence.dto;

import lombok.*;

import java.time.LocalDateTime;

// project 테이블의 한 행
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private Long projectId;
    private Long userId;               // 프로젝트 개설자(메이커)
    private String title;
    private String subtitle;
    private String introduction;
    private String itemInfo;
    private Long goalAmount;
    private Integer category;
    private Integer projectStatus;     // 0=심사대기, 1=승인, 2=반려
    private LocalDateTime fundingStartAt;
    private LocalDateTime fundingEndAt;
    private LocalDateTime paymentDueAt;
    private LocalDateTime deliveryStartAt;

    private Long fundedAmount;

}
