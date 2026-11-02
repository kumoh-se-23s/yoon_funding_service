package persistence.dao;

import lombok.RequiredArgsConstructor;
import persistence.dto.FundingSupportDTO;
import persistence.dto.SupportDTO;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FundingSupportDAO {

    public List<SupportDTO> findSupports(Connection conn, Long supporterId)
            throws SQLException {

        String sql = """
            SELECT
                fs.user_id,
                r.project_id,
                fs.reward_id,
                r.title AS reward_title,
                p.title AS project_title
            FROM funding_support fs
            JOIN reward r
                ON fs.reward_id = r.reward_id
            JOIN project p
                ON r.project_id = p.project_id
            WHERE fs.user_id = ?
              AND cancel_status = FALSE
            ORDER BY fs.support_id
            """;

        List<SupportDTO> supports = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, supporterId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SupportDTO support = SupportDTO.builder()
                            .userId(rs.getLong("user_id"))
                            .projectId(rs.getLong("project_id"))
                            .rewardId(rs.getLong("reward_id"))
                            .rewardTitle(rs.getString("reward_title"))
                            .projectTitle(rs.getString("project_title"))
                            .build();

                    supports.add(support);
                }
            }
        }

        return supports;
    }

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

    public int getRemainStock(Connection conn, Long rewardId) throws RuntimeException{
        String sql = """
            SELECT r.stock_quantity - COUNT(fs.user_id)
            FROM reward r
            JOIN funding_support fs
                ON r.reward_id = fs.reward_id
                AND fs.cancel_status = false
            WHERE r.reward_id = ?
            GROUP BY r.reward_id
            """;

        int remainStock = -1;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, rewardId);

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
}
