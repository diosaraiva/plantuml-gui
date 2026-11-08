#!/usr/bin/env bash
# =============================================================================
#  PlantUML GUI - user launcher (Linux / macOS)
# -----------------------------------------------------------------------------
#  Double-click or run it: it just starts the app, no questions asked.
#  Developers who want the build/clean menu should use dev_java_unix.sh instead.
# =============================================================================

set -u

JAR_NAME="plantuml-gui-java.jar"

# macOS privacy controls (Downloads, Desktop, Documents, removable drives) can block
# reading the folder this script sits in; the JVM then cannot resolve its working dir either.
SCRIPT_DIR="$(cd "$(dirname "$0")" 2>/dev/null && pwd -P 2>/dev/null)"
if [ -z "$SCRIPT_DIR" ] || ! ls "$SCRIPT_DIR" >/dev/null 2>&1; then
    echo "[FAIL]  Cannot read the folder this launcher is in: $(dirname "$0")" >&2
    if [ "$(uname -s)" = "Darwin" ]; then
        echo "        macOS is blocking access to it (\"Operation not permitted\"). Either:" >&2
        echo "          - move the unzipped folder somewhere else, e.g. ~/Applications or ~/plantuml-gui, or" >&2
        echo "          - allow your terminal (Terminal, iTerm, VS Code...) in System Settings >" >&2
        echo "            Privacy & Security > Files and Folders (or Full Disk Access), then reopen it." >&2
    else
        echo "        Check the folder permissions and try again." >&2
    fi
    exit 1
fi

# Run from the app folder so the JVM always has a readable working directory.
cd "$SCRIPT_DIR" || exit 1

if ! command -v java >/dev/null 2>&1; then
    echo "[FAIL]  'java' was not found on PATH. Install Java 17 or newer and try again." >&2
    exit 1
fi

# Unzipped release first, then the developer build output one level up.
for candidate in "$SCRIPT_DIR/$JAR_NAME" "$SCRIPT_DIR/../target/$JAR_NAME"; do
    [ -f "$candidate" ] && { JAR_FILE="$candidate"; break; }
done

if [ -z "${JAR_FILE:-}" ]; then
    echo "[FAIL]  $JAR_NAME was not found next to this script." >&2
    echo "        Download the release zip from https://github.com/diosaraiva/plantuml-gui/releases" >&2
    exit 1
fi

# Keep java_config.ini beside the JAR; otherwise the app would create it one level up.
exec java -Dplantumlgui.config="$SCRIPT_DIR/java_config.ini" -jar "$JAR_FILE" "$@"
