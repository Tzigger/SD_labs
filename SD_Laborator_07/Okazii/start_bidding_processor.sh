#!/usr/bin/env bash
# ============================================================
# start_bidding_processor.sh  —  Start BiddingProcessorMicroservice
#
# Usage: ./start_bidding_processor.sh
# Stop:  CTRL+C  (graceful shutdown via JVM shutdown hook)
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/BiddingProcessorMicroservice/build/libs/BiddingProcessorMicroservice.jar"

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: JAR not found: $JAR"
    echo "Build the project first from VS Code terminal: ./build.sh"
    exit 1
fi

mkdir -p "$SCRIPT_DIR/logs"
echo "[$(date '+%H:%M:%S')] Starting BiddingProcessorMicroservice..."
exec java -jar "$JAR"
