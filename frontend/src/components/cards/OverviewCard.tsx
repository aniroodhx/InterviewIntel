import { InterviewIntelResponse } from '@/types'
import { Card, DifficultyBadge, DifficultyBar, SectionLabel } from './Card'

export function OverviewCard({ data }: { data: InterviewIntelResponse }) {
  return (
    <Card
      icon="🏢"
      title={`${data.company} — ${data.role}`}
      subtitle="Aggregated from Reddit, Glassdoor & Blind"
      badge={<DifficultyBadge difficulty={data.difficulty} />}
    >
      {/* Overview text */}
      <p className="text-sm leading-relaxed mb-4" style={{ color: '#ccc' }}>
        {data.overview}
      </p>

      {/* Interview rounds */}
      {data.rounds && data.rounds.length > 0 && (
        <div className="mb-4">
          <SectionLabel>Interview rounds</SectionLabel>
          <div className="flex flex-col gap-2">
            {data.rounds.map((round, i) => (
              <div key={i} className="flex items-start gap-2">
                {/* Step number */}
                <span
                  className="text-xs w-5 h-5 rounded-full flex items-center justify-center font-medium flex-shrink-0"
                  style={{ background: 'rgba(232,255,71,0.1)', color: '#e8ff47' }}
                >
                  {i + 1}
                </span>

                {/* Round details */}
                <div
                  className="text-xs px-3 py-2 rounded-md w-full"
                  style={{ border: '0.5px solid rgba(255,255,255,0.12)', color: '#aaa' }}
                >
                  <div className="font-medium text-white">{round.name}</div>
                  <div>{round.type} • {round.duration}</div>
                  {round.focus && <div>Focus: {round.focus}</div>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Difficulty bar */}
      <div>
        <SectionLabel>Overall difficulty ({data.difficultyScore}/5)</SectionLabel>
        <DifficultyBar score={data.difficultyScore} />
      </div>

      {/* Cached indicator */}
      {data.isCached && (
        <div className="mt-3 text-xs" style={{ color: '#555' }}>
          ⚡ Cached result
        </div>
      )}
    </Card>
  )
}