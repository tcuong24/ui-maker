import { apiRequest, apiText } from '../../services/apiClient'

export type AnalysisStatus =
  | 'PENDING' | 'CRAWLING' | 'CRAWL_COMPLETED' | 'ANALYZING'
  | 'GENERATING_MARKDOWN' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

export interface AnalysisDetail {
  id: string
  websiteUrl: string
  additionalPaths: string[]
  includeScreenshot: boolean
  status: AnalysisStatus
  progress: number
  errorCode: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
}

export interface AnalysisCreated {
  id: string
  status: AnalysisStatus
  progress: number
  cacheHit: boolean
  sourceAnalysisId: string | null
}

export interface DesignSystem {
  pageCount: number
  colors: Array<{
    value: string
    usageCount: number
    visualArea: number
    pageCount: number
    pageCoverage: number
    prominenceScore: number
    role: 'background' | 'accent' | 'heading' | 'link' | 'text' | 'border' | 'outline' | 'shadow' | 'unknown'
    contexts: string[]
    elements: string[]
    roleCounts: Record<string, number>
    pageUrls: string[]
  }>
  typography: Array<{ fontFamily: string; fontSize: string; fontWeight: string; lineHeight: string; letterSpacing: string; usageCount: number; pageCount: number; pageCoverage: number; pageUrls: string[] }>
  spacing: Array<{ value: string; pixels: number; usageCount: number; pageCount: number; pageCoverage: number; properties: string[]; contexts: string[]; pageUrls: string[] }>
  radii: Array<{ value: string; pixels: number | null; usageCount: number; pageCount: number; pageCoverage: number; corners: string[]; contexts: string[]; pageUrls: string[] }>
  shadows: Array<{ value: string; usageCount: number; pageCount: number; pageCoverage: number; contexts: string[]; pageUrls: string[] }>
  cssVariables: Array<{ name: string; variants: Array<{ value: string; pageCount: number; pageCoverage: number; pageUrls: string[] }> }>
}

export function createAnalysis(websiteUrl: string, forceRefresh = false) {
  return apiRequest<AnalysisCreated>('/analyses', {
    method: 'POST',
    body: JSON.stringify({ websiteUrl, additionalPaths: [], includeScreenshot: true, forceRefresh }),
  })
}

export function regenerateArtifact(id: string) {
  return apiRequest<AnalysisCreated>(`/analyses/${id}/artifact/regenerate`, {
    method: 'POST',
  })
}

export function getAnalysis(id: string, signal?: AbortSignal) {
  return apiRequest<AnalysisDetail>(`/analyses/${id}`, { signal })
}

export function getDesignSystem(id: string) {
  return apiRequest<DesignSystem>(`/analyses/${id}/design-system`)
}

export function getMarkdown(id: string) {
  return apiText(`/analyses/${id}/report.md`)
}
