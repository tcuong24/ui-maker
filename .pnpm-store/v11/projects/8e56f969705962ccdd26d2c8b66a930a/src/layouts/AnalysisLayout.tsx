import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { ROUTES } from '../routes/routePaths'

export function AnalysisLayout() {
  const location = useLocation()
  const hasAnalysisHistory = Boolean(localStorage.getItem('current_analysis_id'))
  const showNewAnalysis = hasAnalysisHistory && location.pathname !== ROUTES.home

  return (
    <div className="analysis-app">
      <header className="app-header">
        <NavLink className="brand" to={ROUTES.home} aria-label="Trang chủ ui-maker">
          <span className="brand-mark" aria-hidden="true">U</span>
          ui-maker
        </NavLink>
        <span className="header-caption">Website design analyzer</span>
        {showNewAnalysis && (
          <NavLink className="new-analysis-link" to={ROUTES.home}>＋ Phân tích website mới</NavLink>
        )}
      </header>
      <main className="app-main"><Outlet /></main>
      <footer className="app-footer" id="about">
        <div className="footer-brand">
          <strong>ui-maker</strong>
          <span>Phân tích website và tạo tài liệu Design System.</span>
        </div>
        <nav className="footer-links" id="docs" aria-label="Liên kết chân trang">
          <a href="#about">About</a>
          <a href="#docs">Docs</a>
          <a href="mailto:hello@ui-maker.app">Liên hệ</a>
        </nav>
      </footer>
    </div>
  )
}
