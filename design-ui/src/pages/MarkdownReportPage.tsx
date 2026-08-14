import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getMarkdown } from '../features/analysis/analysisApi'
import { createRoute } from '../routes/routePaths'

export function MarkdownReportPage() {
  const { analysisId = '' } = useParams()
  const [markdown, setMarkdown] = useState('Đang tải báo cáo Markdown từ Design API...')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!analysisId) return
    getMarkdown(analysisId).then(setMarkdown).catch((reason) => {
      setError(reason instanceof Error ? reason.message : 'Không thể tải báo cáo')
    })
  }, [analysisId])

  function downloadReport() {
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
    const anchor = document.createElement('a')
    anchor.href = URL.createObjectURL(blob)
    anchor.download = `design-system-${analysisId}.md`
    anchor.click()
    URL.revokeObjectURL(anchor.href)
  }

  return <section className="report-page">
    <div className="page-heading"><div><span className="eyebrow">MARKDOWN REPORT</span><h1>Design System Document</h1><p>Báo cáo thực được sinh từ dữ liệu đã phân tích.</p></div><button className="primary-button" onClick={downloadReport} disabled={Boolean(error)}>↓ Tải file .md</button></div>
    {error && <div className="api-error" role="alert"><strong>Không thể tải báo cáo</strong><span>{error}</span></div>}
    <div className="report-editor"><div className="editor-bar"><span>design-system.md</span><Link to={createRoute.analysis(analysisId)}>← Quay lại kết quả</Link></div><pre>{error || markdown}</pre></div>
  </section>
}
