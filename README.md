# vLLM Manager

A REST API backend for remotely managing a vLLM inference server. Built for my personal home server setup using Tailscale for secure remote access.

**Use at your own risk.**

## Overview

This service provides remote control over a vLLM systemd service, allowing you to:

- Start/stop/restart the vLLM service
- Switch between different models
- Monitor service status and GPU utilization
- Remotely shutdown the server

## Requirements

- Python 3.11+
- systemd
- nvidia-smi (for GPU stats)
- Tailscale (or other secure network access)

## Security Model

This backend has **no authentication**. It relies entirely on Tailscale's network-level security. If you can reach the API, you're authorized to use it.

Do not expose this service to the public internet.

## Installation

```bash
# Clone and enter directory
cd /path/to/vllm-server

# Create virtual environment
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

# Copy and configure models
cp config.yaml.example config.yaml
# Edit config.yaml with your models

# Follow post-installation steps
cat POST_INSTALL.md
```

See [POST_INSTALL.md](POST_INSTALL.md) for systemd and sudoers setup.

## Configuration

Create `config.yaml` with your models:

```yaml
models:
  qwen-32b:
    script: /path/to/start_qwen32b.sh

  llama-70b:
    script: /path/to/start_llama70b.sh
```

Each model references a shell script that starts vLLM with the appropriate parameters. See [start_model.sh.example](start_model.sh.example) for a template.

## API

The service runs on port 9090.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/status` | GET | Current state, loaded model, GPU stats |
| `/models` | GET | List all configured models |
| `/start` | POST | Start vLLM with last-used model |
| `/stop` | POST | Stop vLLM service |
| `/restart` | POST | Restart vLLM service |
| `/switch` | POST | Switch to a different model |
| `/shutdown` | POST | Shutdown the server (10s delay) |

### Examples

```bash
# Check status
curl http://server:9090/status

# List models
curl http://server:9090/models

# Switch model
curl -X POST http://server:9090/switch \
  -H "Content-Type: application/json" \
  -d '{"model": "llama-70b"}'

# Shutdown server
curl -X POST http://server:9090/shutdown
```

## License

MIT
