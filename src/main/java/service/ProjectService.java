package service;

import persistence.dao.ProjectDAO;
import persistence.dto.ProjectDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import java.time.LocalDateTime;
import java.util.ArrayList;

import persistence.dto.UserDTO;
import persistence.dto.RewardFundingStatusDTO;
import persistence.dto.ProjectSupporterDTO;

// 프로젝트 조회에 필요한 DB 연결, DAO 호출
public class ProjectService {
    private final DataSource dataSource;
    private final ProjectDAO projectDAO;

    public ProjectService(DataSource dataSource, ProjectDAO projectDAO) {
        this.dataSource = dataSource;
        this.projectDAO = projectDAO;
    }

    // 승인된 프로젝트 목록 조회
    public List<ProjectDTO> findAll() throws SQLException {
        // 풀에서 연결 빌리고 try 끝나면 반환
        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findAll(conn);
        } // 자동으로 conn.close() 호출 -> 연결을 풀에 반환
    }

    // 품절 상태의 프로젝트 id 반환
    public List<Long> findSoldOutProjectIds() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findSoldOutProjectIds(conn);
        }
    }

    // 필터링 기능
    public List<ProjectDTO> findByFilter(int category, int fundingStatus) throws SQLException{
        if(fundingStatus < 0 || fundingStatus > 2){
            throw new IllegalArgumentException("잘못된 진행상태입니다.");
        }

        try(Connection conn = dataSource.getConnection()){
            List<ProjectDTO> projects = projectDAO.findAll(conn);
            List<Long> soldOutIds = projectDAO.findSoldOutProjectIds(conn);
            List<ProjectDTO> filtered = new ArrayList<>();

            LocalDateTime now = LocalDateTime.now();

            for(ProjectDTO project : projects){
                boolean categoryMatches = category == -1 || Integer.valueOf(category).equals(project.getCategory());
                LocalDateTime start = project.getFundingStartAt();
                LocalDateTime end = project.getFundingEndAt();

                boolean started = start != null && end != null && !now.isBefore(start);
                boolean closed = started && (now.isAfter(end) || soldOutIds.contains(project.getProjectId()));
                boolean ongoing = started && (!closed);

                boolean statusMatches = (fundingStatus == 0 || (fundingStatus == 1 && ongoing) || (fundingStatus == 2 && closed));

                if(categoryMatches && statusMatches){
                    filtered.add(project);
                }
            }
            return filtered;
        }
    }

    // 로그인한 메이커의 프로젝트 목록 조회
    public List<ProjectDTO> findByMaker(UserDTO loginUser) throws SQLException {
        validateMaker(loginUser);

        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findByMaker(conn, loginUser.getUserId());
        }
    }

    // 메이커 본인 프로젝트의 리워드별 수량, 후원 금액 조회
    public List<RewardFundingStatusDTO> findRewardFundingStatus(
            UserDTO loginUser, Long projectId) throws SQLException {
        validateMaker(loginUser);

        try (Connection conn = dataSource.getConnection()) {
            validateProjectOwner(conn, projectId, loginUser.getUserId());

            return projectDAO.findRewardFundingStatus(
                    conn, projectId, loginUser.getUserId());
        }
    }

    // 메이커 본인 프로젝트의 후원자, 배송 정보 조회
    public List<ProjectSupporterDTO> findProjectSupporters(
            UserDTO loginUser, Long projectId) throws SQLException {
        validateMaker(loginUser);

        try (Connection conn = dataSource.getConnection()) {
            validateProjectOwner(conn, projectId, loginUser.getUserId());

            return projectDAO.findProjectSupporters(
                    conn, projectId, loginUser.getUserId());
        }
    }

    // 로그인 여부 확인, 메이커 권한 확인
    private void validateMaker(UserDTO loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (!Integer.valueOf(2).equals(loginUser.getRole())) {
            throw new IllegalArgumentException("메이커만 이용할 수 있습니다.");
        }
    }

    // 조회하려는 프로젝트가 본인 소유가 맞는지 확인
    private void validateProjectOwner(
            Connection conn, Long projectId, Long makerId) throws SQLException {
        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("올바른 프로젝트 번호를 입력해 주세요.");
        }

        List<ProjectDTO> projects = projectDAO.findByMaker(conn, makerId);

        for (ProjectDTO project : projects) {
            if (projectId.equals(project.getProjectId())) {
                return;
            }
        }

        throw new IllegalArgumentException(
                "본인의 프로젝트만 조회할 수 있습니다.");
    }
}