package persistence.dto;

import lombok.*;

import java.time.LocalDateTime;

// users 테이블의 한 행(row)
@Getter
@Setter
@ToString
@Builder              // initDB에서 더미데이터를 읽기 쉽게 만들려고 추가
@NoArgsConstructor    // DAO에서 new UserDTO() 후 setter로 채울 때 사용
@AllArgsConstructor   // @Builder가 내부적으로 사용
public class UserDTO {
    private Long userId;
    private String loginId;
    private String password;
    private Integer role;          // 0=관리자, 1=서포터, 2=메이커
    private String name;
    private String nickname;
    private String email;
    private LocalDateTime createdAt;
    private String address;
    private String detailAddress;
}
