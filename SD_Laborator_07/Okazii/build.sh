#!/usr/bin/env bash
# ============================================================
# build.sh  —  Compilează întregul proiect Okazii cu Gradle
#
# Usage: ./build.sh
#
# Produce JARuri în:
#   AuctioneerMicroservice/build/libs/AuctioneerMicroservice.jar
#   BidderMicroservice/build/libs/BidderMicroservice.jar
#   MessageProcessorMicroservice/build/libs/MessageProcessorMicroservice.jar
#   BiddingProcessorMicroservice/build/libs/BiddingProcessorMicroservice.jar
#   HeartBeatMicroservice/build/libs/HeartBeatMicroservice.jar
# ============================================================

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

GREEN='\033[0;32m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     Okazii — Build cu Gradle             ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
echo ""

# Verifică că gradle e disponibil
if ! command -v gradle &>/dev/null; then
    echo -e "${RED}Gradle nu este instalat.${NC}"
    echo "Instalează cu: brew install gradle"
    echo "Sau: https://gradle.org/install/"
    exit 1
fi

echo -e "${CYAN}gradle version: $(gradle --version | head -1)${NC}"
echo ""

cd "$SCRIPT_DIR"

echo "Compilare în curs... (prima rulare durează mai mult)"
gradle shadowJar --parallel --quiet 2>&1

echo ""
echo -e "${GREEN}✓ Build complet! JARuri produse:${NC}"
find . -path "*/build/libs/*.jar" ! -name "*-plain.jar" | sort | while read -r jar; do
    SIZE=$(du -sh "$jar" | cut -f1)
    echo -e "  ${GREEN}✓${NC} $jar  (${SIZE})"
done
echo ""
echo -e "${CYAN}Acum poți rula: ./start_all.sh 10${NC}"
