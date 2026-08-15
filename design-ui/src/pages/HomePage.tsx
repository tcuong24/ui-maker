import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { createRoute } from '../routes/routePaths'
import { createAnalysis } from '../features/analysis/analysisApi'

export function HomePage() {
  const [url, setUrl] = useState('')
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [forceRefresh, setForceRefresh] = useState(false)

  async function startAnalysis(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!url.trim()) return
    setSubmitting(true)
    setError('')
    try {
      const normalizedUrl = /^https?:\/\//i.test(url.trim()) ? url.trim() : `https://${url.trim()}`
      const created = await createAnalysis(normalizedUrl, forceRefresh)
      localStorage.setItem('current_analysis_id', created.id)
      navigate(createRoute.demoAnalysis(created.id))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể bắt đầu phân tích')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="home-page">
      <span className="eyebrow">DESIGN INTELLIGENCE</span>
      <h1>Khám phá Design System của bất kỳ website nào.</h1>
      <p>Nhập URL để thu thập các trang, phân tích màu sắc, typography, spacing, component và tự động sinh báo cáo Markdown.</p>
      <form className="analysis-form" onSubmit={startAnalysis}>
        <input value={url} onChange={(event) => setUrl(event.target.value)} placeholder="https://example.com" aria-label="Địa chỉ website cần phân tích" />
        <button type="submit" disabled={submitting}>{submitting ? 'Đang gửi...' : 'Bắt đầu phân tích'} <span>→</span></button>
      </form>
      <label className="force-refresh-option">
        <input type="checkbox" checked={forceRefresh} onChange={(event) => setForceRefresh(event.target.checked)} />
        Bỏ qua cache và phân tích lại website
      </label>
      {error && <p className="form-error" role="alert">{error}</p>}
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
