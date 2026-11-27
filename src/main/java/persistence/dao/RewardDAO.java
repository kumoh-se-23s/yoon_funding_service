package persistence.dao;

import persistence.dto.RewardDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RewardDAO {

    public int update(
            Connection conn, int totalCount,
            int rewardId, int projectId) throws SQLException {

        String sql = """
            UPDATE reward
            SET total_count = ?
            WHERE reward_id = ?
              AND project_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, totalCount);
            pstmt.setLong(2, rewardId);
            pstmt.setLong(3, projectId);

            return pstmt.executeUpdate();
        }
    }

    public List<RewardDTO> findAll(Connection conn, Long projectId) {
        String sql = """
            SELECT
                reward_id,
                project_id,
                title,
                content,
                img_blob,
                total_count,
                sold_out_status,
                price
            FROM reward
            WHERE project_id = ?
            ORDER BY reward_id
            """;

        List<RewardDTO> rewards = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, projectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    RewardDTO reward = RewardDTO.builder()
                            .rewardId(rs.getLong("reward_id"))
                            .projectId(rs.getLong("project_id"))
                            .title(rs.getString("title"))
                            .content(rs.getString("content"))
                            .imgBlob(rs.getBytes("img_blob"))
                            .totalCount(rs.getInt("total_count"))
                            .soldOutStatus(rs.getBoolean("sold_out_status"))
                            .price(rs.getLong("price"))
                            .build();

                    rewards.add(reward);
                }
                return rewards;
            }

        }catch(SQLException e) {
            throw new RuntimeException(e);
        }


    }

    public RewardDTO findById(Connection conn, Long rewardId) throws SQLException {

        String sql = """
            SELECT *
            FROM reward
            WHERE reward_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, rewardId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                return RewardDTO.builder()
                        .rewardId(rs.getLong("reward_id"))
                        .projectId(rs.getLong("project_id"))
                        .title(rs.getString("title"))
                        .content(rs.getString("content"))
                        .imgBlob(rs.getBytes("img_blob"))
                        .totalCount(rs.getInt("total_count"))
                        .soldOutStatus(rs.getBoolean("sold_out_status"))
                        .price(rs.getLong("price"))
                        .build();
            }
        }
    }

    public int getRemainReward() {
        return 1;
    }

    // 리워드 등록 후 생성된 reward_id 반환 → 후원(funding_support) insert에 사용
    public Long insert(Connection conn, RewardDTO reward) throws SQLException {
        String sql = """
            INSERT INTO reward(
                project_id, title, content, img_blob,
                total_count, price, sold_out_status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, reward.getProjectId());
            pstmt.setString(2, reward.getTitle());
            pstmt.setString(3, reward.getContent());
            pstmt.setBytes(4, reward.getImgBlob());
            pstmt.setInt(5, reward.getTotalCount());
            pstmt.setLong(6, reward.getPrice());
            pstmt.setBoolean(7, reward.isSoldOutStatus());

            pstmt.executeUpdate();
            return ProjectDAO.generatedKey(pstmt);
        }
    }

    // 프로젝트에 등록된 리워드 개수 조회
    // 관리자 승인 전 리워드 존재 여부 확인에 사용
    public int countByProjectId(Connection conn, Long projectId)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM reward WHERE project_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, projectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
    public RewardDTO findByIdForUpdate(Connection conn, Long rewardId)
            throws SQLException {

        String sql = """
            SELECT reward_id
            FROM reward
            WHERE reward_id = ?
            FOR UPDATE
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, rewardId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
            }
        }

        return findById(conn, rewardId);
    }

    public void updateSoldOutStatus(
            Connection conn, Long rewardId, boolean soldOut)
            throws SQLException {

        String sql = """
            UPDATE reward
            SET sold_out_status = ?
            WHERE reward_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, soldOut);
            pstmt.setLong(2, rewardId);
            pstmt.executeUpdate();
        }
    }

}