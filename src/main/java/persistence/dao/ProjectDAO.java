package persistence.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import persistence.dto.ProjectDTO;

import java.sql.*;

import persistence.dto.RewardFundingStatusDTO;
import persistence.dto.ProjectSupporterDTO;

public class ProjectDAO {
    // 프로젝트 한 건 조회
    public ProjectDTO findById(
            Connection conn, Long projectId) throws SQLException {
        return selectById(conn, projectId, false);
    }

    // 프로젝트 등록 후, AUTO_INCREMENT로 만들어진 project_id를 돌려줌
    // → 이 값을 이미지/리워드/심사 이력 insert에 그대로 넘겨서 FK로 사용
    public Long insert(Connection conn, ProjectDTO project) throws SQLException {
        String sql = """
        INSERT INTO project(
            user_id, title, subtitle, introduction, item_info,
            goal_amount, category, approval_status, progress_status,
            funding_start_at, funding_end_at,
            payment_due_at, delivery_start_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String approvalStatus = project.getApprovalStatus();
        String progressStatus = project.getProgressStatus();

        if (approvalStatus == null) {
            approvalStatus = "awaiting";
        }

        if (progressStatus == null) {
            progressStatus = "ready";
        }

        try (PreparedStatement pstmt = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, project.getUserId());
            pstmt.setString(2, project.getTitle());
            pstmt.setString(3, project.getSubtitle());
            pstmt.setString(4, project.getIntroduction());
            pstmt.setString(5, project.getItemInfo());
            pstmt.setLong(6, project.getGoalAmount());
            pstmt.setString(7, project.getCategory());
            pstmt.setString(8, approvalStatus);
            pstmt.setString(9, progressStatus);
            pstmt.setObject(10, project.getFundingStartAt());
            pstmt.setObject(11, project.getFundingEndAt());
            pstmt.setObject(12, project.getPaymentDueAt());
            pstmt.setObject(13, project.getDeliveryStartAt());

            pstmt.executeUpdate();
            return generatedKey(pstmt);
        }
    }

    static Long generatedKey(PreparedStatement pstmt) throws SQLException {
        try (ResultSet rs = pstmt.getGeneratedKeys()) {
            return rs.next() ? rs.getLong(1) : null;
        }
    }

    // 승인된 프로젝트 전체 목록 조회
    public List<ProjectDTO> findAll(Connection conn) throws SQLException {
        return findByFilter(conn, null, 0);
    }

    // 승인된 프로젝트를 카테고리와 진행 상태로 필터링
    // category가 null이면 전체 카테고리
    // fundingStatus: 0은 전체, 1은 진행 중, 2는 마감
    public List<ProjectDTO> findByFilter(
            Connection conn, String category, int fundingStatus)
            throws SQLException {

        String sql = """
        SELECT project_id, user_id, title, subtitle,
               introduction, item_info, goal_amount,
               category, approval_status, progress_status,
               funding_start_at, funding_end_at,
               payment_due_at, delivery_start_at
        FROM project
        WHERE approval_status = ?
        """;

        if (category != null) {
            sql += " AND category = ?";
        }

        if (fundingStatus == 1) {
            sql += " AND progress_status = ?";
        } else if (fundingStatus == 2) {
            sql += " AND progress_status IN (?, ?)";
        }

        sql += " ORDER BY project_id DESC";

        List<ProjectDTO> projects = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int parameterIndex = 1;

            pstmt.setString(parameterIndex++, "approved");

            if (category != null) {
                pstmt.setString(parameterIndex++, category);
            }

            if (fundingStatus == 1) {
                pstmt.setString(parameterIndex, "ongoing");
            } else if (fundingStatus == 2) {
                pstmt.setString(parameterIndex++, "success");
                pstmt.setString(parameterIndex, "fail");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    projects.add(mapProject(rs));
                }
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
                       r.total_count - COALESCE(SUM(fs.reward_count), 0) AS remaining_quantity
                FROM reward r
                LEFT JOIN support fs
                    ON fs.reward_id = r.reward_id
                    AND fs.cancel_status = FALSE
                GROUP BY r.project_id, r.reward_id, r.total_count
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

    // 메이커 본인의 전체 프로젝트 목록 조회
    public List<ProjectDTO> findByMaker(
            Connection conn, Long makerId) throws SQLException {
        return findByMaker(conn, makerId, null);
    }

    // 메이커 본인의 프로젝트 조회
    // projectId가 null이면 전체 목록, 값이 있으면 해당 프로젝트만 조회
    public List<ProjectDTO> findByMaker(
            Connection conn, Long makerId, Long projectId) throws SQLException {

        String sql = """
        SELECT project_id, user_id, title, subtitle,
               introduction, item_info, goal_amount,
               category, approval_status, progress_status,
               funding_start_at, funding_end_at,
               payment_due_at, delivery_start_at
        FROM project
        WHERE user_id = ?
        """;

        if (projectId != null) {
            sql += " AND project_id = ?";
        }

        sql += " ORDER BY project_id DESC";

        List<ProjectDTO> projects = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, makerId);

            if (projectId != null) {
                pstmt.setLong(2, projectId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    projects.add(mapProject(rs));
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
                   r.total_count,
                   COALESCE(SUM(fs.reward_count), 0) AS applied_quantity,
                   r.total_count - COALESCE(SUM(fs.reward_count), 0) AS remaining_quantity,
                   r.price * COALESCE(SUM(fs.reward_count), 0) AS funded_amount
            FROM reward r
            JOIN project p
                ON p.project_id = r.project_id
            LEFT JOIN support fs
                ON fs.reward_id = r.reward_id
                AND fs.cancel_status = FALSE
            WHERE r.project_id = ?
              AND p.user_id = ?
            GROUP BY r.reward_id, r.title, r.price, r.total_count
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
                    reward.setTotalCount(rs.getInt("total_count"));                    reward.setAppliedQuantity(rs.getLong("applied_quantity"));
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
                   fs.reward_count,
                   fs.processed_at,
                   fs.recipient_name,
                   fs.recipient_contact,
                   fs.delivery_address,
                   fs.delivery_detail_address
            FROM support fs
            JOIN reward r
                ON r.reward_id = fs.reward_id
            JOIN project p
                ON p.project_id = r.project_id
            JOIN users u
                ON u.user_id = fs.user_id
            WHERE p.project_id = ?
              AND p.user_id = ?
              AND fs.cancel_status = FALSE
            ORDER BY fs.processed_at DESC, fs.support_id DESC
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
                    supporter.setRewardCount(rs.getInt("reward_count"));
                    supporter.setProcessedAt(rs.getObject("processed_at", LocalDateTime.class));
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

    // 프로젝트 조회 및 행 잠금
    public ProjectDTO findByIdForUpdate(Connection conn, Long projectId)
            throws SQLException {

        return selectById(conn, projectId, true);
    }

    // 일반 단건 조회와 행 잠금 조회 공통 처리
    private ProjectDTO selectById(Connection conn, Long projectId, boolean forUpdate)
            throws SQLException {

        String sql = "SELECT * FROM project WHERE project_id = ?"
                + (forUpdate ? " FOR UPDATE" : "");

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, projectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return mapProject(rs);
            }
        }
    }

    // 프로젝트 상태 변경
    // 기존 상태와 일치할 때만 변경
    public int changeStatus(
            Connection conn,
            Long projectId,
            String before,
            String after) throws SQLException {

        String sql = """
        UPDATE project
        SET approval_status = ?
        WHERE project_id = ?
          AND approval_status = ?
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, after);
            pstmt.setLong(2, projectId);
            pstmt.setString(3, before);

            return pstmt.executeUpdate();
        }
    }

    // 현재 행의 프로젝트 정보를 DTO로 변환
    // rs.next() 호출은 조회 메서드에서 처리
    private ProjectDTO mapProject(ResultSet rs) throws SQLException {
        ProjectDTO project = new ProjectDTO();

        project.setProjectId(rs.getLong("project_id"));
        project.setUserId(rs.getLong("user_id"));
        project.setTitle(rs.getString("title"));
        project.setSubtitle(rs.getString("subtitle"));
        project.setIntroduction(rs.getString("introduction"));
        project.setItemInfo(rs.getString("item_info"));
        project.setGoalAmount(rs.getLong("goal_amount"));
        project.setCategory(rs.getString("category"));
        project.setApprovalStatus(rs.getString("approval_status"));
        project.setProgressStatus(rs.getString("progress_status"));
        project.setFundingStartAt(rs.getObject("funding_start_at", LocalDateTime.class));
        project.setFundingEndAt(rs.getObject("funding_end_at", LocalDateTime.class));
        project.setPaymentDueAt(rs.getObject("payment_due_at", LocalDateTime.class));
        project.setDeliveryStartAt(rs.getObject("delivery_start_at", LocalDateTime.class));

        return project;
    }
}
