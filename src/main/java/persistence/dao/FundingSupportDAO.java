package persistence.dao;

import persistence.dto.FundingSupportDTO;

import java.sql.*;

public class FundingSupportDAO {

    // 후원 등록 후 생성된 support_id 반환 → 리뷰(review) insert에 사용
    public Long insert(Connection conn, FundingSupportDTO support) throws SQLException {
        String sql = "INSERT INTO funding_support(reward_id, user_id, supported_at, recipient_name, recipient_contact, "
                + "delivery_address, delivery_detail_address, cancel_status) "
                + "VALUES(?, ?, COALESCE(?, NOW()), ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, support.getRewardId());
            pstmt.setLong(2, support.getUserId());
            pstmt.setObject(3, support.getSupportedAt());
            pstmt.setString(4, support.getRecipientName());
            pstmt.setString(5, support.getRecipientContact());
            pstmt.setString(6, support.getDeliveryAddress());
            pstmt.setString(7, support.getDeliveryDetailAddress());
            pstmt.setBoolean(8, support.isCancelStatus());

            pstmt.executeUpdate();
            return ProjectDAO.generatedKey(pstmt);
        }
    }
}
