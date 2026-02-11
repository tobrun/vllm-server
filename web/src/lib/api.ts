import { ServerStatus, ModelsResponse } from "./types";

async function request<T>(baseUrl: string, path: string, method = "GET", body?: unknown): Promise<T> {
  const url = `${baseUrl.replace(/\/+$/, "")}${path}`;
  const res = await fetch(url, {
    method,
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json();
}

export const api = {
  getStatus: (baseUrl: string) => request<ServerStatus>(baseUrl, "/status"),
  getModels: (baseUrl: string) => request<ModelsResponse>(baseUrl, "/models"),
  start: (baseUrl: string) => request<unknown>(baseUrl, "/start", "POST"),
  stop: (baseUrl: string) => request<unknown>(baseUrl, "/stop", "POST"),
  restart: (baseUrl: string) => request<unknown>(baseUrl, "/restart", "POST"),
  switchModel: (baseUrl: string, modelId: string) =>
    request<unknown>(baseUrl, "/switch", "POST", { model: modelId }),
  shutdown: (baseUrl: string) => request<unknown>(baseUrl, "/shutdown", "POST"),
};
