# InterviewIntel

> Real interview experiences from Reddit, Glassdoor & Blind — aggregated, structured, and summarized by AI.

Solves the problem of candidates spending hours hunting scattered interview experiences across multiple platforms.
Enter a company + role → get structured intel: rounds, questions, comp, tips.

---

## Architecture

```
User Query ("Amex SDE 1")
        ↓
Next.js Frontend  →  /api/search (Next.js API Route — API key stays server-side)
        ↓
Spring Boot Backend  →  QueryNormalizer ("Amex" → "American Express", "SDE1" → "Software Engineer 1")
        ↓
Redis Cache (hot) → PostgreSQL Cache (warm) → Miss: fetch from DB
        ↓
InterviewPostRepository  (PostgreSQL — real Reddit posts stored here)
        ↓
ClaudeSummarizationService  (grounded prompt — only summarizes what's in the posts)
        ↓
Structured JSON response  →  Frontend cards
        ↓
Background: RedditIngestionService (async, triggered when data is sparse)
```

---

## Quick Start (MVP — Frontend Only)

The fastest way to get running. Uses Claude API directly from the Next.js server.
No Spring Boot, no PostgreSQL, no Redis needed.

```bash
cd frontend
cp .env.example .env.local
# Fill in CLAUDE_API_KEY in .env.local
npm install
npm run dev
```

Open http://localhost:3000. Done.

---

## Full Stack (Production-Grade)

Runs Spring Boot + PostgreSQL + Redis + Next.js together via Docker Compose.

### Step 1: Get your API keys

**Claude API Key**
1. Go to https://console.anthropic.com/
2. Settings → API Keys → Create Key
3. Copy the key — it starts with `sk-ant-`

**Reddit API Credentials**
1. Go to https://www.reddit.com/prefs/apps
2. Click "create another app"
3. Choose type: **script**
4. Name: InterviewIntel
5. Redirect URI: http://localhost (doesn't matter for script type)
6. Copy the **client ID** (under app name) and **client secret**

### Step 2: Configure environment

```bash
cp .env.example .env
# Edit .env and fill in:
#   CLAUDE_API_KEY
#   REDDIT_CLIENT_ID
#   REDDIT_CLIENT_SECRET
```

### Step 3: Run with Docker Compose

```bash
docker-compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Health check: http://localhost:8080/api/v1/health

---

## API Reference

### Search
```
GET /api/v1/search?company=Amex&role=SDE1&location=IN
```

Returns full interview intelligence. Normalizes "Amex" → "American Express", "SDE1" → "Software Engineer 1".
Results are cached in Redis (6h) and PostgreSQL (7 days).

### Trigger Ingestion
```
POST /api/v1/ingest
Body: { "company": "Qualcomm", "role": "SDE 1", "location": "IN" }
```

Fires off async Reddit ingestion in the background. Check back after ~30 seconds.

### Normalize (debug)
```
GET /api/v1/normalize?company=Amex&role=SDE1
```

Returns the normalized company/role strings — useful to verify aliases are working.

### Autocomplete
```
GET /api/v1/autocomplete/companies
GET /api/v1/autocomplete/roles?company=American Express
```

---

## Project Structure

```
interviewintel/
├── frontend/                          # Next.js app
│   └── src/
│       ├── app/
│       │   ├── page.tsx               # Main search page
│       │   ├── layout.tsx
│       │   ├── globals.css
│       │   └── api/search/route.ts    # Server-side API route (API key lives here)
│       ├── components/
│       │   ├── SearchBar.tsx
│       │   ├── ResultsPanel.tsx
│       │   └── cards/                 # OverviewCard, CompensationCard, QuestionsCard, etc.
│       └── types/index.ts
│
├── backend/                           # Spring Boot app
│   └── src/main/java/com/interviewintel/
│       ├── controller/
│       │   └── InterviewController.java    # REST endpoints
│       ├── service/
│       │   ├── SearchService.java          # Orchestrates the full search flow
│       │   ├── ClaudeSummarizationService  # LLM summarization with grounded prompts
│       │   └── QueryNormalizer.java        # "Amex" → "American Express"
│       ├── ingestion/
│       │   └── RedditIngestionService.java # Fetches & stores Reddit posts
│       ├── repository/
│       │   ├── InterviewPostRepository.java
│       │   └── SearchResultRepository.java
│       ├── model/
│       │   ├── InterviewPost.java          # Stores raw scraped posts
│       │   └── SearchResult.java          # Stores cached LLM results
│       └── dto/Dtos.java                  # All request/response shapes
│
├── docker-compose.yml                 # Full stack local dev
├── .env.example                       # Template for environment variables
└── README.md
```

---

## Deployment

### Frontend → Vercel (free tier works)
```bash
cd frontend
# Push to GitHub, then connect repo in vercel.com
# Add environment variable: CLAUDE_API_KEY in Vercel dashboard
```

### Backend → Railway.app (free tier works)
```bash
# Push to GitHub
# Create new Railway project → Deploy from GitHub → select /backend
# Add environment variables in Railway dashboard
# Railway auto-detects the Dockerfile
```

### Database → Railway PostgreSQL
```bash
# In Railway: New Service → PostgreSQL
# Copy DATABASE_URL from Railway → add to backend env vars
```

---

## How to Talk About This in Interviews

**"What does this project do?"**
"It solves a real problem I personally faced — interview experiences are scattered across Reddit, Glassdoor, and Blind. I built a retrieval-based system that ingests posts via the Reddit API, stores and deduplicates them in PostgreSQL, and uses an LLM (Claude) to produce a structured summary. The key engineering decisions were: grounded prompting to prevent hallucination, a two-layer cache (Redis + PostgreSQL) for performance, and a normalization layer so 'Amex' and 'American Express' hit the same data."

**"Why not just call the LLM directly?"**
"Pure LLM answers hallucinate compensation and question data. My approach feeds real posts from the DB into the prompt — the LLM is a summarizer, not an inventor. Each response cites which posts it used."

**"What's your caching strategy?"**
"Two-layer: Redis for hot queries (sub-millisecond, 6h TTL), PostgreSQL for warm cache (7-day TTL). On a cache miss, I fetch posts from the DB, run Claude, and backfill both caches asynchronously."

**"How do you handle data quality?"**
"Every ingested post gets a quality score (0-100) based on upvotes, comment count, body length, and awards. The retrieval query filters by a minimum quality threshold and ranks by score. Short, low-karma posts get filtered out."

---

## Anti-Hallucination Design

The Claude prompt explicitly says:
- "ONLY use information present in the posts below. DO NOT hallucinate."
- "If compensation is not mentioned, return null."
- "If there are only 2-3 posts, say 'limited data' in the overview."

This is what separates this from a toy — the LLM is grounded in real data, not generating from memory.
