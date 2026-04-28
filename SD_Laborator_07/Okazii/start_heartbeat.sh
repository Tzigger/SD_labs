#!/usr/bin/env bash
# ============================================================
# start_heartbeat.sh  —  Start HeartBeatMicroservice
#
# Usage: ./start_heartbeat.sh
# Stop:  CTRL+C  (graceful shutdown via JVM shutdown hook)
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/HeartBeatMicroservice/build/libs/HeartBeatMicroservice.jar"

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: JAR not found: $JAR"
    echo "Build the project first in IntelliJ: Build → Build Project"
    exit 1
fi

mkdir -p "$SCRIPT_DIR/logs"
echo "[$(date '+%H:%M:%S')] Starting HeartBeatMicroservice..."
exec java -jar "$JAR"
