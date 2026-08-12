package com.designmd.designapi.messaging.event;

import java.util.List;

public record SpacingUsagePayload(
        String value,
        double pixels,
        int usageCount,
        List<String> properties,
        List<String> contexts
) {
}
