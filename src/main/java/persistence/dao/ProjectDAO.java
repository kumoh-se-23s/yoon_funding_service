package persistence.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import persistence.dto.ProjectDTO;

import java.sql.*;

import persistence.dto.RewardFundingStatusDTO;
import persistence.dto.ProjectSupporterDTO;

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

    // 메이커 본인의 프로젝트들 조회
    // makerId -> 로그인한 회원의 userId
    public List<ProjectDTO> findByMaker(Connection conn, Long makerId) throws SQLException {
        String sql = """
            SELECT project_id, user_id, title, subtitle,
                   introduction, item_info, goal_amount,
                   category, project_status,
                   funding_start_at, funding_end_at,
                   payment_due_at, delivery_start_at
            FROM project
            WHERE user_id = ?
            ORDER BY project_id DESC
            """; // 여기서도 등록 최신순으로 조회함.

        List<ProjectDTO> projects = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, makerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
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
                            rs.getObject("funding_start_at", LocalDateTime.class));
                    project.setFundingEndAt(
                            rs.getObject("funding_end_at", LocalDateTime.class));
                    project.setPaymentDueAt(
                            rs.getObject("payment_due_at", LocalDateTime.class));
                    project.setDeliveryStartAt(
                            rs.getObject("delivery_start_at", LocalDateTime.class));

                    projects.add(project);
                }
            }
        }



        return projects;
    }

    // 메이커 본인 프로젝트의 리워드별 후원 상태 조회
    public List<RewardFundingStatusDTO> findRewardFundingStatus(
            Connection conn, Long projectId, Long makerId) throws SQLException {
        String sql = """
            SELECT r.reward_id,
                   r.title,
                   r.price,
                   r.stock_quantity,
                   COUNT(fs.support_id) AS applied_quantity,
                   r.stock_quantity - COUNT(fs.support_id) AS remaining_quantity,
                   r.price * COUNT(fs.support_id) AS funded_amount
            FROM reward r
            JOIN project p
                ON p.project_id = r.project_id
            LEFT JOIN funding_support fs
                ON fs.reward_id = r.reward_id
                AND fs.cancel_status = FALSE
            WHERE r.project_id = ?
              AND p.user_id = ?
            GROUP BY r.reward_id, r.title, r.price, r.stock_quantity
            ORDER BY r.reward_id
            """;

        List<RewardFundingStatusDTO> rewards = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, projectId);
            pstmt.setLong(2, makerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    RewardFundingStatusDTO reward = new RewardFundingStatusDTO();

                    reward.setRewardId(rs.getLong("reward_id"));
                    reward.setTitle(rs.getString("title"));
                    reward.setPrice(rs.getLong("price"));
                    reward.setStockQuantity(rs.getInt("stock_quantity"));
                    reward.setAppliedQuantity(rs.getLong("applied_quantity"));
                    reward.setRemainingQuantity(rs.getLong("remaining_quantity"));
                    reward.setFundedAmount(rs.getLong("funded_amount"));

                    rewards.add(reward);
                }
            }
        }

        return rewards;
    }

    // 본인 프로젝트의 정상 후원 내역, 배송 정보 조회
    public List<ProjectSupporterDTO> findProjectSupporters(
            Connection conn, Long projectId, Long makerId) throws SQLException {
        String sql = """
            SELECT fs.support_id,
                   fs.user_id,
                   u.name AS supporter_name,
                   u.nickname AS supporter_nickname,
                   r.title AS reward_title,
                   fs.supported_at,
                   fs.recipient_name,
                   fs.recipient_contact,
                   fs.delivery_address,
                   fs.delivery_detail_address
            FROM funding_support fs
            JOIN reward r
                ON r.reward_id = fs.reward_id
            JOIN project p
                ON p.project_id = r.project_id
            JOIN users u
                ON u.user_id = fs.user_id
            WHERE p.project_id = ?
              AND p.user_id = ?
              AND fs.cancel_status = FALSE
            ORDER BY fs.supported_at DESC, fs.support_id DESC
            """;

        List<ProjectSupporterDTO> supporters = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, projectId);
            pstmt.setLong(2, makerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ProjectSupporterDTO supporter = new ProjectSupporterDTO();

                    supporter.setSupportId(rs.getLong("support_id"));
                    supporter.setUserId(rs.getLong("user_id"));
                    supporter.setSupporterName(rs.getString("supporter_name"));
                    supporter.setSupporterNickname(rs.getString("supporter_nickname"));
                    supporter.setRewardTitle(rs.getString("reward_title"));
                    supporter.setSupportedAt(rs.getObject("supported_at", LocalDateTime.class));
                    supporter.setRecipientName(rs.getString("recipient_name"));
                    supporter.setRecipientContact(rs.getString("recipient_contact"));
                    supporter.setDeliveryAddress(rs.getString("delivery_address"));
                    supporter.setDeliveryDetailAddress(rs.getString("delivery_detail_address"));

                    supporters.add(supporter);
                }
            }
        }

        return supporters;
    }
}
