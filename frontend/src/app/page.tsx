'use client'

import { useState } from 'react'
import { SearchBar } from '@/components/SearchBar'
import { ResultsPanel } from '@/components/ResultsPanel'
import { InterviewIntelResponse, Location, SearchState } from '@/types'

export default function HomePage() {
  const [searchState, setSearchState] = useState<SearchState>({
    loading: false,
    data: null,
    error: null,
  })

  async function handleSearch(company: string, role: string, location: Location) {
    setSearchState({ loading: true, data: null, error: null })

    try {
      const res = await fetch('/api/search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ company, role, location }),
      })

      if (!res.ok) {
        const err = await res.json()
        throw new Error(err.error || 'Search failed')
      }

      const data: InterviewIntelResponse = await res.json()
      setSearchState({ loading: false, data, error: null })
    } catch (err: any) {
      setSearchState({ loading: false, data: null, error: err.message || 'Something went wrong' })
    }
  }

  return (
    <main className="min-h-screen" style={{ background: '#0f0f0f' }}>
      <div className="max-w-3xl mx-auto px-4 py-12">

        {/* Header */}
        <header className="mb-10">
          <h1 className="text-3xl font-semibold tracking-tight mb-2">
            Interview{' '}
            <span style={{ color: '#e8ff47' }}>Intel</span>
          </h1>
          <p className="text-sm" style={{ color: '#888' }}>
            Real interview experiences from across the web — aggregated, not fabricated.
          </p>
        </header>

        {/* Search bar */}
        <SearchBar onSearch={handleSearch} loading={searchState.loading} />

        {/* Results */}
        <ResultsPanel state={searchState} />

        {/* Footer */}
        <footer className="mt-16 text-center text-xs" style={{ color: '#555' }}>
          AI-synthesized from community reports. Salaries are estimates, always verify compensation on{' '}
          <a
            href="https://levels.fyi"
            target="_blank"
            rel="noopener noreferrer"
            style={{ color: '#e8ff47', textDecoration: 'none' }}
          >
            Levels.fyi
          </a>
          .
        </footer>
      </div>
    </main>
  )
}
