package project.persistence.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// review 테이블의 한 행
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long reviewId;
    private Long supportId;
    private String content;
    private BigDecimal score;          // DECIMAL(2,1) → 4.5 같은 값을 정확히 담으려고 BigDecimal 사용
    private LocalDateTime createdAt;   // null이면 DB 기본값(현재 시각)
}
