package persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundingSupportDTO {

    private Long supportId;
    private Long rewardId;
    private Long userId;
    @Builder.Default
    private int rewardCount = 1; // 기존 빌더에서 신청수량 지정 안해도 1로 디폴트 생성

    private LocalDateTime processedAt;

    private String recipientName;
    private String recipientContact;
    private String deliveryAddress;
    private String deliveryDetailAddress;

    private boolean cancelStatus;
}