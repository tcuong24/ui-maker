import { Link, useParams } from 'react-router-dom'
import { createRoute } from '../routes/routePaths'

const steps = [
  { label: 'Kiểm tra URL', detail: 'URL hợp lệ và có thể truy cập', state: 'done' },
  { label: 'Thu thập các trang', detail: 'Đã quét 8 / 11 trang', state: 'done' },
  { label: 'Trích xuất Design System', detail: 'Đang tổng hợp màu sắc và typography', state: 'active' },
  { label: 'Sinh báo cáo Markdown', detail: 'Đang chờ bước phân tích hoàn tất', state: 'waiting' },
]

export function AnalysisPage() {
  const { analysisId = '' } = useParams()

  return (
    <section className="analysis-page">
      <div className="page-heading">
        <div><span className="eyebrow">ANALYSIS ID: {analysisId}</span><h1>linear.app</h1><p>https://linear.app</p></div>
        <span className="running-badge"><i /> Đang phân tích</span>
      </div>

      <div className="progress-card">
        <div className="progress-summary"><div><strong>Tiến trình phân tích</strong><span>Có thể mất từ 1–3 phút</span></div><b>68%</b></div>
        <div className="progress-track"><i /></div>
        <div className="steps-list">
          {steps.map((step) => <div className={`process-step ${step.state}`} key={step.label}><span className="step-icon">{step.state === 'done' ? '✓' : step.state === 'active' ? '●' : '○'}</span><div><strong>{step.label}</strong><small>{step.detail}</small></div></div>)}
        </div>
      </div>

      <div className="result-grid">
        <article><span>MÀU SẮC</span><strong>24</strong><small>8 màu chính</small></article>
        <article><span>TYPOGRAPHY</span><strong>Inter</strong><small>12 text styles</small></article>
        <article><span>COMPONENT</span><strong>36</strong><small>Đã nhận diện</small></article>
        <article><span>TRANG ĐÃ QUÉT</span><strong>8</strong><small>Trên tổng số 11</small></article>
      </div>

      <section className="data-preview">
        <div><span className="eyebrow">KẾT QUẢ TẠM THỜI</span><h2>Dữ liệu đã phân tích</h2></div>
        <div className="token-row"><strong>Primary colors</strong><div className="color-list"><i style={{ background: '#5e6ad2' }} /><i style={{ background: '#8a8f98' }} /><i style={{ background: '#1c1c1f' }} /><i style={{ background: '#f7f8f8' }} /></div></div>
        <div className="token-row"><strong>Typography</strong><span>Inter · 12px — 48px · 400 / 500 / 600</span></div>
        <div className="token-row"><strong>Spacing scale</strong><span>4 · 8 · 12 · 16 · 24 · 32 · 48 · 64px</span></div>
        <Link className="primary-link" to={createRoute.markdownReport(analysisId)}>Xem báo cáo Markdown</Link>
      </section>
    </section>
  )
}
