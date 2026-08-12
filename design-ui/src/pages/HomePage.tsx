import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { createRoute } from '../routes/routePaths'

export function HomePage() {
  const [url, setUrl] = useState('')
  const navigate = useNavigate()

  function startAnalysis(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!url.trim()) return
    navigate(createRoute.analysis('demo-analysis'))
  }

  return (
    <section className="home-page">
      <span className="eyebrow">DESIGN INTELLIGENCE</span>
      <h1>Khám phá Design System của bất kỳ website nào.</h1>
      <p>Nhập URL để thu thập các trang, phân tích màu sắc, typography, spacing, component và tự động sinh báo cáo Markdown.</p>
      <form className="analysis-form" onSubmit={startAnalysis}>
        <input value={url} onChange={(event) => setUrl(event.target.value)} placeholder="https://example.com" aria-label="Địa chỉ website cần phân tích" />
        <button type="submit">Bắt đầu phân tích <span>→</span></button>
      </form>
      <div className="flow-preview" aria-label="Quy trình phân tích">
        <div><b>01</b><strong>Thu thập</strong><span>Quét các trang website</span></div>
        <i>→</i>
        <div><b>02</b><strong>Phân tích</strong><span>Trích xuất design tokens</span></div>
        <i>→</i>
        <div><b>03</b><strong>Tạo tài liệu</strong><span>Xuất báo cáo Markdown</span></div>
      </div>
    </section>
  )
}
