#!/usr/bin/env bash
# ============================================================
# run_bidders.sh  —  Lab Task 1: Launch N BidderMicroservice instances
#
# Runs this AFTER AuctioneerMicroservice is already started.
# Launches N bidder JVM processes in parallel (with a small stagger)
# and waits for all of them to finish.
#
# Usage:
#   ./run_bidders.sh [N]     # default N=100
#   ./run_bidders.sh 10
#
# Stop:
#   CTRL+C — all bidder background processes are killed.
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
N=${1:-100}
JAR="$SCRIPT_DIR/BidderMicroservice/build/libs/BidderMicroservice.jar"
LOG_DIR="$SCRIPT_DIR/logs/bidders"

# ── Colours ────────────────────────────────────────────────
GREEN='\033[0;32m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'

if [[ ! -f "$JAR" ]]; then
    echo -e "${RED}ERROR: JAR not found at $JAR${NC}"
    echo "Build the project first: IntelliJ → Build → Build Project"
    exit 1
fi

mkdir -p "$LOG_DIR"

echo ""
echo -e "${CYAN}Starting $N BidderMicroservice instances...${NC}"
echo ""

PIDS=()

# Kill all bidder processes on CTRL+C
cleanup() {
    echo ""
    echo "Stopping all bidder processes..."
    for PID in "${PIDS[@]}"; do
        kill "$PID" 2>/dev/null || true
    done
}
trap cleanup EXIT INT TERM

for i in $(seq 1 "$N"); do
    java -jar "$JAR" > "$LOG_DIR/bidder_${i}.log" 2>&1 &
    PID=$!
    PIDS+=("$PID")
    # Print one dot per 10 bidders to show progress
    if (( i % 10 == 0 )); then
        echo "[$(date '+%H:%M:%S')] Launched $i / $N bidders..."
    fi
    sleep 0.05
done

echo -e "${GREEN}All $N bidder processes launched. Waiting for results...${NC}"
echo ""

SUCCESS=0; FAILED=0
for PID in "${PIDS[@]}"; do
    if wait "$PID" 2>/dev/null; then ((SUCCESS++)) || true
    else ((FAILED++)) || true
    fi
done

echo ""
echo -e "${CYAN}══════════════════════════════════${NC}"
echo -e "${CYAN}  Results: $SUCCESS succeeded, $FAILED failed (of $N)${NC}"
echo -e "${CYAN}  Logs: $LOG_DIR/${NC}"
echo -e "${CYAN}══════════════════════════════════${NC}"
echo ""
