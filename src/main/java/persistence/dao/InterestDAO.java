package persistence.dao;

import persistence.dto.InterestDTO;

import java.sql.*;

// 관심 프로젝트(좋아요) DAO
// 다른 DAO와 마찬가지로 커넥션은 Service가 넘겨줌 (트랜잭션은 Service가 관리)
public class InterestDAO {

    // 관심 등록 후 DB가 만든 like_id 반환
    public Long insert(Connection conn, InterestDTO interest) throws SQLException {
        String sql = "INSERT INTO interest(user_id, project_id, created_at) "
                + "VALUES(?, ?, COALESCE(?, NOW()))";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, interest.getUserId());
            pstmt.setLong(2, interest.getProjectId());
            pstmt.setObject(3, interest.getCreatedAt());

            pstmt.executeUpdate();
            return ProjectDAO.generatedKey(pstmt);
        }
    }

    // 이 회원이 이 프로젝트를 이미 관심 등록했는지 확인
    // (interest에는 UNIQUE(user_id, project_id)가 걸려 있어서 중복으로 넣으면 오류가 남)
    public boolean exists(Connection conn, Long userId, Long projectId) throws SQLException {
        String sql = "SELECT 1 FROM interest WHERE user_id = ? AND project_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, projectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
