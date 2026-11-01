package project.persistence.dao;

import lombok.RequiredArgsConstructor;
import project.persistence.dto.FundingSupportDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@RequiredArgsConstructor
public class FundingSupportDAO {

    private final DataSource ds;

    public int insert(Connection conn, FundingSupportDTO fsDTO) {

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
                VALUES (?, ?, ?, ?, ?, ?, true)
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, fsDTO.getRewardId());
            pstmt.setLong(2, fsDTO.getUserId());
            pstmt.setString(3, fsDTO.getRecipientName());
            pstmt.setString(4, fsDTO.getRecipientContact());
            pstmt.setString(5, fsDTO.getDeliveryAddress());
            pstmt.setString(6, fsDTO.getDeliveryDetailAddress());

            return pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}