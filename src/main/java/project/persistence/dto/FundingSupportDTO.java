package project.persistence.dto;

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
    private LocalDateTime supportedAt;

    private String recipientName;
    private String recipientContact;
    private String deliveryAddress;
    private String deliveryDetailAddress;

    private boolean cancelStatus;
}