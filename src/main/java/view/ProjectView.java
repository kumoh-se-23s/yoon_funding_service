package view;

import persistence.dto.ProjectDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import persistence.dto.RewardFundingStatusDTO;
import persistence.dto.ProjectSupporterDTO;

import domain.ApprovalStatus;
import domain.ProgressStatus;

// 프로젝트 조회 결과 콘솔에 출력
public class ProjectView {
    public void printProjects(List<ProjectDTO> projects, List<Long> soldOutProjectIds) {
        System.out.println("===== 프로젝트 목록 =====");

        if (projects.isEmpty()) {
            System.out.println("조회할 프로젝트가 없습니다.");
            return;
        }

        System.out.println("프로젝트 수: " + projects.size());

        for (ProjectDTO project : projects) {
            System.out.println("----------------------------------");
            System.out.println("프로젝트 번호: " + project.getProjectId());
            System.out.println("제목: " + project.getTitle());

            if (project.getSubtitle() != null
                    && !project.getSubtitle().isBlank()) {
                System.out.println("부제목: " + project.getSubtitle());
            }

            System.out.printf("목표 금액: %,d원%n", project.getGoalAmount());
            boolean soldOut = soldOutProjectIds.contains(project.getProjectId());
            System.out.println("심사 상태: "
                    + ApprovalStatus.fromDbValue(
                    project.getApprovalStatus()).getDisplayName());
            System.out.println("진행 상태: " + getFundingStatus(project, soldOut));
            System.out.println("펀딩 시작일: " + formatDate(project.getFundingStartAt()));
            System.out.println("펀딩 종료일: " + formatDate(project.getFundingEndAt()));
        }
    }

    // 날짜, 시각 출력 포맷팅
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "미정" : dateTime.format(DATE_FORMAT);
    }

    // 현재 시각과 시작시점 종료시점 비교해 진행 상태 판단
    // 마감의 종류는 두 가지. 1) 종료일 지남 2) 종료일 안 지났지만 리워드 안 남음
    // 펀딩 일정과 리워드 전체 품절 여부로 진행 상태 판단
    private String getFundingStatus(ProjectDTO project, boolean soldOut) {
        ProgressStatus status =
                ProgressStatus.fromDbValue(project.getProgressStatus());

        String displayName = status.getDisplayName();

        if (status == ProgressStatus.ONGOING && soldOut) {
            return displayName + " (리워드 품절)";
        }

        return displayName;
    }

    public int[] inputFilters(Scanner scanner){
        try{
            System.out.println("1: 테크·가전, 2: 패션·잡화, 3: 뷰티, 4: 푸드");
            System.out.println("5: 홈·리빙, 6: 취미·문구, 7: 반려동물, 8: 문화·레저");
            System.out.print("카테고리 번호 입력 (-1: 전체): ");
            // 우리가 정한 카테고리 넘버 입력하는 방식임.

            if(!scanner.hasNextLine()){
                return null;
            }

            int category = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("진행 상태 (0: 전체, 1: 진행 중, 2: 마감): ");
            if (!scanner.hasNextLine()){
                return null;
            }
            int fundingStatus = Integer.parseInt(scanner.nextLine().trim());

            if ((category != -1 && (category < 1 || category > 8))
                    || fundingStatus < 0 || fundingStatus > 2) {
                System.out.println("선택 범위를 다시 확인해주세요.");
                return null;
            }

            return new int[]{category, fundingStatus};
        } catch(NumberFormatException e){
            System.out.println("번호는 정수로 입력해주세요.");
            return null;
        }
    }

    // 메이커 본인의 프로젝트 목록 출력
    public void printMakerProjects(List<ProjectDTO> projects) {
        System.out.println("=== 내 프로젝트 목록 ===");

        if (projects.isEmpty()) {
            System.out.println("등록한 프로젝트가 없습니다.");
            return;
        }

        for (ProjectDTO project : projects) {
            System.out.printf("%d | %s%n",
                    project.getProjectId(), project.getTitle());
        }
    }

    // 선택한 프로젝트의 후원 현황 출력
    public void printFundingStatus(
            ProjectDTO project,
            List<RewardFundingStatusDTO> rewards,
            List<ProjectSupporterDTO> supporters) {

        long totalFundedAmount = 0;
        boolean soldOut = !rewards.isEmpty();

        for (RewardFundingStatusDTO reward : rewards) {
            totalFundedAmount += reward.getFundedAmount();

            if (reward.getRemainingQuantity() > 0) {
                soldOut = false;
            }
        }

        System.out.println();
        System.out.println("=== 프로젝트별 펀딩 현황 ===");
        System.out.println("프로젝트 번호: " + project.getProjectId());
        System.out.println("제목: " + project.getTitle());
        System.out.printf("목표 금액: %,d원%n", project.getGoalAmount());
        System.out.printf("총 후원 금액: %,d원%n", totalFundedAmount);

        if (project.getGoalAmount() > 0) {
            double achievementRate =
                    totalFundedAmount * 100.0 / project.getGoalAmount();
            System.out.printf("목표 달성률: %.1f%%%n", achievementRate);
        }

        System.out.println("심사 상태: "
                + ApprovalStatus.fromDbValue(
                project.getApprovalStatus()).getDisplayName());
        System.out.println("진행 상태: "
                + getFundingStatus(project, soldOut));
        System.out.println("펀딩 시작일: " + formatDate(project.getFundingStartAt()));
        System.out.println("펀딩 종료일: " + formatDate(project.getFundingEndAt()));

        System.out.println();
        System.out.println("=== 리워드별 후원 현황 ===");

        if (rewards.isEmpty()) {
            System.out.println("등록한 리워드가 없습니다.");
        }

        for (RewardFundingStatusDTO reward : rewards) {
            System.out.println("----------------------------------");
            System.out.println("리워드 번호: " + reward.getRewardId());
            System.out.println("리워드 이름: " + reward.getTitle());
            System.out.printf("가격: %,d원%n", reward.getPrice());
            System.out.printf("최초 제공 수량: %,d개%n", reward.getTotalCount());
            System.out.printf("신청 수량: %,d개%n", reward.getAppliedQuantity());
            System.out.printf("남은 수량: %,d개%n", reward.getRemainingQuantity());
            System.out.printf("후원 금액: %,d원%n", reward.getFundedAmount());
        }

        System.out.println();
        System.out.println("=== 후원 내역 및 배송 정보 ===");
        System.out.println("취소되지 않은 후원 건수: " + supporters.size());

        if (supporters.isEmpty()) {
            System.out.println("후원 내역이 없습니다.");
        }

        for (ProjectSupporterDTO supporter : supporters) {
            System.out.println("----------------------------------");
            System.out.println("후원 번호: " + supporter.getSupportId());
            System.out.println("후원자: " + supporter.getSupporterName()
                    + " (" + supporter.getSupporterNickname() + ")");
            System.out.println("회원 번호: " + supporter.getUserId());
            System.out.println("신청 리워드: " + supporter.getRewardTitle());
            System.out.println("후원 일시: " + formatDate(supporter.getProcessedAt()));
            System.out.println("수령인: " + supporter.getRecipientName());
            System.out.println("수령인 연락처: " + supporter.getRecipientContact());
            System.out.println("배송 주소: " + supporter.getDeliveryAddress());

            if (supporter.getDeliveryDetailAddress() != null
                    && !supporter.getDeliveryDetailAddress().isBlank()) {
                System.out.println("상세 주소: "
                        + supporter.getDeliveryDetailAddress());
            }
        }
    }


}