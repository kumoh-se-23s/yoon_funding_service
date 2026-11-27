package persistence.dto;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportDTO {
    Long userId;
    Long projectId;
    Long rewardId;
    Long supportId;

    String rewardTitle;
    String projectTitle;
}
