import { InterviewIntelResponse } from '@/types'
import { Card } from './Card'

export function CompensationCard({ data }: { data: InterviewIntelResponse }) {
  const comp = data.compensation
  if (!comp) return null

  const stats = [
    { label: 'Base salary', value: comp.base },
    { label: 'Total comp', value: comp.totalComp },
    { label: 'Bonus', value: comp.bonus },
    { label: 'Equity / RSU', value: comp.equity },
  ].filter(s => s.value && s.value !== 'null')

  return (
    <Card
      icon="💰"
      title="Compensation"
      subtitle={`${comp.currency} · verify on Levels.fyi`}
    >
      <div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))' }}>
        {stats.map((stat, i) => (
          <div key={i} className="rounded-lg p-3" style={{ background: '#222' }}>
            <div className="text-xs mb-1" style={{ color: '#666' }}>{stat.label}</div>
            <div className="text-base font-medium" style={{ color: '#f0f0f0' }}>{stat.value}</div>
          </div>
        ))}
      </div>

      {comp.note && comp.note !== 'null' && (
        <p className="mt-3 text-xs leading-relaxed" style={{ color: '#666' }}>
          ℹ️ {comp.note}
        </p>
      )}

      {/* Always show Levels.fyi link below comp */}
      <p className="mt-3 text-xs" style={{ color: '#555' }}>
        Salaries are estimates — always verify on{' '}
        <a
          href={`https://www.levels.fyi/companies/${data.company.toLowerCase().replace(/\s+/g, '-')}/`}
          target="_blank"
          rel="noopener noreferrer"
          style={{ color: '#e8ff47', textDecoration: 'none' }}
        >
          Levels.fyi →
        </a>
      </p>
    </Card>
  )
}