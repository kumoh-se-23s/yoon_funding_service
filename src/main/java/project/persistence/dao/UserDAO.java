package project.persistence.dao;

import project.persistence.dto.UserDTO;

import java.sql.*;
import java.time.LocalDateTime;

// DAO는 더 이상 커넥션을 직접 얻지 않음
// → Service가 커넥션을 얻어서 넘겨주고, commit/rollback/close도 Service가 담당
// → 그래서 여러 DAO 메서드를 하나의 트랜잭션으로 묶을 수 있음
public class UserDAO {



    // PK로 회원 한 명 조회
    public UserDTO findById(Connection conn, Long userId) {
        return new UserDTO
    }

    // 로그인 ID로 회원 한 명 조회 (로그인할 때 사용)
    public UserDTO findByLoginId(Connection conn, String loginId)  {

    }


    // ResultSet의 현재 행 → UserDTO
    private UserDTO toDTO(ResultSet rs)  {

    }
}