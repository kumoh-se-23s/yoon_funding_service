package project.persistence.dao;

import project.persistence.dto.ReviewDTO;

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
}
