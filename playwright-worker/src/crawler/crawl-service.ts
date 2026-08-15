import {
  chromium,
  type Browser,
  type BrowserContext,
  type Page,
  type Response,
} from "playwright";
import {
  CrawledPageResult,
  CrawlRequestedEvent,
} from "../contracts/crawl-request-event.js";
import { extractDesignTokens } from "../extraction/design-token-extractor.js";
import { env } from "../config/env.js";
import { PublicUrlGuard } from "../security/public-url-guard.js";

export class CrawlService {
  private browser?: Browser;

  async start(): Promise<void> {
    this.browser = await chromium.launch({
      headless: true,
    });
  }

  async stop(): Promise<void> {
    await this.browser?.close();
  }

  async crawl(event: CrawlRequestedEvent): Promise<CrawledPageResult[]> {
    if (!this.browser) {
      throw new Error("Browser has not been started");
    }

    const guard = new PublicUrlGuard();
    const urls = await this.buildUrls(event, guard);
    const deadline = Date.now() + env.crawlJobTimeoutMs;

    const context = await this.browser.newContext({
      viewport: {
        width: 1440,
        height: 900,
      },
      locale: "vi-VN",
      timezoneId: "Asia/Ho_Chi_Minh",
      userAgent:
        "Mozilla/5.0 (X11; Linux x86_64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/141.0.0.0 Safari/537.36",
      extraHTTPHeaders: {
        "Accept-Language": "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7",
      },
    });

    await context.route("**/*", async (route) => {
      const requestUrl = route.request().url();
      const protocol = new URL(requestUrl).protocol;

      if (["data:", "blob:", "about:"].includes(protocol)) {
        await route.continue();
        return;
      }

      try {
        await guard.assertAllowed(requestUrl);
        await route.continue();
      } catch (error) {
        console.warn(
          `Blocked subrequest ${requestUrl}:`,
          error instanceof Error ? error.message : error,
        );
        await route.abort("blockedbyclient");
      }
    });

    try {
      const results: CrawledPageResult[] = [];

      for (const url of urls) {
        this.assertWithinDeadline(deadline);
        results.push(await this.crawlPage(context, url, deadline, guard));
      }

      return results;
    } finally {
      await context.close();
    }
  }

  private async buildUrls(
    event: CrawlRequestedEvent,
    guard: PublicUrlGuard,
  ): Promise<string[]> {
    const baseUrl = await guard.assertAllowed(event.websiteUrl);

    const urls = [baseUrl.toString()];

    for (const path of event.additionalPaths ?? []) {
      const resolvedUrl = new URL(path, baseUrl);

      if (resolvedUrl.origin !== baseUrl.origin) {
        throw new Error(`Additional path must use the same origin: ${path}`);
      }

      await guard.assertAllowed(resolvedUrl.toString());
      urls.push(resolvedUrl.toString());
    }

    const uniqueUrls = [...new Set(urls)];

    if (uniqueUrls.length > env.maxCrawlPages) {
      throw new Error(
        `Crawl contains ${uniqueUrls.length} pages; maximum is ${env.maxCrawlPages}`,
      );
    }

    return uniqueUrls;
  }

  private async loadPage(
    page: Page,
    url: string,
    deadline: number,
  ): Promise<Response> {
    const maxAttempts = 3;

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      this.assertWithinDeadline(deadline);

      const response = await page.goto(url, {
        waitUntil: "domcontentloaded",
        timeout: Math.min(
          env.navigationTimeoutMs,
          Math.max(1, deadline - Date.now()),
        ),
      });

      if (!response) {
        throw new Error(`Website returned no response: ${url}`);
      }

      if (response.status() !== 429 || attempt === maxAttempts) {
        return response;
      }

      const retryAfterHeader = response.headers()["retry-after"];

      const retryAfterSeconds = Number(retryAfterHeader);

      const delayMs = Number.isFinite(retryAfterSeconds)
        ? Math.min(retryAfterSeconds * 1_000, 30_000)
        : attempt * 3_000;

      console.warn(
        `HTTP 429 for ${url}. ` +
          `Retrying in ${delayMs}ms ` +
          `(${attempt}/${maxAttempts})`,
      );

      if (Date.now() + delayMs >= deadline) {
        throw new Error(`Crawl job timed out while retrying ${url}`);
      }

      await page.waitForTimeout(delayMs);
    }

    throw new Error(`Cannot load website: ${url}`);
  }
  private async crawlPage(
    context: BrowserContext,
    url: string,
    deadline: number,
    guard: PublicUrlGuard,
  ): Promise<CrawledPageResult> {
    const page = await context.newPage();
    const startedAt = Date.now();
    try {
      const response = await this.loadPage(page, url, deadline);

      if (!response) {
        throw new Error(`Website returned no response: ${url}`);
      }

      if (!response.ok()) {
        throw new Error(`Website returned HTTP ${response.status()}: ${url}`);
      }

      const contentLength = Number(response.headers()["content-length"]);

      if (
        Number.isFinite(contentLength) &&
        contentLength > env.maxDocumentBytes
      ) {
        throw new Error(
          `Document is too large (${contentLength} bytes): ${url}`,
        );
      }

      await guard.assertAllowed(page.url());
      this.assertWithinDeadline(deadline);

      await page
        .waitForLoadState("networkidle", {
          timeout: Math.min(5_000, Math.max(1, deadline - Date.now())),
        })
        .catch(() => undefined);

      this.assertWithinDeadline(deadline);
      const tokens = await extractDesignTokens(page);

      return {
        url,
        finalUrl: page.url(),
        title: await page.title(),
        durationMs: Date.now() - startedAt,
        cssVariables: tokens.cssVariables,
        colors: tokens.colors,
        typography: tokens.typography,
        spacing: tokens.spacing,
        radii: tokens.radii,
        shadows: tokens.shadows,
      };
    } finally {
      await page.close();
    }
  }

  private assertWithinDeadline(deadline: number): void {
    if (Date.now() >= deadline) {
      throw new Error(`Crawl job exceeded ${env.crawlJobTimeoutMs}ms timeout`);
    }
  }
}
