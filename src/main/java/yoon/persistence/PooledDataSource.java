package yoon.persistence;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// 커넥션 풀을 싱글톤으로 제공하고 관리하는 클래스
// - static 블록은 클래스가 처음 사용될 때 딱 한 번 실행 → 커넥션 풀(BasicDataSource)도 프로그램 전체에서 하나만 만들어짐
// - 생성자를 private으로 막아서 new PooledDataSource()로 또 만들 수 없게 함
// - 어디서든 PooledDataSource.getDataSource()로 같은 커넥션 풀을 꺼내 씀
public class PooledDataSource {
    private static final BasicDataSource basicDS = new BasicDataSource();

    static {
        // FileInputStream("src/main/resources/...")는 OS 파일 경로 기준이라 jar로 배포하면 못 찾음
        // → 클래스패스 기준으로 찾는 getResourceAsStream 사용
        try (InputStream inputStream = PooledDataSource.class.getClassLoader()
                .getResourceAsStream("config/db.properties")) {

            if (inputStream == null) {
                throw new IllegalStateException("config/db.properties 파일을 찾을 수 없습니다.");
            }

            Properties properties = new Properties();
            properties.load(inputStream);
            basicDS.setDriverClassName(properties.getProperty("DRIVER_CLASS")); //loads the jdbc driver
            basicDS.setUrl(properties.getProperty("DB_CONNECTION_URL"));
            basicDS.setUsername(properties.getProperty("DB_USER"));
            basicDS.setPassword(properties.getProperty("DB_PWD"));

            // Parameters for connection pooling
            // maxTotal >= initialSize, maxIdle >= minIdle, maxTotal = maxIdle 이 되도록 모두 같은 값으로 통일
            basicDS.setInitialSize(10);
            basicDS.setMaxTotal(10);
            basicDS.setMaxIdle(10);
            basicDS.setMinIdle(10);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private PooledDataSource() {
        // 외부에서 객체 생성 금지 (싱글톤)
    }

    public static DataSource getDataSource() {
        return basicDS;
    }
}
