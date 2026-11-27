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
import exception.SupportRejectedException;

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

    public boolean checkCountSupporterByReward(Long userId) {
        try (Connection conn = ds.getConnection()) {
            return fsDAO.countSupporterByReward(conn, userId) >= 2;

        } catch (SQLException e) {
            throw new RuntimeException("리워드별 후원 체크 실패", e);
        }

    }

    public List<SupportDTO> findSupports(Long supporterId) {
        try (Connection conn = ds.getConnection()) {
            return fsDAO.findSupports(conn, supporterId);

        } catch (SQLException e) {
            throw new RuntimeException("후원 내역 조회 실패", e);
        }
    }

    public void support(SupportRequestDTO srd) {
        if (srd == null
                || srd.getUserId() == null
                || srd.getProjectId() == null
                || srd.getRewardId() == null
                || srd.getUserId() <= 0
                || srd.getProjectId() <= 0
                || srd.getRewardId() <= 0) {
            throw new SupportRejectedException("올바른 후원 신청 정보가 필요합니다.");
        }

        if (srd.getRewardCount() < 1) {
            throw new SupportRejectedException("신청 수량은 1개 이상이어야 합니다.");
        }

        try (Connection conn = ds.getConnection()) {
            try {
                conn.setAutoCommit(false);

                RewardDTO requestRewardDTO =
                        rewardDAO.findByIdForUpdate(conn, srd.getRewardId());

                if (requestRewardDTO == null
                        || !srd.getProjectId().equals(requestRewardDTO.getProjectId())) {
                    throw new SupportRejectedException(
                            "선택한 프로젝트에 속하는 리워드가 아닙니다.");
                }

                UserDTO supporterDTO =
                        suppoerterDAO.findById(conn, srd.getUserId());

                ProjectDTO requestProjectDTO =
                        projectDAO.findById(conn, srd.getProjectId());

                FundingSupport fundingSupport = new FundingSupport(
                        srd,
                        supporterDTO,
                        requestProjectDTO,
                        requestRewardDTO
                );

                FundingSupportDTO support = fundingSupport.supportToDTO();

                int remainingStock =
                        fsDAO.getRemainStock(conn, support.getRewardId());

                if (remainingStock < support.getRewardCount()) {
                    throw new SupportRejectedException(
                            "남은 수량이 부족합니다. 남은 수량: " + remainingStock);
                }

                fsDAO.insert(conn, support);

                rewardDAO.updateSoldOutStatus(
                        conn,
                        support.getRewardId(),
                        remainingStock == support.getRewardCount()
                );

                conn.commit();

            } catch (SupportRejectedException e) {
                conn.rollback();
                throw e;

            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException(
                        "후원 처리 중 시스템 오류가 발생했습니다.", e);

            } finally {
                conn.setAutoCommit(true);
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
