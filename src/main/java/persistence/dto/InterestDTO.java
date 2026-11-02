package persistence.dto;

import lombok.*;

import java.time.LocalDateTime;

// interest 테이블의 한 행 (관심 프로젝트 = 좋아요)
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestDTO {
    private Long likeId;               // PK (AUTO_INCREMENT)
    private Long userId;               // 좋아요 누른 회원
    private Long projectId;            // 좋아요 받은 프로젝트
    private LocalDateTime createdAt;   // null이면 DB 기본값(현재 시각)
}
