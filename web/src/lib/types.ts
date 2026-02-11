export type ServerState =
  | "running"
  | "stopped"
  | "starting"
  | "stopping"
  | "error"
  | "shutting_down";

export interface GpuStats {
  fan_speed_percent: number;
  memory_used_mb: number;
  memory_total_mb: number;
  temperature_c: number;
  gpu_count: number;
}

export interface ServerStatus {
  state: ServerState;
  model: string | null;
  error: string | null;
  gpu: GpuStats | null;
}

export interface Model {
  id: string;
  script: string;
  active: boolean;
}

export interface ModelsResponse {
  models: Model[];
}

export const stateLabel: Record<ServerState, string> = {
  running: "Running",
  stopped: "Stopped",
  starting: "Starting",
  stopping: "Stopping",
  error: "Error",
  shutting_down: "Shutting Down",
};
