#!/usr/bin/env bash
# =============================================================================
#  run.sh  –  Build, deploy and run the JEE-App + JEE-Client
#
#  Usage:
#    ./run.sh           – build + deploy + run client
#    ./run.sh build     – only build the Maven project (creates the EAR)
#    ./run.sh deploy    – only deploy the EAR to GlassFish (starts server first)
#    ./run.sh client    – only compile & run the JEE-Client
#    ./run.sh stop      – stop the GlassFish domain
#    ./run.sh undeploy  – undeploy the application from GlassFish
#    ./run.sh status    – show GlassFish domain status
# =============================================================================

set -euo pipefail

# --------------------------------------------------------------------------
# Paths  (adjust if your layout differs)
# --------------------------------------------------------------------------
GLASSFISH_HOME="$HOME/development/glassfish5"
DOMAIN="domain1"

# GlassFish 5 requires Java 8 – Java 11+ causes a NullPointerException in asadmin.
# asadmin respects the AS_JAVA env variable to select the JVM it launches.
export AS_JAVA="/usr/lib/jvm/java-8-openjdk-amd64"
ASADMIN="$GLASSFISH_HOME/bin/asadmin"

WORKSPACE="$HOME/SD/SD_Laborator_02"
JEE_APP="$WORKSPACE/JEE-App"
JEE_CLIENT="$WORKSPACE/JEE-Client"

# Name of the deployed application (used by asadmin deploy/undeploy)
APP_NAME="JEE-App"

# EAR artifact produced by Maven
EAR_FILE="$JEE_APP/ear/target/JEE-App.ear"

# Classpath for the standalone client
CLIENT_CLASSPATH="$JEE_CLIENT/out/production/JEE-Client"
CLIENT_CLASSPATH="$CLIENT_CLASSPATH:$JEE_CLIENT/interfaces.jar"
CLIENT_CLASSPATH="$CLIENT_CLASSPATH:$GLASSFISH_HOME/glassfish/lib/gf-client.jar"

# --------------------------------------------------------------------------
# Colours
# --------------------------------------------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

info()    { echo -e "${CYAN}[INFO]${RESET}  $*"; }
success() { echo -e "${GREEN}[OK]${RESET}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
error()   { echo -e "${RED}[ERROR]${RESET} $*" >&2; }
step()    { echo -e "\n${BOLD}==> $*${RESET}"; }

# --------------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------------
glassfish_running() {
    "$ASADMIN" list-domains 2>/dev/null | grep -q "${DOMAIN}.*running"
}

start_glassfish() {
    step "Starting GlassFish domain '${DOMAIN}'"
    if glassfish_running; then
        warn "GlassFish domain '${DOMAIN}' is already running – skipping start."
    else
        "$ASADMIN" start-domain "$DOMAIN"
        success "GlassFish started."
    fi
}

stop_glassfish() {
    step "Stopping GlassFish domain '${DOMAIN}'"
    if glassfish_running; then
        "$ASADMIN" stop-domain "$DOMAIN"
        success "GlassFish stopped."
    else
        warn "GlassFish domain '${DOMAIN}' is not running."
    fi
}

build_app() {
    step "Building JEE-App with Maven"
    cd "$JEE_APP"
    mvn clean package -q
    success "Build successful  →  $EAR_FILE"
    cd "$WORKSPACE"
}

deploy_app() {
    step "Deploying EAR to GlassFish"

    if [ ! -f "$EAR_FILE" ]; then
        error "EAR file not found: $EAR_FILE"
        error "Run './run.sh build' first."
        exit 1
    fi

    # Undeploy previous version if it exists (ignore errors)
    if "$ASADMIN" list-applications 2>/dev/null | grep -q "^${APP_NAME}"; then
        info "Undeploying previous version of '${APP_NAME}'..."
        "$ASADMIN" undeploy --cascade=true "$APP_NAME" || true
    fi

    "$ASADMIN" deploy --force=true --name="$APP_NAME" "$EAR_FILE"
    success "Application '${APP_NAME}' deployed successfully."
}

compile_client() {
    step "Compiling JEE-Client"
    mkdir -p "$JEE_CLIENT/out/production/JEE-Client"

    javac \
        -cp "$JEE_CLIENT/interfaces.jar:$GLASSFISH_HOME/glassfish/lib/gf-client.jar" \
        -d  "$JEE_CLIENT/out/production/JEE-Client" \
        "$JEE_CLIENT/src/JEEClient.java"

    success "JEE-Client compiled."
}

run_client() {
    step "Running JEE-Client"

    # Compile first if the class file is missing
    if [ ! -f "$JEE_CLIENT/out/production/JEE-Client/JEEClient.class" ]; then
        warn "JEEClient.class not found – compiling first..."
        compile_client
    fi

    info "Connecting to GlassFish IIOP on localhost:3700 ..."
    # gf-client.jar requires Java 8 (uses sun.misc.Unsafe.defineClass removed in Java 11)
    "$AS_JAVA/bin/java" \
        -cp "$CLIENT_CLASSPATH" \
        JEEClient

    success "Client finished."
}

undeploy_app() {
    step "Undeploying '${APP_NAME}' from GlassFish"
    start_glassfish
    if "$ASADMIN" list-applications 2>/dev/null | grep -q "^${APP_NAME}"; then
        "$ASADMIN" undeploy --cascade=true "$APP_NAME"
        success "Application undeployed."
    else
        warn "Application '${APP_NAME}' is not currently deployed."
    fi
}

show_status() {
    step "GlassFish status"
    "$ASADMIN" list-domains
    echo ""
    if glassfish_running; then
        info "Deployed applications:"
        "$ASADMIN" list-applications || true
    fi
}

# --------------------------------------------------------------------------
# Main dispatcher
# --------------------------------------------------------------------------
ACTION="${1:-all}"

case "$ACTION" in
    build)
        build_app
        ;;
    deploy)
        start_glassfish
        deploy_app
        ;;
    client)
        run_client
        ;;
    stop)
        stop_glassfish
        ;;
    undeploy)
        undeploy_app
        ;;
    status)
        show_status
        ;;
    all|"")
        build_app
        start_glassfish
        deploy_app
        info "Waiting 3 seconds for the application to fully initialise..."
        sleep 3
        run_client
        ;;
    *)
        error "Unknown action: '$ACTION'"
        echo "Usage: $0 [build|deploy|client|stop|undeploy|status]"
        exit 1
        ;;
esac
