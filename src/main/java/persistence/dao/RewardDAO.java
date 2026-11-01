package persistence.dao;

import persistence.dto.RewardDTO;

import java.sql.*;

public class RewardDAO {

    //프롲게트에 등록된 리워드 개수 조회 
    public int countByProjectId(Connection conn, Long projectId)
        throws SQLException {

    String sql =
            "SELECT COUNT(*) FROM reward WHERE project_id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, projectId);

        try (ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
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
