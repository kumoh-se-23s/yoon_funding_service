package project.domain;

import project.persistence.dto.FundingSupportDTO;
import project.persistence.dto.SupportRequestDTO;
import project.persistence.dto.UserDTO;

import java.time.LocalDateTime;

public class FundingSupport {

    private final Long userId;
    private final Long rewardId;

    private final String recipientName;
    private final String deliveryAddress;
    private final String deliveryDetailAddress;

    public FundingSupport(SupportRequestDTO request) {

        if (request == null) {
            throw new IllegalArgumentException("후원 신청 정보가 없습니다.");
        }
        else {
            if (request.getUserId() == null) {
                throw new IllegalArgumentException("사용자 ID가 필요합니다.");
            }

            if (request.getProjectId() == null) {
                throw new IllegalArgumentException("프로젝트 ID가 필요합니다.");
            }

            if (request.getRewardId() == null) {
                throw new IllegalArgumentException("리워드 ID가 필요합니다.");
            }

            if (request.getRecipientName() == null
                    || request.getRecipientName().isBlank()) {
                UserDAO userdao = new UserDAO();
                UserDTO userinfo = UserDAO
                throw new IllegalArgumentException("수령인 이름이 필요합니다.");
            }

            if (request.getDeliveryAddress() == null
                    || request.getDeliveryAddress().isBlank()) {
                throw new IllegalArgumentException("배송 주소가 필요합니다.");
            }

        }

        this.userId = request.getUserId();
        this.rewardId = request.getRewardId();
        this.recipientName = request.getRecipientName();
        this.deliveryAddress = request.getDeliveryAddress();
        this.deliveryDetailAddress = request.getDeliveryDetailAddress();
    }

    public FundingSupportDTO supportToDTO() {

        return FundingSupportDTO.builder()
                .userId(userId)
                .rewardId(rewardId)
                .supportedAt(LocalDateTime.now())
                .recipientName(recipientName)
                .deliveryAddress(deliveryAddress)
                .deliveryDetailAddress(deliveryDetailAddress)
                .cancelStatus(true)
                .build();
    }
}
