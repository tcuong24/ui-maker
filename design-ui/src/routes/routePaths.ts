export const ROUTES = {
  home: '/',
  demoAnalysis: '/demo-analisis',
  analysis: '/analyses/:analysisId',
  markdownReport: '/analyses/:analysisId/report',
} as const

export const createRoute = {
  demoAnalysis: (analysisId: string) => `/demo-analisis?id=${encodeURIComponent(analysisId)}`,
  analysis: (analysisId: string) => `/analyses/${analysisId}`,
  markdownReport: (analysisId: string) => `/analyses/${analysisId}/report`,
}
