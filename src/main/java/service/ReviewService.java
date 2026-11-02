package service;

import lombok.*;
import persistence.dao.ReviewDAO;
import persistence.dto.ReviewDTO;
import persistence.dto.UserDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Getter
@Setter
@ToString
@Builder
@RequiredArgsConstructor
public class ReviewService {
    private final DataSource ds;
    private final ReviewDAO reviewDAO;

    public Long append(ReviewDTO review, Long supportUserId, Long projectId) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);

            if(!reviewDAO.isSupported(conn, supportUserId, projectId))
                throw new SQLException("지원하지 않은 리워드에 리뷰 등록 불가능");

            Long reviewId = reviewDAO.insert(conn, review);
            conn.commit();

            return reviewId;

        } catch (SQLException e) {
            throw new RuntimeException("리뷰 등록 실패", e);
        }
    }

    public boolean isSupporter(UserDTO loginUser) {
        return loginUser.getRole() == 1;
    }
}
