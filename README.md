# InterviewIntel — AI Interview Intelligence Platform

![Next.js](https://img.shields.io/badge/Next.js-14-black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.0-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Cache-336791)
![Gemini](https://img.shields.io/badge/Gemini-2.5_Flash-orange)

InterviewIntel aggregates real interview experiences from across the web and structures them into actionable intel — rounds, compensation, topic breakdown, community questions, practice questions, and prep tips. Enter a company + role, get everything you need to walk in prepared.

## Features

- **Interview Round Breakdown** — Exact round structure reported by real candidates
- **Compensation Data** — Realistic salary ranges (INR LPA / USD), always verify on Levels.fyi
- **Topic Frequency Chart** — Visual breakdown of what the company actually tests
- **Questions from the Community** — Actual questions candidates reported from Reddit, LeetCode, Naukri, GFG
- **Practice Questions** — AI-generated topic-wise questions based on what this company typically asks
- **Insider Prep Tips** — Company-specific, not generic advice
- **Live Hiring Signal** — Apply CTA with direct careers page link when actively hiring
- **Two-Layer Caching** — In-memory (1hr TTL) + PostgreSQL (7-day TTL) to eliminate redundant API calls
- **Query Normalization** — "Amex" and "American Express" resolve to the same cache entry

## Tech Stack

- **Frontend**: Next.js 14, TypeScript, Tailwind CSS, Recharts
- **AI**: Google Gemini — fallback chain: `gemini-2.5-flash → gemini-2.0-flash → gemini-2.0-flash-lite`
- **Data**: SerpAPI (Google search across LeetCode, Reddit, Naukri, GFG) + Reddit JSON API fallback
- **Cache**: PostgreSQL (Railway) + in-memory Map
- **Hosting**: Vercel (frontend) + Railway (PostgreSQL)

## Running Locally

```bash
cd frontend
cp .env.example .env.local
# Fill in GEMINI_API_KEY, SERP_API_KEY, DATABASE_URL in .env.local
npm install
npm run dev
```

Open https://interview-intel-theta.vercel.app/

## Environment Variables

| Variable | Where to get it |
|---|---|
| `GEMINI_API_KEY` | [aistudio.google.com](https://aistudio.google.com) — free |
| `SERP_API_KEY` | [serpapi.com](https://serpapi.com) — 100 free searches/month |
| `DATABASE_URL` | Railway → New Project → PostgreSQL → copy connection string |

## Deployment

- **Frontend**: Vercel — set Root Directory to `frontend`, add all three env vars
- **Database**: Railway → New Project → PostgreSQL → copy `DATABASE_URL` → add to Vercel env vars

The `search_cache` table auto-creates on first search — no migrations needed.

## License

Copyright © 2026 Anirudh S. Distributed under the MIT License. See [LICENSE](./LICENSE) for more information.
