"use client";

import { GpuStats } from "@/lib/types";
import { GaugeRing } from "./gauge-ring";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function GpuGauges({ gpu }: { gpu: GpuStats }) {
  const memPercent =
    gpu.memory_total_mb > 0
      ? (gpu.memory_used_mb / gpu.memory_total_mb) * 100
      : 0;
  const memLabel = `${(gpu.memory_used_mb / 1024).toFixed(1)} / ${(gpu.memory_total_mb / 1024).toFixed(1)} GB`;

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          GPU{gpu.gpu_count > 1 ? ` (${gpu.gpu_count}x)` : ""}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex justify-around items-center">
          <GaugeRing
            value={gpu.fan_speed_percent}
            label="Fan"
            unit="%"
            thresholds={{ yellow: 80, red: 95 }}
          />
          <GaugeRing
            value={memPercent}
            label="VRAM"
            unit={memLabel}
            rawValue={`${Math.round(memPercent)}%`}
            thresholds={{ yellow: 80, red: 95 }}
          />
          <GaugeRing
            value={gpu.temperature_c}
            label="Temp"
            unit={`${gpu.temperature_c}°C`}
            rawValue={`${gpu.temperature_c}`}
            thresholds={{ yellow: 70, red: 85 }}
          />
        </div>
      </CardContent>
    </Card>
  );
}
