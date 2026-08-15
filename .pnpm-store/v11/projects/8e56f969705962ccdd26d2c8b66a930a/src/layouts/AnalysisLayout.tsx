import { NavLink, Outlet } from 'react-router-dom'
import { ROUTES } from '../routes/routePaths'

export function AnalysisLayout() {
  return (
    <div className="analysis-app">
      <header className="app-header">
        <NavLink className="brand" to={ROUTES.home}>
          <span className="brand-mark">U</span>
          ui-maker
        </NavLink>
        <span className="header-caption">Website design analyzer</span>
        <NavLink className="new-analysis-link" to={ROUTES.home}>＋ Phân tích website mới</NavLink>
      </header>
      <main className="app-main"><Outlet /></main>
      <footer className="app-footer">
        <strong>ui-maker</strong>
        <span>Phân tích website và tạo tài liệu Design System.</span>
      </footer>
    </div>
  )
}
