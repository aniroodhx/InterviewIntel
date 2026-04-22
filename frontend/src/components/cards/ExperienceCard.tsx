import { InterviewIntelResponse } from '@/types'
import { Card, SectionLabel } from './Card'

export function ExperienceCard({ data }: { data: InterviewIntelResponse }) {
  return (
    <Card icon="💬" title="Collective interview experience">
      {/* Main experience paragraph */}
      <p className="text-sm leading-relaxed mb-4" style={{ color: '#ccc' }}>
        {data.collectiveExperience}
      </p>

      {/* Key topics */}
      {data.topics && data.topics.length > 0 && (
        <div className="mb-4">
          <SectionLabel>Key focus areas</SectionLabel>
          <div className="flex flex-wrap gap-2">
            {data.topics.map((topic, i) => (
              <span
                key={i}
                className="text-xs px-2.5 py-1 rounded-md"
                style={{ background: '#222', border: '0.5px solid rgba(255,255,255,0.08)', color: '#aaa' }}
              >
                {topic}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Source refs if available */}
      {data.sources && data.sources.length > 0 && (
        <div>
          <SectionLabel>Source posts</SectionLabel>
          <div className="flex flex-col gap-1.5">
            {data.sources.map((src, i) => (
              <a
                key={i}
                href={src.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-2 text-xs group"
                style={{ color: '#555', textDecoration: 'none' }}
              >
                <span
                  className="px-1.5 py-0.5 rounded text-xs flex-shrink-0"
                  style={{ background: '#222', color: '#666' }}
                >
                  {src.platform}
                </span>
                <span className="truncate group-hover:text-[#e8ff47] transition-colors">
                  {src.title}
                </span>
                {src.upvotes > 0 && (
                  <span className="flex-shrink-0" style={{ color: '#444' }}>↑{src.upvotes}</span>
                )}
              </a>
            ))}
          </div>
        </div>
      )}

      {data.sourcePostCount !== undefined && (
        <div className="mt-3 text-xs" style={{ color: '#555' }}>
          Based on {data.sourcePostCount} community posts
        </div>
      )}
    </Card>
  )
}
