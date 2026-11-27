package view;

import lombok.*;
import persistence.dao.ReviewDAO;
import persistence.dto.ReviewDTO;
import persistence.dto.SupportDTO;
import persistence.dto.UserDTO;
import service.FundingSupportService;
import service.ReviewService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

@Getter
@Setter
@ToString
@Builder
@RequiredArgsConstructor
public class ReviewView {
    private final ReviewDAO reviewDAO;
    private final ReviewService reviewService;
    private final FundingSupportService fss;

    public void input(Scanner sc, UserDTO loginUser) {
        System.out.println("리뷰를 등록하시겠습니까?(y / n)");
        char cont = Character.toUpperCase(sc.next().charAt(0));

        if (cont != 'Y') {
            return;
        }

        if (!reviewService.isSupporter(loginUser)) {
            throw new IllegalStateException("해당 유저는 서포터가 아닙니다.");
        }

        sc.nextLine(); // 버퍼 비우기

        List<SupportDTO> supports = fss.findSupports(loginUser.getUserId());
        printSupports(supports);

        if (supports.isEmpty()) {
            return;
        }

        System.out.print("리뷰를 등록할 프로젝트 ID: ");

        if (!sc.hasNextLong()) {
            sc.nextLine();
            System.out.println("[안내] 프로젝트 ID는 정수로 입력해 주세요.");
            return;
        }

        Long projectId = sc.nextLong();
        sc.nextLine();

        SupportDTO selectedSupport = null;

        for (SupportDTO support : supports) {
            if (projectId.equals(support.getProjectId())) {
                selectedSupport = support;
                break;
            }
        }

        if (selectedSupport == null) {
            System.out.println("[안내] 본인이 후원한 프로젝트를 선택해 주세요.");
            return;
        }

        System.out.print("리뷰 내용: ");
        String content = sc.nextLine();

        System.out.print("평점(0.0 ~ 5.0): ");
        BigDecimal score = sc.nextBigDecimal();
        sc.nextLine();

        ReviewDTO reviewDTO = ReviewDTO.builder()
                .supportId(selectedSupport.getSupportId())
                .content(content)
                .score(score)
                .build();

        Long reviewId = reviewService.append(
                reviewDTO,
                loginUser.getUserId(),
                selectedSupport.getProjectId()
        );

        System.out.println("리뷰 등록 완료! 리뷰 ID: " + reviewId);
    }

    private void printSupports(List<SupportDTO> supports) {

        System.out.println("\n========== 나의 후원 내역 ==========");

        if (supports.isEmpty()) {
            System.out.println("후원 내역이 없습니다.");
            return;
        }

        for (int i = 0; i < supports.size(); i++) {
            SupportDTO support = supports.get(i);

            System.out.println("[" + (i + 1) + "번째 후원]");
            System.out.println("프로젝트: " + support.getProjectTitle());
            System.out.println("프로젝트 ID: " + support.getProjectId());
            System.out.println("리워드: " + support.getRewardTitle());
            System.out.println("리워드 ID: " + support.getRewardId());
            System.out.println("----------------------------------");
        }

        System.out.println("총 후원 건수: " + supports.size());
    }
}

