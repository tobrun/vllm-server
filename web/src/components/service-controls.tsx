"use client";

import { Button } from "@/components/ui/button";
import { ServerState } from "@/lib/types";
import { Play, Square, RotateCcw } from "lucide-react";

interface ServiceControlsProps {
  state: ServerState;
  isLoading: boolean;
  onStart: () => void;
  onStop: () => void;
  onRestart: () => void;
}

export function ServiceControls({ state, isLoading, onStart, onStop, onRestart }: ServiceControlsProps) {
  const isRunning = state === "running";
  const isStopped = state === "stopped" || state === "error";
  const isBusy = state === "starting" || state === "stopping" || state === "shutting_down";

  return (
    <div className="flex gap-3">
      {isStopped && (
        <Button
          onClick={onStart}
          disabled={isLoading || isBusy}
          className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white"
        >
          <Play className="h-4 w-4 mr-2" />
          Start
        </Button>
      )}
      {isRunning && (
        <>
          <Button
            onClick={onStop}
            disabled={isLoading || isBusy}
            variant="secondary"
            className="flex-1"
          >
            <Square className="h-4 w-4 mr-2" />
            Stop
          </Button>
          <Button
            onClick={onRestart}
            disabled={isLoading || isBusy}
            variant="secondary"
            className="flex-1"
          >
            <RotateCcw className="h-4 w-4 mr-2" />
            Restart
          </Button>
        </>
      )}
      {isBusy && (
        <Button disabled className="flex-1" variant="secondary">
          <div className="h-4 w-4 mr-2 animate-spin rounded-full border-2 border-current border-t-transparent" />
          Working...
        </Button>
      )}
    </div>
  );
}
