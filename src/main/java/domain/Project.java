package domain;

import persistence.dto.ProjectDTO;

import java.time.LocalDateTime;
import java.util.List;

public class Project {
    public final static String[] approvalStatus = {"심사대기", "승인", "반려"};
    public final static String[] successStatus = {"시작전", "진행중", "실패", "성공"};

    public static boolean isApprovl (ProjectDTO projectDTO) {
        return Project.getApprovalStatus(projectDTO).equals("승인");
    }

    public static String getApprovalStatus(ProjectDTO projectDTO) {
        int status = projectDTO.getProjectStatus();

        return approvalStatus[checkNullPoint(status)];
    }

    public static String getSuccessStatus(ProjectDTO projectDTO) {
        if (!Project.isApprovl(projectDTO))
            return approvalStatus[projectDTO.getProjectStatus()];

        LocalDateTime start = projectDTO.getFundingStartAt();
        LocalDateTime end = projectDTO.getFundingEndAt();

        checkNullPoint(start, end);

        if(LocalDateTime.now().isBefore(start))
            return successStatus[0];
        else if(LocalDateTime.now().isBefore(end))
            return successStatus[1];
        else {
            Long goalMount = checkNullPoint(projectDTO.getGoalAmount());
            Long fundedAmount = checkNullPoint(projectDTO.getGoalAmount());
            if (goalMount >= fundedAmount)
                return successStatus[2];
            else
                return successStatus[3];
        }

    }

    private static <T> T checkNullPoint(T filedOfProjectDTO) {
        if(filedOfProjectDTO == null)
            throw new NullPointerException("null인 projectDTO의 필드 조회");

        return filedOfProjectDTO;
    }

    private static <T> List<T> checkNullPoint(T filed1OfProjectDTO, T filed2OfProjectDTO) {
        if(filed1OfProjectDTO == null || filed2OfProjectDTO == null)
            throw new NullPointerException("null인 projectDTO의 필드 조회");

        return List.of(filed1OfProjectDTO, filed2OfProjectDTO);
    }
}
