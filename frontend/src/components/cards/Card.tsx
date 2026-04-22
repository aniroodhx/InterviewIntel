import { ReactNode } from 'react'

interface CardProps {
  icon: string
  title: string
  subtitle?: string
  badge?: ReactNode
  children: ReactNode
}

export function Card({ icon, title, subtitle, badge, children }: CardProps) {
  return (
    <div
      className="rounded-xl p-5"
      style={{ background: '#1a1a1a', border: '0.5px solid rgba(255,255,255,0.08)' }}
    >
      {/* Header */}
      <div className="flex items-center gap-3 mb-4">
        <div
          className="w-8 h-8 rounded-lg flex items-center justify-center text-sm flex-shrink-0"
          style={{ background: '#222' }}
        >
          {icon}
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-medium" style={{ color: '#f0f0f0' }}>{title}</div>
          {subtitle && <div className="text-xs mt-0.5" style={{ color: '#666' }}>{subtitle}</div>}
        </div>
        {badge}
      </div>

      {/* Divider */}
      <div className="mb-4" style={{ borderTop: '0.5px solid rgba(255,255,255,0.06)' }} />

      {/* Content */}
      {children}
    </div>
  )
}

// Reusable section label
export function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <div
      className="text-xs font-medium uppercase tracking-wider mb-2"
      style={{ color: '#555', letterSpacing: '0.06em' }}
    >
      {children}
    </div>
  )
}

// Difficulty badge
export function DifficultyBadge({ difficulty }: { difficulty: string }) {
  const colors: Record<string, { bg: string; color: string }> = {
    Easy:    { bg: 'rgba(99,153,34,0.15)',  color: '#8dc63f' },
    Medium:  { bg: 'rgba(186,117,23,0.15)', color: '#f0a533' },
    Hard:    { bg: 'rgba(163,45,45,0.15)',  color: '#e8706f' },
    Unknown: { bg: 'rgba(100,100,100,0.15)', color: '#888' },
  }
  const c = colors[difficulty] || colors.Unknown
  return (
    <span
      className="text-xs px-3 py-1 rounded-full font-medium"
      style={{ background: c.bg, color: c.color }}
    >
      {difficulty}
    </span>
  )
}

// Difficulty bar (5 segments)
export function DifficultyBar({ score }: { score: number }) {
  const color = score <= 2 ? '#639922' : score <= 3 ? '#BA7517' : '#A32D2D'
  return (
    <div className="flex gap-1 mt-2">
      {[1, 2, 3, 4, 5].map(i => (
        <div
          key={i}
          className="h-1.5 flex-1 rounded-full"
          style={{ background: i <= score ? color : 'rgba(255,255,255,0.08)' }}
        />
      ))}
    </div>
  )
}

// Category badge for questions
export function CategoryBadge({ category }: { category: string }) {
  const colors: Record<string, { bg: string; color: string }> = {
    DSA:              { bg: 'rgba(55,138,221,0.12)', color: '#64b0f4' },
    'System Design':  { bg: 'rgba(83,74,183,0.15)',  color: '#a89cf7' },
    Behavioral:       { bg: 'rgba(99,153,34,0.15)',  color: '#8dc63f' },
    'CS Fundamentals':{ bg: 'rgba(186,117,23,0.15)', color: '#f0a533' },
    Domain:           { bg: 'rgba(163,45,45,0.12)',  color: '#e8706f' },
  }
  const c = colors[category] || { bg: 'rgba(255,255,255,0.06)', color: '#888' }
  return (
    <span
      className="text-xs px-2.5 py-0.5 rounded-full font-medium flex-shrink-0"
      style={{ background: c.bg, color: c.color }}
    >
      {category}
    </span>
  )
}
