import persistence.PooledDataSource;
import persistence.dao.*;
import persistence.dto.UserDTO;
import service.InitDBService;
import service.UserService;
import view.UserView;

import persistence.dto.ProjectDTO;
import persistence.dto.RewardDTO;

import service.ProjectRegistrationService;
import service.ProjectApprovalService;

import java.time.LocalDateTime;
import java.util.List;
import java.io.InputStream;
import java.io.IOException;


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
// DB 검사 2, 3번: 프로젝트 등록 및 승인 신청
private static final ProjectRegistrationService projectRegistrationService =
        new ProjectRegistrationService(
                dataSource,
                projectDAO,
                rewardDAO,
                projectApprovalDAO
        );

// DB 검사 4번: 관리자 프로젝트 승인
private static final ProjectApprovalService projectApprovalService =
        new ProjectApprovalService(
                dataSource,
                projectDAO,
                rewardDAO,
                projectApprovalDAO
        );
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
// DB 검사 2번: 메이커 프로젝트 등록
private static void test02RegisterProject() {

    if (!loginFromConsole(3)) {
        return;
    }

    try {
        Long projectId = projectRegistrationService.registerProject(
                loginUser,
                demoProject("[검사2] 메이커 프로젝트 기본 정보 등록")
        );

        System.out.println("[검사 2 성공]");
        System.out.println("등록된 project_id = " + projectId);

        System.out.println("DB 확인 SQL:");
        System.out.println(
                "SELECT * FROM project WHERE project_id = "
                        + projectId + ";"
        );

    } catch (RuntimeException e) {
        System.out.println("[검사 2 실패] " + e.getMessage());

    } finally {
        logout();
    }
}
    // DB 검사 3번: 리워드 등록 및 프로젝트 승인 신청
private static void test03RegisterReward() {

    if (!loginFromConsole(3)) {
        return;
    }

    try {
        // 테스트 전용 프로젝트 생성
        Long projectId = projectRegistrationService.registerProject(
                loginUser,
                demoProject("[검사3] 리워드 등록 및 승인 신청")
        );

        // 리워드 등록 및 승인 신청
        List<Long> rewardIds =
                projectRegistrationService.registerRewardsAndApply(
                        loginUser,
                        projectId,
                        demoRewards()
                );

        System.out.println("[검사 3 성공]");
        System.out.println("project_id = " + projectId);
        System.out.println("reward_id = " + rewardIds);
        System.out.println("신청 상태 = 0(심사대기)");

        System.out.println("DB 확인 SQL:");

        System.out.println(
                "SELECT * FROM reward WHERE project_id = "
                        + projectId + ";"
        );

        System.out.println(
                "SELECT * FROM project_approval WHERE project_id = "
                        + projectId + ";"
        );

    } catch (RuntimeException e) {
        System.out.println("[검사 3 실패] " + e.getMessage());

    } finally {
        logout();
    }
}
    // DB 검사 4번: 관리자 프로젝트 승인
private static void test04ApproveProject() {

    Long projectId;

    // 1. 승인 대기 프로젝트 준비
    try {
        UserDTO fixtureMaker = userService.login(
                "user15",
                "User1234!"
        );

        if (fixtureMaker == null) {
            System.out.println(
                    "[검사 4 준비 실패] 테스트용 메이커 계정이 없습니다."
            );
            return;
        }

        // 2. 프로젝트 생성
        projectId = projectRegistrationService.registerProject(
                fixtureMaker,
                demoProject("[검사4] 관리자 승인 대기 프로젝트")
        );

        // 3. 리워드 등록 및 승인 신청
        projectRegistrationService.registerRewardsAndApply(
                fixtureMaker,
                projectId,
                demoRewards()
        );

        System.out.println("[검사 4 준비 완료]");
        System.out.println("project_id = " + projectId);
        System.out.println("프로젝트 상태 = 0(심사대기)");
        System.out.println("관리자 계정으로 로그인하세요.");

    } catch (RuntimeException e) {
        System.out.println("[검사 4 준비 실패] " + e.getMessage());
        return;
    }

    // 4. 관리자 로그인
    if (!loginFromConsole(3)) {
        return;
    }

    try {
        // 5. 프로젝트 승인
        projectApprovalService.approveProject(
                loginUser,
                projectId
        );

        System.out.println("[검사 4 성공]");
        System.out.println("project_id = " + projectId);
        System.out.println("프로젝트 상태 = 1(승인)");

        System.out.println("DB 확인 SQL:");

        System.out.println(
                "SELECT project_id, project_status " +
                "FROM project WHERE project_id = " +
                projectId + ";"
        );

        System.out.println(
                "SELECT approval_id, project_id, " +
                "project_status, eval_at " +
                "FROM project_approval " +
                "WHERE project_id = " + projectId +
                " ORDER BY approval_id DESC;"
        );

    } catch (RuntimeException e) {
        System.out.println("[검사 4 실패] " + e.getMessage());

    } finally {
        logout();
    }
}
    // 테스트용 프로젝트 생성
private static ProjectDTO demoProject(String title) {

    LocalDateTime start =
            LocalDateTime.now().plusDays(2).withNano(0);

    LocalDateTime end = start.plusDays(30);

    return ProjectDTO.builder()
            .title(title)
            .subtitle("데이터베이스 검사 시연용")
            .introduction("메이커가 등록한 프로젝트입니다.")
            .itemInfo("검사용 기본 제품 정보")
            .goalAmount(300_000L)
            .category(1)
            .fundingStartAt(start)
            .fundingEndAt(end)
            .paymentDueAt(end.plusDays(2))
            .deliveryStartAt(end.plusDays(14))
            .build();
}
    // 테스트용 리워드 2개 생성
private static List<RewardDTO> demoRewards() {

    return List.of(

            RewardDTO.builder()
                    .title("[검사용] 기본 리워드")
                    .content("기본 상품 1개")
                    .imgBlob(loadDemoImage("D_main.jpg"))
                    .stockQuantity(10)
                    .price(30_000L)
                    .build(),

            RewardDTO.builder()
                    .title("[검사용] 프리미엄 리워드")
                    .content("기본 상품 + 추가 구성품")
                    .imgBlob(loadDemoImage("D_reward_collector.jpg"))
                    .stockQuantity(5)
                    .price(50_000L)
                    .build()
    );
}
    // 테스트 이미지 파일을 byte[]로 읽기
private static byte[] loadDemoImage(String fileName) {

    try (InputStream is =
                 Main.class.getClassLoader()
                         .getResourceAsStream("images/" + fileName)) {

        if (is == null) {
            throw new IllegalStateException(
                    "테스트 이미지 누락: " + fileName
            );
        }

        return is.readAllBytes();

    } catch (IOException e) {

        throw new IllegalStateException(
                "이미지 읽기 실패: " + fileName, e
        );
    }
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
