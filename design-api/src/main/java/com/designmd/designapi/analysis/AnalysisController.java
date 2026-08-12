package com.designmd.designapi.analysis;

import com.designmd.designapi.analysis.request.CreateAnalysisRequest;
import com.designmd.designapi.analysis.response.AnalysisCreatedResponse;
import com.designmd.designapi.analysis.response.AnalysisDetailResponse;
import com.designmd.designapi.analysis.response.AnalysisSummaryResponse;
import com.designmd.designapi.common.response.ApiResponse;
import com.designmd.designapi.crawl.response.CrawledPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analyses")
@RequiredArgsConstructor
public class AnalysisController {
    private final AnalysisCommandService commandService;
    private final AnalysisQueryService queryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AnalysisCreatedResponse> create(
            @Valid @RequestBody CreateAnalysisRequest request
    ) {
        return ApiResponse.<AnalysisCreatedResponse>builder()
                .result(commandService.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<Page<AnalysisSummaryResponse>> getMyAnalyses(
            Pageable pageable
    ) {
        return ApiResponse
                .<Page<AnalysisSummaryResponse>>builder()
                .result(queryService.getMyAnalyses(pageable))
                .build();
    }

    @GetMapping("/{analysisId}")
    public ApiResponse<AnalysisDetailResponse> getById(
            @PathVariable String analysisId
    ) {
        return ApiResponse.<AnalysisDetailResponse>builder()
                .result(queryService.getById(analysisId))
                .build();
    }

    @DeleteMapping("/{analysisId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String analysisId
    ) {
        commandService.delete(analysisId);
    }
    @GetMapping("/{analysisId}/pages")
    public ApiResponse<List<CrawledPageResponse>> getPages(
            @PathVariable String analysisId
    ) {

        return ApiResponse
                .<List<CrawledPageResponse>>builder()
                .result(queryService.getPages(analysisId))
                .build();
    }
}
