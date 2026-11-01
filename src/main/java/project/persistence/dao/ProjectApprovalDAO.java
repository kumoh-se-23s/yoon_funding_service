package project.persistence.dao;

import project.persistence.dto.ProjectApprovalDTO;

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
