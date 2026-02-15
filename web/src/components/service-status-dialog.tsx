"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ServiceStatusResponse } from "@/lib/types";

interface ServiceStatusDialogProps {
  open: boolean;
  status: ServiceStatusResponse | null;
  isLoading: boolean;
  onOpenChange: (open: boolean) => void;
  onRefresh: (lines: number) => void;
}

export function ServiceStatusDialog({
  open,
  status,
  isLoading,
  onOpenChange,
  onRefresh,
}: ServiceStatusDialogProps) {
  const [lines, setLines] = useState("120");
  const [view, setView] = useState<"both" | "systemctl" | "journal">("both");

  useEffect(() => {
    if (open && status?.lines) {
      setLines(String(status.lines));
    }
  }, [open, status?.lines]);

  const parsedLines = Math.max(1, Math.min(500, parseInt(lines, 10) || 120));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Service Status</DialogTitle>
        </DialogHeader>
        <div className="flex flex-wrap items-center gap-2">
          <Input
            type="number"
            min={1}
            max={500}
            value={lines}
            onChange={(e) => setLines(e.target.value)}
            className="w-28"
          />
          <Button
            variant={view === "both" ? "default" : "secondary"}
            onClick={() => setView("both")}
          >
            Both
          </Button>
          <Button
            variant={view === "systemctl" ? "default" : "secondary"}
            onClick={() => setView("systemctl")}
          >
            systemctl
          </Button>
          <Button
            variant={view === "journal" ? "default" : "secondary"}
            onClick={() => setView("journal")}
          >
            journalctl
          </Button>
        </div>
        <div className="max-h-[65vh] space-y-3 overflow-y-auto pr-1 text-xs">
          {isLoading && <p className="text-muted-foreground">Loading latest service output...</p>}
          {!isLoading && !status && <p className="text-muted-foreground">No service output loaded yet.</p>}
          {!isLoading && status && (
            <>
              <p className="text-muted-foreground">
                {status.service} | {status.lines} lines | {status.generated_at}
              </p>
              {(view === "both" || view === "systemctl") && (
                <pre className="rounded-md border border-border bg-black/30 p-3 whitespace-pre-wrap">
                  {"--- systemctl status ---\n"}
                  {status.systemctl_status_output}
                </pre>
              )}
              {(view === "both" || view === "journal") && (
                <pre className="rounded-md border border-border bg-black/30 p-3 whitespace-pre-wrap">
                  {"--- journalctl ---\n"}
                  {status.journal_output}
                </pre>
              )}
            </>
          )}
        </div>
        <DialogFooter>
          <Button variant="secondary" onClick={() => onOpenChange(false)}>
            Close
          </Button>
          <Button onClick={() => onRefresh(parsedLines)} disabled={isLoading}>
            Refresh
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
