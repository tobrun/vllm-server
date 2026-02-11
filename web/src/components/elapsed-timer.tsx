"use client";

import { useEffect, useState } from "react";

export function ElapsedTimer({ startMs }: { startMs: number }) {
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    const update = () => setElapsed(Math.floor((Date.now() - startMs) / 1000));
    update();
    const id = setInterval(update, 1000);
    return () => clearInterval(id);
  }, [startMs]);

  const mins = Math.floor(elapsed / 60);
  const secs = elapsed % 60;
  const display = mins > 0 ? `${mins}m ${secs}s` : `${secs}s`;

  return (
    <span className="text-sm text-muted-foreground tabular-nums">
      {display}
    </span>
  );
}
