import type { Page } from "playwright";

export async function captureScreenshot(page: Page): Promise<Buffer> {
  const pageHeight = await page.evaluate(
    () => document.documentElement.scrollHeight,
  );

  const fullPage = pageHeight <= 12_000;

  return page.screenshot({
    type: "jpeg",
    quality: 75,
    fullPage,
    animations: "disabled",
    caret: "hide",
    scale: "css",
    timeout: 15_000,
  });
}
