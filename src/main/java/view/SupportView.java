package view;

import domain.FundingSupport;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import persistence.dao.FundingSupportDAO;
import persistence.dao.ProjectDAO;
import persistence.dao.RewardDAO;
import persistence.dao.UserDAO;
import persistence.dto.ProjectDTO;
import persistence.dto.RewardDTO;
import persistence.dto.SupportRequestDTO;
import persistence.dto.UserDTO;
import service.FundingSupportService;
import service.ProjectService;
import service.RewardService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;
import exception.SupportRejectedException;

@RequiredArgsConstructor
public class SupportView {
    private final DataSource ds;
    @NonNull
    private UserDTO loginUser;
    @NonNull
    private Scanner sc;

    @NonNull
    private ProjectView projectView;
    @NonNull
    private ProjectService projectService;

    @NonNull
    private RewardView rewardView;
    @NonNull
    private RewardService rewardService;

    @NonNull
    private FundingSupportService fss;

    private ProjectDTO supportProject = null;
    private RewardDTO supportReward = null;

    public void supportView() {
        while (true) {
            try {
                SupportRequestDTO request = input();

                if (request == null) {
                    System.out.println("후원 신청을 종료했습니다.");
                    return;
                }

                fss.support(request);
                System.out.println("후원 신청이 완료되었습니다.");
                return;

            } catch (SupportRejectedException e) {
                System.out.println("[안내] " + e.getMessage());

            } catch (java.util.InputMismatchException e) {
                sc.nextLine();
                System.out.println("[안내] 번호는 정수로 입력해주세요.");
            }
        }
    }

    public SupportRequestDTO input() {
        if (this.supportProject == null) {
            this.supportProject = projectInput();

            if (this.supportProject == null) {
                return null;
            }
        } else {
            this.supportProject =
                    projectService.findById(this.supportProject.getProjectId());
        }

        if (this.supportProject == null
                || !"approved".equals(this.supportProject.getApprovalStatus())
                || !"ongoing".equals(this.supportProject.getProgressStatus())) {
            this.supportProject = null;
            throw new SupportRejectedException(
                    "현재 후원할 수 없는 프로젝트입니다. 다른 프로젝트를 선택해주세요.");
        }

        this.supportReward = rewardInput(supportProject.getProjectId());

        if (this.supportReward == null) {
            return null;
        }

        int rewardCount = inputRewardCount();

        if (rewardCount == 0) {
            return null;
        }

        SupportRequestDTO request = SupportRequestDTO.builder()
                .userId(loginUser.getUserId())
                .projectId(supportProject.getProjectId())
                .rewardId(supportReward.getRewardId())
                .rewardCount(rewardCount)
                .build();

        String choice;

        while (true) {
            System.out.print("수령인 정보를 따로 입력하시겠습니까? (y/n): ");
            choice = sc.nextLine().trim();

            if (choice.equalsIgnoreCase("y")
                    || choice.equalsIgnoreCase("n")) {
                break;
            }

            System.out.println("y 또는 n을 입력해주세요.");
        }

        if (choice.equalsIgnoreCase("y")) {
            System.out.print("수령인 이름: ");
            request.setRecipientName(sc.nextLine().trim());

            System.out.print("전화번호: ");
            request.setRecipientContact(sc.nextLine().trim());

            System.out.print("배송 주소: ");
            request.setDeliveryAddress(sc.nextLine().trim());

            System.out.print("상세 주소: ");
            request.setDeliveryDetailAddress(sc.nextLine().trim());
        }

        return request;
    }

    private ProjectDTO projectInput() {
        viewProjects();

        System.out.print("프로젝트 번호(0: 신청 종료): ");
        Long projectId = sc.nextLong();
        sc.nextLine();

        if (projectId == 0L) {
            return null;
        }

        ProjectDTO project = projectService.findById(projectId);

        if (project == null) {
            throw new SupportRejectedException(
                    "목록에 있는 프로젝트 번호를 선택해주세요.");
        }

        return project;
    }

    private void viewProjects() {
        try {
            projectView.printProjects(
                    projectService.findAll(),
                    projectService.findSoldOutProjectIds()
            );
        } catch (SQLException e) {
            System.out.println("프로젝트 조회 실패: " + e.getMessage());
        }
    }

    private RewardDTO rewardInput(Long projectId) {
        rewardView.printRewards(projectId);

        System.out.print("리워드 번호(0: 신청 종료): ");
        Long rewardId = sc.nextLong();
        sc.nextLine();

        if (rewardId == 0L) {
            return null;
        }

        RewardDTO reward = rewardService.findById(rewardId);

        if (reward == null || !projectId.equals(reward.getProjectId())) {
            throw new SupportRejectedException(
                    "목록에 있는 리워드 번호를 선택해주세요.");
        }

        if (fss.getRemainStock(rewardId) <= 0) {
            throw new SupportRejectedException("선택한 리워드는 품절되었습니다.");
        }

        return reward;
    }

    private int inputRewardCount() {
        System.out.print("신청 수량(0: 신청 종료): ");

        int rewardCount;

        try {
            rewardCount = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new SupportRejectedException("수량은 정수로 입력해주세요.");
        }

        if (rewardCount < 0) {
            throw new SupportRejectedException("신청 수량은 1개 이상이어야 합니다.");
        }

        if (rewardCount == 0) {
            return 0;
        }

        int remainingStock = fss.getRemainStock(supportReward.getRewardId());

        if (rewardCount > remainingStock) {
            throw new SupportRejectedException(
                    "남은 수량이 부족합니다. 남은 수량: " + remainingStock);
        }

        return rewardCount;
    }

}
