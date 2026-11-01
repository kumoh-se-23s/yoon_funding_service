package project.persistence.dao;

import lombok.RequiredArgsConstructor;
import project.persistence.dto.FundingSupportDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public class FundingSupportDAO {

    private final DataSource ds;

    public int insert(Connection conn, FundingSupportDTO fsDTO) throws RuntimeException {

        String sql = """
                INSERT INTO funding_support (
                    reward_id,
                    user_id,
                    recipient_name,
                    recipient_contact,
                    delivery_address,
                    delivery_detail_address,
                    cancel_status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, fsDTO.getRewardId());
            pstmt.setLong(2, fsDTO.getUserId());
            pstmt.setString(3, fsDTO.getRecipientName());
            pstmt.setString(4, fsDTO.getRecipientContact());
            pstmt.setString(5, fsDTO.getDeliveryAddress());
            pstmt.setString(6, fsDTO.getDeliveryDetailAddress());
            pstmt.setBoolean(7, fsDTO.isCancelStatus());

            return pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("err: FundingSupportDAO.insert");
            throw new RuntimeException(e);
        }
    }

    public int getRemainStock(Connection conn, FundingSupportDTO fsDTO) throws RuntimeException{
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
}