export type Difficulty = 'Easy' | 'Medium' | 'Hard' | 'Unknown'
export type Location = 'IN' | 'US'

export interface Compensation {
  currency: string
  base: string | null
  totalComp: string | null
  bonus: string | null
  equity: string | null
  note: string | null
}

export interface Question {
  category: 'DSA' | 'System Design' | 'Behavioral' | 'CS Fundamentals' | 'Domain' | string
  question: string
  difficulty: Difficulty
  frequency: number
}

export interface TopicFrequency {
  topic: string
  count: number
  percentage: number
}

export interface SourceRef {
  platform: string
  url: string
  title: string
  upvotes: number
}

export interface InterviewIntelResponse {
  company: string
  role: string
  location: Location
  overview: string
  difficulty: Difficulty
  difficultyScore: number
  collectiveExperience: string
  rounds: {
    name: string
    type: string
    duration: string
    format?: string
    focus?: string
    outcome?: string
  }[]
  topics: string[]
  compensation: Compensation
  communityQuestions: Question[]   // extracted from real posts
  practiceQuestions: Question[]    // always AI-generated
  questions?: Question[]           // legacy — kept for cache compatibility
  topicFrequencies: TopicFrequency[]
  tips: string[]
  sources?: SourceRef[]
  sourcePostCount?: number
  dataSource?: 'serp' | 'reddit' | 'ai-generated'
  sourcesFound?: string
  hasOpenings: boolean
  careersUrl: string | null
  isCached: boolean
  generatedAt: string
}

export interface SearchState {
  loading: boolean
  data: InterviewIntelResponse | null
  error: string | null
}