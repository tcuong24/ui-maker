import { RouterProvider, createBrowserRouter } from 'react-router-dom'
import { AnalysisLayout } from '../layouts/AnalysisLayout'
import { AnalysisPage } from '../pages/AnalysisPage'
import { HomePage } from '../pages/HomePage'
import { MarkdownReportPage } from '../pages/MarkdownReportPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ROUTES } from './routePaths'

const router = createBrowserRouter([
  {
    element: <AnalysisLayout />,
    children: [
      { path: ROUTES.home, element: <HomePage /> },
      { path: ROUTES.demoAnalysis, element: <AnalysisPage /> },
      { path: ROUTES.analysis, element: <AnalysisPage /> },
      { path: ROUTES.markdownReport, element: <MarkdownReportPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

export function AppRoutes() {
  return <RouterProvider router={router} />
}
