package domain;

import persistence.dto.*;

import java.time.LocalDateTime;
import exception.SupportRejectedException;

public class FundingSupport {

    private final Long userId;
    private final Long projectId;
    private final Long rewardId;
    private final int rewardCount;

    private String recipientName;
    private String recipientContact;
    private String deliveryAddress;
    private String deliveryDetailAddress;

    public FundingSupport (
            SupportRequestDTO request,
            UserDTO supporterDTO,
            ProjectDTO requestProjectDTO,
            RewardDTO requestRewardDTO
    ) throws RuntimeException {
        if (request == null) {
            throw new SupportRejectedException("후원 신청 정보가 없습니다.");
        }
        else if (request.getRewardCount() < 1) {
            throw new SupportRejectedException("신청 수량은 1개 이상이어야 합니다.");
        }
        else if (
                supporterDTO == null
                        || request.getUserId() == null
                        || !request.getUserId().equals(supporterDTO.getUserId())
                        || !"supporter".equals(supporterDTO.getRole())
        ) {
            throw new SupportRejectedException("유효하지 않은 신청자 정보.");
        }
        else if (requestProjectDTO == null || requestProjectDTO.getProjectId() == null || !requestProjectDTO.getProjectId().equals(request.getProjectId())) {
            throw new SupportRejectedException("유효하지 않은 신청 프로젝트 정보.");
        }
        else if (!"approved".equals(requestProjectDTO.getApprovalStatus())) {
            String err = "신청 프로젝트는 승인 되지 않음.";
            throw new SupportRejectedException(err);
        }
        else if (!"ongoing".equals(requestProjectDTO.getProgressStatus())) {
            String err = "진행중인 프로젝트가 아닙니다.";
            throw new SupportRejectedException(err);
        }
        else if (requestRewardDTO == null || requestRewardDTO.getRewardId() == null || !requestRewardDTO.getRewardId().equals(request.getRewardId())) {
            throw new SupportRejectedException("유효하지 않은 신청 리워드 정보.");
        }
        else {
            this.userId = request.getUserId();
            this.projectId = request.getProjectId();
            this.rewardId = request.getRewardId();
            this.rewardCount = request.getRewardCount();

            this.recipientName = request.getRecipientName();
            this.recipientContact = request.getRecipientContact();
            this.deliveryAddress = request.getDeliveryAddress();
            this.deliveryDetailAddress = request.getDeliveryDetailAddress();

            if ( this.recipientName == null
                    ||  this.recipientName.isBlank()
            ) {
                if(supporterDTO.getName() == null || supporterDTO.getName().isBlank())
                    throw new SupportRejectedException("수령인 이름이 필요합니다.");
                this.recipientName = supporterDTO.getName();
            }

            if (this.recipientContact == null
                    || this.recipientContact.isBlank()) {

                if (supporterDTO.getContact() == null
                        || supporterDTO.getContact().isBlank()) {
                    throw new SupportRejectedException("수령인 연락처가 필요합니다.");
                }

                this.recipientContact = supporterDTO.getContact();
            }

            if (request.getDeliveryAddress() == null
                    || request.getDeliveryAddress().isBlank()
            ) {
                if(supporterDTO.getAddress() == null || supporterDTO.getAddress().isBlank())
                    throw new SupportRejectedException("배송 주소가 필요합니다.");
                this.deliveryAddress = supporterDTO.getAddress();
            }

            if (request.getDeliveryAddress() == null
                    || request.getDeliveryAddress().isBlank()
            ) {
                if(supporterDTO.getDetailAddress() == null || supporterDTO.getDetailAddress().isBlank())
                    throw new SupportRejectedException("배송 상세 주소가 필요합니다.");
                this.deliveryDetailAddress = supporterDTO.getDetailAddress();
            }
        }
    }

    public static int checkRewardStock (int stock) throws RuntimeException {
        if(stock <= 0) {
            String err = "신청 수량이 남아 있지 않음.";
            throw new SupportRejectedException(err);
        }

        return stock;
    }

    public FundingSupportDTO supportToDTO() {

        return FundingSupportDTO.builder()
                .userId(userId)
                .rewardId(rewardId)
                .rewardCount(rewardCount)
                .processedAt(LocalDateTime.now())
                .recipientName(recipientName)
                .recipientContact(recipientContact)
                .deliveryAddress(deliveryAddress)
                .deliveryDetailAddress(deliveryDetailAddress)
                .cancelStatus(false)
                .build();
    }
}
