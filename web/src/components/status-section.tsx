"use client";

import { ServerStatus } from "@/lib/types";
import { StateChip } from "./state-chip";
import { ElapsedTimer } from "./elapsed-timer";
import { Card, CardContent } from "@/components/ui/card";

interface StatusSectionProps {
  status: ServerStatus;
  transitionStartMs: number | null;
}

export function StatusSection({ status, transitionStartMs }: StatusSectionProps) {
  const isTransitional = status.state === "starting" || status.state === "stopping";

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-muted-foreground">Status</span>
          <StateChip state={status.state} />
        </div>
        {status.model && (
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-muted-foreground">Model</span>
            <span className="text-sm font-mono">{status.model}</span>
          </div>
        )}
        {status.error && (
          <div className="text-sm text-red-400 bg-red-500/10 rounded-md px-3 py-2">
            {status.error}
          </div>
        )}
        {isTransitional && transitionStartMs && (
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-muted-foreground">Elapsed</span>
            <ElapsedTimer startMs={transitionStartMs} />
          </div>
        )}
      </CardContent>
    </Card>
  );
}
