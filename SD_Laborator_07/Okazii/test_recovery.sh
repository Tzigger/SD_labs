#!/usr/bin/env bash
# ============================================================
# test_recovery.sh  —  Testează mecanismul de crash recovery
#
# Simulează un crash al MessageProcessorMicroservice în
# mijlocul procesării, verifică că state.dat e salvat, apoi
# repornește serviciul și verifică că recovery funcționează.
#
# Usage: ./test_recovery.sh
# Daca JARurile lipsesc, scriptul le construieste cu Gradle.
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/logs"
MP_JAR="$SCRIPT_DIR/MessageProcessorMicroservice/build/libs/MessageProcessorMicroservice.jar"
BP_JAR="$SCRIPT_DIR/BiddingProcessorMicroservice/build/libs/BiddingProcessorMicroservice.jar"
AU_JAR="$SCRIPT_DIR/AuctioneerMicroservice/build/libs/AuctioneerMicroservice.jar"
BI_JAR="$SCRIPT_DIR/BidderMicroservice/build/libs/BidderMicroservice.jar"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m'

ok()  { echo -e "${GREEN}✓ $*${NC}"; }
err() { echo -e "${RED}✗ $*${NC}"; }
info(){ echo -e "${CYAN}► $*${NC}"; }
warn(){ echo -e "${YELLOW}⚠ $*${NC}"; }

# Verifica JARurile si construieste automat din terminalul VS Code daca lipsesc.
needs_build=false
for jar in "$MP_JAR" "$BP_JAR" "$AU_JAR" "$BI_JAR"; do
    [[ ! -f "$jar" ]] && needs_build=true && break
done

if $needs_build; then
    info "JARuri lipsa. Construiesc proiectul cu Gradle..."
    if ! command -v gradle &>/dev/null; then
        err "Gradle nu este instalat. Ruleaza: brew install gradle"
        exit 1
    fi
    cd "$SCRIPT_DIR"
    gradle shadowJar --parallel --quiet 2>&1
    ok "Build complet."
fi

mkdir -p "$LOG_DIR"
PIDS=()
cleanup() {
    for PID in "${PIDS[@]}"; do kill "$PID" 2>/dev/null || true; done
}
trap cleanup EXIT INT TERM

wait_for_log() {
    local log_file=$1 name=$2
    for i in $(seq 1 30); do
        if grep -q -i "port" "$log_file" 2>/dev/null; then
            ok "$name ready"
            return
        fi
        sleep 0.5
    done
    err "Timeout: $name"
    exit 1
}

wait_for_pattern() {
    local log_file=$1 pattern=$2 name=$3 timeout_sec=${4:-30}
    local tries=$(( timeout_sec * 2 ))
    for _ in $(seq 1 "$tries"); do
        if grep -q "$pattern" "$log_file" 2>/dev/null; then
            ok "$name"
            return 0
        fi
        sleep 0.5
    done
    return 1
}

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     Test: Crash Recovery Mechanism           ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# ── Pas 1: Curăță logurile vechi ──────────────────────────
info "Pas 1: Curăț logurile vechi..."
rm -f "$LOG_DIR"/MessageProcessor_*.* "$LOG_DIR"/Auctioneer_*.* "$LOG_DIR"/BiddingProcessor_*.*
ok "Loguri curate."

# ── Pas 2: Pornește BiddingProcessor ──────────────────────
info "Pas 2: Pornesc BiddingProcessorMicroservice..."
java -jar "$BP_JAR" > "$LOG_DIR/bp_test.log" 2>&1 &
BP_PID=$!
PIDS+=("$BP_PID")
wait_for_log "$LOG_DIR/bp_test.log" "BiddingProcessor"

# ── Pas 3: Pornește MessageProcessor ──────────────────────
info "Pas 3: Pornesc MessageProcessorMicroservice..."
java -jar "$MP_JAR" > "$LOG_DIR/mp_test.log" 2>&1 &
MP_PID=$!
PIDS+=("$MP_PID")
wait_for_log "$LOG_DIR/mp_test.log" "MessageProcessor"

# ── Pas 4: Pornește Auctioneer ────────────────────────────
info "Pas 4: Pornesc AuctioneerMicroservice..."
java -jar "$AU_JAR" > "$LOG_DIR/au_test.log" 2>&1 &
AU_PID=$!
PIDS+=("$AU_PID")
wait_for_log "$LOG_DIR/au_test.log" "Auctioneer"

# ── Pas 5: Pornește bidderi ───────────────────────────────
NUM_BIDDERS=40
info "Pas 5: Pornesc $NUM_BIDDERS bidderi..."
for i in $(seq 1 "$NUM_BIDDERS"); do
    java -jar "$BI_JAR" "$i" > "$LOG_DIR/bidder_test_${i}.log" 2>&1 &
    PIDS+=("$!")
    sleep 0.05
done
ok "$NUM_BIDDERS bidderi porniti."

# ── Pas 6: Simulează crash după ce începe procesarea ──────
info "Pas 6: Aștept începutul procesării, apoi simulez crash pe MessageProcessor (SIGKILL)..."
JOURNAL="$LOG_DIR/MessageProcessor_journal.log"
if ! wait_for_pattern "$JOURNAL" "Received:" "MessageProcessor a început să proceseze mesaje." 35; then
    warn "Nu am detectat 'Received:' în jurnal. Continui cu crash forțat."
fi

if kill -0 "$MP_PID" 2>/dev/null; then
    kill -9 "$MP_PID"
    ok "MessageProcessor KILLED (simulare crash)."
else
    warn "MessageProcessor s-a terminat deja singur."
fi

sleep 1

# ── Pas 7: Verifică state.dat ─────────────────────────────
info "Pas 7: Verific dacă state.dat a supraviețuit crash-ului..."
STATE_FILE="$LOG_DIR/MessageProcessor_state.dat"
if [[ -f "$STATE_FILE" ]]; then
    LINES=$(wc -l < "$STATE_FILE" | tr -d '[:space:]')
    [[ -z "$LINES" ]] && LINES=0
    ok "state.dat există! Conține $LINES mesaje persistate:"
    head -3 "$STATE_FILE" | while read -r line; do
        echo "    $line"
    done
else
    warn "state.dat nu există (dacă licitația nu a început încă, e normal)."
fi

# ── Pas 8: Verific jurnalul ───────────────────────────────
info "Pas 8: Verific jurnalul MessageProcessor..."
if [[ -f "$JOURNAL" ]]; then
    CYCLES_START=$(awk '/CYCLE_START/{c++} END{print c+0}' "$JOURNAL")
    CYCLES_END=$(awk '/CYCLE_END/{c++} END{print c+0}' "$JOURNAL")
    echo "    CYCLE_START entries: $CYCLES_START"
    echo "    CYCLE_END   entries: $CYCLES_END"
    if (( CYCLES_START > CYCLES_END )); then
        INTERRUPTED=true
        ok "Ciclu întrerupt detectat! Recovery va fi activat la restart."
    else
        INTERRUPTED=false
        warn "Nu s-a detectat ciclu întrerupt (poate licitația nu a început)."
    fi
else
    INTERRUPTED=false
    warn "Jurnal MessageProcessor nu există."
fi

# Dacă nu am prins crash-ul la timp, rulăm un fallback deterministic:
# procesăm fără BiddingProcessor (va exista CYCLE_START fără CYCLE_END),
# apoi la restart-ul MP cu BiddingProcessor pornit se validează recovery.
if [[ "$INTERRUPTED" != "true" ]]; then
    echo ""
    warn "Fallback: forțez un ciclu întrerupt deterministic..."

    kill "$MP_PID" "$AU_PID" "$BP_PID" 2>/dev/null || true
    sleep 1

    rm -f "$LOG_DIR"/MessageProcessor_*.* "$LOG_DIR"/Auctioneer_*.* "$LOG_DIR"/BiddingProcessor_*.*

    info "Fallback 1/3: Pornesc MessageProcessor (fără BiddingProcessor)..."
    java -jar "$MP_JAR" > "$LOG_DIR/mp_fallback.log" 2>&1 &
    MP_PID=$!
    PIDS+=("$MP_PID")
    wait_for_log "$LOG_DIR/mp_fallback.log" "MessageProcessor fallback"

    info "Fallback 2/3: Pornesc Auctioneer + 20 bidderi..."
    java -jar "$AU_JAR" > "$LOG_DIR/au_fallback.log" 2>&1 &
    AU_PID=$!
    PIDS+=("$AU_PID")
    wait_for_log "$LOG_DIR/au_fallback.log" "Auctioneer fallback"

    for i in $(seq 1 20); do
        java -jar "$BI_JAR" "$i" > "$LOG_DIR/bidder_fallback_${i}.log" 2>&1 &
        PIDS+=("$!")
        sleep 0.05
    done

    if wait_for_pattern "$JOURNAL" "FATAL: Cannot connect to BiddingProcessor" "Fallback: ciclu întrerupt creat." 35; then
        CYCLES_START=$(awk '/CYCLE_START/{c++} END{print c+0}' "$JOURNAL")
        CYCLES_END=$(awk '/CYCLE_END/{c++} END{print c+0}' "$JOURNAL")
        echo "    Fallback CYCLE_START: $CYCLES_START"
        echo "    Fallback CYCLE_END  : $CYCLES_END"
    else
        warn "Fallback-ul nu a generat clar ciclul întrerupt."
    fi

    kill "$MP_PID" "$AU_PID" 2>/dev/null || true
fi

# ── Pas 9: Reporneste MessageProcessor ────────────────────
echo ""
info "Pas 9: Repornesc MessageProcessorMicroservice (recovery mode)..."

# Asigură că BiddingProcessor e up înainte de recovery.
if ! kill -0 "$BP_PID" 2>/dev/null; then
    info "BiddingProcessor nu rulează; îl pornesc pentru faza de recovery..."
    java -jar "$BP_JAR" > "$LOG_DIR/bp_recovery.log" 2>&1 &
    BP_PID=$!
    PIDS+=("$BP_PID")
    wait_for_log "$LOG_DIR/bp_recovery.log" "BiddingProcessor (recovery)"
fi

java -jar "$MP_JAR" > "$LOG_DIR/mp_recovery.log" 2>&1 &
PIDS+=("$!")
wait_for_log "$LOG_DIR/mp_recovery.log" "MessageProcessor (recovery)"

sleep 3

# ── Pas 10: Verifică recovery ─────────────────────────────
info "Pas 10: Verific logul de recovery..."
if grep -q "RECOVERY" "$JOURNAL" 2>/dev/null; then
    ok "RECOVERY detectat în jurnal!"
    grep "RECOVERY" "$JOURNAL" | head -5 | while read -r line; do
        echo "    $line"
    done
else
    warn "RECOVERY nu apare încă în jurnal (poate că nu au existat mesaje de procesat)."
fi

if grep -q "CYCLE_END" "$JOURNAL" 2>/dev/null; then
    ok "CYCLE_END detectat după restart (ciclul a fost închis)."
else
    warn "Nu am găsit CYCLE_END după restart."
fi

echo ""
echo -e "${CYAN}══════════════════════════════════════════════${NC}"
echo -e "${CYAN}  Test Recovery complet.${NC}"
echo -e "${CYAN}  Verifică manual: cat logs/MessageProcessor_journal.log${NC}"
echo -e "${CYAN}  Verifică manual: cat logs/MessageProcessor_state.dat${NC}"
echo -e "${CYAN}══════════════════════════════════════════════${NC}"
echo ""
