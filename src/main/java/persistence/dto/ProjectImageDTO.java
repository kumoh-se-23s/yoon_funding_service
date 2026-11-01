package persistence.dto;

import lombok.*;

import java.time.LocalDateTime;

// project_image 테이블의 한 행
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectImageDTO {
    private Long imageId;
    private Long projectId;
    @ToString.Exclude          // byte[]는 출력하면 너무 길어서 toString에서 제외
    private byte[] imgBlob;
    private boolean thumbnail; // is_thumbnail
    private Integer sortOrder;
    private LocalDateTime createdAt;   // null이면 DB 기본값(현재 시각)
}
