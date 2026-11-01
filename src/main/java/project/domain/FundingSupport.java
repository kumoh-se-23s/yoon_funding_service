package project.domain;

import project.persistence.dao.UserDAO;
import project.persistence.dto.*;

import java.time.LocalDateTime;
import java.util.Locale;

public class FundingSupport {

    private final Long userId;
    private final Long projectId;
    private final Long rewardId;

    private String recipientName;
    private String deliveryAddress;
    private String deliveryDetailAddress;

    public FundingSupport (
            SupportRequestDTO request,
            UserDTO userDTO,
            ProjectDTO projectDTO,
            RewardDTO rewardDTO
    ) throws RuntimeException {
        if (request == null) {
            throw new IllegalArgumentException("후원 신청 정보가 없습니다.");
        }
        else if (userDTO == null || request.getUserId() == null || request.getUserId() != userDTO.getUserId()) {
            throw new IllegalArgumentException("유효하지 않은 신청자 정보.");
        }
        else if (projectDTO == null || projectDTO.getProjectId() == null || projectDTO.getProjectId() != request.getProjectId()) {
            throw new IllegalArgumentException("유효하지 않은 신청 프로젝트 정보.");
        }
        else if (!Project.getApprovalStatus(projectDTO).equals("승인")) {
            String err = "신청 프로젝트는 승인 되지 않음.";
            System.out.println(err);
            throw new IllegalStateException(err);
        }
        else if (!Project.getSuccessStatus(projectDTO).equals("진행중")) {
            String err = "진행중인 프로젝트가 아닙니다.";
            System.out.println(err);
            throw new IllegalStateException(err);
        }
        else if (rewardDTO == null || rewardDTO.getRewardId() == null || request.getRewardId() != rewardDTO.getRewardId()) {
            throw new IllegalArgumentException("유효하지 않은 신청 리워드 정보.");
        }
        else {
            this.userId = request.getUserId();
            this.projectId = request.getProjectId();
            this.rewardId = request.getRewardId();

            this.recipientName = request.getRecipientName();
            this.deliveryAddress = request.getDeliveryAddress();
            this.deliveryDetailAddress = request.getDeliveryDetailAddress();

            if ( this.recipientName == null
                    ||  this.recipientName.isBlank()
            ) {
                if(userDTO.getName() == null || userDTO.getName().isBlank())
                    throw new IllegalArgumentException("수령인 이름이 필요합니다.");
                this.recipientName = userDTO.getName();
            }

            if (request.getDeliveryAddress() == null
                    || request.getDeliveryAddress().isBlank()
            ) {
                if(userDTO.getAddress() == null || userDTO.getAddress().isBlank())
                    throw new IllegalArgumentException("배송 주소가 필요합니다.");
                this.deliveryAddress = userDTO.getAddress();
            }

            if (request.getDeliveryAddress() == null
                    || request.getDeliveryAddress().isBlank()
            ) {
                if(userDTO.getDetailAddress() == null || userDTO.getDetailAddress().isBlank())
                    throw new IllegalArgumentException("배송 상세 주소가 필요합니다.");
                this.deliveryDetailAddress = userDTO.getDetailAddress();
            }
        }
    }

    public int checkRewardStock (int stock) throws RuntimeException {
        if(stock == 0) {
            String err = "신청 수량이 남아 있지 않음.";
            System.out.println(err);
            throw new IllegalStateException(err);
        }

        return stock;
    }

    public FundingSupportDTO supportToDTO() {

        return FundingSupportDTO.builder()
                .userId(userId)
                .rewardId(rewardId)
                .supportedAt(LocalDateTime.now())
                .recipientName(recipientName)
                .deliveryAddress(deliveryAddress)
                .deliveryDetailAddress(deliveryDetailAddress)
                .cancelStatus(false)
                .build();
    }
}
