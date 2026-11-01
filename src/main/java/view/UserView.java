package view;

import persistence.dto.UserDTO;

import java.util.Scanner;

// 콘솔 입출력 담당 (입력 받기 + 결과 출력)
public class UserView {
    private static final String[] ROLE_NAMES = {"관리자", "서포터", "메이커"};

    // System.in을 감싼 Scanner는 프로그램 전체에서 하나만 쓰고 close 하지 않음
    // (close 하면 System.in까지 닫혀서 이후 입력을 못 받음)
    private final Scanner scanner = new Scanner(System.in);

    // 한 줄 입력 (앞뒤 공백 제거)
    public String input(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // 로그인 정보 입력 → {아이디, 비밀번호}
    public String[] inputLogin() {
        String loginId = input("아이디   : ");
        String password = input("비밀번호 : ");
        return new String[]{loginId, password};
    }

    // MyPage 수정 폼 입력
    // [ ] 안은 현재 값. 그냥 Enter를 치면 null → Service에서 기존 값 유지
    public UserDTO inputProfileForm(UserDTO current) {
        System.out.println("=== 개인정보 수정 (바꾸지 않을 항목은 그냥 Enter) ===");
        UserDTO form = new UserDTO();
        form.setNickname(emptyToNull(input("닉네임   [" + current.getNickname() + "] : ")));
        form.setEmail(emptyToNull(inputEmail("이메일   [" + current.getEmail() + "] : ")));
        form.setAddress(emptyToNull(input("주소     [" + current.getAddress() + "] : ")));
        form.setDetailAddress(emptyToNull(input("상세주소 [" + current.getDetailAddress() + "] : ")));
        return form;
    }

    // 새 비밀번호 입력 (Enter면 변경 안 함 → null). 확인 입력이 다르면 다시 입력
    public String inputNewPassword() {
        while (true) {
            String newPassword = input("새 비밀번호 (변경 안 하면 Enter) : ");
            if (newPassword.isEmpty()) {
                return null;
            }
            String confirm = input("새 비밀번호 확인 : ");
            if (newPassword.equals(confirm)) {
                return newPassword;
            }
            System.out.println("비밀번호가 서로 다릅니다. 다시 입력해 주세요.");
        }
    }

    // 이메일은 형식이 틀리면 다시 입력 (Enter는 그대로 통과 = 변경 안 함)
    private String inputEmail(String prompt) {
        while (true) {
            String email = input(prompt);
            if (email.isEmpty() || email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
                return email;
            }
            System.out.println("이메일 형식이 올바르지 않습니다. (예: abc@example.com)");
        }
    }

    private String emptyToNull(String s) {
        return s.isEmpty() ? null : s;
    }

    public void printLogin(UserDTO loginUser) {
        if (loginUser == null) {
            System.out.println("로그인 실패 : 아이디 또는 비밀번호가 올바르지 않습니다.");
            return;
        }
        System.out.println("로그인 성공 : " + loginUser.getNickname()
                + "(" + loginUser.getLoginId() + ", " + ROLE_NAMES[loginUser.getRole()] + ")");
        System.out.println("-------------------------------------");
    }

    public void printUser(String label, UserDTO user) {
        System.out.println("[" + label + "]");
        if (user == null) {
            System.out.println("회원 정보가 없습니다.");
            return;
        }
        System.out.println("user_id        : " + user.getUserId());
        System.out.println("login_id       : " + user.getLoginId());
        System.out.println("nickname       : " + user.getNickname());
        System.out.println("email          : " + user.getEmail());
        System.out.println("address        : " + user.getAddress());
        System.out.println("detail_address : " + user.getDetailAddress());
        System.out.println("-------------------------------------");
    }
}
