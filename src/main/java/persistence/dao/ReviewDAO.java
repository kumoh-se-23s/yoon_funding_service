package persistence.dao;

import persistence.dto.ReviewDTO;

import java.sql.*;

public class ReviewDAO {

    public Long insert(Connection conn, ReviewDTO review) throws SQLException {
        String sql = "INSERT INTO review(support_id, content, score, created_at) "
                + "VALUES(?, ?, ?, COALESCE(?, NOW()))";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, review.getSupportId());
            pstmt.setString(2, review.getContent());
            pstmt.setBigDecimal(3, review.getScore());
            pstmt.setObject(4, review.getCreatedAt());

            pstmt.executeUpdate();
            return ProjectDAO.generatedKey(pstmt);
        }
    }

    public boolean isSupported(Connection conn, Long supportUserId, Long projectId)
            throws SQLException {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM funding_support fs
                JOIN reward r
                    ON fs.reward_id = r.reward_id
                WHERE fs.user_id = ?
                  AND r.project_id = ?
            )
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, supportUserId);
            pstmt.setLong(2, projectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

}
