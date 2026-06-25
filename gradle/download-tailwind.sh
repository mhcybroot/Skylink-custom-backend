#!/bin/bash

# Configuration
VERSION="v3.4.4"
OS="linux-x64"
CLI_DIR="./gradle"
CLI_PATH="$CLI_DIR/tailwindcss"

# Ensure the gradle directory exists
mkdir -p "$CLI_DIR"

# Download the Standalone CLI if it doesn't exist
if [ ! -f "$CLI_PATH" ]; then
    echo "Downloading Tailwind CSS Standalone CLI ($VERSION)..."
    curl -sLO "https://github.com/tailwindlabs/tailwindcss/releases/download/$VERSION/tailwindcss-$OS"
    
    # Move and make executable
    mv "tailwindcss-$OS" "$CLI_PATH"
    chmod +x "$CLI_PATH"
    
    echo "Tailwind CLI downloaded successfully."
else
    echo "Tailwind CLI already exists at $CLI_PATH."
fi
