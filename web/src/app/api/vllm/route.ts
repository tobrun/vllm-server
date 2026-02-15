import { NextRequest, NextResponse } from "next/server";

interface ProxyRequestBody {
  baseUrl: string;
  path: string;
  method?: "GET" | "POST";
  body?: unknown;
}

function buildTargetUrl(baseUrl: string, path: string): string {
  const normalizedBase = baseUrl.trim().replace(/\/+$/, "");
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${normalizedBase}${normalizedPath}`;
}

export async function POST(req: NextRequest) {
  let payload: ProxyRequestBody;

  try {
    payload = (await req.json()) as ProxyRequestBody;
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  if (!payload.baseUrl || !payload.path) {
    return NextResponse.json({ error: "baseUrl and path are required" }, { status: 400 });
  }

  const method = payload.method ?? "GET";
  if (method !== "GET" && method !== "POST") {
    return NextResponse.json({ error: "Unsupported method" }, { status: 400 });
  }

  let parsedBase: URL;
  try {
    parsedBase = new URL(payload.baseUrl);
  } catch {
    return NextResponse.json({ error: "Invalid baseUrl" }, { status: 400 });
  }

  if (parsedBase.protocol !== "http:" && parsedBase.protocol !== "https:") {
    return NextResponse.json({ error: "baseUrl must use http or https" }, { status: 400 });
  }

  const targetUrl = buildTargetUrl(parsedBase.toString(), payload.path);

  try {
    const upstream = await fetch(targetUrl, {
      method,
      headers: payload.body !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: payload.body !== undefined ? JSON.stringify(payload.body) : undefined,
      cache: "no-store",
    });

    const text = await upstream.text();
    const contentType = upstream.headers.get("content-type") ?? "application/json";

    return new NextResponse(text, {
      status: upstream.status,
      headers: { "content-type": contentType },
    });
  } catch {
    return NextResponse.json(
      { error: "Unable to reach backend. Check server URL and network access." },
      { status: 502 },
    );
  }
}
