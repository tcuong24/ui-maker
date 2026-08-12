export interface ScreenshotPayload {
  storageKey: string;
  contentType: "image/jpeg" | "image/png";
  byteSize: number;
  width: number;
  height: number;
  fullPage: boolean;
  capturedAt: string;
}
export interface CrawlRequestedEvent {
  eventId: string;
  schemaVersion: number;
  analysisJobId: string;
  websiteUrl: string;
  additionalPaths: string[];
  includeScreenshot: boolean;
  occurredAt: string;
}
export interface SpacingUsage {
  value: string;
  pixels: number;
  usageCount: number;
  properties: string[];
  contexts: string[];
}
export interface RadiusUsage {
  value: string;
  pixels: number | null;
  usageCount: number;
  corners: string[];
  contexts: string[];
}
export interface ShadowUsage {
  value: string;
  usageCount: number;
  contexts: string[];
}
export interface CrawledPageResult {
  url: string;
  finalUrl: string;
  title: string;
  durationMs: number;

  screenshot?: ScreenshotPayload;

  cssVariables: Record<string, string>;
  colors: ColorUsage[];
  typography: TypographyUsage[];
  spacing: SpacingUsage[];
  radii: RadiusUsage[];
  shadows: ShadowUsage[];
}

export interface CrawlCompletedEvent {
  eventId: string;
  schemaVersion: number;
  sourceEventId: string;
  analysisJobId: string;
  pages: CrawledPageResult[];
  startedAt: string;
  completedAt: string;
}

export interface CrawlFailedEvent {
  eventId: string;
  schemaVersion: number;
  sourceEventId: string;
  analysisJobId: string;
  errorCode: string;
  errorMessage: string;
  failedAt: string;
}
export interface ColorUsage {
  value: string;
  usageCount: number;
  contexts: string[];
}

export interface TypographyUsage {
  fontFamily: string;
  fontSize: string;
  fontWeight: string;
  lineHeight: string;
  letterSpacing: string;
  usageCount: number;
}