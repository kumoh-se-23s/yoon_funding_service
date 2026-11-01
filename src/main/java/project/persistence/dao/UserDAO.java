package project.persistence.dao;

import project.persistence.dto.UserDTO;

import java.sql.*;
import java.time.LocalDateTime;

// DAO는 더 이상 커넥션을 직접 얻지 않음
// → Service가 커넥션을 얻어서 넘겨주고, commit/rollback/close도 Service가 담당
// → 그래서 여러 DAO 메서드를 하나의 트랜잭션으로 묶을 수 있음
public class UserDAO {

    // 회원 등록 : user_id는 넣지 않음 → DB가 AUTO_INCREMENT로 겹치지 않는 번호를 만들어 줌
    // 만들어진 user_id를 돌려주므로, 호출한 쪽은 그 값을 받아서 프로젝트/후원의 FK로 사용
    public Long insert(Connection conn, UserDTO user) throws SQLException {
        String sql = "INSERT INTO users(login_id, password, role, name, nickname, email, address, detail_address) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getLoginId());
            pstmt.setString(2, user.getPassword());
            pstmt.setInt(3, user.getRole());
            pstmt.setString(4, user.getName());
            pstmt.setString(5, user.getNickname());
            pstmt.setString(6, user.getEmail());
            pstmt.setString(7, user.getAddress());
            pstmt.setString(8, user.getDetailAddress());

            pstmt.executeUpdate();
            return ProjectDAO.generatedKey(pstmt);
        }
    }

    // PK로 회원 한 명 조회
    public UserDTO findById(Connection conn, Long userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    // 로그인 ID로 회원 한 명 조회 (로그인할 때 사용)
    public UserDTO findByLoginId(Connection conn, String loginId) throws SQLException {
        String sql = "SELECT * FROM users WHERE login_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, loginId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    // (MyPage) 개인정보 수정
    // login_id, name, role, created_at은 수정 대상이 아니므로 SET에서 제외
    public int updateProfile(Connection conn, UserDTO user) throws SQLException {
        String sql = "UPDATE users "
                + "SET password = ?, nickname = ?, email = ?, address = ?, detail_address = ? "
                + "WHERE user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getPassword());
            pstmt.setString(2, user.getNickname());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getAddress());
            pstmt.setString(5, user.getDetailAddress());
            pstmt.setLong(6, user.getUserId());

            return pstmt.executeUpdate(); // 수정된 행 수 (1이면 성공)
        }
    }

    // ResultSet의 현재 행 → UserDTO
    private UserDTO toDTO(ResultSet rs) throws SQLException {
        UserDTO user = new UserDTO();
        user.setUserId(rs.getLong("user_id"));
        user.setLoginId(rs.getString("login_id"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getInt("role"));
        user.setName(rs.getString("name"));
        user.setNickname(rs.getString("nickname"));
        user.setEmail(rs.getString("email"));
        user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        user.setAddress(rs.getString("address"));
        user.setDetailAddress(rs.getString("detail_address"));
        return user;
    }
}
