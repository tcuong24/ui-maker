package com.designmd.designapi.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisUrlNormalizerTests {

    private final AnalysisUrlNormalizer normalizer = new AnalysisUrlNormalizer();

    @Test
    void normalizesEquivalentRootUrlsToTheSameCacheKey() {
        var first = normalizer.normalize(
                "HTTPS://Example.COM:443",
                List.of("/pricing", "/about"),
                true
        );
        var second = normalizer.normalize(
                "https://example.com/",
                List.of("/about", "/pricing"),
                true
        );

        assertThat(first.normalizedUrl()).isEqualTo("https://example.com/");
        assertThat(first.domain()).isEqualTo("example.com");
        assertThat(first.cacheKey()).isEqualTo(second.cacheKey());
    }

    @Test
    void changesCacheKeyWhenScreenshotOptionChanges() {
        var withScreenshot = normalizer.normalize(
                "https://example.com",
                List.of(),
                true
        );
        var withoutScreenshot = normalizer.normalize(
                "https://example.com",
                List.of(),
                false
        );

        assertThat(withScreenshot.cacheKey())
                .isNotEqualTo(withoutScreenshot.cacheKey());
    }
}
