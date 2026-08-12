package com.designmd.designapi.messaging.event;

public record TypographyUsagePayload(String fontFamily,String fontSize,String fontWeight, String lineHeight, String letterSpacing,int usageCount) {
}
