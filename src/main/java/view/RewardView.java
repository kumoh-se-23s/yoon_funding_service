package view;

import lombok.RequiredArgsConstructor;
import persistence.dao.RewardDAO;
import persistence.dto.FundingSupportDTO;
import persistence.dto.ProjectDTO;
import persistence.dto.RewardDTO;
import service.*;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class RewardView {
    private RewardService rewardService;
    private FundingSupportService fss;

    public void printRewards(Long projectId) {
        List<RewardDTO> rewards = rewardService.findAll(projectId);

        System.out.println("========== 리워드 목록 ==========");

        FundingSupportDTO checkRemain = new FundingSupportDTO();

        for (RewardDTO reward : rewards) {
            System.out.println("리워드 ID: " + reward.getRewardId());
            System.out.println("리워드 이름: " + reward.getTitle());
            System.out.println("금액: " + reward.getPrice());
            System.out.println("남은 수량: " + fss.getRemainStock(reward.getRewardId()));
            System.out.println("--------------------------------");
        }
    }


}
