package persistence.dao;

import persistence.dto.ProjectApprovalDTO;

import java.sql.*;

public class ProjectApprovalDAO {

    public Long insert(Connection conn, ProjectApprovalDTO approval) throws SQLException {
        String sql = "INSERT INTO project_approval(project_id, project_status, reject_reason, eval_at, submission_at) "
                + "VALUES(?, ?, ?, ?, COALESCE(?, NOW()))";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, approval.getProjectId());
            pstmt.setInt(2, approval.getProjectStatus());
            pstmt.setString(3, approval.getRejectReason());   // 승인이면 null
            pstmt.setObject(4, approval.getEvalAt());
            pstmt.setObject(5, approval.getSubmissionAt());

            pstmt.executeUpdate();
            return ProjectDAO.generatedKey(pstmt);
        }
    }
}
// 가장 최근 신청/심사 기록 조회
// 승인 처리 시에는 해당 행을 잠금
public ProjectApprovalDTO findLatestByProject(
        Connection conn,
        Long projectId,
        boolean forUpdate) throws SQLException {

    String sql = "SELECT * FROM project_approval WHERE project_id = ? "
            + "ORDER BY approval_id DESC LIMIT 1"
            + (forUpdate ? " FOR UPDATE" : "");

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, projectId);

        try (ResultSet rs = pstmt.executeQuery()) {

            if (!rs.next()) {
                return null;
            }

            ProjectApprovalDTO approval = new ProjectApprovalDTO();

            approval.setApprovalId(rs.getLong("approval_id"));
            approval.setProjectId(rs.getLong("project_id"));
            approval.setProjectStatus(rs.getInt("project_status"));
            approval.setRejectReason(rs.getString("reject_reason"));

            approval.setEvalAt(
                    rs.getObject("eval_at", java.time.LocalDateTime.class));

            approval.setSubmissionAt(
                    rs.getObject("submission_at", java.time.LocalDateTime.class));

            return approval;
        }
    }
}
