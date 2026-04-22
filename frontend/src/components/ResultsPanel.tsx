'use client'

import { SearchState } from '@/types'
import { OverviewCard } from './cards/OverviewCard'
import { ExperienceCard } from './cards/ExperienceCard'
import { CompensationCard } from './cards/CompensationCard'
import { QuestionsCard } from './cards/QuestionsCard'
import { TopicsChart } from './cards/TopicsChart'
import { TipsCard } from './cards/TipsCard'
import { ApplyCard } from './cards/ApplyCard'

interface ResultsPanelProps {
  state: SearchState
}

export function ResultsPanel({ state }: ResultsPanelProps) {
  const { loading, data, error } = state

  if (loading) {
    return (
      <div className="flex flex-col gap-4 animate-pulse">
        {[180, 140, 120, 200, 160, 160, 130].map((h, i) => (
          <div
            key={i}
            className="rounded-xl"
            style={{ height: h, background: '#1a1a1a', border: '0.5px solid rgba(255,255,255,0.06)' }}
          />
        ))}
      </div>
    )
  }

  if (error) {
    return (
      <div
        className="rounded-xl p-6 text-sm"
        style={{ background: 'rgba(163,45,45,0.1)', border: '0.5px solid rgba(163,45,45,0.3)', color: '#e8706f' }}
      >
        {error}
      </div>
    )
  }

  if (!data) {
    return (
      <div className="text-sm text-center py-12" style={{ color: '#555' }}>
        Search for a company and role to get started.
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 animate-[slideUp_0.4s_ease-out]">
      <OverviewCard data={data} />
      <ExperienceCard data={data} />
      <CompensationCard data={data} />
      <TopicsChart data={data} />
      <QuestionsCard data={data} />
      <TipsCard data={data} />
      <ApplyCard data={data} />
    </div>
  )
}