import { ServerStatus, ModelsResponse, ServiceStatusResponse } from "./types";

async function request<T>(baseUrl: string, path: string, method = "GET", body?: unknown): Promise<T> {
  const res = await fetch("/api/vllm", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      baseUrl,
      path,
      method,
      body,
    }),
  });

  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(text || `HTTP ${res.status}`);
  }

  return (await res.json()) as T;
}

export const api = {
  getStatus: (baseUrl: string) => request<ServerStatus>(baseUrl, "/status"),
  getModels: (baseUrl: string) => request<ModelsResponse>(baseUrl, "/models"),
  getServiceStatus: (baseUrl: string, lines = 120) =>
    request<ServiceStatusResponse>(baseUrl, `/service/status?lines=${lines}`),
  start: (baseUrl: string) => request<unknown>(baseUrl, "/start", "POST"),
  stop: (baseUrl: string) => request<unknown>(baseUrl, "/stop", "POST"),
  restart: (baseUrl: string) => request<unknown>(baseUrl, "/restart", "POST"),
  switchModel: (baseUrl: string, modelId: string) =>
    request<unknown>(baseUrl, "/switch", "POST", { model: modelId }),
  shutdown: (baseUrl: string) => request<unknown>(baseUrl, "/shutdown", "POST"),
};
