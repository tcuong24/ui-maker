import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getMarkdown, regenerateArtifact } from '../features/analysis/analysisApi'
import { createRoute } from '../routes/routePaths'

export function MarkdownReportPage() {
  const { analysisId = '' } = useParams()
  const navigate = useNavigate()
  const [markdown, setMarkdown] = useState('Đang tải Agent Design Contract từ Design API...')
  const [error, setError] = useState('')
  const [regenerating, setRegenerating] = useState(false)

  useEffect(() => {
    if (!analysisId) return
    getMarkdown(analysisId).then(setMarkdown).catch((reason) => {
      setError(reason instanceof Error ? reason.message : 'Không thể tải Agent Design Contract')
    })
  }, [analysisId])

  function downloadReport() {
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
    const anchor = document.createElement('a')
    anchor.href = URL.createObjectURL(blob)
    anchor.download = `ui-maker-agent-contract-${analysisId}.md`
    anchor.click()
    URL.revokeObjectURL(anchor.href)
  }

  async function regenerate() {
    if (!analysisId) return
    setRegenerating(true)
    setError('')
    try {
      await regenerateArtifact(analysisId)
      navigate(createRoute.analysis(analysisId))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể tạo lại artifact')
      setRegenerating(false)
    }
  }

  return <section className="report-page">
    <div className="page-heading"><div><span className="eyebrow">AGENT-READY MARKDOWN</span><h1>Agent Design Contract</h1><p>Dữ liệu thiết kế đã được tổng hợp, xếp hạng và chuẩn hóa để coding agent sử dụng.</p></div><div><button className="primary-button" onClick={regenerate} disabled={regenerating}>{regenerating ? 'Đang tạo lại...' : '↻ Tạo lại từ dữ liệu đã lưu'}</button> <button className="primary-button" onClick={downloadReport} disabled={Boolean(error)}>↓ Tải file .md</button></div></div>
    {error && <div className="api-error" role="alert"><strong>Không thể tải báo cáo</strong><span>{error}</span></div>}
    <div className="report-editor"><div className="editor-bar"><span>ui-maker-agent-contract.md</span><Link to={createRoute.analysis(analysisId)}>← Quay lại kết quả</Link></div><pre>{error || markdown}</pre></div>
  </section>
}
