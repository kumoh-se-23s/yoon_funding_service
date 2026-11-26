package service;

import persistence.dao.ProjectDAO;
import persistence.dto.ProjectDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import exception.DataAccessException;

import java.util.List;
import java.util.ArrayList;

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

    // 승인된 프로젝트 목록 조회
    public List<ProjectDTO> findAll() {
        // 풀에서 연결 빌리고 try 끝나면 반환
        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findAll(conn);
        } catch (SQLException e) {
            throw new DataAccessException("프로젝트 목록 조회 실패", e);
        }
    }

    // 품절 상태의 프로젝트 id 반환
    public List<Long> findSoldOutProjectIds() {
        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findSoldOutProjectIds(conn);
        } catch (SQLException e) {
            throw new DataAccessException("품절 프로젝트 조회 실패", e);
        }
    }

    // 필터링 기능
    public List<ProjectDTO> findByFilter(
            int category, int fundingStatus) {

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

        try (Connection conn = dataSource.getConnection()) {
            List<ProjectDTO> projects = projectDAO.findAll(conn);
            List<ProjectDTO> filtered = new ArrayList<>();

            for (ProjectDTO project : projects) {
                boolean categoryMatches = category == -1
                        || categories[category - 1].equals(project.getCategory());

                String progressStatus = project.getProgressStatus();

                boolean ongoing = "ongoing".equals(progressStatus);
                boolean closed = "success".equals(progressStatus)
                        || "fail".equals(progressStatus);

                boolean statusMatches = fundingStatus == 0
                        || (fundingStatus == 1 && ongoing)
                        || (fundingStatus == 2 && closed);

                if (categoryMatches && statusMatches) {
                    filtered.add(project);
                }
            }

            return filtered;
        } catch (SQLException e) {
            throw new DataAccessException("프로젝트 필터 조회 실패", e);
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

    // 메이커 본인 프로젝트의 펀딩 현황을 한 번에 조회
    public ProjectFundingStatusDTO findFundingStatus(UserDTO loginUser, Long projectId) {

        validateMaker(loginUser);

        try (Connection conn = dataSource.getConnection()) {
            ProjectDTO project = validateProjectOwner(
                    conn, projectId, loginUser.getUserId());

            List<RewardFundingStatusDTO> rewards = projectDAO.findRewardFundingStatus(
                            conn, projectId, loginUser.getUserId());

            List<ProjectSupporterDTO> supporters =
                    projectDAO.findProjectSupporters(
                            conn, projectId, loginUser.getUserId());

            return new ProjectFundingStatusDTO(
                    project, rewards, supporters);

        } catch (SQLException e) {
            throw new DataAccessException("프로젝트 펀딩 현황 조회 실패", e);
        }
    }
}