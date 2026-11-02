package service;

import domain.FundingSupport;
import lombok.*;
import persistence.dao.FundingSupportDAO;
import persistence.dao.ProjectDAO;
import persistence.dao.RewardDAO;
import persistence.dao.UserDAO;
import persistence.dto.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@RequiredArgsConstructor
public class FundingSupportService {
    private final DataSource ds;
    private final FundingSupportDAO fsDAO;
    private final UserDAO suppoerterDAO;
    private final ProjectDAO projectDAO;
    private final RewardDAO rewardDAO;

    public List<SupportDTO> findSupports(Long supporterId) {
        try (Connection conn = ds.getConnection()) {
            return fsDAO.findSupports(conn, supporterId);

        } catch (SQLException e) {
            throw new RuntimeException("후원 내역 조회 실패", e);
        }
    }

    public void support (SupportRequestDTO srd) {
        try (Connection conn = ds.getConnection()) {

            try {
                conn.setAutoCommit(false);

                UserDTO suppoerterDTO = suppoerterDAO.findById(conn, srd.getUserId());

                ProjectDTO requestProjectDTO = projectDAO.findById(conn, srd.getProjectId());

                RewardDTO requestRewardDTO = rewardDAO.findById(conn, srd.getRewardId());

                FundingSupport fs = new FundingSupport(
                        srd,
                        suppoerterDTO,
                        requestProjectDTO,
                        requestRewardDTO
                );

                FundingSupportDTO requestFundingSupportDTO = fs.supportToDTO();

                fs.checkRewardStock(fsDAO.getRemainStock(conn, requestFundingSupportDTO.getRewardId()));
                fsDAO.insert(conn, requestFundingSupportDTO);

                conn.commit();

            } catch (Exception e) {

                conn.rollback();
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getRemainStock(Long rewardId) {
        try(Connection conn = ds.getConnection()) {
            return fsDAO.getRemainStock(conn, rewardId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
