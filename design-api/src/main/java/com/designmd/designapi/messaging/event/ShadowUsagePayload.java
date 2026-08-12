package com.designmd.designapi.messaging.event;

import java.util.List;

public record ShadowUsagePayload(
        String value,
        int usageCount,
        List<String> contexts
) {
}
