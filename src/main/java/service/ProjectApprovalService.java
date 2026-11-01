package service;

import persistence.dao.ProjectApprovalDAO;
import persistence.dao.ProjectDAO;
import persistence.dao.RewardDAO;
import persistence.dto.ProjectApprovalDTO;
import persistence.dto.ProjectDTO;
import persistence.dto.UserDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** 중간 DB 검사 4번 담당: 관리자만 프로젝트를 승인할 수 있다. */
public class ProjectApprovalService {
    private static final int ADMIN = 0;
    private static final int PENDING = 0;
    private static final int APPROVED = 1;

    private final DataSource dataSource;
    private final ProjectDAO projectDAO;
    private final RewardDAO rewardDAO;
    private final ProjectApprovalDAO approvalDAO;

    public ProjectApprovalService(DataSource dataSource, ProjectDAO projectDAO,
                                  RewardDAO rewardDAO, ProjectApprovalDAO approvalDAO) {
        this.dataSource = dataSource;
        this.projectDAO = projectDAO;
        this.rewardDAO = rewardDAO;
        this.approvalDAO = approvalDAO;
    }

    /**
     * 프로젝트 상태 UPDATE + 해당 최신 심사 이력 UPDATE를 하나의 트랜잭션으로 실행한다.
     * 승인 대상이 대기 상태가 아니거나 리워드가 없으면 수정하지 않는다.
     */
    public void approveProject(UserDTO loginUser, Long projectId) {
        if (loginUser == null || loginUser.getUserId() == null
                || !Integer.valueOf(ADMIN).equals(loginUser.getRole())) {
            throw new SecurityException("관리자로 로그인해야 프로젝트를 승인할 수 있습니다.");
        }
        if (projectId == null) throw new IllegalArgumentException("프로젝트 ID가 필요합니다.");

        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);
                ProjectDTO project = projectDAO.findByIdForUpdate(conn, projectId);
                if (project == null || project.getProjectStatus() != PENDING) {
                    throw new IllegalStateException("승인 대기 상태의 프로젝트가 아닙니다: " + projectId);
                }
                if (rewardDAO.countByProjectId(conn, projectId) == 0) {
                    throw new IllegalStateException("리워드 등록을 마치지 않은 프로젝트는 승인할 수 없습니다.");
                }
                ProjectApprovalDTO latest = approvalDAO.findLatestByProject(conn, projectId, true);
                if (latest == null || latest.getProjectStatus() != PENDING) {
                    throw new IllegalStateException("승인 대기 중인 심사 신청이 없습니다.");
                }

                if (approvalDAO.approvePending(conn, latest.getApprovalId()) != 1
                        || projectDAO.changeStatus(conn, projectId, PENDING, APPROVED) != 1) {
                    throw new IllegalStateException("승인 상태 갱신 실패");
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new IllegalStateException("프로젝트 승인 실패 → 전체 rollback: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("프로젝트 승인 DB 연결 실패", e);
        }
    }
}
