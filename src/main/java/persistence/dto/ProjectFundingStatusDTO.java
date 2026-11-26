package persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProjectFundingStatusDTO {
    private final ProjectDTO project;
    private final List<RewardFundingStatusDTO> rewards;
    private final List<ProjectSupporterDTO> supporters;
}