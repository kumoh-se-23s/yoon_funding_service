package service;

import persistence.dao.UserDAO;
import persistence.dto.UserDTO;
import util.PasswordUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

// 커넥션은 Service에서 얻고, 트랜잭션(commit/rollback)도 Service에서 관리 → DAO에는 conn만 넘기도록 하자!!!!!!!!!!!!!!!!!!
public class UserService {
    private static final int DUPLICATE_KEY = 1062; // MySQL ER_DUP_ENTRY

    private final DataSource dataSource;
    private final UserDAO userDAO;

    public UserService(DataSource dataSource, UserDAO userDAO) { // 의존성 주입
        this.dataSource = dataSource;
        this.userDAO = userDAO;
    }

    // 로그인 : 아이디로 회원을 찾고, 입력한 비밀번호가 저장된 bcrypt 해시와 맞는지 확인
    // 성공하면 로그인한 회원 정보, 실패하면 null
    public UserDTO login(String loginId, String rawPassword) {
        try (Connection conn = dataSource.getConnection()) {   // 조회만 하므로 트랜잭션 불필요 굿굿
            UserDTO user = userDAO.findByLoginId(conn, loginId);

            // 아이디가 없거나 비밀번호가 틀림 → 어느 쪽이 틀렸는지는 알려주지 않는 게 보안상 원칙
            if (user == null || !PasswordUtil.matches(rawPassword, user.getPassword())) {
                return null;
            }

            user.setPassword(null); // 로그인 정보에는 비밀번호 해시를 들고 다니지 않음
            return user;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UserDTO findById(Long userId) {
        try (Connection conn = dataSource.getConnection()) {
            return userDAO.findById(conn, userId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // (MyPage) 개인정보 수정 : 로그인한 회원 "본인"의 정보만 수정
    // loginUser   : 로그인한 회원 (어떤 회원을 수정할지는 오직 여기서 결정)
    // form        : MyPage에서 입력한 값. null인 항목은 기존 값 유지
    // newPassword : 새 비밀번호(평문). 바꾸지 않으면 null
    public boolean updateProfile(UserDTO loginUser, UserDTO form, String newPassword) {
        if (loginUser == null) {
            System.out.println("로그인이 필요합니다.");
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);   // 조회 + 수정을 하나의 트랜잭션으로

                // 1) DB에 있는 현재 정보 조회
                UserDTO current = userDAO.findById(conn, loginUser.getUserId());
                if (current == null) {
                    conn.rollback();
                    System.out.println("존재하지 않는 회원입니다. user_id = " + loginUser.getUserId());
                    return false;
                }

                // 2) 입력한 항목만 덮어쓰기 (입력 안 한 항목은 기존 값 그대로)
                if (form.getNickname() != null) current.setNickname(form.getNickname());
                if (form.getEmail() != null) current.setEmail(form.getEmail());
                if (form.getAddress() != null) current.setAddress(form.getAddress());
                if (form.getDetailAddress() != null) current.setDetailAddress(form.getDetailAddress());

                // current의 password는 이미 해시된 값 → 새 비밀번호가 있을 때만 해시해서 교체
                if (newPassword != null && !newPassword.isBlank()) {
                    current.setPassword(PasswordUtil.hash(newPassword));
                }

                // 3) 수정
                int result = userDAO.updateProfile(conn, current);

                conn.commit();
                return result == 1;
            } catch (SQLException e) {
                conn.rollback();   // 먼저 rollback → 이 트랜잭션에서 바뀐 건 전부 취소

                // MySQL 에러 코드 1062 = 중복 키(UNIQUE 제약조건 위반)
                // 어떤 UNIQUE 키가 겹쳤는지는 에러 메시지에 들어 있는 키 이름으로 구분
                if (e.getErrorCode() == DUPLICATE_KEY && e.getMessage().contains("uk_users_nickname")) {
                    System.out.println("중복된 닉네임입니다. 업데이트 불가");
                } else if (e.getErrorCode() == DUPLICATE_KEY && e.getMessage().contains("uk_users_email")) {
                    System.out.println("중복된 이메일입니다. 업데이트 불가");
                } else {
                    System.out.println("개인정보 수정 실패 - rollback : " + e.getMessage());
                }
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
