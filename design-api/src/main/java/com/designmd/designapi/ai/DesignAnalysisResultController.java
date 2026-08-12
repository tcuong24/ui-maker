package com.designmd.designapi.ai;

import com.designmd.designapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/analyses/{analysisId}")
@RequiredArgsConstructor
public class DesignAnalysisResultController {

    private final DesignAnalysisResultService resultService;

    @GetMapping("/style")
    public ApiResponse<Map<String, Object>> getStyle(
            @PathVariable String analysisId
    ) {
        return ApiResponse
                .<Map<String, Object>>builder()
                .result(resultService.getStyle(analysisId))
                .build();
    }

    @GetMapping(
            value = "/report.md",
            produces = "text/markdown;charset=UTF-8"
    )
    public ResponseEntity<String> downloadMarkdown(
            @PathVariable String analysisId
    ) {
        String markdown =
                resultService.getMarkdown(analysisId);

        String filename =
                "design-system-" + analysisId + ".md";

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                filename,
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                "text/markdown;charset=UTF-8"
                        )
                )
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(markdown);
    }
}