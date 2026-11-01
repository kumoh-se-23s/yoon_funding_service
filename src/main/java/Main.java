import persistence.PooledDataSource;
import persistence.dao.*;
import persistence.dto.UserDTO;
import service.InitDBService;
import service.UserService;
import view.UserView;

import javax.sql.DataSource;

import service.ProjectService;
import view.ProjectView;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    // ---------------- 객체 준비 ----------------
    // 싱글톤 커넥션 풀 : 프로그램 전체에서 이 하나만 사용
    private static final DataSource dataSource = PooledDataSource.getDataSource();

    // DAO는 커넥션을 직접 얻지 않으므로 DataSource가 필요 없음 (Service가 conn을 넘겨줌)
    private static final UserDAO userDAO = new UserDAO();
    private static final ProjectDAO projectDAO = new ProjectDAO();
    private static final ProjectImageDAO projectImageDAO = new ProjectImageDAO();
    private static final RewardDAO rewardDAO = new RewardDAO();
    private static final ProjectApprovalDAO projectApprovalDAO = new ProjectApprovalDAO();
    private static final FundingSupportDAO fundingSupportDAO = new FundingSupportDAO();
    private static final ReviewDAO reviewDAO = new ReviewDAO();

    // Service는 DataSource를 받아 커넥션을 얻고 트랜잭션을 관리
    private static final InitDBService initDBService = new InitDBService(
            dataSource, new InitDAO(), userDAO, projectDAO, projectImageDAO,
            rewardDAO, projectApprovalDAO, fundingSupportDAO, reviewDAO);
    private static final UserService userService = new UserService(dataSource, userDAO);

    private static final UserView userView = new UserView();

    // 현재 로그인한 회원 (로그인 세션 역할). 로그아웃 상태면 null
    private static UserDTO loginUser = null;

    // 프로젝트 조회 서비스, 화면 객체
    private static final ProjectService projectService = new ProjectService(dataSource, projectDAO);
    private static final ProjectView projectView = new ProjectView();

    public static void main(String[] args) {
        initDB();                       //  검사에 필요한 사전 데이터를 insert하는 메소드 (실행 후 주석 처리)
        // 검사할 항목 하나만 주석 해제하여 실행

        // test01ViewFundingStatus();      // MAKER     - 프로젝트별 펀딩 현황 보기
        // test02RegisterProject();        // MAKER     - 프로젝트 기본 정보 등록
        // test03RegisterReward();         // MAKER     - 리워드 등록 및 프로젝트 신청
        // test04ApproveProject();         // ADMIN     - 프로젝트 등록 승인
         test05UpdateProfile();          // SUPPORTER - 개인정보 수정
        // test06ViewProjects();           // SUPPORTER - 프로젝트 보기
        //test07FilterProjects();         // SUPPORTER - 프로젝트 필터링
        // test08ApplyReward();            // SUPPORTER - 리워드 신청
        // test09RegisterReview();         // SUPPORTER - 리뷰 및 별점 등록
        // test10ConcurrencyControl();     // SUPPORTER - 제공 가능 수량 동시성 제어
    }


    private static void initDB() {
        initDBService.initDB();
    }

    private static boolean login(String loginId, String password) {
        loginUser = userService.login(loginId, password);
        userView.printLogin(loginUser);
        return loginUser != null;
    }

    private static void logout() {
        loginUser = null;
    }

    private static boolean loginFromConsole(int maxTry) {
        for (int i = 1; i <= maxTry; i++) {
            System.out.println("=== 로그인 (" + i + "/" + maxTry + ") ===");
            String[] input = userView.inputLogin();
            if (login(input[0], input[1])) {
                return true;
            }
        }
        System.out.println("로그인에 " + maxTry + "번 실패하여 종료합니다.");
        return false;
    }

    private static void test05UpdateProfile() {
        // 1) 로그인
        if (!loginFromConsole(3)) {
            return;
        }

        // 2) 현재 정보 보여주기
        UserDTO current = userService.findById(loginUser.getUserId());
        userView.printUser("현재 정보", current);

        // 3) 바꿀 정보 입력 (Enter = 기존 값 유지)
        UserDTO form = userView.inputProfileForm(current);
        String newPassword = userView.inputNewPassword();

        boolean nothingChanged = form.getNickname() == null && form.getEmail() == null
                && form.getAddress() == null && form.getDetailAddress() == null && newPassword == null;
        if (nothingChanged) {
            System.out.println("변경할 내용이 없어 수정하지 않았습니다.");
            logout();
            return;
        }

        // 4) 수정 실행 → 수정 대상은 loginUser로만 결정됨 (다른 사람 정보는 못 바꿈)
        boolean success = userService.updateProfile(loginUser, form, newPassword);
        if (success) {
            System.out.println("개인정보 수정 성공");
            userView.printUser("수정 후", userService.findById(loginUser.getUserId()));
            if (newPassword != null) {
                System.out.println("비밀번호가 변경되었습니다. 다음 로그인부터 새 비밀번호를 사용하세요.");
            }
        } else {
            // 실패 사유(중복 닉네임 등)는 Service에서 rollback 후 출력됨
            userView.printUser("현재 정보 (변경 없음)", userService.findById(loginUser.getUserId()));
        }

        logout();
    }

    private static void test06ViewProjects() {
        try {
            projectView.printProjects(
                    projectService.findAll(),
                    projectService.findSoldOutProjectIds()
            );
        } catch (SQLException e) {
            System.out.println("프로젝트 조회 실패: " + e.getMessage());
        }
    }

    private static void test07FilterProjects() {
        Scanner scanner = new Scanner(System.in);

        int[] filters = projectView.inputFilters(scanner);
        if (filters == null) {
            return;
        }

        try {
            projectView.printProjects(
                    projectService.findByFilter(filters[0], filters[1]),
                    projectService.findSoldOutProjectIds()
            );
        } catch (SQLException e) {
            System.out.println("프로젝트 필터 조회 실패: " + e.getMessage());
        }
    }
}
