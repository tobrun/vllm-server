import asyncio
import json
import logging
import subprocess
import sys
from pathlib import Path
from typing import Optional

import yaml
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    stream=sys.stdout,
)
logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).parent
CONFIG_PATH = BASE_DIR / "config.yaml"
STATE_PATH = BASE_DIR / "state.json"
VLLM_ENV_PATH = Path("/etc/vllm-manager/vllm.env")
VLLM_SERVICE = "vllm.service"
VLLM_HEALTH_URL = "http://localhost:8000/health"
HEALTH_POLL_INTERVAL = 5
HEALTH_POLL_TIMEOUT = 900  # 15 minutes
SHUTDOWN_DELAY = 10


class SwitchRequest(BaseModel):
    model: str


class AppState:
    def __init__(self):
        self.state: str = "stopped"
        self.current_model: Optional[str] = None
        self.error_message: Optional[str] = None
        self._state_lock = asyncio.Lock()

    async def set_state(self, state: str, model: Optional[str] = None, error: Optional[str] = None):
        async with self._state_lock:
            self.state = state
            if model is not None:
                self.current_model = model
            self.error_message = error
            logger.info(f"State transition: {state}, model={self.current_model}, error={error}")


app_state = AppState()
app = FastAPI(title="vLLM Manager", version="1.0.0")


def load_config() -> dict:
    if not CONFIG_PATH.exists():
        raise HTTPException(status_code=500, detail=f"Config file not found: {CONFIG_PATH}")
    with open(CONFIG_PATH) as f:
        return yaml.safe_load(f)


def load_state() -> dict:
    if STATE_PATH.exists():
        with open(STATE_PATH) as f:
            return json.load(f)
    return {}


def save_state(state: dict):
    with open(STATE_PATH, "w") as f:
        json.dump(state, f, indent=2)


def run_systemctl(action: str) -> tuple[int, str, str]:
    cmd = ["sudo", "systemctl", action, VLLM_SERVICE]
    logger.info(f"Running: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.returncode, result.stdout, result.stderr


def get_systemd_state() -> str:
    result = subprocess.run(
        ["systemctl", "is-active", VLLM_SERVICE],
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def update_vllm_env(script_path: str):
    VLLM_ENV_PATH.parent.mkdir(parents=True, exist_ok=True)
    content = f"MODEL_SCRIPT={script_path}\n"
    logger.info(f"Updating {VLLM_ENV_PATH} with script: {script_path}")
    # Write via sudo since /etc/vllm-manager may require root
    subprocess.run(
        ["sudo", "tee", str(VLLM_ENV_PATH)],
        input=content,
        capture_output=True,
        text=True,
        check=True,
    )


def daemon_reload():
    subprocess.run(["sudo", "systemctl", "daemon-reload"], check=True)


def get_gpu_stats() -> Optional[dict]:
    try:
        result = subprocess.run(
            [
                "nvidia-smi",
                "--query-gpu=utilization.gpu,memory.used,memory.total,temperature.gpu",
                "--format=csv,noheader,nounits",
            ],
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            return None

        # Parse first GPU (or aggregate if multiple)
        lines = result.stdout.strip().split("\n")
        if not lines or not lines[0]:
            return None

        # Sum across all GPUs
        total_util = 0
        total_mem_used = 0
        total_mem_total = 0
        max_temp = 0
        gpu_count = 0

        for line in lines:
            parts = [p.strip() for p in line.split(",")]
            if len(parts) >= 4:
                total_util += int(parts[0])
                total_mem_used += int(parts[1])
                total_mem_total += int(parts[2])
                max_temp = max(max_temp, int(parts[3]))
                gpu_count += 1

        if gpu_count == 0:
            return None

        return {
            "utilization_percent": total_util // gpu_count,
            "memory_used_mb": total_mem_used,
            "memory_total_mb": total_mem_total,
            "temperature_c": max_temp,
            "gpu_count": gpu_count,
        }
    except Exception as e:
        logger.warning(f"Failed to get GPU stats: {e}")
        return None


def validate_model_files(script_path: str) -> tuple[bool, Optional[str]]:
    script = Path(script_path)
    if not script.exists():
        return False, f"Script not found: {script_path}"
    if not script.is_file():
        return False, f"Script is not a file: {script_path}"
    return True, None


async def check_vllm_health() -> bool:
    try:
        proc = await asyncio.create_subprocess_exec(
            "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}", VLLM_HEALTH_URL,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        stdout, _ = await proc.communicate()
        return stdout.decode().strip() == "200"
    except Exception:
        return False


async def wait_for_vllm_ready():
    """Poll vLLM health endpoint until ready or timeout."""
    elapsed = 0
    while elapsed < HEALTH_POLL_TIMEOUT:
        if await check_vllm_health():
            return True
        await asyncio.sleep(HEALTH_POLL_INTERVAL)
        elapsed += HEALTH_POLL_INTERVAL
        logger.info(f"Waiting for vLLM to be ready... ({elapsed}s)")
    return False


async def start_vllm_async(model_id: str, script_path: str):
    """Background task to start vLLM and wait for it to be ready."""
    try:
        await app_state.set_state("starting", model=model_id)

        update_vllm_env(script_path)
        daemon_reload()

        returncode, stdout, stderr = run_systemctl("start")
        if returncode != 0:
            await app_state.set_state("error", error=f"Failed to start vLLM: {stderr}")
            return

        if await wait_for_vllm_ready():
            await app_state.set_state("running")
            save_state({"last_model": model_id})
        else:
            await app_state.set_state("error", error="vLLM health check timeout")
    except Exception as e:
        await app_state.set_state("error", error=str(e))


async def stop_vllm_async():
    """Background task to stop vLLM."""
    try:
        await app_state.set_state("stopping")
        returncode, stdout, stderr = run_systemctl("stop")
        if returncode != 0:
            await app_state.set_state("error", error=f"Failed to stop vLLM: {stderr}")
        else:
            await app_state.set_state("stopped")
    except Exception as e:
        await app_state.set_state("error", error=str(e))


@app.on_event("startup")
async def startup_event():
    """Auto-start vLLM with the last-selected model on backend startup."""
    logger.info("vLLM Manager starting up...")

    # Check if vLLM is already running
    systemd_state = get_systemd_state()
    if systemd_state == "active":
        state = load_state()
        model_id = state.get("last_model")
        await app_state.set_state("running", model=model_id)
        logger.info(f"vLLM already running with model: {model_id}")
        return

    # Load last model and auto-start
    state = load_state()
    model_id = state.get("last_model")

    if not model_id:
        config = load_config()
        models = config.get("models", {})
        if models:
            model_id = next(iter(models.keys()))
            logger.info(f"No last model, using first configured: {model_id}")
        else:
            logger.warning("No models configured, staying stopped")
            return

    config = load_config()
    models = config.get("models", {})
    if model_id not in models:
        logger.error(f"Last model '{model_id}' not in config, staying stopped")
        return

    script_path = models[model_id]["script"]
    asyncio.create_task(start_vllm_async(model_id, script_path))


@app.get("/status")
async def get_status():
    gpu = get_gpu_stats()
    response = {
        "state": app_state.state,
        "model": app_state.current_model,
    }
    if app_state.error_message:
        response["error"] = app_state.error_message
    if gpu:
        response["gpu"] = gpu
    return response


@app.get("/models")
async def get_models():
    config = load_config()
    models = config.get("models", {})
    return {
        "models": [
            {
                "id": model_id,
                "script": model_config["script"],
                "active": model_id == app_state.current_model,
            }
            for model_id, model_config in models.items()
        ]
    }


@app.post("/stop")
async def stop_service():
    if app_state.state in ("stopped", "stopping"):
        return {"status": app_state.state, "previous_model": app_state.current_model}

    previous_model = app_state.current_model
    asyncio.create_task(stop_vllm_async())

    return {"status": "stopping", "previous_model": previous_model}


@app.post("/start")
async def start_service():
    if app_state.state in ("running", "starting"):
        return {"status": app_state.state, "model": app_state.current_model}

    state = load_state()
    model_id = state.get("last_model")

    if not model_id:
        config = load_config()
        models = config.get("models", {})
        if not models:
            raise HTTPException(status_code=400, detail="No models configured")
        model_id = next(iter(models.keys()))

    config = load_config()
    models = config.get("models", {})
    if model_id not in models:
        raise HTTPException(status_code=400, detail=f"Model '{model_id}' not found in config")

    script_path = models[model_id]["script"]
    asyncio.create_task(start_vllm_async(model_id, script_path))

    return {"status": "starting", "model": model_id}


@app.post("/restart")
async def restart_service():
    if app_state.state == "starting":
        return {"status": "starting", "model": app_state.current_model}

    model_id = app_state.current_model
    if not model_id:
        state = load_state()
        model_id = state.get("last_model")

    if not model_id:
        raise HTTPException(status_code=400, detail="No model to restart")

    config = load_config()
    models = config.get("models", {})
    if model_id not in models:
        raise HTTPException(status_code=400, detail=f"Model '{model_id}' not found in config")

    script_path = models[model_id]["script"]

    async def restart_async():
        await stop_vllm_async()
        # Wait for stop to complete
        while app_state.state == "stopping":
            await asyncio.sleep(0.5)
        if app_state.state == "stopped":
            await start_vllm_async(model_id, script_path)

    asyncio.create_task(restart_async())
    return {"status": "restarting", "model": model_id}


@app.post("/switch")
async def switch_model(request: SwitchRequest):
    config = load_config()
    models = config.get("models", {})

    if request.model not in models:
        raise HTTPException(status_code=404, detail=f"Model '{request.model}' not found in config")

    script_path = models[request.model]["script"]

    # Validate model files before switching
    valid, error = validate_model_files(script_path)
    if not valid:
        raise HTTPException(status_code=400, detail=f"Model validation failed: {error}")

    previous_model = app_state.current_model

    if app_state.state in ("running", "starting"):
        # Need to stop first, then start with new model
        async def switch_async():
            await stop_vllm_async()
            while app_state.state == "stopping":
                await asyncio.sleep(0.5)
            if app_state.state in ("stopped", "error"):
                await start_vllm_async(request.model, script_path)

        asyncio.create_task(switch_async())
    else:
        # Can start directly
        asyncio.create_task(start_vllm_async(request.model, script_path))

    return {
        "status": "switching",
        "previous_model": previous_model,
        "new_model": request.model,
    }


@app.post("/shutdown")
async def shutdown_server():
    async def delayed_shutdown():
        logger.info(f"Server shutdown scheduled in {SHUTDOWN_DELAY} seconds")
        await asyncio.sleep(SHUTDOWN_DELAY)
        logger.info("Executing shutdown...")
        subprocess.run(["sudo", "shutdown", "now"])

    asyncio.create_task(delayed_shutdown())
    return {
        "status": "shutting_down",
        "message": f"Server will shut down in {SHUTDOWN_DELAY} seconds",
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=9090)
