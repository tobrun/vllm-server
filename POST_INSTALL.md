# Post-Installation Steps

Run these commands to complete the vLLM Manager installation.

## 1. Configure and install sudoers rules

Enables stop/start/restart/shutdown without password prompts.

```bash
# Copy and customize the example (replace YOUR_USER with your username)
cp sudoers.vllm-manager.example sudoers.vllm-manager
sed -i 's/YOUR_USER/your_username/g' sudoers.vllm-manager

# Install
sudo cp sudoers.vllm-manager /etc/sudoers.d/vllm-manager
sudo chmod 440 /etc/sudoers.d/vllm-manager
sudo visudo -c -f /etc/sudoers.d/vllm-manager
```

## 2. Configure and install vLLM service

Enables model switching via environment file.

```bash
# Copy and customize the example (update paths)
cp vllm.service.example vllm.service
# Edit vllm.service to update paths for your system

# Install
sudo cp vllm.service /etc/systemd/system/
sudo systemctl daemon-reload
```

## 3. Configure and install the manager service

```bash
# Copy and customize the example (replace YOUR_USER and paths)
cp vllm-manager.service.example vllm-manager.service
# Edit vllm-manager.service to update User, Group, WorkingDirectory, and ExecStart

# Install
sudo cp vllm-manager.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable vllm-manager
sudo systemctl start vllm-manager
```

## 4. Verify installation

```bash
# Check service status
sudo systemctl status vllm-manager

# Test API
curl http://localhost:9090/status
curl http://localhost:9090/models
```

## API Usage

```bash
# Stop vLLM
curl -X POST http://localhost:9090/stop

# Start vLLM
curl -X POST http://localhost:9090/start

# Restart vLLM
curl -X POST http://localhost:9090/restart

# Switch model
curl -X POST http://localhost:9090/switch -H "Content-Type: application/json" -d '{"model": "minimax-m2"}'

# Shutdown server (10s delay)
curl -X POST http://localhost:9090/shutdown
```
