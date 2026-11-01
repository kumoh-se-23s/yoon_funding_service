package service;

import persistence.dao.ProjectApprovalDAO;
import persistence.dao.ProjectDAO;
import persistence.dao.RewardDAO;
import persistence.dto.ProjectApprovalDTO;
import persistence.dto.ProjectDTO;
import persistence.dto.RewardDTO;
import persistence.dto.UserDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 중간 DB 검사 2, 3번 담당.
 * 로그인 정보/소유권/상태를 Service에서 검증하고 DAO는 SQL만 실행한다.
 * 여러 개의 리워드와 승인 신청은 반드시 같은 커넥션/트랜잭션으로 처리한다.
 */
public class ProjectRegistrationService {
    private static final int MAKER = 2;
    private static final int PENDING = 0;
    private static final int APPROVED = 1;
    private static final int REJECTED = 2;

    private final DataSource dataSource;
    private final ProjectDAO projectDAO;
    private final RewardDAO rewardDAO;
    private final ProjectApprovalDAO approvalDAO;

    public ProjectRegistrationService(DataSource dataSource, ProjectDAO projectDAO,
                                      RewardDAO rewardDAO, ProjectApprovalDAO approvalDAO) {
        this.dataSource = dataSource;
        this.projectDAO = projectDAO;
        this.rewardDAO = rewardDAO;
        this.approvalDAO = approvalDAO;
    }

    /** 2번: 메이커의 프로젝트 기본 정보 INSERT. 등록 직후에는 심사 요청 전 상태(0). */
    public Long registerProject(UserDTO loginUser, ProjectDTO form) {
        requireMaker(loginUser);
        validateProject(form);

        // 클라이언트가 user_id / 상태를 임의 지정하더라도 현재 로그인한 메이커로 덮어쓴다.
        form.setUserId(loginUser.getUserId());
        form.setProjectStatus(PENDING);

        try (Connection conn = dataSource.getConnection()) {
            return projectDAO.insert(conn, form);
        } catch (SQLException e) {
            throw new IllegalStateException("프로젝트 등록 INSERT 실패", e);
        }
    }

    /**
     * 3번: 리워드 N개 INSERT 후 프로젝트 승인 신청 INSERT.
     * 모든 쿼리가 성공해야 commit, 하나라도 실패하면 rollback.
     * 기존에 반려된 프로젝트(2)라면 재신청할 수도 있다.
     *
     * @return 새로 저장한 reward_id 목록
     */
    public List<Long> registerRewardsAndApply(UserDTO loginUser, Long projectId, List<RewardDTO> rewards) {
        requireMaker(loginUser);
        if (projectId == null || rewards == null || rewards.isEmpty()) {
            throw new IllegalArgumentException("프로젝트 ID와 최소 1개의 리워드가 필요합니다.");
        }
        for (RewardDTO reward : rewards) {
            if (reward == null || reward.getTitle() == null || reward.getTitle().isBlank()
                    || reward.getPrice() == null || reward.getPrice() <= 0
                    || reward.getStockQuantity() == null || reward.getStockQuantity() < 0) {
                throw new IllegalArgumentException("리워드명, 0보다 큰 금액, 0 이상의 재고를 입력하세요.");
            }
        }

        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);
                ProjectDTO project = projectDAO.findByIdForUpdate(conn, projectId);
                if (project == null) {
                    throw new IllegalArgumentException("존재하지 않는 프로젝트입니다: " + projectId);
                }
                if (!loginUser.getUserId().equals(project.getUserId())) {
                    throw new SecurityException("다른 메이커의 프로젝트에는 리워드를 등록할 수 없습니다.");
                }
                int status = project.getProjectStatus();
                if (status == APPROVED || (status != PENDING && status != REJECTED)) {
                    throw new IllegalStateException("이미 승인되었거나 재신청할 수 없는 프로젝트입니다.");
                }

                // 프로젝트 등록(test02)은 project_status=0만 INSERT한다.
                // 실제 승인 신청 여부는 project_approval의 최신 행을 통해 판단한다.
                ProjectApprovalDTO latest = approvalDAO.findLatestByProject(conn, projectId, false);
                if (latest != null && latest.getProjectStatus() == PENDING) {
                    throw new IllegalStateException("이미 심사 대기 중인 프로젝트입니다.");
                }

                List<Long> rewardIds = new ArrayList<>();
                for (RewardDTO reward : rewards) {
                    // 전달받은 project_id가 다른 값이어도 검증된 프로젝트 ID를 사용
                    reward.setProjectId(projectId);
                    rewardIds.add(rewardDAO.insert(conn, reward));
                }

                if (status == REJECTED && projectDAO.changeStatus(conn, projectId, REJECTED, PENDING) != 1) {
                    throw new IllegalStateException("프로젝트 재신청 상태 변경에 실패했습니다.");
                }

                // 0=심사 대기, 평가 날짜는 아직 없으며 신청 시각은 DB NOW()
                approvalDAO.insert(conn, ProjectApprovalDTO.builder()
                        .projectId(projectId)
                        .projectStatus(PENDING)
                        .rejectReason(null)
                        .evalAt(null)
                        .submissionAt(null)
                        .build());

                conn.commit();
                return rewardIds;
            } catch (Exception e) {
                conn.rollback();
                throw new IllegalStateException("리워드 등록/승인 신청 실패 → 전체 rollback: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true); // 커넥션 풀에 반납하기 전에 기본 상태 복구
            }
        } catch (SQLException e) {
            throw new IllegalStateException("리워드 등록 DB 연결 실패", e);
        }
    }

    private static void requireMaker(UserDTO user) {
        if (user == null || user.getUserId() == null || !Integer.valueOf(MAKER).equals(user.getRole())) {
            throw new SecurityException("메이커 로그인 후 이용할 수 있습니다.");
        }
    }

    private static void validateProject(ProjectDTO project) {
        if (project == null || project.getTitle() == null || project.getTitle().isBlank()
                || project.getIntroduction() == null || project.getIntroduction().isBlank()
                || project.getGoalAmount() == null || project.getGoalAmount() <= 0
                || project.getCategory() == null || project.getCategory() < 0
                || project.getFundingStartAt() == null || project.getFundingEndAt() == null
                || !project.getFundingEndAt().isAfter(project.getFundingStartAt())) {
            throw new IllegalArgumentException("제목/소개/목표금액/카테고리/유효한 펀딩 기간이 필요합니다.");
        }
    }
}
