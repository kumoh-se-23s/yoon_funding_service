package service;

import persistence.dao.ProjectDAO;
import persistence.dto.ProjectDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import exception.DataAccessException;

import java.util.List;

import persistence.dto.UserDTO;
import persistence.dto.RewardFundingStatusDTO;
import persistence.dto.ProjectSupporterDTO;
import persistence.dto.ProjectFundingStatusDTO;

// 프로젝트 조회에 필요한 DB 연결, DAO 호출
public class ProjectService {
    private final DataSource dataSource;
    private final ProjectDAO projectDAO;

    public ProjectService(DataSource dataSource, ProjectDAO projectDAO) {
        this.dataSource = dataSource;
        this.projectDAO = projectDAO;
    }


    public ProjectDTO findById(Long id)  {
        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("프로젝트 단건 조회 실패", e);
        }
    }

    // 기존 호출은 최신순으로 조회
    public List<ProjectDTO> findAll() {
        return findAll(1);
    }

    // 전체 목록에 정렬 적용
    // sort: 1 최신순, 2 모집금액순, 3 마감임박순, 4 좋아요순
    public List<ProjectDTO> findAll(int sort) {
        return findByFilter(-1, 0, sort);
    }

    // 품절 상태의 프로젝트 id 반환
    public List<Long> findSoldOutProjectIds() {
        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findSoldOutProjectIds(conn);
        } catch (SQLException e) {
            throw new DataAccessException("품절 프로젝트 조회 실패", e);
        }
    }

    // 기존 필터 조회 호출은 최신순 유지
    public List<ProjectDTO> findByFilter(
            int category, int fundingStatus) {

        return findByFilter(category, fundingStatus, 1);
    }

    // 카테고리, 진행 상태 필터와 정렬을 함께 적용
    public List<ProjectDTO> findByFilter(
            int category, int fundingStatus, int sort) {

        String[] categories = {
                "tech", "fashion", "beauty", "food",
                "living", "hobby", "pet", "culture"
        };

        if (category != -1
                && (category < 1 || category > categories.length)) {
            throw new IllegalArgumentException("잘못된 카테고리입니다.");
        }

        if (fundingStatus < 0 || fundingStatus > 2) {
            throw new IllegalArgumentException("잘못된 진행상태입니다.");
        }

        if (sort < 1 || sort > 4) {
            throw new IllegalArgumentException("잘못된 정렬 기준입니다.");
        }

        String categoryValue = category == -1 ? null : categories[category - 1];

        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findByFilter(conn, categoryValue, fundingStatus, sort);
        } catch (SQLException e) {
            throw new DataAccessException("프로젝트 목록 조회 실패", e);
        }
    }

    // 로그인한 메이커의 프로젝트 목록 조회
    public List<ProjectDTO> findByMaker(UserDTO loginUser) {
        validateMaker(loginUser);

        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findByMaker(conn, loginUser.getUserId());
        } catch (SQLException e) {
            throw new DataAccessException("메이커 프로젝트 목록 조회 실패", e);
        }
    }

    // 메이커 본인 프로젝트의 리워드별 수량, 후원 금액 조회
    public List<RewardFundingStatusDTO> findRewardFundingStatus(
            UserDTO loginUser, Long projectId) {
        validateMaker(loginUser);

        try (Connection conn = dataSource.getConnection()) {
            validateProjectOwner(conn, projectId, loginUser.getUserId());

            return projectDAO.findRewardFundingStatus(
                    conn, projectId, loginUser.getUserId());
        } catch (SQLException e) {
            throw new DataAccessException("리워드별 후원 현황 조회 실패", e);
        }
    }

    // 메이커 본인 프로젝트의 후원자, 배송 정보 조회
    public List<ProjectSupporterDTO> findProjectSupporters(
            UserDTO loginUser, Long projectId) {
        validateMaker(loginUser);

        try (Connection conn = dataSource.getConnection()) {
            validateProjectOwner(conn, projectId, loginUser.getUserId());

            return projectDAO.findProjectSupporters(
                    conn, projectId, loginUser.getUserId());
        } catch (SQLException e) {
            throw new DataAccessException("프로젝트 후원자 및 배송 정보 조회 실패", e);
        }
    }

    // 로그인 여부 확인, 메이커 권한 확인
    private void validateMaker(UserDTO loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (!"maker".equals(loginUser.getRole())) {
            throw new IllegalArgumentException("메이커만 이용할 수 있습니다.");
        }
    }

    // 본인 소유의 프로젝트인지 확인하고, 해당 프로젝트 반환
    private ProjectDTO validateProjectOwner(Connection conn, Long projectId, Long makerId) throws SQLException {

        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("올바른 프로젝트 번호를 입력해 주세요.");
        }

        List<ProjectDTO> projects = projectDAO.findByMaker(conn, makerId, projectId);

        if (projects.isEmpty()) {
            throw new IllegalArgumentException("본인의 프로젝트만 조회할 수 있습니다.");
        }

        return projects.get(0);
    }

    // 메이커 본인 프로젝트의 펀딩 현황을 "같은 시점" 기준으로 조회
    public ProjectFundingStatusDTO findFundingStatus(UserDTO loginUser, Long projectId) {
        validateMaker(loginUser);

        try (Connection conn = dataSource.getConnection()) {
            // 풀에서 빌린 연결의 기존 설정 보관
            boolean oldAutoCommit = conn.getAutoCommit();
            boolean oldReadOnly = conn.isReadOnly();
            int oldIsolation = conn.getTransactionIsolation();

            try {
                // 조회용 설정을 적용한 뒤 트랜잭션 시작
                conn.setTransactionIsolation(
                        Connection.TRANSACTION_REPEATABLE_READ);
                conn.setReadOnly(true);
                conn.setAutoCommit(false);

                // 첫 SELECT할 때 조회 기준 시점 지정
                ProjectDTO project = validateProjectOwner(conn, projectId, loginUser.getUserId());

                List<RewardFundingStatusDTO> rewards =
                        projectDAO.findRewardFundingStatus(conn, projectId, loginUser.getUserId());

                List<ProjectSupporterDTO> supporters =
                        projectDAO.findProjectSupporters(conn, projectId, loginUser.getUserId());

                ProjectFundingStatusDTO result =
                        new ProjectFundingStatusDTO(project, rewards, supporters);

                conn.commit();
                return result;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    // rollback 실패하면 원래 오류에 함께 기록
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                // 연결을 풀에 반환하기 전에 기존 설정 복구
                conn.setAutoCommit(oldAutoCommit);
                conn.setReadOnly(oldReadOnly);
                conn.setTransactionIsolation(oldIsolation);
            }

        } catch (SQLException e) {
            throw new DataAccessException("프로젝트 펀딩 현황 조회 실패", e);
        }
    }
}