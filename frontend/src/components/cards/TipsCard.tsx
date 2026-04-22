import { InterviewIntelResponse } from '@/types'
import { Card } from './Card'

export function TipsCard({ data }: { data: InterviewIntelResponse }) {
  const tips = data.tips
  if (!tips || tips.length === 0) return null

  return (
    <Card icon="✅" title="Insider prep tips">
      <div className="flex flex-col gap-3">
        {tips.map((tip, i) => (
          <div key={i} className="flex items-start gap-3">
            {/* Numbered circle */}
            <span
              className="w-5 h-5 rounded-full flex items-center justify-center text-xs font-medium flex-shrink-0 mt-0.5"
              style={{ background: 'rgba(232,255,71,0.1)', color: '#e8ff47' }}
            >
              {i + 1}
            </span>
            <p className="text-sm leading-relaxed" style={{ color: '#ccc' }}>
              {tip}
            </p>
          </div>
        ))}
      </div>
    </Card>
  )
}
