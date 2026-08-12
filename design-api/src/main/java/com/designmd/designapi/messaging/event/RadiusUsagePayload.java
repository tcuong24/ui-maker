package com.designmd.designapi.messaging.event;

import java.util.List;

public record RadiusUsagePayload(
        String value,
        Double pixels,
        int usageCount,
        List<String> corners,
        List<String> contexts
) {
}
