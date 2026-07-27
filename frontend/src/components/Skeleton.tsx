interface SkeletonLineProps {
  width?: string
  height?: string
}

export function SkeletonLine({ width = '100%', height = '0.9em' }: SkeletonLineProps) {
  return <span className="skeleton skeleton-line" style={{ width, height }} />
}

export function ProductCardSkeleton() {
  return (
    <li className="product-card" aria-hidden="true">
      <div className="product-card__image skeleton" />
      <SkeletonLine width="85%" height="0.95rem" />
      <SkeletonLine width="40%" height="0.82rem" />
      <SkeletonLine width="55%" height="1.1rem" />
    </li>
  )
}

export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <ul className="product-grid" aria-hidden="true">
      {Array.from({ length: count }, (_, index) => (
        <ProductCardSkeleton key={index} />
      ))}
    </ul>
  )
}

export function CardListSkeleton({ count = 3 }: { count?: number }) {
  return (
    <ul className="list" aria-hidden="true">
      {Array.from({ length: count }, (_, index) => (
        <li key={index} className="card">
          <SkeletonLine width="50%" height="1rem" />
          <SkeletonLine width="85%" />
          <SkeletonLine width="35%" height="0.8rem" />
        </li>
      ))}
    </ul>
  )
}

export function TableSkeleton({ rows = 5, columns = 4 }: { rows?: number; columns?: number }) {
  return (
    <div className="table-scroll" aria-hidden="true">
      <table className="table">
        <tbody>
          {Array.from({ length: rows }, (_, r) => (
            <tr key={r}>
              {Array.from({ length: columns }, (_, c) => (
                <td key={c}>
                  <SkeletonLine width={c === 0 ? '90%' : '60%'} />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function StatGridSkeleton({ count = 4 }: { count?: number }) {
  return (
    <div className="stat-grid" aria-hidden="true">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="stat-tile">
          <SkeletonLine width="60%" height="0.82rem" />
          <SkeletonLine width="45%" height="1.3rem" />
        </div>
      ))}
    </div>
  )
}
