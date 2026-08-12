package com.designmd.designapi.messaging.event;

import java.util.List;

public record ColorUsagePayload(String value, int usageCount, List<String> contexts) {
}
