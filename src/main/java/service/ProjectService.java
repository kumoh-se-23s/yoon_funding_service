package service;

import persistence.dao.ProjectDAO;
import persistence.dto.ProjectDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import java.time.LocalDateTime;
import java.util.ArrayList;

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
}