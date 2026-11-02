package persistence.dao;

import persistence.dto.RewardDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RewardDAO {

    public int update(Connection conn, int stockQuantity, int rewardId) throws SQLException {
        String sql = """
        UPDATE reward
        SET stock_quantity = ?
        WHERE reward_id = ?
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, stockQuantity);
            pstmt.setLong(2, rewardId);

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
                stock_quantity,
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
                            .stockQuantity(rs.getInt("stock_quantity"))
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
                        .stockQuantity(rs.getInt("stock_quantity"))
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
        String sql = "INSERT INTO reward(project_id, title, content, img_blob, stock_quantity, price) "
                + "VALUES(?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, reward.getProjectId());
            pstmt.setString(2, reward.getTitle());
            pstmt.setString(3, reward.getContent());
            pstmt.setBytes(4, reward.getImgBlob());
            pstmt.setInt(5, reward.getStockQuantity());
            pstmt.setLong(6, reward.getPrice());

            pstmt.executeUpdate();
            return ProjectDAO.generatedKey(pstmt);
        }
    }
}
