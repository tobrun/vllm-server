import asyncio
import json
import logging
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

import yaml
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
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
HEALTH_REQUEST_TIMEOUT = 3
SERVICE_LOG_DEFAULT_LINES = 120
SERVICE_LOG_MAX_LINES = 500
SHUTDOWN_DELAY = 10


class SwitchRequest(BaseModel):
    model: str


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


class AppState:
    def __init__(self):
        self.state: str = "stopped"
        self.current_model: Optional[str] = None
        self.error_message: Optional[str] = None
        self.last_state_change_at: str = utc_now_iso()
        self.last_reconciled_at: Optional[str] = None
        self.last_health_http_code: Optional[int] = None
        self.last_health_error: Optional[str] = None
        self.last_systemd_active_state: Optional[str] = None
        self.last_systemd_sub_state: Optional[str] = None
        self.last_systemd_result: Optional[str] = None
        self.last_systemd_exec_main_status: Optional[str] = None
        self.last_inferred_state: Optional[str] = None
        self.last_inference_reason: Optional[str] = None
        self._state_lock = asyncio.Lock()

    async def set_state(self, state: str, model: Optional[str] = None, error: Optional[str] = None):
        async with self._state_lock:
            self.state = state
            if model is not None:
                self.current_model = model
            self.error_message = error
            self.last_state_change_at = utc_now_iso()
            logger.info(f"State transition: {state}, model={self.current_model}, error={error}")

    async def update_runtime_checks(
        self,
        systemd_props: dict,
        health_http_code: Optional[int],
        health_error: Optional[str],
        inferred_state: str,
        inference_reason: Optional[str],
    ):
        async with self._state_lock:
            self.last_reconciled_at = utc_now_iso()
            self.last_health_http_code = health_http_code
            self.last_health_error = health_error
            self.last_systemd_active_state = systemd_props.get("active_state")
            self.last_systemd_sub_state = systemd_props.get("sub_state")
            self.last_systemd_result = systemd_props.get("result")
            self.last_systemd_exec_main_status = systemd_props.get("exec_main_status")
            self.last_inferred_state = inferred_state
            self.last_inference_reason = inference_reason


app_state = AppState()
app = FastAPI(title="vLLM Manager", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


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
    return get_systemd_properties().get("active_state", "unknown")


def get_systemd_properties() -> dict:
    defaults = {
        "active_state": "unknown",
        "sub_state": "unknown",
        "result": "unknown",
        "exec_main_status": None,
        "exec_main_code": None,
    }
    try:
        result = subprocess.run(
            [
                "systemctl",
                "show",
                VLLM_SERVICE,
                "--property=ActiveState,SubState,Result,ExecMainStatus,ExecMainCode",
                "--no-pager",
            ],
            capture_output=True,
            text=True,
            timeout=5,
        )
        if result.returncode != 0:
            logger.warning(f"Failed to query systemd state: {result.stderr.strip()}")
            return defaults

        props = defaults.copy()
        for line in result.stdout.splitlines():
            if "=" not in line:
                continue
            key, value = line.split("=", 1)
            value = value.strip() or None
            if key == "ActiveState":
                props["active_state"] = value or "unknown"
            elif key == "SubState":
                props["sub_state"] = value or "unknown"
            elif key == "Result":
                props["result"] = value or "unknown"
            elif key == "ExecMainStatus":
                props["exec_main_status"] = value
            elif key == "ExecMainCode":
                props["exec_main_code"] = value
        return props
    except Exception as e:
        logger.warning(f"Failed to read systemd properties: {e}")
        return defaults


def get_service_output(lines: int = SERVICE_LOG_DEFAULT_LINES) -> dict:
    safe_lines = max(1, min(lines, SERVICE_LOG_MAX_LINES))

    try:
        status_result = subprocess.run(
            ["systemctl", "status", VLLM_SERVICE, "--no-pager", "-n", str(safe_lines)],
            capture_output=True,
            text=True,
            timeout=8,
        )
        status_output = status_result.stdout.strip() or status_result.stderr.strip()
        if not status_output:
            status_output = "No status output available"
    except Exception as e:
        status_output = f"Failed to read systemctl status: {e}"

    try:
        journal_result = subprocess.run(
            ["journalctl", "-u", VLLM_SERVICE, "--no-pager", "-n", str(safe_lines)],
            capture_output=True,
            text=True,
            timeout=8,
        )
        journal_output = journal_result.stdout.strip() or journal_result.stderr.strip()
        if not journal_output:
            journal_output = "No journal output available"
    except Exception as e:
        journal_output = f"Failed to read journalctl output: {e}"

    return {
        "service": VLLM_SERVICE,
        "lines": safe_lines,
        "systemctl_status_output": status_output,
        "journal_output": journal_output,
        "generated_at": utc_now_iso(),
    }


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
                "--query-gpu=fan.speed,memory.used,memory.total,temperature.gpu",
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
        max_fan = 0
        total_mem_used = 0
        total_mem_total = 0
        max_temp = 0
        gpu_count = 0

        for line in lines:
            parts = [p.strip() for p in line.split(",")]
            if len(parts) >= 4:
                max_fan = max(max_fan, int(parts[0]))
                total_mem_used += int(parts[1])
                total_mem_total += int(parts[2])
                max_temp = max(max_temp, int(parts[3]))
                gpu_count += 1

        if gpu_count == 0:
            return None

        return {
            "fan_speed_percent": max_fan,
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


async def detect_running_model() -> Optional[str]:
    """Query vLLM's OpenAI-compatible API to discover the currently loaded model."""
    try:
        proc = await asyncio.create_subprocess_exec(
            "curl", "-s", VLLM_HEALTH_URL.replace("/health", "/v1/models"),
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        stdout, _ = await proc.communicate()
        if proc.returncode != 0:
            return None
        data = json.loads(stdout.decode())
        models = data.get("data", [])
        if models:
            return models[0].get("id")
    except Exception as e:
        logger.warning(f"Failed to detect running model from vLLM: {e}")
    return None


async def match_vllm_model_to_config(vllm_model_id: str) -> Optional[str]:
    """Try to match a vLLM model ID to a config model ID."""
    try:
        config = load_config()
        models = config.get("models", {})
        # Check if any config model ID is a substring of the vLLM model path
        for model_id in models:
            if model_id in vllm_model_id.lower().replace("-", "").replace("_", ""):
                return model_id
        # Also try matching by checking script contents for the vLLM model path
        vllm_model_name = vllm_model_id.split("/")[-1].lower()
        for model_id in models:
            if model_id.replace("-", "") in vllm_model_name.replace("-", "").replace("_", "").lower():
                return model_id
    except Exception as e:
        logger.warning(f"Failed to match vLLM model to config: {e}")
    return None


async def check_vllm_health() -> bool:
    ok, _, _ = await check_vllm_health_details()
    return ok


async def check_vllm_health_details() -> tuple[bool, Optional[int], Optional[str]]:
    try:
        proc = await asyncio.create_subprocess_exec(
            "curl", "-s", "-m", str(HEALTH_REQUEST_TIMEOUT), "-o", "/dev/null", "-w", "%{http_code}", VLLM_HEALTH_URL,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        stdout, stderr = await proc.communicate()
        if proc.returncode != 0:
            err = stderr.decode().strip() or f"curl exited with status {proc.returncode}"
            return False, None, err
        raw_code = stdout.decode().strip()
        code = int(raw_code) if raw_code.isdigit() else None
        return code == 200, code, None if code == 200 else f"Health returned HTTP {raw_code}"
    except Exception as e:
        return False, None, str(e)


def build_service_failure_message(systemd_props: dict) -> str:
    result = systemd_props.get("result")
    exit_status = systemd_props.get("exec_main_status")
    details = []
    if result and result != "unknown":
        details.append(f"result={result}")
    if exit_status:
        details.append(f"exec_main_status={exit_status}")
    suffix = f" ({', '.join(details)})" if details else ""
    return f"vLLM systemd service is failed{suffix}"


def infer_state(systemd_props: dict, manager_state: str, health_ok: bool, health_error: Optional[str]) -> tuple[str, Optional[str]]:
    active_state = systemd_props.get("active_state")

    if active_state == "active":
        if health_ok:
            return "running", None
        if manager_state == "starting":
            return "starting", "vLLM process is active but health endpoint is not ready yet"
        return "error", f"vLLM service is active but health endpoint is unreachable: {health_error or 'unknown error'}"

    if active_state == "activating":
        return "starting", None

    if active_state == "deactivating":
        return "stopping", None

    if active_state == "failed":
        return "error", build_service_failure_message(systemd_props)

    if active_state == "inactive":
        if manager_state == "stopping":
            return "stopped", None
        return "stopped", None

    return manager_state, "Unable to determine vLLM service state from systemd"


async def reconcile_runtime_state(update_app_state: bool = True) -> dict:
    systemd_props = get_systemd_properties()
    health_ok, health_http_code, health_error = await check_vllm_health_details()
    inferred_state, reason = infer_state(systemd_props, app_state.state, health_ok, health_error)

    await app_state.update_runtime_checks(
        systemd_props=systemd_props,
        health_http_code=health_http_code,
        health_error=health_error,
        inferred_state=inferred_state,
        inference_reason=reason,
    )

    if update_app_state:
        if inferred_state == "error":
            if app_state.state != "error" or app_state.error_message != reason:
                await app_state.set_state("error", error=reason)
        elif inferred_state != app_state.state:
            await app_state.set_state(inferred_state)
        elif app_state.state != "error":
            app_state.error_message = None

    return {
        "systemd": {
            "service": VLLM_SERVICE,
            "active_state": systemd_props.get("active_state"),
            "sub_state": systemd_props.get("sub_state"),
            "result": systemd_props.get("result"),
            "exec_main_status": systemd_props.get("exec_main_status"),
            "exec_main_code": systemd_props.get("exec_main_code"),
        },
        "health": {
            "url": VLLM_HEALTH_URL,
            "ok": health_ok,
            "http_code": health_http_code,
            "error": health_error,
        },
        "inferred_state": inferred_state,
        "inference_reason": reason,
        "checked_at": app_state.last_reconciled_at,
    }


async def wait_for_vllm_ready():
    """Poll vLLM health endpoint until ready or timeout."""
    elapsed = 0
    while elapsed < HEALTH_POLL_TIMEOUT:
        systemd_props = get_systemd_properties()
        if systemd_props.get("active_state") == "failed":
            return False, build_service_failure_message(systemd_props)

        health_ok, _, health_error = await check_vllm_health_details()
        if health_ok:
            return True, None

        await asyncio.sleep(HEALTH_POLL_INTERVAL)
        elapsed += HEALTH_POLL_INTERVAL
        logger.info(f"Waiting for vLLM to be ready... ({elapsed}s), health_error={health_error}")
    return False, "vLLM health check timeout"


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

        ready, failure_reason = await wait_for_vllm_ready()
        if ready:
            await app_state.set_state("running")
            save_state({"last_model": model_id})
        else:
            await app_state.set_state("error", error=failure_reason or "vLLM failed to become ready")
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

    state = load_state()
    app_state.current_model = state.get("last_model")

    diagnostics = await reconcile_runtime_state(update_app_state=True)
    inferred_state = diagnostics["inferred_state"]
    if inferred_state in ("running", "starting", "stopping", "error"):
        logger.info(f"Startup reconciliation detected state={inferred_state}")

        if inferred_state == "running" and not app_state.current_model:
            vllm_model = await detect_running_model()
            if vllm_model:
                logger.info(f"Detected running vLLM model: {vllm_model}")
                model_id = await match_vllm_model_to_config(vllm_model)
                if model_id:
                    app_state.current_model = model_id
                    save_state({"last_model": model_id})
                else:
                    app_state.current_model = vllm_model.split("/")[-1]
        return

    # Load last model and auto-start
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
    diagnostics = await reconcile_runtime_state(update_app_state=True)
    gpu = get_gpu_stats()
    response = {
        "state": app_state.state,
        "model": app_state.current_model,
        "last_state_change_at": app_state.last_state_change_at,
        "checks": diagnostics,
    }
    if app_state.error_message:
        response["error"] = app_state.error_message
    if gpu:
        response["gpu"] = gpu
    return response


@app.get("/service/status")
async def get_service_status(lines: int = SERVICE_LOG_DEFAULT_LINES):
    diagnostics = await reconcile_runtime_state(update_app_state=False)
    service_output = get_service_output(lines=lines)
    service_output["checks"] = diagnostics
    return service_output


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
