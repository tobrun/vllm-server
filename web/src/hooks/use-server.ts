"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";
import { ServerStatus, Model, ServiceStatusResponse } from "@/lib/types";

const POLL_INTERVAL = 5000;
const URL_STORAGE_KEY = "vllm_server_url";
const MODEL_USAGE_PREFIX = "vllm_model_usage_";

export interface ServerState {
  serverUrl: string | null;
  status: ServerStatus | null;
  models: Model[];
  isReachable: boolean;
  isLoading: boolean;
  transitionStartMs: number | null;
  lastError: string | null;
  modelUsage: Record<string, number>;
  serviceStatus: ServiceStatusResponse | null;
  serviceStatusLoading: boolean;
  loadServiceStatus: (lines?: number) => Promise<void>;
}

export function useServer() {
  const [serverUrl, setServerUrlState] = useState<string | null>(null);
  const [status, setStatus] = useState<ServerStatus | null>(null);
  const [models, setModels] = useState<Model[]>([]);
  const [isReachable, setIsReachable] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [transitionStartMs, setTransitionStartMs] = useState<number | null>(null);
  const [lastError, setLastError] = useState<string | null>(null);
  const [modelUsage, setModelUsage] = useState<Record<string, number>>({});
  const [serviceStatus, setServiceStatus] = useState<ServiceStatusResponse | null>(null);
  const [serviceStatusLoading, setServiceStatusLoading] = useState(false);
  const prevStateRef = useRef<string | null>(null);

  // Load server URL and model usage from localStorage on mount
  useEffect(() => {
    const saved = localStorage.getItem(URL_STORAGE_KEY);
    if (saved) setServerUrlState(saved);

    const usage: Record<string, number> = {};
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key?.startsWith(MODEL_USAGE_PREFIX)) {
        const modelId = key.slice(MODEL_USAGE_PREFIX.length);
        usage[modelId] = parseInt(localStorage.getItem(key) || "0", 10);
      }
    }
    setModelUsage(usage);
  }, []);

  const setServerUrl = useCallback((url: string) => {
    const trimmed = url.trim().replace(/\/+$/, "");
    localStorage.setItem(URL_STORAGE_KEY, trimmed);
    setServerUrlState(trimmed);
  }, []);

  const recordModelUsage = useCallback((modelId: string) => {
    const ts = Date.now();
    localStorage.setItem(`${MODEL_USAGE_PREFIX}${modelId}`, ts.toString());
    setModelUsage((prev) => ({ ...prev, [modelId]: ts }));
  }, []);

  // Polling
  useEffect(() => {
    if (!serverUrl) return;

    let mounted = true;
    const poll = async () => {
      try {
        const [statusRes, modelsRes] = await Promise.all([
          api.getStatus(serverUrl),
          api.getModels(serverUrl),
        ]);
        if (!mounted) return;

        setStatus(statusRes);
        setModels(modelsRes.models);
        setIsReachable(true);

        // Track transition timer
        const currentState = statusRes.state;
        const isTransitional = currentState === "starting" || currentState === "stopping";
        const wasTransitional = prevStateRef.current === "starting" || prevStateRef.current === "stopping";

        if (isTransitional && !wasTransitional) {
          setTransitionStartMs(Date.now());
        } else if (!isTransitional) {
          setTransitionStartMs(null);
        }
        prevStateRef.current = currentState;
      } catch {
        if (!mounted) return;
        setIsReachable(false);
      }
    };

    poll();
    const id = setInterval(poll, POLL_INTERVAL);
    return () => {
      mounted = false;
      clearInterval(id);
    };
  }, [serverUrl]);

  const fireAction = useCallback(
    async (action: () => Promise<unknown>, errorLabel: string) => {
      setIsLoading(true);
      try {
        await action();
      } catch (e) {
        setLastError(`${errorLabel}: ${e instanceof Error ? e.message : "Unknown error"}`);
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  const start = useCallback(
    () => fireAction(() => api.start(serverUrl!), "Start failed"),
    [serverUrl, fireAction],
  );
  const stop = useCallback(
    () => fireAction(() => api.stop(serverUrl!), "Stop failed"),
    [serverUrl, fireAction],
  );
  const restart = useCallback(
    () => fireAction(() => api.restart(serverUrl!), "Restart failed"),
    [serverUrl, fireAction],
  );
  const switchModel = useCallback(
    (modelId: string) =>
      fireAction(async () => {
        await api.switchModel(serverUrl!, modelId);
        recordModelUsage(modelId);
      }, "Switch failed"),
    [serverUrl, fireAction, recordModelUsage],
  );
  const shutdown = useCallback(
    () => fireAction(() => api.shutdown(serverUrl!), "Shutdown failed"),
    [serverUrl, fireAction],
  );

  const loadServiceStatus = useCallback(async (lines = 120) => {
    if (!serverUrl) return;
    setServiceStatusLoading(true);
    try {
      const payload = await api.getServiceStatus(serverUrl, lines);
      setServiceStatus(payload);
    } catch (e) {
      setLastError(`Service status failed: ${e instanceof Error ? e.message : "Unknown error"}`);
    } finally {
      setServiceStatusLoading(false);
    }
  }, [serverUrl]);

  const clearError = useCallback(() => setLastError(null), []);

  return {
    serverUrl,
    setServerUrl,
    status,
    models,
    isReachable,
    isLoading,
    transitionStartMs,
    lastError,
    clearError,
    modelUsage,
    serviceStatus,
    serviceStatusLoading,
    start,
    stop,
    restart,
    switchModel,
    shutdown,
    loadServiceStatus,
  };
}
