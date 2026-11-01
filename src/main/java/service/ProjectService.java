package service;

import persistence.dao.ProjectDAO;
import persistence.dto.ProjectDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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

    public List<Long> findSoldOutProjectIds() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.findSoldOutProjectIds(conn);
        }
    }
}