package service;

import lombok.RequiredArgsConstructor;
import persistence.dao.ProjectDAO;
import persistence.dao.RewardDAO;
import persistence.dto.ProjectDTO;
import persistence.dto.RewardDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@RequiredArgsConstructor
public class RewardService {
    private final DataSource dataSource;
    private final RewardDAO rewardDAO;

    public RewardDTO findById(Long id) {
        try (Connection conn = dataSource.getConnection()) {
            return rewardDAO.findById(conn, id);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<RewardDTO> findAll(Long projectId) {
        // 풀에서 연결 빌리고 try 끝나면 반환
        try (Connection conn = dataSource.getConnection()) {
            return rewardDAO.findAll(conn, projectId);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
