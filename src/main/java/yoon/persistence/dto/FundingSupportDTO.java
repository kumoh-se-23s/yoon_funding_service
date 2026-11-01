package yoon.persistence.dto;

import lombok.*;

import java.time.LocalDateTime;

// funding_support 테이블의 한 행 (후원)
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundingSupportDTO {
    private Long supportId;
    private Long rewardId;
    private Long userId;               // 후원자(서포터)
    private LocalDateTime supportedAt; // null이면 DB 기본값(현재 시각)
    private String recipientName;
    private String recipientContact;
    private String deliveryAddress;
    private String deliveryDetailAddress;
    private boolean cancelStatus;      // false=정상, true=취소
}
