package project.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import project.domain.FundingSupport;
import project.persistence.dao.FundingSupportDAO;
import project.persistence.dto.SupportRequestDTO;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;


@AllArgsConstructor
public class FundingSupportService {
    private final DataSource ds;
    private final FundingSupportDAO fs;

    public void support (SupportRequestDTO srd) {
        try (Connection conn = ds.getConnection()) {

            try {
                conn.setAutoCommit(false);

                boardDAO.insert(conn, board);
                memberDAO.increasePoint(conn, board.getWriter());

                conn.commit();

            } catch (Exception e) {

                conn.rollback();
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } 


    }
}
