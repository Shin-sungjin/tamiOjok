import { CATEGORICAL_COLORS } from './chartPalette'

export interface BarChartDatum {
  label: string
  value: number
}

export function BarChart({ data, unit = '건' }: { data: BarChartDatum[]; unit?: string }) {
  const max = Math.max(1, ...data.map((d) => d.value))

  return (
    <div className="chart chart--bar">
      {data.map((d, i) => {
        const widthPercent = (d.value / max) * 100
        const color = CATEGORICAL_COLORS[i % CATEGORICAL_COLORS.length]
        return (
          <div key={d.label} className="chart-bar-row">
            <span className="chart-bar-row__label">{d.label}</span>
            <div className="chart-bar-row__track">
              <div
                className="chart-bar-row__fill"
                style={{ width: `${widthPercent}%`, background: color }}
              />
            </div>
            <span className="chart-bar-row__value">
              {d.value.toLocaleString()}
              {unit}
            </span>
          </div>
        )
      })}
      {data.every((d) => d.value === 0) && <p className="chart__empty">데이터가 없습니다.</p>}
    </div>
  )
}
