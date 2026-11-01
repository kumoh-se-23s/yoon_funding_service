package util;

import org.mindrot.jbcrypt.BCrypt;

// 비밀번호 해시 도우미
// bcrypt는 매번 랜덤 salt를 섞어서, 같은 비밀번호라도 해시값이 매번 다르게 나옴
// → DB에는 "$2a$10$..." 형태의 60자 문자열이 저장됨 (users.password VARCHAR(255)에 충분히 들어감)
public class PasswordUtil {

    // 평문 비밀번호 → 해시값 (회원 등록, 비밀번호 변경 시 사용)
    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    // 입력한 평문 비밀번호가 DB에 저장된 해시값과 맞는지 확인 (로그인 시 사용)
    // 해시는 되돌릴 수 없으므로, "같은 방식으로 해시해서 비교"하는 이 메서드로만 확인 가능
    public static boolean matches(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
