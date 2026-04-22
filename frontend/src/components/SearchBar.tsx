'use client'

import { useState, KeyboardEvent } from 'react'
import { Location } from '@/types'

interface SearchBarProps {
  onSearch: (company: string, role: string, location: Location) => void
  loading: boolean
}

export function SearchBar({ onSearch, loading }: SearchBarProps) {
  const [company, setCompany] = useState('')
  const [role, setRole] = useState('')
  const [location, setLocation] = useState<Location>('IN')

  function handleSearch() {
    if (!company.trim() || !role.trim() || loading) return
    onSearch(company.trim(), role.trim(), location)
  }

  function handleKey(e: KeyboardEvent) {
    if (e.key === 'Enter') handleSearch()
  }

  return (
    <div
      className="rounded-xl p-5 mb-8"
      style={{ background: '#1a1a1a', border: '0.5px solid rgba(255,255,255,0.1)' }}
    >
      {/* Inputs row */}
      <div className="flex gap-3 flex-wrap mb-4">
        <input
          type="text"
          placeholder="Company  (e.g. Amex, Qualcomm)"
          value={company}
          onChange={e => setCompany(e.target.value)}
          onKeyDown={handleKey}
          className="flex-1 min-w-[160px] rounded-lg px-4 py-2.5 text-sm transition-colors"
          style={{
            background: '#222',
            border: '0.5px solid rgba(255,255,255,0.12)',
            color: '#f0f0f0',
          }}
        />
        <input
          type="text"
          placeholder="Role  (e.g. SDE 1, SDE1)"
          value={role}
          onChange={e => setRole(e.target.value)}
          onKeyDown={handleKey}
          className="flex-1 min-w-[140px] rounded-lg px-4 py-2.5 text-sm transition-colors"
          style={{
            background: '#222',
            border: '0.5px solid rgba(255,255,255,0.12)',
            color: '#f0f0f0',
          }}
        />
      </div>

      {/* Bottom row: location toggle + search button */}
      <div className="flex items-center justify-between gap-3 flex-wrap">
        {/* Location toggle */}
        <div
          className="flex rounded-lg overflow-hidden"
          style={{ border: '0.5px solid rgba(255,255,255,0.12)' }}
        >
          {(['IN', 'US'] as Location[]).map(loc => (
            <button
              key={loc}
              onClick={() => setLocation(loc)}
              className="px-4 py-2 text-xs font-medium transition-all"
              style={{
                background: location === loc ? 'rgba(232,255,71,0.1)' : 'transparent',
                color: location === loc ? '#e8ff47' : '#888',
                border: 'none',
                cursor: 'pointer',
              }}
            >
              {loc === 'IN' ? '🇮🇳 India' : '🇺🇸 United States'}
            </button>
          ))}
        </div>

        {/* Search button */}
        <button
          onClick={handleSearch}
          disabled={loading || !company.trim() || !role.trim()}
          className="px-6 py-2.5 rounded-lg text-sm font-semibold transition-all"
          style={{
            background: loading ? 'rgba(232,255,71,0.5)' : '#e8ff47',
            color: '#0f0f0f',
            border: 'none',
            cursor: loading ? 'not-allowed' : 'pointer',
            opacity: !company.trim() || !role.trim() ? 0.5 : 1,
          }}
        >
          {loading ? (
            <span className="flex items-center gap-2">
              <span
                className="inline-block w-4 h-4 rounded-full border-2 border-transparent animate-spin"
                style={{ borderTopColor: '#0f0f0f' }}
              />
              Searching...
            </span>
          ) : (
            'Search →'
          )}
        </button>
      </div>
    </div>
  )
}
