import { Link } from 'react-router-dom'
import { ROUTES } from '../routes/routePaths'

export function NotFoundPage() {
  return (
    <main className="centered-page">
      <section className="page-card auth-card">
        <span className="eyebrow">404</span>
        <h1>Không tìm thấy trang</h1>
        <p>Đường dẫn bạn truy cập không tồn tại.</p>
        <Link className="primary-link" to={ROUTES.home}>Phân tích website</Link>
      </section>
    </main>
  )
}
