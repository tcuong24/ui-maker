import { Link, useParams } from 'react-router-dom'
import { createRoute } from '../routes/routePaths'

const markdown = `# Linear Design System

> Generated automatically by DesignScope

## Colors

| Token | Value | Usage |
| --- | --- | --- |
| Primary | #5E6AD2 | Buttons, links, active states |
| Surface | #F7F8F8 | Page background |
| Text | #1C1C1F | Primary content |

## Typography

- **Font family:** Inter
- **Heading:** 48px / 600
- **Body:** 14px / 400

## Spacing

Base unit: 4px`

export function MarkdownReportPage() {
  const { analysisId = '' } = useParams()

  function downloadReport() {
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
    const anchor = document.createElement('a')
    anchor.href = URL.createObjectURL(blob)
    anchor.download = `design-system-${analysisId}.md`
    anchor.click()
    URL.revokeObjectURL(anchor.href)
  }

  return (
    <section className="report-page">
      <div className="page-heading"><div><span className="eyebrow">MARKDOWN REPORT</span><h1>Design System Document</h1><p>Báo cáo được sinh từ dữ liệu đã phân tích.</p></div><button className="primary-button" onClick={downloadReport}>↓ Tải file .md</button></div>
      <div className="report-editor"><div className="editor-bar"><span>design-system.md</span><Link to={createRoute.analysis(analysisId)}>← Quay lại kết quả</Link></div><pre>{markdown}</pre></div>
    </section>
  )
}
