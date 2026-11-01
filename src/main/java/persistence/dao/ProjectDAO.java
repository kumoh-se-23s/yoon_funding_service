package persistence.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // 승인된 프로젝트 목록 조회
    public List<ProjectDTO> findAll(Connection conn) throws SQLException {
        String sql = """
                SELECT project_id, user_id, title, subtitle,
                        introduction, item_info, goal_amount,
                        category, project_status,
                        funding_start_at, funding_end_at,
                        payment_due_at, delivery_start_at
                FROM project
                WHERE project_status = 1
                ORDER BY project_id DESC
                """; // project_id는 Auto Increment 속성이므로, 등록한 최신순으로 보여줌. DESC
        List<ProjectDTO> projects = new ArrayList<>();

        try(PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()){

            while(rs.next()){
                ProjectDTO project = new ProjectDTO();

                project.setProjectId(rs.getLong("project_id"));
                project.setUserId(rs.getLong("user_id"));
                project.setTitle(rs.getString("title"));
                project.setSubtitle(rs.getString("subtitle"));
                project.setIntroduction(rs.getString("introduction"));
                project.setItemInfo(rs.getString("item_info"));
                project.setGoalAmount(rs.getLong("goal_amount"));
                project.setCategory(rs.getInt("category"));
                project.setProjectStatus(rs.getInt("project_status"));

                project.setFundingStartAt(rs.getObject("funding_start_at", LocalDateTime.class));
                project.setFundingEndAt(rs.getObject("funding_end_at", LocalDateTime.class));
                project.setPaymentDueAt(rs.getObject("payment_due_at", LocalDateTime.class));
                project.setDeliveryStartAt(rs.getObject("delivery_start_at", LocalDateTime.class));

                projects.add(project);
            }
        }

        return projects;
    }

    // 리워드가 존재하고, 모든 리워드 잔량이 0 이하인 프로젝트 번호 조회
    public List<Long> findSoldOutProjectIds(Connection conn) throws SQLException {
        String sql = """
            SELECT remaining.project_id
            FROM (
                SELECT r.project_id,
                       r.reward_id,
                       r.stock_quantity - COUNT(fs.support_id) AS remaining_quantity
                FROM reward r
                LEFT JOIN funding_support fs
                    ON fs.reward_id = r.reward_id
                    AND fs.cancel_status = FALSE
                GROUP BY r.project_id, r.reward_id, r.stock_quantity
            ) remaining
            GROUP BY remaining.project_id
            HAVING MAX(remaining.remaining_quantity) <= 0
            """;

        List<Long> projectIds = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                projectIds.add(rs.getLong("project_id"));
            }
        }

        return projectIds;
    }

// 프로젝트 ID로 단일 프로젝트 조회
public ProjectDTO findById(Connection conn, Long projectId)
        throws SQLException {

    return selectById(conn, projectId, false);
}


// 프로젝트 조회 및 행 잠금
// 승인 처리 중 다른 트랜잭션의 동시 변경 방지
public ProjectDTO findByIdForUpdate(Connection conn, Long projectId)
        throws SQLException {

    return selectById(conn, projectId, true);
}


// 일반 조회와 행 잠금 조회의 공통 처리
private ProjectDTO selectById(
        Connection conn, Long projectId, boolean forUpdate)
        throws SQLException {

    String sql = "SELECT * FROM project WHERE project_id = ?"
            + (forUpdate ? " FOR UPDATE" : "");

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, projectId);

        try (ResultSet rs = pstmt.executeQuery()) {

            if (!rs.next()) {
                return null;
            }

            ProjectDTO project = new ProjectDTO();

            project.setProjectId(rs.getLong("project_id"));
            project.setUserId(rs.getLong("user_id"));
            project.setTitle(rs.getString("title"));
            project.setSubtitle(rs.getString("subtitle"));
            project.setIntroduction(rs.getString("introduction"));
            project.setItemInfo(rs.getString("item_info"));
            project.setGoalAmount(rs.getLong("goal_amount"));
            project.setCategory(rs.getInt("category"));
            project.setProjectStatus(rs.getInt("project_status"));

            project.setFundingStartAt(
                rs.getObject("funding_start_at",
                    java.time.LocalDateTime.class));

            project.setFundingEndAt(
                rs.getObject("funding_end_at",
                    java.time.LocalDateTime.class));

            project.setPaymentDueAt(
                rs.getObject("payment_due_at",
                    java.time.LocalDateTime.class));

            project.setDeliveryStartAt(
                rs.getObject("delivery_start_at",
                    java.time.LocalDateTime.class));

            return project;
        }
    }
}
}
// 기존 상태와 일치할 때만 프로젝트 상태 변경
public int changeStatus(Connection conn, Long projectId,
                        int before, int after) throws SQLException {

    String sql = "UPDATE project SET project_status = ? " +
                 "WHERE project_id = ? AND project_status = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, after);
        pstmt.setLong(2, projectId);
        pstmt.setInt(3, before);

        return pstmt.executeUpdate();
    }
}
