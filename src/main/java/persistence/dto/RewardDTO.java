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
    private Integer totalCount; // 총 제공 수량
    private boolean soldOutStatus; // 품절 여부
    private Long price;
}
