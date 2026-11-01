package project.persistence.dto;

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

    private String recipientName;
    private String deliveryAddress;
    private String deliveryDetailAddress;
}
