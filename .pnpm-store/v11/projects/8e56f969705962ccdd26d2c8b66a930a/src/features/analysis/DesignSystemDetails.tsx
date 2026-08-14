import { useMemo, useState } from 'react'
import type { DesignSystem } from './analysisApi'

type Tab = 'overview' | 'colors' | 'spacing' | 'typography' | 'radius' | 'shadow' | 'variables'

function copy(value: string) {
  void navigator.clipboard?.writeText(value)
}

function numberFromCss(value: string) {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function pickColor(data: DesignSystem, roles: string[], fallback = '—') {
  return [...data.colors]
    .filter((color) => roles.includes(color.role) || roles.some((role) => color.roleCounts?.[role]))
    .sort((a, b) => b.prominenceScore - a.prominenceScore || b.usageCount - a.usageCount)[0]?.value ?? fallback
}

export function DesignSystemDetails({ data, sourceUrl }: { data: DesignSystem; sourceUrl?: string }) {
  const [tab, setTab] = useState<Tab>('overview')
  const spacingGroups = useMemo(() => ({
    padding: data.spacing.filter((item) => item.properties.some((property) => property.toLowerCase().includes('padding'))),
    margin: data.spacing.filter((item) => item.properties.some((property) => property.toLowerCase().includes('margin'))),
    gap: data.spacing.filter((item) => item.properties.some((property) => property.toLowerCase().includes('gap'))),
    other: data.spacing.filter((item) => !item.properties.some((property) => /padding|margin|gap/i.test(property))),
  }), [data.spacing])

  const tabs: Array<{ id: Tab; label: string; count: number }> = [
    { id: 'overview', label: 'Tổng quan chuẩn hóa', count: 0 },
    { id: 'colors', label: 'Màu sắc', count: data.colors.length },
    { id: 'spacing', label: 'Spacing', count: data.spacing.length },
    { id: 'typography', label: 'Typography', count: data.typography.length },
    { id: 'radius', label: 'Radius', count: data.radii.length },
    { id: 'shadow', label: 'Shadow', count: data.shadows.length },
    { id: 'variables', label: 'CSS Variables', count: data.cssVariables.length },
  ]

  const normalized = useMemo(() => {
    const sortedType = [...data.typography].sort((a, b) => numberFromCss(b.fontSize) - numberFromCss(a.fontSize) || b.usageCount - a.usageCount)
    const mono = data.typography.find((item) => /mono|consolas|courier|code/i.test(item.fontFamily))
    const body = [...data.typography].sort((a, b) => b.usageCount - a.usageCount)[0]
    const spacingScale = [...new Set(data.spacing.map((item) => item.pixels).filter((value) => value > 0))].sort((a, b) => a - b)
    const radiusScale = [...data.radii].filter((item) => item.pixels != null).sort((a, b) => (a.pixels ?? 0) - (b.pixels ?? 0))
    return {
      colors: {
        primary: pickColor(data, ['accent', 'link']),
        background: pickColor(data, ['background']),
        surface: pickColor(data, ['surface', 'background'], data.colors[1]?.value ?? '—'),
        border: pickColor(data, ['border', 'outline']),
        text: pickColor(data, ['text', 'heading']),
        textMuted: [...data.colors].filter((item) => item.role === 'text').sort((a, b) => a.prominenceScore - b.prominenceScore)[0]?.value ?? '—',
      },
      type: { display: sortedType[0], heading: sortedType[1] ?? sortedType[0], body, mono },
      spacingScale,
      base: spacingScale[0] ?? 0,
      radii: { sm: radiusScale[0], md: radiusScale[Math.min(1, radiusScale.length - 1)], lg: radiusScale[Math.max(radiusScale.length - 2, 0)], xl: radiusScale[radiusScale.length - 1] },
    }
  }, [data])

  return <div className="design-details">
    <div className="details-tabs" role="tablist" aria-label="Chi tiết Design System">
      {tabs.map((item) => <button type="button" role="tab" aria-selected={tab === item.id} className={tab === item.id ? 'active' : ''} key={item.id} onClick={() => setTab(item.id)}>{item.label}{item.count > 0 && <b>{item.count}</b>}</button>)}
    </div>

    {tab === 'overview' && <div className="explicit-summary">
      <section><div className="summary-heading"><span>01</span><div><h3>Color system</h3><p>Màu được gán vai trò dựa trên context và mức độ nổi bật đo được.</p></div></div><div className="semantic-token-grid">{Object.entries(normalized.colors).map(([name, value]) => <article key={name}><i style={{ background: value }} /><div><code>{name.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}</code><strong>{value}</strong></div><button type="button" onClick={() => copy(value)}>Sao chép</button></article>)}</div></section>
      <section><div className="summary-heading"><span>02</span><div><h3>Typography hierarchy</h3><p>Các style tiêu biểu được phân cấp theo kích thước và tần suất sử dụng.</p></div></div><div className="type-hierarchy">{Object.entries(normalized.type).map(([role, item]) => item && <article key={role}><span>{role}</span><div className="hierarchy-preview" style={{ fontFamily: item.fontFamily, fontWeight: item.fontWeight }}>Ag</div><dl><div><dt>Font</dt><dd>{item.fontFamily}</dd></div><div><dt>Size</dt><dd>{item.fontSize}</dd></div><div><dt>Weight</dt><dd>{item.fontWeight}</dd></div><div><dt>Line height</dt><dd>{item.lineHeight || 'normal'}</dd></div></dl></article>)}</div></section>
      <section><div className="summary-heading"><span>03</span><div><h3>Spacing scale</h3><p>Base unit và toàn bộ thang đo, dùng chung cho padding, margin và gap.</p></div></div><div className="scale-summary"><div><small>BASE UNIT</small><strong>{normalized.base}px</strong></div><div><small>SCALE</small><p>[{normalized.spacingScale.join(', ')}]</p></div></div></section>
      <section><div className="summary-heading"><span>04</span><div><h3>Radius scale</h3><p>Các giá trị đại diện được đặt tên từ nhỏ đến lớn.</p></div></div><div className="radius-scale">{Object.entries(normalized.radii).map(([name, item]) => item && <article key={name}><div style={{ borderRadius: item.value }} /><code>{name}</code><strong>{item.value}</strong></article>)}</div></section>
      <section className="source-summary"><span>Nguồn phân tích</span><strong>{sourceUrl || 'Website hiện tại'}</strong><small>{data.pageCount} trang · {data.colors.length} màu · {data.typography.length} typography · {data.spacing.length} spacing</small></section>
    </div>}

    {tab === 'colors' && <div className="color-detail-grid">{data.colors.map((color, index) => <article className="color-card" key={`${color.value}-${index}`}>
      <button type="button" className="color-surface" style={{ background: color.value }} onClick={() => copy(color.value)} aria-label={`Sao chép màu ${color.value}`}><span>Nhấn để sao chép</span></button>
      <div className="color-meta"><strong>{color.value}</strong><span className="role-chip">{color.role || 'unknown'}</span><small>{color.usageCount} lần dùng · {Math.round(color.pageCoverage * 100)}% trang</small>{color.contexts?.length > 0 && <em>{color.contexts.slice(0, 3).join(' · ')}</em>}</div>
    </article>)}</div>}

    {tab === 'spacing' && <div className="spacing-sections">{Object.entries(spacingGroups).map(([group, items]) => items.length > 0 && <section key={group}>
      <div className="subsection-title"><h3>{group === 'other' ? 'Khác' : group}</h3><span>{items.length} giá trị</span></div>
      <div className="spacing-grid">{items.map((item, index) => <article key={`${group}-${item.value}-${index}`}>
        <div className="space-visual"><i style={{ width: `${Math.min(Math.max(item.pixels, 2), 160)}px` }} /></div>
        <div><strong>{item.value}</strong><small>{item.pixels}px · {item.usageCount} lần dùng</small><span>{item.properties.join(', ')}</span></div>
      </article>)}</div>
    </section>)}</div>}

    {tab === 'typography' && <div className="detail-table-wrap"><table className="detail-table"><thead><tr><th>Preview</th><th>Font family</th><th>Size</th><th>Weight</th><th>Line height</th><th>Sử dụng</th></tr></thead><tbody>{data.typography.map((type, index) => <tr key={`${type.fontFamily}-${type.fontSize}-${type.fontWeight}-${index}`}><td><span className="type-preview" style={{ fontFamily: type.fontFamily, fontSize: type.fontSize, fontWeight: type.fontWeight }}>Aa</span></td><td><strong>{type.fontFamily}</strong></td><td>{type.fontSize}</td><td>{type.fontWeight}</td><td>{type.lineHeight || '—'}</td><td>{type.usageCount}</td></tr>)}</tbody></table></div>}

    {tab === 'radius' && <div className="token-card-grid">{data.radii.map((radius, index) => <article key={`${radius.value}-${index}`}><div className="radius-preview" style={{ borderRadius: radius.value }} /><strong>{radius.value}</strong><small>{radius.pixels ?? '—'}px · {radius.usageCount} lần</small><span>{radius.corners?.join(', ')}</span></article>)}</div>}

    {tab === 'shadow' && <div className="token-card-grid">{data.shadows.map((shadow, index) => <article key={`${shadow.value}-${index}`}><div className="shadow-preview" style={{ boxShadow: shadow.value }} /><strong>{shadow.value}</strong><small>{shadow.usageCount} lần · {shadow.pageCount} trang</small></article>)}</div>}

    {tab === 'variables' && <div className="detail-table-wrap"><table className="detail-table"><thead><tr><th>Tên biến</th><th>Giá trị</th><th>Số trang</th><th></th></tr></thead><tbody>{data.cssVariables.flatMap((variable) => variable.variants.map((variant, index) => <tr key={`${variable.name}-${index}`}><td><code>{variable.name}</code></td><td><code>{variant.value}</code></td><td>{variant.pageCount}</td><td><button className="copy-button" type="button" onClick={() => copy(`${variable.name}: ${variant.value};`)}>Sao chép</button></td></tr>))}</tbody></table></div>}
  </div>
}
