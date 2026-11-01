package project.persistence.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

// initDB 전용 DAO : 모든 테이블을 비워서 initDB를 몇 번이고 다시 실행할 수 있게 해줌
// DAO는 커넥션을 직접 얻거나 닫지 않음 → Service가 넘겨준 Connection으로만 작업 (트랜잭션은 Service가 관리)
public class InitDAO {

    // FOREIGN_KEY_CHECKS를 잠깐 끄고 TRUNCATE 하면 FK 순서 상관없이 비울 수 있음
    // TRUNCATE는 AUTO_INCREMENT도 1로 되돌려서, 다시 넣으면 PK가 항상 1, 2, 3 ... 으로 들어감
    // ※ TRUNCATE는 DDL이라 실행 즉시 자동 커밋됨(rollback 불가) → Service에서 트랜잭션 시작 "전"에 호출
    private static final String[] TABLES = {
            "review", "funding_support", "project_approval", "reward",
            "interest", "project_image", "project", "users"
    };

    public void clearAllTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String table : TABLES) {
                    stmt.executeUpdate("TRUNCATE TABLE " + table);
                }
            } finally {
                // 커넥션 풀에 돌려줄 커넥션이니까 반드시 원래대로 켜두기
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }
}
