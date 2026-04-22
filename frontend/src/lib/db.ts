import { Pool } from "pg";

declare global {
  //Prevent multiple connections in dev 
  var pgPool: Pool | undefined;
}

const pool =
  global.pgPool ||
  new Pool({
    connectionString: process.env.DATABASE_URL,
    max: 10,
    ssl:
      process.env.NODE_ENV === "production"
        ? { rejectUnauthorized: false }
        : false,
  });

//Reuse connection in development
if (process.env.NODE_ENV !== "production") {
  global.pgPool = pool;
}

export async function query(text: string, params?: any[]) {
  const start = Date.now();

  try {
    const res = await pool.query(text, params);
    return res;
  } catch (err) {
    console.error("DB ERROR:", err);
    throw err;
  } finally {
    const duration = Date.now() - start;
    console.log("DB query executed in", duration, "ms");
  }
}