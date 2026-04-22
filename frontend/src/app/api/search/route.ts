import { query } from "@/lib/db";
import { getFromMemory, setToMemory } from "@/lib/cache";
import { NextRequest, NextResponse } from "next/server";

interface Post {
  title: string;
  body: string;
  source: string;
  url: string;
  score?: number;
}

//SerpAPI fetcher 
async function fetchViaSerp(company: string, role: string): Promise<Post[]> {
  const serpKey = process.env.SERP_API_KEY;
  if (!serpKey) return [];
  const q = encodeURIComponent(
    `${company} ${role} interview experience site:leetcode.com OR site:reddit.com OR site:naukri.com OR site:geeksforgeeks.org`
  );
  const url = `https://serpapi.com/search.json?engine=google&q=${q}&num=10&api_key=${serpKey}`;
  try {
    const res = await fetch(url, { cache: "no-store" });
    if (!res.ok) { console.warn("SerpAPI failed:", res.status); return []; }
    const data = await res.json();
    const results = data?.organic_results || [];
    const posts: Post[] = [];
    for (const r of results) {
      const link: string = r.link || "";
      const snippet: string = r.snippet || "";
      const title: string = r.title || "";
      if (snippet.length < 80) continue;
      let source = "web";
      if (link.includes("leetcode.com")) source = "leetcode";
      else if (link.includes("reddit.com")) source = "reddit";
      else if (link.includes("naukri.com")) source = "naukri";
      else if (link.includes("geeksforgeeks.org")) source = "geeksforgeeks";
      else if (link.includes("glassdoor")) source = "glassdoor";
      else if (link.includes("ambitionbox")) source = "ambitionbox";
      if (source === "leetcode" && link.includes("/discuss/")) {
        try {
          const pageRes = await fetch(link, {
            headers: { "User-Agent": "Mozilla/5.0 (compatible; InterviewIntel/1.0)" },
            cache: "no-store",
          });
          if (pageRes.ok) {
            const html = await pageRes.text();
            const match = html.match(/"content":"([\s\S]{100,3000}?)","(?:title|author)"/);
            if (match) {
              const body = match[1].replace(/\\n/g, "\n").replace(/\\"/g, '"').replace(/\\u[\dA-Fa-f]{4}/g, "").slice(0, 2000);
              posts.push({ title, body, source, url: link });
              continue;
            }
          }
        } catch { /* fall through to snippet */ }
      }
      posts.push({ title, body: snippet, source, url: link });
    }
    console.log(`SerpAPI: fetched ${posts.length} results for ${company} ${role}`);
    return posts;
  } catch (err) {
    console.warn("SerpAPI fetch error:", err);
    return [];
  }
}

//Reddit fallback fetcher
async function fetchRedditPosts(company: string, role: string): Promise<Post[]> {
  const queries = [
    `${company} ${role} interview experience`,
    `${company} interview experience`,
    `${company} SDE interview`,
  ];
  const subreddits = ["cscareerquestionsindia", "cscareerquestions", "leetcode", "developersIndia"];
  const allPosts: Post[] = [];
  for (const sub of subreddits) {
    let fetched = false;
    for (const q of queries) {
      if (fetched) break;
      const encodedQ = encodeURIComponent(q);
      const url = `https://www.reddit.com/r/${sub}/search.json?q=${encodedQ}&restrict_sr=1&sort=relevance&limit=8&t=year`;
      try {
        const res = await fetch(url, {
          headers: { "User-Agent": "InterviewIntel/1.0 (interview research tool)", "Accept": "application/json" },
          cache: "no-store",
        });
        if (res.status === 429) { console.warn(`Reddit rate limit on r/${sub}`); break; }
        if (!res.ok) continue;
        const data = await res.json();
        const posts: Post[] = (data?.data?.children || [])
          .map((child: any) => ({
            title: child.data.title || "",
            body: (child.data.selftext || "").slice(0, 1500),
            source: "reddit",
            url: `https://reddit.com${child.data.permalink}`,
            score: child.data.score || 0,
          }))
          .filter((p: Post) => p.body.length > 80);
        if (posts.length > 0) { allPosts.push(...posts); fetched = true; }
      } catch (err) { console.warn(`Reddit fetch failed for r/${sub}:`, err); }
    }
  }
  const seen = new Set<string>();
  return allPosts
    .filter((p) => { const k = p.title.toLowerCase().slice(0, 60); if (seen.has(k)) return false; seen.add(k); return true; })
    .sort((a, b) => (b.score || 0) - (a.score || 0))
    .slice(0, 10);
}

function formatPostsForPrompt(posts: Post[]): string {
  return posts
    .map((p, i) => `--- Source ${i + 1} [${p.source.toUpperCase()}] ---\nTitle: ${p.title}\n${p.body}`)
    .join("\n\n");
}

//Main route
export async function POST(req: NextRequest) {
  const { company, role, location } = await req.json();
  if (!company || !role) {
    return NextResponse.json({ error: "company and role are required" }, { status: 400 });
  }

  const companyAliases: Record<string, string> = {
    amex: "American Express", americanexpress: "American Express",
    ms: "Microsoft", msft: "Microsoft",
    goog: "Google", googl: "Google",
    fb: "Meta", facebook: "Meta",
    amzn: "Amazon", aws: "Amazon Web Services",
    qcom: "Qualcomm", nvda: "Nvidia", intc: "Intel",
    jpm: "JPMorgan Chase",
    tcs: "Tata Consultancy Services",
    infy: "Infosys", hcl: "HCL Technologies",
  };
  const roleAliases: Record<string, string> = {
    sde1: "Software Engineer 1", "sde 1": "Software Engineer 1", "sde-1": "Software Engineer 1",
    sde2: "Software Engineer 2", "sde 2": "Software Engineer 2",
    swe: "Software Engineer", swe1: "Software Engineer 1",
    mle: "Machine Learning Engineer",
    sdet: "Software Development Engineer in Test",
  };

  const normCompany = companyAliases[company.toLowerCase().replace(/\s/g, "")] || company;
  const normRole = roleAliases[role.toLowerCase().replace(/\s+/g, " ").trim()] || role;
  const locationLabel = location === "US" ? "United States" : "India";
  const currency = location === "US" ? "USD (annual)" : "INR LPA";
  const cacheKey = `${normCompany}-${normRole}-${location || "IN"}`.toLowerCase().trim();

  // Memory cache
  const memoryHit = getFromMemory(cacheKey);
  if (memoryHit) return NextResponse.json({ ...memoryHit, isCached: true, source: "memory" });

  // DB cache
  try {
    const dbRes = await query("SELECT response FROM cache WHERE query = $1", [cacheKey]);
    if (dbRes.rows.length > 0) {
      const cached = dbRes.rows[0].response;
      setToMemory(cacheKey, cached);
      return NextResponse.json({ ...cached, isCached: true, source: "db" });
    }
  } catch (err) { console.error("DB read failed:", err); }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) return NextResponse.json({ error: "API key not configured on server" }, { status: 500 });

  //Fetch real posts — SerpAPI first, Reddit fallback
  let posts: Post[] = await fetchViaSerp(normCompany, normRole);
  let dataSource = "serp";
  if (posts.length === 0) {
    console.log("SerpAPI returned nothing, falling back to Reddit...");
    posts = await fetchRedditPosts(normCompany, normRole);
    dataSource = posts.length > 0 ? "reddit" : "ai-generated";
  }

  const sourcePostCount = posts.length;
  const hasRealData = sourcePostCount > 0;
  const postsContext = hasRealData ? formatPostsForPrompt(posts) : "";
  const sourcesFound = hasRealData ? Array.from(new Set<string>(posts.map((p) => p.source))).join(", ") : "";
  console.log(`Data: ${sourcePostCount} posts from [${sourcesFound || "none"}] for ${normCompany} ${normRole}`);

  //Single Gemini prompt — always generates both sections
  const prompt = `You are an expert interview research analyst.

${hasRealData ? `You have ${sourcePostCount} real posts from candidates who interviewed at ${normCompany} for ${normRole} roles. Sources: ${sourcesFound}.

REAL CANDIDATE POSTS:
${postsContext}` : `No real posts were found. Use your knowledge of ${normCompany} ${normRole} interview experiences.`}

Company: ${normCompany} | Role: ${normRole} | Location: ${locationLabel} | Compensation unit: ${currency}

RULES:
1. rounds, tips, collectiveExperience — base on real posts if available, otherwise use your knowledge.
2. compensation — always provide realistic ${currency} ranges based on market data for ${normCompany} ${normRole} in ${locationLabel}. Never return null. Note whether figures came from posts or are estimated.
3. difficultyScore: 1=very easy, 5=very hard.
4. topicFrequencies — never use "Coding" or "DSA" or "Data Structures and Algorithms" as a topic — these are too vague. Always break down into specific subtopics like "Arrays & Strings", "Sliding Window", "Trees & Graphs", "Dynamic Programming", "Binary Search", "Linked Lists", "OOPs", "System Design", "OS/Networking" etc. Percentages must add up to 100%.
5. communityQuestions — extract ONLY questions explicitly mentioned by candidates in the posts. If none found, return empty array [].
6. practiceQuestions — ALWAYS generate 6-8 high-quality practice questions regardless of real data. Cover DSA (3-4), System Design (1), Behavioral (2), CS Fundamentals (1). These should be realistic for ${normCompany} ${normRole}.
7. hasOpenings — set true if actively hiring. careersUrl must be their real careers page URL.
${hasRealData ? `8. In overview, mention this is based on ${sourcePostCount} real candidate reports from ${sourcesFound}.` : ""}

Return ONLY valid JSON, no markdown, no backticks:
{
  "overview": "2-3 sentences about the interview process",
  "difficulty": "Medium",
  "difficultyScore": 3,
  "collectiveExperience": "3-4 sentences: timeline, offer rate, surprises, feedback",
  "rounds": [
    { "name": "Round name", "type": "Phone/Video/Coding/Online", "duration": "X min", "focus": "What is tested" }
  ],
  "topics": ["Specific topic names candidates mentioned"],
  "compensation": {
    "currency": "${currency}",
    "base": "realistic range e.g. 18-24 LPA",
    "totalComp": "realistic total comp range",
    "bonus": "joining or annual bonus info",
    "equity": "RSU/ESOP info or null",
    "note": "state whether from posts or estimated from market data"
  },
  "communityQuestions": [
    { "category": "DSA", "question": "Exact question a candidate mentioned in posts", "difficulty": "Medium", "frequency": 5 }
  ],
  "practiceQuestions": [
    { "category": "DSA", "question": "Good practice question for this company/role", "difficulty": "Medium", "frequency": 8 },
    { "category": "DSA", "question": "Good practice question", "difficulty": "Medium", "frequency": 6 },
    { "category": "DSA", "question": "Good practice question", "difficulty": "Hard", "frequency": 4 },
    { "category": "System Design", "question": "System design question typical for this company", "difficulty": "Medium", "frequency": 5 },
    { "category": "Behavioral", "question": "Behavioral question typical for this company", "difficulty": "Easy", "frequency": 9 },
    { "category": "Behavioral", "question": "Another behavioral question", "difficulty": "Easy", "frequency": 7 },
    { "category": "CS Fundamentals", "question": "CS fundamentals question", "difficulty": "Medium", "frequency": 5 }
  ],
  "topicFrequencies": [
    { "topic": "Arrays & Strings", "count": 12, "percentage": 35 },
    { "topic": "Trees & Graphs", "count": 8, "percentage": 24 },
    { "topic": "Dynamic Programming", "count": 6, "percentage": 18 },
    { "topic": "System Design", "count": 5, "percentage": 15 },
    { "topic": "OS/Networking", "count": 3, "percentage": 8 }
  ],
  "tips": [
    "Specific tip based on what this company actually tests",
    "Tip about their process or culture",
    "Tip about negotiation or the offer stage"
  ],
  "hasOpenings": true,
  "careersUrl": "https://www.amazon.jobs"
}`;

  //Gemini call with model fallback
  const MODELS = ["gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.0-flash-lite"];
  let rawText = "";
  for (const model of MODELS) {
    try {
      const res = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: {
              temperature: hasRealData ? 0.2 : 0.4,
              maxOutputTokens: 8192,
              thinkingConfig: { thinkingBudget: 0 },
            },
          }),
        }
      );
      if (res.status === 503 || res.status === 429) { console.warn(`${model} unavailable, trying next...`); continue; }
      if (!res.ok) continue;
      const data = await res.json();
      rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text || "";
      if (rawText) { console.log(`Success with model: ${model}`); break; }
    } catch { continue; }
  }

  if (!rawText) return NextResponse.json({ error: "All models failed. Try again in a minute." }, { status: 502 });

  let parsed: Record<string, unknown>;
  try {
    const cleaned = rawText.replace(/```json|```/g, "").trim();
    parsed = JSON.parse(cleaned);
  } catch (err) {
    console.error("JSON parse error. Raw:", rawText);
    return NextResponse.json({ error: "AI returned invalid JSON." }, { status: 500 });
  }

  const finalResponse = {
    ...parsed,
    company: normCompany,
    role: normRole,
    location,
    overview: parsed.overview || "",
    difficulty: parsed.difficulty || "Medium",
    difficultyScore: parsed.difficultyScore || 3,
    collectiveExperience: parsed.collectiveExperience || "",
    rounds: Array.isArray(parsed.rounds) ? parsed.rounds : [],
    topics: Array.isArray(parsed.topics) ? parsed.topics : [],
    compensation: parsed.compensation || null,
    communityQuestions: Array.isArray(parsed.communityQuestions) ? parsed.communityQuestions : [],
    practiceQuestions: Array.isArray(parsed.practiceQuestions) ? parsed.practiceQuestions : [],
    topicFrequencies: Array.isArray(parsed.topicFrequencies) ? parsed.topicFrequencies : [],
    tips: Array.isArray(parsed.tips) ? parsed.tips : [],
    hasOpenings: typeof parsed.hasOpenings === "boolean" ? parsed.hasOpenings : false,
    careersUrl: typeof parsed.careersUrl === "string" && parsed.careersUrl.startsWith("http") ? parsed.careersUrl : null,
    sourcePostCount,
    dataSource,
    sourcesFound,
    isCached: false,
    generatedAt: new Date().toISOString(),
  };

  query(
    "INSERT INTO cache (query, response) VALUES ($1, $2) ON CONFLICT (query) DO UPDATE SET response = EXCLUDED.response",
    [cacheKey, finalResponse]
  ).catch((err) => console.error("DB write failed:", err));

  setToMemory(cacheKey, finalResponse);
  return NextResponse.json(finalResponse);
}