import { useEffect, useMemo, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { getDesignSystem, type DesignSystem } from '../features/analysis/analysisApi'
import { DesignSystemDetails } from '../features/analysis/DesignSystemDetails'
import { useAnalysisProgress } from '../features/analysis/useAnalysisProgress'
import { createRoute } from '../routes/routePaths'

const statusLabels: Record<string, string> = {
  PENDING: 'Đang chờ', CRAWLING: 'Đang thu thập website', CRAWL_COMPLETED: 'Đã thu thập xong',
  ANALYZING: 'Đang phân tích', GENERATING_MARKDOWN: 'Đang sinh Markdown', COMPLETED: 'Hoàn tất',
  FAILED: 'Phân tích thất bại', CANCELLED: 'Đã hủy',
}

export function AnalysisPage() {
  const params = useParams()
  const [searchParams] = useSearchParams()
  const analysisId = params.analysisId || searchParams.get('id') || localStorage.getItem('current_analysis_id') || ''
  const { analysis, error } = useAnalysisProgress(analysisId)
  const [designSystem, setDesignSystem] = useState<DesignSystem | null>(null)
  const [resultError, setResultError] = useState('')

  useEffect(() => {
    if (!analysisId || analysis?.status !== 'COMPLETED') return
    getDesignSystem(analysisId).then(setDesignSystem).catch((reason) =>
      setResultError(reason instanceof Error ? reason.message : 'Không thể tải Design System'))
  }, [analysisId, analysis?.status])

  const hostname = useMemo(() => {
    try { return analysis ? new URL(analysis.websiteUrl).hostname : 'Đang tải...' }
    catch { return analysis?.websiteUrl ?? 'Đang tải...' }
  }, [analysis])

  if (!analysisId) return <section className="page-card"><h1>Chưa có phiên phân tích</h1><p>Hãy nhập URL ở trang chủ để bắt đầu.</p><Link className="primary-link" to="/">Về trang chủ</Link></section>

  const progress = Math.max(0, Math.min(100, analysis?.progress ?? 0))
  const status = analysis?.status ?? 'PENDING'
  const crawlDone = progress >= 60
  const analyzeDone = status === 'GENERATING_MARKDOWN' || status === 'COMPLETED'
  const markdownDone = status === 'COMPLETED'
  const steps = [
    { label: 'Kiểm tra URL', detail: analysis ? 'URL hợp lệ và đã được tiếp nhận' : 'Đang kết nối Design API', state: analysis ? 'done' : 'active' },
    { label: 'Thu thập các trang', detail: crawlDone ? 'Đã hoàn tất thu thập website' : 'Worker đang quét các trang', state: crawlDone ? 'done' : status === 'CRAWLING' ? 'active' : 'waiting' },
    { label: 'Trích xuất Design System', detail: analyzeDone ? 'Đã tổng hợp dữ liệu thiết kế' : 'Màu sắc, typography và tokens', state: analyzeDone ? 'done' : status === 'ANALYZING' ? 'active' : 'waiting' },
    { label: 'Sinh báo cáo Markdown', detail: markdownDone ? 'File Markdown đã sẵn sàng' : 'Chờ kết quả phân tích', state: markdownDone ? 'done' : status === 'GENERATING_MARKDOWN' ? 'active' : 'waiting' },
  ]

  return <section className="analysis-page">
    <div className="page-heading"><div><span className="eyebrow">ANALYSIS ID: {analysisId}</span><h1>{hostname}</h1><p>{analysis?.websiteUrl ?? 'Đang lấy dữ liệu từ Design API...'}</p></div><span className={`running-badge status-${status.toLowerCase()}`}><i /> {statusLabels[status]}</span></div>
    {(error || analysis?.errorMessage) && <div className="api-error" role="alert"><strong>Không thể tiếp tục phân tích</strong><span>{analysis?.errorMessage || error}</span></div>}
    <div className="progress-card">
      <div className="progress-summary"><div><strong>Tiến trình phân tích</strong><span>Cập nhật tự động mỗi 1,5 giây</span></div><b>{progress}%</b></div>
      <div className="progress-track"><i style={{ width: `${progress}%` }} /></div>
      <div className="steps-list">{steps.map((step) => <div className={`process-step ${step.state}`} key={step.label}><span className="step-icon">{step.state === 'done' ? '✓' : step.state === 'active' ? '●' : '○'}</span><div><strong>{step.label}</strong><small>{step.detail}</small></div></div>)}</div>
    </div>
    <div className="result-grid">
      <article><span>MÀU SẮC</span><strong>{designSystem?.colors?.length ?? '—'}</strong><small>{designSystem ? 'Màu được phát hiện' : 'Chờ hoàn tất'}</small></article>
      <article><span>TYPOGRAPHY</span><strong>{designSystem?.typography?.[0]?.fontFamily ?? '—'}</strong><small>{designSystem ? `${designSystem.typography.length} styles` : 'Chờ hoàn tất'}</small></article>
      <article><span>DESIGN TOKENS</span><strong>{designSystem ? designSystem.spacing.length + designSystem.radii.length + designSystem.shadows.length + designSystem.cssVariables.length : '—'}</strong><small>Spacing, radius, shadow, CSS</small></article>
      <article><span>TRANG ĐÃ QUÉT</span><strong>{designSystem?.pageCount ?? '—'}</strong><small>Từ dữ liệu thực</small></article>
    </div>
    <section className="data-preview"><div><span className="eyebrow">KẾT QUẢ PHÂN TÍCH</span><h2>Dữ liệu Design System</h2></div>
      {!designSystem && !resultError && <p className="result-note">Kết quả sẽ tự động hiển thị khi trạng thái chuyển sang Hoàn tất.</p>}
      {resultError && <p className="form-error">{resultError}</p>}
      {designSystem && <DesignSystemDetails data={designSystem} sourceUrl={analysis?.websiteUrl} />}
      {status === 'COMPLETED' && <Link className="primary-link" to={createRoute.markdownReport(analysisId)}>Xem báo cáo Markdown</Link>}
    </section>
  </section>
}
