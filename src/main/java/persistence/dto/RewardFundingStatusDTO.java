package persistence.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RewardFundingStatusDTO {
    private Long rewardId;
    private String title;
    private Long price;
    private int stockQuantity;
    private long appliedQuantity;
    private long remainingQuantity;
    private long fundedAmount;
}