#!/usr/bin/env bash
# ============================================================
# start_auctioneer.sh  —  Start AuctioneerMicroservice
#
# Usage: ./start_auctioneer.sh
# Stop:  CTRL+C  (graceful shutdown via JVM shutdown hook)
#
# NOTE: MessageProcessorMicroservice and BiddingProcessorMicroservice
# must already be running before you start this script.
# You have AUCTION_DURATION seconds (default 15) after this starts
# to launch your bidder processes.
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/AuctioneerMicroservice/build/libs/AuctioneerMicroservice.jar"

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: JAR not found: $JAR"
    echo "Build the project first in IntelliJ: Build → Build Project"
    exit 1
fi

mkdir -p "$SCRIPT_DIR/logs"
echo "[$(date '+%H:%M:%S')] Starting AuctioneerMicroservice..."
echo "[$(date '+%H:%M:%S')] You have 15 seconds to start your bidder processes!"
exec java -jar "$JAR"
