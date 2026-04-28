#!/usr/bin/env bash
# ============================================================
# start_all.sh  —  Full Okazii Auction Orchestrator
#
# Pornește toate serviciile în ordine corectă, lansează N
# bidderi, și curăță totul la CTRL+C.
#
# Usage:
#   ./start_all.sh [NUM_BIDDERS]    # implicit: 5
#   ./start_all.sh 100
#
# Stop: CTRL+C oricând
#
# Dacă JARurile nu există, compilează automat cu Gradle.
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NUM_BIDDERS=${1:-5}
LOG_DIR="$SCRIPT_DIR/logs"
BIDDER_LOG_DIR="$LOG_DIR/bidders"

# ── Căi JAR (Gradle: <module>/build/libs/<module>.jar) ─────
HB_JAR="$SCRIPT_DIR/HeartBeatMicroservice/build/libs/HeartBeatMicroservice.jar"
MP_JAR="$SCRIPT_DIR/MessageProcessorMicroservice/build/libs/MessageProcessorMicroservice.jar"
BP_JAR="$SCRIPT_DIR/BiddingProcessorMicroservice/build/libs/BiddingProcessorMicroservice.jar"
AU_JAR="$SCRIPT_DIR/AuctioneerMicroservice/build/libs/AuctioneerMicroservice.jar"
BI_JAR="$SCRIPT_DIR/BidderMicroservice/build/libs/BidderMicroservice.jar"

# ── Culori ─────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; NC='\033[0m'

log() { echo -e "${BLUE}[$(date '+%H:%M:%S')]${NC} $*"; }
ok()  { echo -e "${GREEN}[$(date '+%H:%M:%S')] ✓${NC} $*"; }
err() { echo -e "${RED}[$(date '+%H:%M:%S')] ✗${NC} $*"; }

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║        Okazii Auction — Full Start           ║${NC}"
echo -e "${CYAN}║        Bidders: $NUM_BIDDERS$(printf '%*s' $((38 - ${#NUM_BIDDERS})) '')║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# ── Build automat dacă JARurile lipsesc ────────────────────
needs_build=false
for jar in "$HB_JAR" "$MP_JAR" "$BP_JAR" "$AU_JAR" "$BI_JAR"; do
    [[ ! -f "$jar" ]] && needs_build=true && break
done

if $needs_build; then
    log "JARuri lipsă. Se compilează cu Gradle..."
    if ! command -v gradle &>/dev/null; then
        err "Gradle nu este instalat. Rulează: brew install gradle"
        exit 1
    fi
    cd "$SCRIPT_DIR"
    gradle shadowJar --parallel --quiet 2>&1
    ok "Build complet."
fi

mkdir -p "$LOG_DIR" "$BIDDER_LOG_DIR"

# ── PID tracking & cleanup ─────────────────────────────────
PIDS=()
cleanup() {
    echo ""
    log "Shutdown: opresc toate procesele..."
    for PID in "${PIDS[@]}"; do
        kill "$PID" 2>/dev/null || true
    done
    ok "Toate procesele oprite."
}
trap cleanup EXIT INT TERM

# ── Așteaptă confirmarea în log ─────────────────────────────
wait_for_log() {
    local log_file=$1 name=$2
    for _ in $(seq 1 60); do
        if grep -q -i "port" "$log_file" 2>/dev/null; then
            ok "$name gata"
            return
        fi
        sleep 0.5
    done
    err "Timeout: $name"
    exit 1
}

# ── 1. HeartBeatMicroservice ───────────────────────────────
log "Pornesc HeartBeatMicroservice..."
java -jar "$HB_JAR" > "$LOG_DIR/heartbeat_stdout.log" 2>&1 &
PIDS+=("$!")
wait_for_log "$LOG_DIR/heartbeat_stdout.log" "HeartBeatMicroservice"

# ── 2. MessageProcessorMicroservice ───────────────────────
log "Pornesc MessageProcessorMicroservice..."
java -jar "$MP_JAR" > "$LOG_DIR/msgprocessor_stdout.log" 2>&1 &
PIDS+=("$!")
wait_for_log "$LOG_DIR/msgprocessor_stdout.log" "MessageProcessorMicroservice"

# ── 3. BiddingProcessorMicroservice ───────────────────────
log "Pornesc BiddingProcessorMicroservice..."
java -jar "$BP_JAR" > "$LOG_DIR/biddingprocessor_stdout.log" 2>&1 &
PIDS+=("$!")
wait_for_log "$LOG_DIR/biddingprocessor_stdout.log" "BiddingProcessorMicroservice"

# ── 4. AuctioneerMicroservice ──────────────────────────────
log "Pornesc AuctioneerMicroservice (fereastră de 15s)..."
java -jar "$AU_JAR" > "$LOG_DIR/auctioneer_stdout.log" 2>&1 &
AU_PID=$!
PIDS+=("$AU_PID")
wait_for_log "$LOG_DIR/auctioneer_stdout.log" "AuctioneerMicroservice"

# ── 5. Bidderi ────────────────────────────────────────────
echo ""
log "Lansez $NUM_BIDDERS bidderi..."
BIDDER_PIDS=()
for i in $(seq 1 "$NUM_BIDDERS"); do
    java -jar "$BI_JAR" > "$BIDDER_LOG_DIR/bidder_${i}.log" 2>&1 &
    BIDDER_PIDS+=("$!")
    PIDS+=("$!")
    sleep 0.05
done
ok "Toți $NUM_BIDDERS bidderii lansați."

echo ""
echo -e "${YELLOW}Licitația rulează... (CTRL+C pentru a opri oricând)${NC}"
echo -e "${YELLOW}Poți vizualiza logurile live cu: ./watch_logs.sh${NC}"
echo ""

# ── Așteaptă rezultatele bidderilor ───────────────────────
SUCCESS=0; FAILED=0
for PID in "${BIDDER_PIDS[@]}"; do
    if wait "$PID" 2>/dev/null; then ((SUCCESS++)) || true
    else ((FAILED++)) || true
    fi
done

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║              Licitație completă!             ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════╣${NC}"
printf "${CYAN}║${NC}  Bidderi reușiți  : %-23s${CYAN}║${NC}\n" "$SUCCESS / $NUM_BIDDERS"
printf "${CYAN}║${NC}  Bidderi eșuați   : %-23s${CYAN}║${NC}\n" "$FAILED / $NUM_BIDDERS"
echo -e "${CYAN}╠══════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  Loguri în: logs/                           ║${NC}"
echo -e "${CYAN}║    Auctioneer_journal.log                   ║${NC}"
echo -e "${CYAN}║    MessageProcessor_journal.log             ║${NC}"
echo -e "${CYAN}║    BiddingProcessor_journal.log             ║${NC}"
echo -e "${CYAN}║    master_metrics.log                       ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

wait "${AU_PID}" 2>/dev/null || true
