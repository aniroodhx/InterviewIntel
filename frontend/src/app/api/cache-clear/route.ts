import { query } from "@/lib/db";
import { NextRequest, NextResponse } from "next/server";

// DELETE /api/cache-clear?all=true  — clears everything
// DELETE /api/cache-clear?key=amazon-software engineer 1-in  — clears one entry
export async function DELETE(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const key = searchParams.get("key");
  const all = searchParams.get("all");
  try {
    if (all === "true") {
      await query("DELETE FROM cache", []);
      return NextResponse.json({ message: "All cache cleared" });
    }
    if (key) {
      await query("DELETE FROM cache WHERE query = $1", [key.toLowerCase().trim()]);
      return NextResponse.json({ message: `Cache cleared for: ${key}` });
    }
    return NextResponse.json({ error: "Provide ?key=... or ?all=true" }, { status: 400 });
  } catch (err) {
    return NextResponse.json({ error: "DB error" }, { status: 500 });
  }
}

// GET /api/cache-clear — list all cached keys
export async function GET() {
  try {
    const res = await query("SELECT query, created_at FROM cache ORDER BY created_at DESC", []);
    return NextResponse.json({ count: res.rows.length, keys: res.rows });
  } catch (err) {
    return NextResponse.json({ error: "DB error" }, { status: 500 });
  }
}