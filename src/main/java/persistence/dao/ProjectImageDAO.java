package persistence.dao;

import persistence.dto.ProjectImageDTO;

import java.sql.*;

public class ProjectImageDAO {

    public Long insert(Connection conn, ProjectImageDTO image) throws SQLException {
        // created_at이 null이면 COALESCE로 현재 시각을 넣음 (DB 기본값과 같은 효과)
        String sql = "INSERT INTO project_image(project_id, img_blob, is_thumbnail, sort_order, created_at) "
                + "VALUES(?, ?, ?, ?, COALESCE(?, NOW()))";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, image.getProjectId());
            pstmt.setBytes(2, image.getImgBlob());
            pstmt.setBoolean(3, image.isThumbnail());
            pstmt.setInt(4, image.getSortOrder());
            pstmt.setObject(5, image.getCreatedAt());

            pstmt.executeUpdate();
            return ProjectDAO.generatedKey(pstmt);
        }
    }
}
