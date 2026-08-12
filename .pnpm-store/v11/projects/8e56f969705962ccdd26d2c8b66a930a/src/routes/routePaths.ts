export const ROUTES = {
  home: '/',
  analysis: '/analyses/:analysisId',
  markdownReport: '/analyses/:analysisId/report',
} as const

export const createRoute = {
  analysis: (analysisId: string) => `/analyses/${analysisId}`,
  markdownReport: (analysisId: string) => `/analyses/${analysisId}/report`,
}
