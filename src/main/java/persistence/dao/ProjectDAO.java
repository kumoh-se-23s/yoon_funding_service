package persistence.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import persistence.dto.ProjectDTO;

import javax.sql.DataSource;
import java.sql.*;

public class ProjectDAO {


    public ProjectDTO findById(Connection conn, Long projectId) throws SQLException {

        String sql = """
            SELECT *
            FROM project
            WHERE project_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, projectId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                Timestamp fundingStartAt = rs.getTimestamp("funding_start_at");
                Timestamp fundingEndAt = rs.getTimestamp("funding_end_at");
                Timestamp paymentDueAt = rs.getTimestamp("payment_due_at");
                Timestamp deliveryStartAt = rs.getTimestamp("delivery_start_at");

                return ProjectDTO.builder()
                        .projectId(rs.getLong("project_id"))
                        .userId(rs.getLong("user_id"))
                        .title(rs.getString("title"))
                        .subtitle(rs.getString("subtitle"))
                        .introduction(rs.getString("introduction"))
                        .itemInfo(rs.getString("item_info"))
                        .goalAmount(rs.getLong("goal_amount"))
                        .category(rs.getInt("category"))
                        .projectStatus(rs.getInt("project_status"))
                        .fundingStartAt(fundingStartAt != null ? fundingStartAt.toLocalDateTime() : null)
                        .fundingEndAt(fundingEndAt != null ? fundingEndAt.toLocalDateTime() : null)
                        .paymentDueAt(paymentDueAt != null ? paymentDueAt.toLocalDateTime() : null)
                        .deliveryStartAt(deliveryStartAt != null ? deliveryStartAt.toLocalDateTime() : null)
                        .build();
            }
        }
    }

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
    /*
    public Long getFundedAmount(Connection conn, ProjectDTO old) {
        String sql = """
            
                SELECT
                    COALESCE(SUM(r.price), 0) AS current_amount
                FROM reward r
                JOIN funding_support fs
                    ON r.reward_id = fs.reward_id
                WHERE r.project_id = ?
                GROUP BY r.project_id;
            """;

        int remainStock = -1;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, fsDTO.getRewardId());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    remainStock =  rs.getInt(1);
                }
            }

            if(remainStock < 0) {
                throw new IllegalStateException("유효하지 않은 후원자 수");
            }

            return remainStock;
        } catch (SQLException e) {
            System.err.println("후원 신청자 수 조회 실패");
            throw new RuntimeException(e);
        }
    }
    */

}
