import { InterviewIntelResponse, Question } from '@/types'
import { Card, CategoryBadge, DifficultyBadge } from './Card'

function QuestionGroup({ questions }: { questions: Question[] }) {
  const grouped = questions.reduce<Record<string, Question[]>>((acc, q) => {
    const cat = q.category || 'Other'
    if (!acc[cat]) acc[cat] = []
    acc[cat].push(q)
    return acc
  }, {})

  return (
    <div className="flex flex-col gap-5">
      {Object.entries(grouped).map(([category, qs]) => (
        <div key={category}>
          <div className="flex items-center gap-2 mb-2">
            <CategoryBadge category={category} />
            <span className="text-xs" style={{ color: '#555' }}>
              {qs.length} question{qs.length > 1 ? 's' : ''}
            </span>
          </div>
          <div className="flex flex-col gap-2">
            {qs.map((q, i) => (
              <div
                key={i}
                className="flex items-start gap-3 rounded-lg p-3"
                style={{ background: '#222' }}
              >
                <div className="w-1.5 h-1.5 rounded-full flex-shrink-0 mt-1.5" style={{ background: 'rgba(255,255,255,0.2)' }} />
                <div className="flex-1 min-w-0">
                  <p className="text-sm leading-relaxed" style={{ color: '#ddd' }}>{q.question}</p>
                </div>
                <div className="flex flex-col items-end gap-1 flex-shrink-0">
                  <DifficultyBadge difficulty={q.difficulty} />
                  {q.frequency > 1 && (
                    <span className="text-xs" style={{ color: '#555' }}>×{q.frequency}</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

export function QuestionsCard({ data }: { data: InterviewIntelResponse }) {
  const communityQs = data.communityQuestions || []
  const practiceQs = data.practiceQuestions || []

  return (
    <>
      {/* Section 1 — Questions from community posts */}
      <Card icon="🧠" title="Questions from the community" subtitle="Reported by candidates in real posts">
        {communityQs.length > 0 ? (
          <QuestionGroup questions={communityQs} />
        ) : (
          <p className="text-sm" style={{ color: '#555' }}>
            No specific questions were reported in the posts we found for this company and role.
          </p>
        )}
      </Card>

      {/* Section 2 — AI-generated practice questions */}
      <Card icon="💡" title="Practice questions" subtitle="AI-generated based on what this company typically tests">
        {practiceQs.length > 0 ? (
          <QuestionGroup questions={practiceQs} />
        ) : (
          <p className="text-sm" style={{ color: '#555' }}>Practice questions could not be generated.</p>
        )}
      </Card>
    </>
  )
}