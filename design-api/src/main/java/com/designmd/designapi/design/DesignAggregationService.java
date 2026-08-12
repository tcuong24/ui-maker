package com.designmd.designapi.design;

import com.designmd.designapi.crawl.CrawledPage;
import com.designmd.designapi.crawl.CrawledPageService;
import com.designmd.designapi.design.model.*;
import com.designmd.designapi.messaging.event.TypographyUsagePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DesignAggregationService {
    private final CrawledPageService crawledPageService;
    private final DesignSystemRepository repository;


    private static class EvidenceAccumulator {

        private long usageCount;

        private final Set<String> pageUrls =
                new LinkedHashSet<>();

        private final Set<String> contexts =
                new LinkedHashSet<>();

        private final Set<String> attributes =
                new LinkedHashSet<>();

        void add(
                String pageUrl,
                int count,
                List<String> newContexts
        ) {
            add(
                    pageUrl,
                    count,
                    newContexts,
                    null
            );
        }

        void add(
                String pageUrl,
                int count,
                List<String> newContexts,
                List<String> newAttributes
        ) {
            usageCount += count;
            pageUrls.add(pageUrl);

            addLimited(
                    contexts,
                    newContexts,
                    20
            );

            addLimited(
                    attributes,
                    newAttributes,
                    20
            );
        }

        private void addLimited(
                Set<String> target,
                List<String> values,
                int limit
        ) {
            if (values == null) {
                return;
            }

            for (String value : values) {
                if (target.size() >= limit) {
                    break;
                }

                if (value != null && !value.isBlank()) {
                    target.add(value);
                }
            }
        }

        int pageCount() {
            return pageUrls.size();
        }

        double coverage(int totalPages) {
            if (totalPages == 0) {
                return 0;
            }

            return Math.round(
                    ((double) pageCount() / totalPages)
                            * 10_000
            ) / 10_000.0;
        }
    };

    private List<AggregatedCssVariable> aggregateCssVariables(
            List<CrawledPage> pages,
            int totalPages
    ) {
        Map<
                String,
                Map<String, Set<String>>
                > variableMap = new TreeMap<>();

        for (CrawledPage page : pages) {
            if (page.getCssVariables() == null) {
                continue;
            }

            for (var entry :
                    page.getCssVariables().entrySet()) {

                String name = entry.getKey() == null
                        ? ""
                        : entry.getKey().trim();

                String value = normalizeWhitespace(
                        entry.getValue()
                );

                if (name.isBlank() || value.isBlank()) {
                    continue;
                }

                variableMap
                        .computeIfAbsent(
                                name,
                                ignored -> new HashMap<>()
                        )
                        .computeIfAbsent(
                                value,
                                ignored ->
                                        new LinkedHashSet<>()
                        )
                        .add(page.getPageUrl());
            }
        }

        List<AggregatedCssVariable> result =
                new ArrayList<>();

        for (var variableEntry :
                variableMap.entrySet()) {

            String variableName =
                    variableEntry.getKey();

            List<
                    AggregatedCssVariable.CssVariableVariant
                    > variants = new ArrayList<>();

            for (var valueEntry :
                    variableEntry.getValue().entrySet()) {

                Set<String> pageUrls =
                        valueEntry.getValue();

                variants.add(
                        new AggregatedCssVariable
                                .CssVariableVariant(
                                valueEntry.getKey(),
                                pageUrls.size(),
                                calculateCoverage(
                                        pageUrls.size(),
                                        totalPages
                                ),
                                List.copyOf(pageUrls)
                        )
                );
            }

            variants = variants.stream()
                    .sorted(
                            Comparator.comparingInt(
                                    AggregatedCssVariable
                                            .CssVariableVariant
                                            ::pageCount
                            ).reversed()
                    )
                    .toList();

            result.add(
                    new AggregatedCssVariable(
                            variableName,
                            variants
                    )
            );
        }

        return result.stream()
                .limit(500)
                .toList();
    }

    private String normalizeWhitespace(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("\\s+", " ");
    }

    private double calculateCoverage(
            int pageCount,
            int totalPages
    ) {
        if (totalPages == 0) {
            return 0;
        }

        return Math.round(
                ((double) pageCount / totalPages)
                        * 10_000
        ) / 10_000.0;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }


    private List<AggregatedSpacing> aggregateSpacing(
            List<CrawledPage> pages,
            int totalPages
    ) {
        Map<String, EvidenceAccumulator> accumulatorMap =
                new HashMap<>();

        Map<String, Double> pixelsByValue =
                new HashMap<>();

        for (CrawledPage page : pages) {
            if (page.getSpacing() == null) {
                continue;
            }

            for (var spacing : page.getSpacing()) {
                String key = normalize(
                        spacing.value()
                );

                if (key.isBlank()) {
                    continue;
                }

                pixelsByValue.putIfAbsent(
                        key,
                        spacing.pixels()
                );

                EvidenceAccumulator accumulator =
                        accumulatorMap.computeIfAbsent(
                                key,
                                ignored ->
                                        new EvidenceAccumulator()
                        );

                accumulator.add(
                        page.getPageUrl(),
                        spacing.usageCount(),
                        spacing.contexts(),
                        spacing.properties()
                );
            }
        }

        List<AggregatedSpacing> result =
                new ArrayList<>();

        for (var entry : accumulatorMap.entrySet()) {
            String value = entry.getKey();

            EvidenceAccumulator data =
                    entry.getValue();

            result.add(
                    new AggregatedSpacing(
                            value,
                            pixelsByValue.getOrDefault(
                                    value,
                                    0.0
                            ),
                            data.usageCount,
                            data.pageCount(),
                            data.coverage(totalPages),
                            List.copyOf(data.attributes),
                            List.copyOf(data.contexts),
                            List.copyOf(data.pageUrls)
                    )
            );
        }

        return result.stream()
                .sorted(
                        Comparator.comparingLong(
                                AggregatedSpacing::usageCount
                        ).reversed()
                )
                .limit(100)
                .toList();
    }


    private List<AggregatedColor> aggregateColors(
            List<CrawledPage> pages,
            int totalPages
    ) {
        Map<String, EvidenceAccumulator> accumulatorMap =
                new HashMap<>();

        for (CrawledPage page : pages) {
            if (page.getColors() == null) {
                continue;
            }

            for (var color : page.getColors()) {
                String key = normalize(
                        color.value()
                );

                if (key.isBlank()) {
                    continue;
                }

                EvidenceAccumulator accumulator =
                        accumulatorMap.computeIfAbsent(
                                key,
                                ignored ->
                                        new EvidenceAccumulator()
                        );

                accumulator.add(
                        page.getPageUrl(),
                        color.usageCount(),
                        color.contexts()
                );
            }
        }

        List<AggregatedColor> result =
                new ArrayList<>();

        for (var entry : accumulatorMap.entrySet()) {
            EvidenceAccumulator data =
                    entry.getValue();

            result.add(
                    new AggregatedColor(
                            entry.getKey(),
                            data.usageCount,
                            data.pageCount(),
                            data.coverage(totalPages),
                            List.copyOf(data.contexts),
                            List.copyOf(data.pageUrls)
                    )
            );
        }

        return result.stream()
                .sorted(
                        Comparator.comparingLong(
                                AggregatedColor::usageCount
                        ).reversed()
                )
                .limit(100)
                .toList();
    }

    private List<AggregatedTypography> aggregateTypography(
            List<CrawledPage> pages,
            int totalPages
    ) {
        Map<String, EvidenceAccumulator> accumulatorMap =
                new HashMap<>();

        Map<String, TypographyUsagePayload> sampleByKey =
                new HashMap<>();

        for (CrawledPage page : pages) {
            if (page.getTypography() == null) {
                continue;
            }

            for (var typography : page.getTypography()) {
                String key = typographyKey(
                        typography
                );

                if (key.isBlank()) {
                    continue;
                }

                sampleByKey.putIfAbsent(
                        key,
                        typography
                );

                accumulatorMap
                        .computeIfAbsent(
                                key,
                                ignored ->
                                        new EvidenceAccumulator()
                        )
                        .add(
                                page.getPageUrl(),
                                typography.usageCount(),
                                null
                        );
            }
        }

        List<AggregatedTypography> result =
                new ArrayList<>();

        for (var entry : accumulatorMap.entrySet()) {
            String key = entry.getKey();

            TypographyUsagePayload sample =
                    sampleByKey.get(key);

            EvidenceAccumulator data =
                    entry.getValue();

            result.add(
                    new AggregatedTypography(
                            sample.fontFamily(),
                            sample.fontSize(),
                            sample.fontWeight(),
                            sample.lineHeight(),
                            sample.letterSpacing(),
                            data.usageCount,
                            data.pageCount(),
                            data.coverage(totalPages),
                            List.copyOf(data.pageUrls)
                    )
            );
        }

        return result.stream()
                .sorted(
                        Comparator.comparingLong(
                                AggregatedTypography::usageCount
                        ).reversed()
                )
                .limit(100)
                .toList();
    }

    private String typographyKey(
            TypographyUsagePayload typography
    ) {
        return String.join(
                "\u001F",
                normalize(typography.fontFamily()),
                normalize(typography.fontSize()),
                normalize(typography.fontWeight()),
                normalize(typography.lineHeight()),
                normalize(typography.letterSpacing())
        );
    }

    private List<AggregatedRadius> aggregateRadii(
            List<CrawledPage> pages,
            int totalPages
    ) {
        Map<String, EvidenceAccumulator> accumulatorMap =
                new HashMap<>();

        Map<String, Double> pixelsByValue =
                new HashMap<>();

        for (CrawledPage page : pages) {
            if (page.getRadii() == null) {
                continue;
            }

            for (var radius : page.getRadii()) {
                String key = normalize(
                        radius.value()
                );

                if (key.isBlank()) {
                    continue;
                }

                if (!pixelsByValue.containsKey(key)) {
                    pixelsByValue.put(
                            key,
                            radius.pixels()
                    );
                }

                accumulatorMap
                        .computeIfAbsent(
                                key,
                                ignored ->
                                        new EvidenceAccumulator()
                        )
                        .add(
                                page.getPageUrl(),
                                radius.usageCount(),
                                radius.contexts(),
                                radius.corners()
                        );
            }
        }

        List<AggregatedRadius> result =
                new ArrayList<>();

        for (var entry : accumulatorMap.entrySet()) {
            String value = entry.getKey();

            EvidenceAccumulator data =
                    entry.getValue();

            result.add(
                    new AggregatedRadius(
                            value,
                            pixelsByValue.get(value),
                            data.usageCount,
                            data.pageCount(),
                            data.coverage(totalPages),
                            List.copyOf(data.attributes),
                            List.copyOf(data.contexts),
                            List.copyOf(data.pageUrls)
                    )
            );
        }

        return result.stream()
                .sorted(
                        Comparator.comparingLong(
                                AggregatedRadius::usageCount
                        ).reversed()
                )
                .limit(100)
                .toList();
    }

    private List<AggregatedShadow> aggregateShadows(
            List<CrawledPage> pages,
            int totalPages
    ) {
        Map<String, EvidenceAccumulator> accumulatorMap =
                new HashMap<>();

        for (CrawledPage page : pages) {
            if (page.getShadows() == null) {
                continue;
            }

            for (var shadow : page.getShadows()) {
                String key = normalize(
                        shadow.value()
                );

                if (key.isBlank()) {
                    continue;
                }

                accumulatorMap
                        .computeIfAbsent(
                                key,
                                ignored ->
                                        new EvidenceAccumulator()
                        )
                        .add(
                                page.getPageUrl(),
                                shadow.usageCount(),
                                shadow.contexts()
                        );
            }
        }

        List<AggregatedShadow> result =
                new ArrayList<>();

        for (var entry : accumulatorMap.entrySet()) {
            EvidenceAccumulator data =
                    entry.getValue();

            result.add(
                    new AggregatedShadow(
                            entry.getKey(),
                            data.usageCount,
                            data.pageCount(),
                            data.coverage(totalPages),
                            List.copyOf(data.contexts),
                            List.copyOf(data.pageUrls)
                    )
            );
        }

        return result.stream()
                .sorted(
                        Comparator.comparingLong(
                                AggregatedShadow::usageCount
                        ).reversed()
                )
                .limit(100)
                .toList();
    }

    public DesignSystemSnapshot rebuild(
            String analysisJobId
    ) {
        List<CrawledPage> pages =
                crawledPageService.getByAnalysisId(
                        analysisJobId
                );

        int totalPages = pages.size();
        Instant now = Instant.now();

        DesignSystemSnapshot snapshot = repository
                .findByAnalysisJobId(analysisJobId)
                .orElseGet(() ->
                        DesignSystemSnapshot.builder()
                                .analysisJobId(analysisJobId)
                                .generatedAt(now)
                                .build()
                );

        snapshot.setPageCount(totalPages);

        snapshot.setColors(
                aggregateColors(
                        pages,
                        totalPages
                )
        );

        snapshot.setSpacing(
                aggregateSpacing(
                        pages,
                        totalPages
                )
        );
        snapshot.setTypography(
                aggregateTypography(
                        pages,
                        totalPages
                )
        );

        snapshot.setRadii(
                aggregateRadii(
                        pages,
                        totalPages
                )
        );

        snapshot.setShadows(
                aggregateShadows(
                        pages,
                        totalPages
                )
        );

        snapshot.setCssVariables(
                aggregateCssVariables(
                        pages,
                        totalPages
                )
        );

        snapshot.setUpdatedAt(now);

        if (snapshot.getGeneratedAt() == null) {
            snapshot.setGeneratedAt(now);
        }

        return repository.save(snapshot);
    }


}
