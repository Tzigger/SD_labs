#!/usr/bin/env bash
# ============================================================
# watch_logs.sh  —  Live log viewer pentru Okazii
#
# Afișează în timp real logurile din toate microserviciile.
# Rulează asta în al doilea terminal în timp ce start_all.sh
# rulează în primul.
#
# Usage: ./watch_logs.sh
# Stop:  CTRL+C
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/logs"

# Culori pentru fiecare serviciu
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; MAGENTA='\033[0;35m'
WHITE='\033[1;37m'; NC='\033[0m'

clear
echo -e "${WHITE}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${WHITE}║           Okazii — Live Log Viewer                       ║${NC}"
echo -e "${WHITE}║           CTRL+C pentru a opri                           ║${NC}"
echo -e "${WHITE}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""

# Așteaptă să apară directorul de loguri
echo -e "${YELLOW}Se așteaptă pornirea serviciilor (logs/ directory)...${NC}"
while [[ ! -d "$LOG_DIR" ]]; do sleep 0.5; done
echo -e "${GREEN}✓ Directorul logs/ găsit. Se monitorizează...${NC}"
echo ""

# tail pe toate logurile simultan cu prefix colorat per fișier
tail -F \
    "$LOG_DIR/Auctioneer_journal.log" \
    "$LOG_DIR/MessageProcessor_journal.log" \
    "$LOG_DIR/BiddingProcessor_journal.log" \
    "$LOG_DIR/HeartBeat_journal.log" \
    "$LOG_DIR/master_metrics.log" \
    "$LOG_DIR/auctioneer_stdout.log" \
    "$LOG_DIR/msgprocessor_stdout.log" \
    "$LOG_DIR/biddingprocessor_stdout.log" \
    2>/dev/null | awk '
        /==> .*Auctioneer_journal/ { color = "\033[0;34m"; label = "[AUCTIONEER JOURNAL]  "; next }
        /==> .*MessageProcessor_journal/ { color = "\033[0;32m"; label = "[MSGPROCESSOR JOURNAL]"; next }
        /==> .*BiddingProcessor_journal/ { color = "\033[0;35m"; label = "[BIDDINGPROC JOURNAL] "; next }
        /==> .*HeartBeat_journal/ { color = "\033[0;36m"; label = "[HEARTBEAT JOURNAL]   "; next }
        /==> .*master_metrics/ { color = "\033[1;33m"; label = "[MASTER METRICS]      "; next }
        /==> .*auctioneer_stdout/ { color = "\033[0;34m"; label = "[AUCTIONEER OUT]      "; next }
        /==> .*msgprocessor_stdout/ { color = "\033[0;32m"; label = "[MSGPROCESSOR OUT]    "; next }
        /==> .*biddingprocessor_stdout/ { color = "\033[0;35m"; label = "[BIDDINGPROC OUT]     "; next }
        /==>/ { color = "\033[0;37m"; label = "[LOG]                 "; next }
        { printf "%s%s%s %s\n", color, label, "\033[0m", $0 }
    '
