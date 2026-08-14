package com.designmd.designapi.messaging.event;

import java.util.List;
import java.util.Map;

public record ColorUsagePayload(
        String value,
        int usageCount,
        double visualArea,
        List<String> contexts,
        List<String> elements,
        Map<String, Integer> roleCounts
) {
}
