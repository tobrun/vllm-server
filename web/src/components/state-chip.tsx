import { Badge } from "@/components/ui/badge";
import { ServerState, stateLabel } from "@/lib/types";

const stateColors: Record<ServerState, string> = {
  running: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  stopped: "bg-zinc-500/20 text-zinc-400 border-zinc-500/30",
  starting: "bg-yellow-500/20 text-yellow-400 border-yellow-500/30",
  stopping: "bg-yellow-500/20 text-yellow-400 border-yellow-500/30",
  error: "bg-red-500/20 text-red-400 border-red-500/30",
  shutting_down: "bg-red-500/20 text-red-400 border-red-500/30",
};

export function StateChip({ state }: { state: ServerState }) {
  return (
    <Badge variant="outline" className={`${stateColors[state]} text-sm px-3 py-1`}>
      {stateLabel[state]}
    </Badge>
  );
}
