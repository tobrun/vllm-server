"use client";

import { Model } from "@/lib/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface ModelListProps {
  models: Model[];
  modelUsage: Record<string, number>;
  isLoading: boolean;
  currentState: string;
  onSwitch: (modelId: string) => void;
}

export function ModelList({ models, modelUsage, isLoading, currentState, onSwitch }: ModelListProps) {
  const sorted = [...models].sort((a, b) => {
    const aTime = modelUsage[a.id] ?? 0;
    const bTime = modelUsage[b.id] ?? 0;
    return bTime - aTime;
  });

  const canSwitch = currentState === "running" || currentState === "stopped";

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">Models</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-1">
        {sorted.map((model) => (
          <button
            key={model.id}
            onClick={() => !model.active && canSwitch && onSwitch(model.id)}
            disabled={model.active || isLoading || !canSwitch}
            className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors ${
              model.active
                ? "bg-primary/10 border border-primary/30"
                : canSwitch && !isLoading
                  ? "hover:bg-accent cursor-pointer"
                  : "opacity-50 cursor-not-allowed"
            }`}
          >
            <div
              className={`h-3 w-3 rounded-full border-2 flex items-center justify-center ${
                model.active ? "border-primary" : "border-muted-foreground"
              }`}
            >
              {model.active && <div className="h-1.5 w-1.5 rounded-full bg-primary" />}
            </div>
            <span className="text-sm font-mono">{model.id}</span>
          </button>
        ))}
        {sorted.length === 0 && (
          <span className="text-sm text-muted-foreground py-2">No models configured</span>
        )}
      </CardContent>
    </Card>
  );
}
