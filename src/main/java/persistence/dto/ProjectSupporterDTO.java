package persistence.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProjectSupporterDTO {
    private Long supportId;
    private Long userId;
    private String supporterName;
    private String supporterNickname;
    private String rewardTitle;
    private int rewardCount;
    private LocalDateTime processedAt;
    private String recipientName;
    private String recipientContact;
    private String deliveryAddress;
    private String deliveryDetailAddress;
}