package persistence.dto;

import lombok.*;

// reward 테이블의 한 행
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardDTO {
    private Long rewardId;
    private Long projectId;
    private String title;
    private String content;
    @ToString.Exclude
    private byte[] imgBlob;
    private Integer stockQuantity;
    private Long price;
}
