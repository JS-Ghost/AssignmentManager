#!/usr/bin/env bash
# ==============================================================================
# Launch Assignment Manager on Ubuntu / Linux
# ==============================================================================

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "[1/2] Compiling Java classes..."
mkdir -p build/classes
javac -encoding UTF-8 -cp "lib/*" -d build/classes src/assignmentmanager/*.java

echo "[2/2] Launching Assignment Manager..."
java -cp "build/classes:lib/*" assignmentmanager.AssignmentManager
