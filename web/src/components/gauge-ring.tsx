"use client";

import { useEffect, useState } from "react";

interface GaugeRingProps {
  value: number; // 0-100
  label: string;
  unit: string;
  rawValue?: string;
  thresholds?: { yellow: number; red: number };
}

export function GaugeRing({
  value,
  label,
  unit,
  rawValue,
  thresholds = { yellow: 80, red: 95 },
}: GaugeRingProps) {
  const [animated, setAnimated] = useState(0);

  useEffect(() => {
    const timeout = setTimeout(() => setAnimated(value), 50);
    return () => clearTimeout(timeout);
  }, [value]);

  const size = 120;
  const stroke = 10;
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const startAngle = 135;
  const sweepAngle = 270;
  const progress = Math.min(Math.max(animated, 0), 100) / 100;
  const dashOffset = circumference * (1 - (progress * sweepAngle) / 360);

  const color =
    animated >= thresholds.red
      ? "#ef4444"
      : animated >= thresholds.yellow
        ? "#eab308"
        : "#2dd4bf";

  const bgColor = "rgba(255,255,255,0.08)";

  return (
    <div className="flex flex-col items-center gap-1">
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="transform -rotate-[0deg]">
          {/* Background arc */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke={bgColor}
            strokeWidth={stroke}
            strokeDasharray={`${(circumference * sweepAngle) / 360} ${circumference}`}
            strokeDashoffset={0}
            strokeLinecap="round"
            transform={`rotate(${startAngle} ${size / 2} ${size / 2})`}
          />
          {/* Value arc */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke={color}
            strokeWidth={stroke}
            strokeDasharray={`${(circumference * sweepAngle) / 360} ${circumference}`}
            strokeDashoffset={dashOffset}
            strokeLinecap="round"
            transform={`rotate(${startAngle} ${size / 2} ${size / 2})`}
            className="transition-all duration-300 ease-out"
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xl font-bold tabular-nums" style={{ color }}>
            {rawValue ?? `${Math.round(animated)}`}
          </span>
          <span className="text-xs text-muted-foreground">{unit}</span>
        </div>
      </div>
      <span className="text-sm text-muted-foreground">{label}</span>
    </div>
  );
}
