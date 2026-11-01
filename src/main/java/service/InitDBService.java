package service;

import persistence.dao.*;
import persistence.dto.*;
import util.PasswordUtil;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

// 검사에 필요한 사전(더미) 데이터를 한 번에 넣는 서비스
// - 실행할 때마다 모든 테이블을 비우고 다시 넣기 때문에 여러 번 실행해도 결과가 항상 같음
// - 모든 insert를 "하나의 커넥션, 하나의 트랜잭션"으로 묶음
//   → 중간에 하나라도 실패하면 rollback 되어 반쪽짜리 데이터가 남지 않음
public class InitDBService {
    private final DataSource dataSource;
    private final InitDAO initDAO;
    private final UserDAO userDAO;
    private final ProjectDAO projectDAO;
    private final ProjectImageDAO projectImageDAO;
    private final RewardDAO rewardDAO;
    private final ProjectApprovalDAO projectApprovalDAO;
    private final FundingSupportDAO fundingSupportDAO;
    private final ReviewDAO reviewDAO;

    public InitDBService(DataSource dataSource, InitDAO initDAO, UserDAO userDAO, ProjectDAO projectDAO,
                         ProjectImageDAO projectImageDAO, RewardDAO rewardDAO, ProjectApprovalDAO projectApprovalDAO,
                         FundingSupportDAO fundingSupportDAO, ReviewDAO reviewDAO) {
        this.dataSource = dataSource;
        this.initDAO = initDAO;
        this.userDAO = userDAO;
        this.projectDAO = projectDAO;
        this.projectImageDAO = projectImageDAO;
        this.rewardDAO = rewardDAO;
        this.projectApprovalDAO = projectApprovalDAO;
        this.fundingSupportDAO = fundingSupportDAO;
        this.reviewDAO = reviewDAO;
    }

    // 프로젝트 상태 코드
    private static final int APPROVED = 1;
    private static final int REJECTED = 2;

    // 사용자 역할 코드
    private static final int ROLE_ADMIN = 0;
    private static final int ROLE_SUPPORTER = 1;
    private static final int ROLE_MAKER = 2;

    // 메이커는 숫자 PK 대신 login_id로 가리킴 (PK는 DB가 AUTO_INCREMENT로 만들기 때문)
    private static final String MAKER_FUNDING_FAIRY = "user14"; // 펀딩요정(신유진)
    private static final String MAKER_MINJUN = "user01";        // 달리는민준(김민준)
    private static final String MAKER_TAEHYUN = "user15";       // 밤하늘별(권태현)

    // insert 후 DB가 만들어 준 user_id 보관 : login_id → user_id
    // login_id는 UNIQUE라 키가 겹치지 않음
    private final Map<String, Long> userIds = new HashMap<>();

    public void initDB() {
        try (Connection conn = dataSource.getConnection()) {
            initDAO.clearAllTables(conn);
            userIds.clear();   // 이전 실행에서 받아 둔 user_id는 버림

            try {
                conn.setAutoCommit(false);   // 여기부터 하나의 트랜잭션

                insertUsers(conn);
                insertFundingFairyProjectA(conn); // 승인 거절
                insertFundingFairyProjectB(conn); // 승인 + 펀딩 실패
                insertMinjunProjectA(conn);       // 승인 + 펀딩 성공 + 배송 지남 + 리뷰
                insertMinjunProjectB(conn);       // 1차 반려 → 재신청 승인 + 현재 진행 중
                insertTaehyunTreatment(conn);     // 승인 + 현재 진행 중
                insertTaehyunDronix(conn);        // 승인 + 펀딩 성공 + 배송 지남 + 리뷰

                conn.commit();
                System.out.println("initDB 완료 : 사전 데이터 입력이 끝났습니다.");
            } catch (Exception e) {
                conn.rollback();             // 하나라도 실패하면 전부 취소
                throw new RuntimeException("initDB 실패 - 모든 insert를 rollback 했습니다.", e);
            } finally {
                conn.setAutoCommit(true);    // 풀에 돌려주기 전에 원래 상태로
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // 1. 회원 (role → 0:관리자, 1:서포터, 2:메이커)
    private void insertUsers(Connection conn) throws SQLException {
        // 비밀번호는 insertUser()에서 bcrypt로 해시해서 저장 (여기 적힌 건 로그인할 때 입력할 평문)
        // 관리자
        insertUser(conn, "admin01", "Admin1234!", ROLE_ADMIN, "최관리", "관리자_최", "admin01@example.com", "서울특별시 강남구 테헤란로 311", "12층 1203호");
        insertUser(conn, "admin02", "Admin1234!", ROLE_ADMIN, "박운영", "운영자_박", "admin02@example.com", "경기도 성남시 분당구 판교역로 235", "5층 502호");

        // user_id는 적지 않음 → 넣는 순서대로 DB가 1, 2, 3 ... 을 붙여 줌
        // 달리는민준은 프로젝트를 개설한 메이커 → 2
        insertUser(conn, "user01", "User1234!", ROLE_MAKER, "김민준", "달리는민준", "user01@example.com", "서울특별시 마포구 월드컵로 240", "1302동 1702호");

        // 서포터
        insertUser(conn, "user02", "User1234!", ROLE_SUPPORTER, "이서연", "책읽는서연", "user02@example.com", "서울특별시 송파구 올림픽로 435", "113동 1204호");
        insertUser(conn, "user03", "User1234!", ROLE_SUPPORTER, "박지호", "후원러지호", "user03@example.com", "경기도 수원시 영통구 광교중앙로 145", "4층 403호");
        insertUser(conn, "user04", "User1234!", ROLE_SUPPORTER, "최수아", "커피좋아", "user04@example.com", "부산광역시 해운대구 해운대로 570", "2층 202호");
        insertUser(conn, "user05", "User1234!", ROLE_SUPPORTER, "정도윤", "코딩하는곰", "user05@example.com", "대구광역시 수성구 달구벌대로 2420", "205동 1503호");
        insertUser(conn, "user06", "User1234!", ROLE_SUPPORTER, "강하은", "별헤는밤", "user06@example.com", "인천광역시 연수구 센트럴로 160", "3층 301호");
        insertUser(conn, "user07", "User1234!", ROLE_SUPPORTER, "조현우", "여행가자", "user07@example.com", "대전광역시 유성구 대학로 99", "102동 802호");
        insertUser(conn, "user08", "User1234!", ROLE_SUPPORTER, "윤지우", "고양이집사", "user08@example.com", "광주광역시 서구 상무중앙로 110", "7층 706호");
        insertUser(conn, "user09", "User1234!", ROLE_SUPPORTER, "장예린", "작은행복", "user09@example.com", "경기도 고양시 일산동구 중앙로 1275", "1001동 2305호");
        insertUser(conn, "user10", "User1234!", ROLE_SUPPORTER, "임준서", "맛집탐험", "user10@example.com", "서울특별시 노원구 동일로 1414", "6층 601호");
        insertUser(conn, "user11", "User1234!", ROLE_SUPPORTER, "한서윤", "푸른바다", "user11@example.com", "울산광역시 남구 삼산로 282", "B동 1105호");
        insertUser(conn, "user12", "User1234!", ROLE_SUPPORTER, "오지안", "새벽산책", "user12@example.com", "경기도 용인시 수지구 포은대로 536", "301동 1601호");
        insertUser(conn, "user13", "User1234!", ROLE_SUPPORTER, "서민재", "소소한하루", "user13@example.com", "충청북도 청주시 상당구 상당로 82", "2층 201호");

        // 메이커
        insertUser(conn, "user14", "User1234!", ROLE_MAKER, "신유진", "펀딩요정", "user14@example.com", "서울특별시 관악구 관악로 145", "4층 408호");
        insertUser(conn, "user15", "User1234!", ROLE_MAKER, "권태현", "밤하늘별", "user15@example.com", "경기도 부천시 길주로 210", "108동 903호");
    }

    // 2. "펀딩요정"의 프로젝트A : 승인 거절 (후원/리뷰 없음)
    private void insertFundingFairyProjectA(Connection conn) throws SQLException {
        Long projectId = projectDAO.insert(conn, ProjectDTO.builder()
                .userId(uid(MAKER_FUNDING_FAIRY))
                .title("포그미 휴대용 미니 가습기")
                .subtitle(null)
                .introduction("건조한 사무실과 침실에서 간편하게 사용할 수 있는 충전식 휴대용 미니 가습기입니다.")
                .itemInfo(null)
                .goalAmount(1_500_000L)
                .category(0)
                .projectStatus(REJECTED)
                .fundingStartAt(null)
                .fundingEndAt(null)
                .paymentDueAt(null)
                .deliveryStartAt(null)
                .build());

        insertImage(conn, projectId, "A_main.jpg", true, 1, null);
        insertImage(conn, projectId, "A_detail.jpg", false, 2, "2026-08-18 13:21:00");

        insertReward(conn, projectId, "기본 리워드", "휴대용 미니 가습기 1개", "A_main.jpg", 30, 25_000L);

        insertApproval(conn, projectId, REJECTED, "상품 상세 정보와 펀딩 일정이 충분하지 않습니다.",
                "2026-08-20 14:00:00", "2026-08-21 11:30:00");
    }


    // 3. "펀딩요정"의 프로젝트B : 승인 + 펀딩 실패 (리뷰 없음)
    private void insertFundingFairyProjectB(Connection conn) throws SQLException {
        Long projectId = projectDAO.insert(conn, ProjectDTO.builder()
                .userId(uid(MAKER_FUNDING_FAIRY))
                .title("[먼작귀 콜라보] 키링크 포켓 미니 키보드")
                .subtitle("가방에 쏙 들어가는 68키 블루투스 키보드")
                .introduction("노트북과 태블릿을 자주 사용하는 사용자를 위해 휴대성을 높인 초소형 블루투스 키보드입니다. 작은 크기에서도 문서 작성에 필요한 주요 키 배열을 유지했습니다.")
                .itemInfo("68키 배열, 블루투스 연결, USB-C 충전 방식의 휴대용 키보드입니다.")
                .goalAmount(1_000_000L)
                .category(0)
                .projectStatus(APPROVED)
                .fundingStartAt(dt("2026-09-01 00:00:00"))
                .fundingEndAt(dt("2026-09-30 23:59:59"))
                .paymentDueAt(dt("2026-10-03 12:00:00"))
                .deliveryStartAt(dt("2026-10-15 00:00:00"))
                .build());

        insertImage(conn, projectId, "B_main.jpg", true, 1, null);
        insertImage(conn, projectId, "B_detail.jpg", false, 2, null);

        // insert가 돌려준 reward_id를 변수에 담아두고 후원 데이터에서 사용
        Long rewardB1 = insertReward(conn, projectId, "얼리버드 키보드", "포켓 미니 키보드 1개", "B_main.jpg", 30, 70_000L);
        Long rewardB2 = insertReward(conn, projectId, "키보드 먼작귀 파우치 세트", "포켓 미니 키보드 1개 + 전용 먼작귀 파우치", "B_reward_set.jpg", 20, 100_000L);

        insertApproval(conn, projectId, APPROVED, null, "2026-08-25 14:00:00", "2026-08-27 11:00:00");

        insertSupport(conn, rewardB1, uid("user03"), "2026-09-05 15:20:00", "박지호", "010-3141-5926", "경기도 수원시 영통구 광교중앙로 145", "4층 403호", false);
        insertSupport(conn, rewardB1, uid("user04"), "2026-09-12 09:40:00", "최수아", "010-2718-2818", "부산광역시 해운대구 해운대로 570", "2층 202호", false);
        insertSupport(conn, rewardB2, uid("user05"), "2026-09-20 18:10:00", "정도윤", "010-1618-0339", "대구광역시 수성구 달구벌대로 2420", "205동 1503호", false);
        insertSupport(conn, rewardB2, uid("user06"), "2026-09-22 20:15:00", "강하은", "010-2236-0679", "인천광역시 연수구 센트럴로 160", "3층 301호", true); // 취소된 후원
    }


    // 4. "달리는민준"의 프로젝트A : 승인 → 펀딩 성공 → 배송 날짜 지남 → 리뷰 존재
    private void insertMinjunProjectA(Connection conn) throws SQLException {
        Long projectId = projectDAO.insert(conn, ProjectDTO.builder()
                .userId(uid(MAKER_MINJUN))
                .title("집중을 켜는 타이머 무드램프, FOCUS LIGHT")
                .subtitle("공부와 휴식 시간을 빛으로 알려주는 데스크 무드램프")
                .introduction("집중 시간과 휴식 시간을 빛의 변화로 알려주는 데스크용 타이머 무드램프입니다.")
                .itemInfo("25분 집중·5분 휴식 모드, 밝기 조절, USB-C 전원을 지원합니다.")
                .goalAmount(100_000L)
                .category(1)
                .projectStatus(APPROVED)
                .fundingStartAt(dt("2026-08-01 00:00:00"))
                .fundingEndAt(dt("2026-08-31 23:59:59"))
                .paymentDueAt(dt("2026-09-02 10:00:00"))
                .deliveryStartAt(dt("2026-09-20 09:00:00"))
                .build());

        insertImage(conn, projectId, "C_main.jpg", true, 1, null);
        insertImage(conn, projectId, "C_detail.jpg", false, 2, null);

        Long rewardA1 = insertReward(conn, projectId, "얼리버드 FOCUS LIGHT", "FOCUS LIGHT 1개 + USB-C 케이블", "C_main.jpg", 5, 30_000L);
        Long rewardA2 = insertReward(conn, projectId, "FOCUS LIGHT 기본 세트", "FOCUS LIGHT 1개 + USB-C 케이블 + 파우치", "C_reward_set.jpg", 20, 40_000L);

        insertApproval(conn, projectId, APPROVED, null, "2026-07-27 10:00:00", "2026-07-28 14:00:00");

        // insert가 돌려준 support_id를 받아서 리뷰에 사용
        Long supportA1 = insertSupport(conn, rewardA1, uid("user02"), "2026-08-05 13:20:00", "이서연", "010-1111-1111", "서울특별시 송파구 올림픽로 435", "113동 1204호", false);
        Long supportA2 = insertSupport(conn, rewardA2, uid("user03"), "2026-08-11 15:10:00", "박지호", "010-2222-2222", "경기도 수원시 영통구 광교중앙로 145", "4층 403호", false);
        insertSupport(conn, rewardA2, uid("user04"), "2026-08-20 19:30:00", "최수아", "010-3333-3333", "부산광역시 해운대구 해운대로 570", "2층 202호", false);

        insertReview(conn, supportA1, "책상 위에 두고 쓰기 좋고 집중 시간을 확인하기 편했습니다.", "4.5", "2026-09-28 18:30:00");
        insertReview(conn, supportA2, "조작이 간단하고 무드등으로도 사용할 수 있어서 만족합니다.", "5.0", "2026-10-03 21:10:00");
    }


    // 5. "달리는민준"의 프로젝트B : 1차 반려 → 재신청 → 승인 → 현재 진행 중 (11/3 마감)
    private void insertMinjunProjectB(Connection conn) throws SQLException {
        Long projectId = projectDAO.insert(conn, ProjectDTO.builder()
                .userId(uid(MAKER_MINJUN))
                .title("냥냥마을을 지켜라! 전략 카드게임")
                .subtitle("귀여운 고양이들과 만드는 2~4인용 전략 카드게임")
                .introduction("고양이 주민을 모으고 건물을 건설해 가장 높은 점수를 만드는 전략 카드게임입니다.")
                .itemInfo("고양이 카드 80장, 건물 카드 30장, 토큰, 점수판, 설명서로 구성됩니다.")
                .goalAmount(120_000L)
                .category(2)
                .projectStatus(APPROVED) // 최종 상태 = 승인
                .fundingStartAt(dt("2026-09-01 00:00:00"))
                .fundingEndAt(dt("2026-11-03 23:59:59"))
                .paymentDueAt(dt("2026-11-05 10:00:00"))
                .deliveryStartAt(dt("2026-11-20 09:00:00"))
                .build());

        insertImage(conn, projectId, "D_main.jpg", true, 1, null);
        insertImage(conn, projectId, "D_detail.jpg", false, 2, null);

        Long rewardB1 = insertReward(conn, projectId, "냥냥마을 기본 세트", "카드게임 본품 1세트", "D_main.jpg", 20, 30_000L);
        Long rewardB2 = insertReward(conn, projectId, "냥냥마을 컬렉터 세트", "카드게임 본품 + 아크릴 키링 3종 + 특별 카드 5장", "D_reward_collector.jpg", 5, 50_000L);

        // 심사 이력 2건 : 1차 반려 → 수정 후 2차 신청 승인
        insertApproval(conn, projectId, REJECTED, "리워드 구성 및 상품 설명이 부족합니다.", "2026-08-20 10:00:00", "2026-08-21 13:00:00");
        insertApproval(conn, projectId, APPROVED, null, "2026-08-23 11:00:00", "2026-08-24 16:00:00");

        insertSupport(conn, rewardB1, uid("user05"), "2026-09-05 12:00:00", "정도윤", "010-4444-4444", "대구광역시 수성구 달구벌대로 2420", "205동 1503호", false);
        insertSupport(conn, rewardB1, uid("user06"), "2026-09-10 17:20:00", "강하은", "010-5555-5555", "인천광역시 연수구 센트럴로 160", "3층 301호", false);
        insertSupport(conn, rewardB1, uid("user07"), "2026-09-15 09:45:00", "조현우", "010-6666-6666", "대전광역시 유성구 대학로 99", "102동 802호", false);
        insertSupport(conn, rewardB2, uid("user08"), "2026-09-22 20:15:00", "윤지우", "010-7777-7777", "광주광역시 서구 상무중앙로 110", "7층 706호", false);
    }


    // 6. "밤하늘별"의 워터트리트먼트 : 승인 → 현재 진행 중 (11/3 마감)
    private void insertTaehyunTreatment(Connection conn) throws SQLException {
        Long projectId = projectDAO.insert(conn, ProjectDTO.builder()
                .userId(uid(MAKER_TAEHYUN))
                .title("빛나는 머릿결되기까지 5초! 워터트리트먼트")
                .subtitle("머릿결집사를 위한 헤어 케어")
                .introduction("자연유래 성분을 활용한 헤어 케어 프로젝트입니다. 젖은 모발에 사용하는 워터트리트먼트로 부드럽고 윤기 있는 머릿결 관리를 돕습니다.")
                .itemInfo("워터트리트먼트 트리트먼트 민트휩샴푸 유자휩샴푸")
                .goalAmount(1_000_000L)
                .category(1)
                .projectStatus(APPROVED)
                .fundingStartAt(dt("2026-10-01 00:00:00"))
                .fundingEndAt(dt("2026-11-03 23:59:59"))
                .paymentDueAt(dt("2026-11-05 10:00:00"))
                .deliveryStartAt(dt("2026-11-20 09:00:00"))
                .build());

        insertImage(conn, projectId, "E_main.jpg", true, 1, null);
        insertImage(conn, projectId, "E_detail.jpg", false, 2, null);

        Long rewardE1 = insertReward(conn, projectId, "싱글세트", "워터트리트먼트 1개 + 민트휩 샴푸 1개", "E_main.jpg", 100, 38_100L);
        Long rewardE2 = insertReward(conn, projectId, "트리트먼트만 2개", "트리트먼트 2개", "E_reward_pair.jpg", 50, 21_900L);
        Long rewardE3 = insertReward(conn, projectId, "트리플세트", "민트휩삼푸 3개+워터트리트먼트 3개", "E_reward_triple.jpg", 50, 21_900L);

        // 추가: 승인된 프로젝트이므로 심사 이력 1건
        insertApproval(conn, projectId, APPROVED, null, "2026-09-22 10:00:00", "2026-09-24 15:00:00");

        // 추가: 진행 중 후원 (서포터 user12, user13, user02)
        insertSupport(conn, rewardE1, uid("user12"), "2026-10-05 11:30:00", "오지안", "010-8888-8888", "경기도 용인시 수지구 포은대로 536", "301동 1601호", false);
        insertSupport(conn, rewardE2, uid("user13"), "2026-10-12 14:20:00", "서민재", "010-9999-9999", "충청북도 청주시 상당구 상당로 82", "2층 201호", false);
        insertSupport(conn, rewardE3, uid("user02"), "2026-10-20 21:05:00", "이서연", "010-1111-1111", "서울특별시 송파구 올림픽로 435", "113동 1204호", false);
    }


    // 7. "밤하늘별"의 드로닉스 : 승인 → 펀딩 성공 → 배송 날짜 지남 → 리뷰 존재
    private void insertTaehyunDronix(Connection conn) throws SQLException {
        Long projectId = projectDAO.insert(conn, ProjectDTO.builder()
                .userId(uid(MAKER_TAEHYUN))
                .title("이제 드론을 조립하자! 드로닉스")
                .subtitle("이제 드론을 직접 만들고 날려보자!")
                .introduction("드론을 직접 조립하고 비행해 보는 프로젝트 드로닉스입니다. 우리에게 성큼 다가선 드론! 이제 다양한 드론을 직접 제작하고 그 원리를 알아보세요! 디지털 제품 카테고리에 등록되어 있습니다.")
                .itemInfo("드로닉스키트 드론3D베이스출력물 스마트스킨")
                .goalAmount(200_000L)                        // 변경: 1,000,000 → 200,000 (아래 후원으로 펀딩 성공이 되도록)
                .category(1)
                .projectStatus(APPROVED)
                .fundingStartAt(dt("2026-09-01 00:00:00"))
                .fundingEndAt(dt("2026-09-28 23:59:59"))
                .paymentDueAt(dt("2026-09-30 10:00:00"))
                .deliveryStartAt(dt("2026-10-10 09:00:00"))
                .build());

        insertImage(conn, projectId, "F_main.jpg", true, 1, null);
        insertImage(conn, projectId, "F_detail.jpg", false, 2, null);

        Long rewardF1 = insertReward(conn, projectId, "드로닉스 단일 상품", "드로닉스키트 1개+워크북 1개", "F_main.jpg", 30, 50_000L);
        Long rewardF2 = insertReward(conn, projectId, "드로닉스 확장 구성", "드로닉스키트 1개+워크북 1개+스마트스킨 1개", "F_reward_set.jpg", 20, 80_000L);

        // 추가: 승인된 프로젝트이므로 심사 이력 1건
        insertApproval(conn, projectId, APPROVED, null, "2026-08-18 10:00:00", "2026-08-19 14:00:00");

        // 추가: 리뷰를 달 후원 데이터 (서포터 user09, user10, user11) → 합계 210,000원
        Long supportF1 = insertSupport(conn, rewardF1, uid("user09"), "2026-09-03 10:15:00", "장예린", "010-1212-1212", "경기도 고양시 일산동구 중앙로 1275", "1001동 2305호", false);
        Long supportF2 = insertSupport(conn, rewardF2, uid("user10"), "2026-09-10 16:40:00", "임준서", "010-3434-3434", "서울특별시 노원구 동일로 1414", "6층 601호", false);
        insertSupport(conn, rewardF2, uid("user11"), "2026-09-21 08:50:00", "한서윤", "010-5656-5656", "울산광역시 남구 삼산로 282", "B동 1105호", false);

        insertReview(conn, supportF1, "직접 드론을 조립해 볼 수 있다는 점이 흥미롭고 구성도 만족스러웠습니다.", "4.5", "2026-10-15 20:00:00");
        insertReview(conn, supportF2, "조립 과정이 생각보다 어렵고 설명이 조금 더 자세했으면 좋겠습니다.", "2.0", "2026-10-18 21:30:00");
    }



    // 반복되는 insert를 짧게 쓰기 위한 도우미 메서드들 (모두 같은 conn = 같은 트랜잭션)
    // user_id는 AI → DB가 만든 user_id를 받아서 userIds에 보관
    private void insertUser(Connection conn, String loginId, String password, int role, String name,
                            String nickname, String email, String address, String detailAddress) throws SQLException {
        Long userId = userDAO.insert(conn, UserDTO.builder()
                .loginId(loginId)
                .password(PasswordUtil.hash(password)) // 평문 → bcrypt 해시
                .role(role)
                .name(name)
                .nickname(nickname)
                .email(email)
                .address(address)
                .detailAddress(detailAddress)
                .build());
        userIds.put(loginId, userId);
    }

    // login_id로 DB가 만들어 준 user_id 찾기 (아직 insert 안 된 회원이면 바로 오류 → rollback)
    private Long uid(String loginId) {
        Long userId = userIds.get(loginId);
        if (userId == null) {
            throw new IllegalStateException("먼저 등록되지 않은 회원입니다 : " + loginId);
        }
        return userId;
    }

    private void insertImage(Connection conn, Long projectId, String fileName, boolean thumbnail, int sortOrder,
                             String createdAt) throws SQLException {
        projectImageDAO.insert(conn, ProjectImageDTO.builder()
                .projectId(projectId)
                .imgBlob(img(fileName))
                .thumbnail(thumbnail)
                .sortOrder(sortOrder)
                .createdAt(dt(createdAt))
                .build());
    }

    private Long insertReward(Connection conn, Long projectId, String title, String content, String fileName,
                              int stockQuantity, long price) throws SQLException {
        return rewardDAO.insert(conn, RewardDTO.builder()
                .projectId(projectId)
                .title(title)
                .content(content)
                .imgBlob(img(fileName))
                .stockQuantity(stockQuantity)
                .price(price)
                .build());
    }

    private void insertApproval(Connection conn, Long projectId, int status, String rejectReason,
                                String submissionAt, String evalAt) throws SQLException {
        projectApprovalDAO.insert(conn, ProjectApprovalDTO.builder()
                .projectId(projectId)
                .projectStatus(status)
                .rejectReason(rejectReason)
                .submissionAt(dt(submissionAt))
                .evalAt(dt(evalAt))
                .build());
    }

    private Long insertSupport(Connection conn, Long rewardId, Long userId, String supportedAt, String recipientName,
                               String recipientContact, String deliveryAddress, String deliveryDetailAddress,
                               boolean cancelStatus) throws SQLException {
        return fundingSupportDAO.insert(conn, FundingSupportDTO.builder()
                .rewardId(rewardId)
                .userId(userId)
                .supportedAt(dt(supportedAt))
                .recipientName(recipientName)
                .recipientContact(recipientContact)
                .deliveryAddress(deliveryAddress)
                .deliveryDetailAddress(deliveryDetailAddress)
                .cancelStatus(cancelStatus)
                .build());
    }

    private void insertReview(Connection conn, Long supportId, String content, String score,
                              String createdAt) throws SQLException {
        reviewDAO.insert(conn, ReviewDTO.builder()
                .supportId(supportId)
                .content(content)
                .score(new BigDecimal(score))
                .createdAt(dt(createdAt))
                .build());
    }

    // "2026-09-01 00:00:00" 형태의 문자열 → LocalDateTime (null이면 null)
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static LocalDateTime dt(String text) {
        return text == null ? null : LocalDateTime.parse(text, FORMAT);
    }

    // 이미지 BLOB 만들기 : src/main/resources/images/ 안의 파일을 클래스패스 기준으로 읽음
    // (seed_images.sql에 들어 있던 사진들을 파일로 꺼내 둔 것)
    private static byte[] img(String fileName) {
        try (InputStream in = InitDBService.class.getClassLoader().getResourceAsStream("images/" + fileName)) {
            if (in == null) {
                // 파일이 없으면 파일 이름을 바이트로 넣음 (img_blob이 NOT NULL이라 빈 값은 안 됨 ㅠ..아귀찬아)
                return fileName.getBytes(StandardCharsets.UTF_8);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
