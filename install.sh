#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== vLLM Manager Installation ==="

# Install Python dependencies
echo "Installing Python dependencies..."
pip3 install -r "$SCRIPT_DIR/requirements.txt"

# Create vllm-manager config directory
echo "Creating /etc/vllm-manager directory..."
sudo mkdir -p /etc/vllm-manager

echo ""
echo "=== Installation Complete ==="
echo ""
echo "Next steps: See POST_INSTALL.md"
