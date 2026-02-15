"use client";

import { useState } from "react";
import { useServer } from "@/hooks/use-server";
import { ServerUrlPrompt } from "@/components/server-url-prompt";
import { StatusSection } from "@/components/status-section";
import { GpuGauges } from "@/components/gpu-gauges";
import { ModelList } from "@/components/model-list";
import { ServiceControls } from "@/components/service-controls";
import { ServiceStatusDialog } from "@/components/service-status-dialog";
import { ShutdownSection } from "@/components/shutdown-section";
import { UnreachableBanner } from "@/components/unreachable-banner";
import { SettingsDialog } from "@/components/settings-dialog";

export default function Dashboard() {
  const {
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
  } = useServer();
  const [showServiceStatus, setShowServiceStatus] = useState(false);

  if (!serverUrl) {
    return <ServerUrlPrompt onSubmit={setServerUrl} />;
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b border-border bg-background/80 backdrop-blur-sm">
        <div className="mx-auto flex h-14 max-w-lg items-center justify-between px-4">
          <h1 className="text-lg font-semibold">vLLM Remote</h1>
          <SettingsDialog currentUrl={serverUrl} onSave={setServerUrl} />
        </div>
      </header>

      {/* Content */}
      <main className="mx-auto flex max-w-lg flex-col gap-4 p-4">
        {/* Error toast */}
        {lastError && (
          <div
            className="flex items-center justify-between rounded-lg bg-red-500/10 border border-red-500/30 px-4 py-3 text-sm text-red-400 cursor-pointer"
            onClick={clearError}
          >
            <span>{lastError}</span>
            <span className="text-xs ml-2">dismiss</span>
          </div>
        )}

        {/* Status */}
        {status && (
          <>
            <StatusSection status={status} transitionStartMs={transitionStartMs} />

            {/* GPU Gauges */}
            {status.gpu && <GpuGauges gpu={status.gpu} />}

            {/* Models */}
            <ModelList
              models={models}
              modelUsage={modelUsage}
              isLoading={isLoading}
              currentState={status.state}
              onSwitch={switchModel}
            />

            {/* Service Controls */}
            <ServiceControls
              state={status.state}
              isLoading={isLoading}
              onStart={start}
              onStop={stop}
              onRestart={restart}
              onServiceStatus={() => {
                setShowServiceStatus(true);
                void loadServiceStatus(120);
              }}
            />

            {/* Shutdown */}
            <ShutdownSection isLoading={isLoading} onShutdown={shutdown} />
          </>
        )}

        {/* Loading state before first poll */}
        {!status && isReachable && (
          <div className="flex items-center justify-center py-12">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          </div>
        )}

        {/* Unreachable */}
        {!isReachable && <UnreachableBanner />}
      </main>

      <ServiceStatusDialog
        open={showServiceStatus}
        status={serviceStatus}
        isLoading={serviceStatusLoading}
        onOpenChange={setShowServiceStatus}
        onRefresh={(lines) => void loadServiceStatus(lines)}
      />
    </div>
  );
}
