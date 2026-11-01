package persistence.dao;

import persistence.dto.ProjectDTO;

import java.sql.*;

public class ProjectDAO {

    // 프로젝트 등록 후, AUTO_INCREMENT로 만들어진 project_id를 돌려줌
    // → 이 값을 이미지/리워드/심사 이력 insert에 그대로 넘겨서 FK로 사용
    public Long insert(Connection conn, ProjectDTO project) throws SQLException {
        String sql = "INSERT INTO project(user_id, title, subtitle, introduction, item_info, goal_amount, category, "
                + "project_status, funding_start_at, funding_end_at, payment_due_at, delivery_start_at) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // RETURN_GENERATED_KEYS : insert 후 DB가 만든 PK를 받아오겠다는 옵션
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, project.getUserId());
            pstmt.setString(2, project.getTitle());
            pstmt.setString(3, project.getSubtitle());       // null 가능
            pstmt.setString(4, project.getIntroduction());
            pstmt.setString(5, project.getItemInfo());       // null 가능
            pstmt.setLong(6, project.getGoalAmount());
            pstmt.setInt(7, project.getCategory());
            pstmt.setInt(8, project.getProjectStatus());
            pstmt.setObject(9, project.getFundingStartAt());  // LocalDateTime, null 가능
            pstmt.setObject(10, project.getFundingEndAt());
            pstmt.setObject(11, project.getPaymentDueAt());
            pstmt.setObject(12, project.getDeliveryStartAt());

            pstmt.executeUpdate();
            return generatedKey(pstmt);
        }
    }

    static Long generatedKey(PreparedStatement pstmt) throws SQLException {
        try (ResultSet rs = pstmt.getGeneratedKeys()) {
            return rs.next() ? rs.getLong(1) : null;
        }
    }
}
