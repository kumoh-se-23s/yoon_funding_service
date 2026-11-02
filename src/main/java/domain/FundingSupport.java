package domain;

import persistence.dto.*;

import java.time.LocalDateTime;

public class FundingSupport {

    private final Long userId;
    private final Long projectId;
    private final Long rewardId;

    private String recipientName;
    private String deliveryAddress;
    private String deliveryDetailAddress;

    public FundingSupport (
            SupportRequestDTO request,
            UserDTO supporterDTO,
            ProjectDTO requestProjectDTO,
            RewardDTO requestRewardDTO
    ) throws RuntimeException {
        if (request == null) {
            throw new IllegalArgumentException("후원 신청 정보가 없습니다.");
        }
        else if (
                supporterDTO == null
                        || request.getUserId() == null
                        || request.getUserId() != supporterDTO.getUserId()
                        || supporterDTO.getRole()!= 1
        ) {
            throw new IllegalArgumentException("유효하지 않은 신청자 정보.");
        }
        else if (requestProjectDTO == null || requestProjectDTO.getProjectId() == null || requestProjectDTO.getProjectId() != request.getProjectId()) {
            throw new IllegalArgumentException("유효하지 않은 신청 프로젝트 정보.");
        }
        else if (!Project.getApprovalStatus(requestProjectDTO).equals("승인")) {
            String err = "신청 프로젝트는 승인 되지 않음.";
            System.out.println(err);
            throw new IllegalStateException(err);
        }
        else if (!Project.getSuccessStatus(requestProjectDTO).equals("진행중")) {
            String err = "진행중인 프로젝트가 아닙니다.";
            System.out.println(err);
            throw new IllegalStateException(err);
        }
        else if (requestRewardDTO == null || requestRewardDTO.getRewardId() == null || request.getRewardId() != requestRewardDTO.getRewardId()) {
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
                if(supporterDTO.getName() == null || supporterDTO.getName().isBlank())
                    throw new IllegalArgumentException("수령인 이름이 필요합니다.");
                this.recipientName = supporterDTO.getName();
            }

            if (request.getDeliveryAddress() == null
                    || request.getDeliveryAddress().isBlank()
            ) {
                if(supporterDTO.getAddress() == null || supporterDTO.getAddress().isBlank())
                    throw new IllegalArgumentException("배송 주소가 필요합니다.");
                this.deliveryAddress = supporterDTO.getAddress();
            }

            if (request.getDeliveryAddress() == null
                    || request.getDeliveryAddress().isBlank()
            ) {
                if(supporterDTO.getDetailAddress() == null || supporterDTO.getDetailAddress().isBlank())
                    throw new IllegalArgumentException("배송 상세 주소가 필요합니다.");
                this.deliveryDetailAddress = supporterDTO.getDetailAddress();
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
