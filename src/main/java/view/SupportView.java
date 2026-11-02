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


    private ProjectDTO supportProject = null;
    private RewardDTO supportReward = null;

    public void supportView() {
        SupportRequestDTO requestDTO = input();
        FundingSupportService fss = new FundingSupportService(
                ds, new FundingSupportDAO(), new UserDAO(), new ProjectDAO(), new RewardDAO()
        );

        fss.support(requestDTO);
    }

    private SupportRequestDTO input () {
        this.supportProject = projectInput();
        this.supportReward = rewardInput(supportProject.getProjectId());

        SupportRequestDTO request = SupportRequestDTO.builder()
                .userId(loginUser.getUserId())
                .projectId(supportProject.getProjectId())
                .rewardId(supportReward.getRewardId())
                .build();

        System.out.println("수령인 정보를 입력하시겠습니까? (y/n)");
        char cont = Character.toUpperCase(sc.next().charAt(0));
        if(cont == 'Y') {
            System.out.println("수령인이름: ");
            String recipientName = sc.next();
            request.setRecipientName(recipientName);

            System.out.println("배송주소: ");
            String deliveryAddress = sc.next();
            request.setDeliveryAddress(deliveryAddress);

            System.out.println("상세주소: ");
            String deliveryDetailAddress = sc.next();
            request.setDeliveryDetailAddress(deliveryDetailAddress);
        }

        return request;
    }

    private ProjectDTO projectInput() {
        viewProjects();
        System.out.println("프로젝트 번호를 선택해주세요");
        Long projectId = sc.nextLong();

        ProjectDTO supportProject = projectService.findById(projectId);

        return supportProject;
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
        System.out.println("리워드 번호를 선택해주세요");
        Long rewardId = sc.nextLong();

        RewardDTO supportReward = rewardService.findById(rewardId);

        return supportReward;
    }


}
