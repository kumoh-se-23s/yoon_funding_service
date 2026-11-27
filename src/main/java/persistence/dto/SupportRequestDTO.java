package persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequestDTO {
    private Long userId;

    private Long projectId;
    private Long rewardId;

    @Builder.Default
    private int rewardCount = 1;

    private String recipientName;
    private String recipientContact;
    private String deliveryAddress;
    private String deliveryDetailAddress;
}
