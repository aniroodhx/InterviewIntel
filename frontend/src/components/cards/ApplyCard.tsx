import { InterviewIntelResponse } from '@/types'

export function ApplyCard({ data }: { data: InterviewIntelResponse }) {
  if (!data.hasOpenings || !data.careersUrl) return null

  return (
    <div
      className="rounded-xl px-5 py-4 flex items-center justify-between gap-4"
      style={{
        background: 'rgba(232,255,71,0.05)',
        border: '0.5px solid rgba(232,255,71,0.25)',
      }}
    >
      <div>
        <div className="flex items-center gap-2 mb-0.5">
          <span
            className="w-2 h-2 rounded-full flex-shrink-0"
            style={{ background: '#e8ff47', boxShadow: '0 0 6px #e8ff47' }}
          />
          <span className="text-sm font-semibold" style={{ color: '#e8ff47' }}>
            Actively Hiring
          </span>
        </div>
        <p className="text-xs" style={{ color: '#666' }}>
          {data.company} has open {data.role} positions — apply directly on their careers page.
        </p>
      </div>

      <a
        href={data.careersUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="flex-shrink-0 text-xs font-semibold px-4 py-2 rounded-lg transition-opacity hover:opacity-80"
        style={{ background: '#e8ff47', color: '#0f0f0f' }}
      >
        Apply Now →
      </a>
    </div>
  )
}
