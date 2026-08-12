package com.designmd.designapi.analysis.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAnalysisRequest(@NotBlank(message = "Website URL is required")
                                    @Size(max = 2048, message = "Website URL is too long")
                                    @Pattern(
                                            regexp = "(?i)^https?://.+$",
                                            message = "Website URL must use HTTP or HTTPS"
                                    )
                                    String websiteUrl,

                                    @Size(max = 10, message = "Maximum 10 additional paths")
                                    List<@NotBlank(message = "Additional path cannot be blank")
                                            @Size(max = 2048, message = "Additional path is too long")
                                            String> additionalPaths,

                                    boolean includeScreenshot) {

}
