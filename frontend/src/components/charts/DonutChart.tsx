import { CATEGORICAL_COLORS } from './chartPalette'

export interface DonutDatum {
  label: string
  value: number
}

const SIZE = 160
const STROKE = 26
const RADIUS = (SIZE - STROKE) / 2
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const GAP_DEG = 2.5

export function DonutChart({ data, unit = '건' }: { data: DonutDatum[]; unit?: string }) {
  const total = data.reduce((sum, d) => sum + d.value, 0)

  let cursorDeg = -90
  const segments = data.map((d, i) => {
    const fraction = total > 0 ? d.value / total : 0
    const sweepDeg = Math.max(0, fraction * 360 - (total > 0 ? GAP_DEG : 0))
    const segment = {
      ...d,
      color: CATEGORICAL_COLORS[i % CATEGORICAL_COLORS.length],
      startDeg: cursorDeg,
      sweepDeg,
      percent: total > 0 ? Math.round(fraction * 100) : 0,
    }
    cursorDeg += fraction * 360
    return segment
  })

  return (
    <div className="chart chart--donut">
      <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`} role="img" aria-label="배송 현황 도넛 차트">
        <circle cx={SIZE / 2} cy={SIZE / 2} r={RADIUS} fill="none" stroke="#e8dcc6" strokeWidth={STROKE} />
        {total > 0 &&
          segments.map((s) => {
            const dash = (s.sweepDeg / 360) * CIRCUMFERENCE
            const gap = CIRCUMFERENCE - dash
            const rotation = s.startDeg
            return (
              <circle
                key={s.label}
                cx={SIZE / 2}
                cy={SIZE / 2}
                r={RADIUS}
                fill="none"
                stroke={s.color}
                strokeWidth={STROKE}
                strokeDasharray={`${dash} ${gap}`}
                strokeLinecap="round"
                transform={`rotate(${rotation} ${SIZE / 2} ${SIZE / 2})`}
              />
            )
          })}
        <text x="50%" y="47%" textAnchor="middle" className="chart-donut__total-value">
          {total.toLocaleString()}
        </text>
        <text x="50%" y="60%" textAnchor="middle" className="chart-donut__total-label">
          전체 {unit}
        </text>
      </svg>

      <ul className="chart-legend">
        {segments.map((s) => (
          <li key={s.label} className="chart-legend__item">
            <span className="chart-legend__swatch" style={{ background: s.color }} />
            <span className="chart-legend__label">{s.label}</span>
            <span className="chart-legend__value">
              {s.value.toLocaleString()}
              {unit} ({s.percent}%)
            </span>
          </li>
        ))}
      </ul>
      {total === 0 && <p className="chart__empty">데이터가 없습니다.</p>}
    </div>
  )
}
