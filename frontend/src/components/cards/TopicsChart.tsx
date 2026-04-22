'use client'

import { InterviewIntelResponse } from '@/types'
import { Card, SectionLabel } from './Card'

export function TopicsChart({ data }: { data: InterviewIntelResponse }) {
  const freqs = data.topicFrequencies
  if (!freqs || freqs.length === 0) return null

  // Sort by percentage descending
  const sorted = [...freqs].sort((a, b) => b.percentage - a.percentage)
  const max = sorted[0]?.percentage || 100

  // Color ramp based on rank
  const barColors = ['#e8ff47', '#8dc63f', '#f0a533', '#64b0f4', '#a89cf7', '#e8706f']

  return (
    <Card icon="📊" title="Topic frequency breakdown" subtitle="What interviewers actually ask about">
      <div className="flex flex-col gap-3">
        {sorted.map((item, i) => (
          <div key={i}>
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs" style={{ color: '#ccc' }}>{item.topic}</span>
              <span className="text-xs font-medium" style={{ color: barColors[i] || '#888' }}>
                {Math.round(item.percentage)}%
              </span>
            </div>
            {/* Progress bar */}
            <div
              className="w-full rounded-full overflow-hidden"
              style={{ height: 6, background: 'rgba(255,255,255,0.06)' }}
            >
              <div
                className="h-full rounded-full transition-all"
                style={{
                  width: `${(item.percentage / max) * 100}%`,
                  background: barColors[i] || '#888',
                }}
              />
            </div>
          </div>
        ))}
      </div>

      {/* Legend note */}
      <p className="mt-4 text-xs" style={{ color: '#555' }}>
        Based on topic mentions across community interview reports.
      </p>
    </Card>
  )
}
