package com.designmd.designapi.design;


import com.designmd.designapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analyses/{analysisId}/design-system")
@RequiredArgsConstructor
public class DesignSystemController {

    private final DesignSystemQueryService designSystemQueryService;

    @GetMapping
    public ApiResponse<DesignSystemSnapshot> getDesignSystem(
            @PathVariable String analysisId
    ) {
        DesignSystemSnapshot snapshot =
                designSystemQueryService.getByAnalysisId(analysisId);

        return ApiResponse.<DesignSystemSnapshot>builder()
                .result(snapshot)
                .build();
    }
}