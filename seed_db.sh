#!/usr/bin/env bash
# Direct shortcut to scripts/seed_ubuntu.sh
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
bash "$DIR/scripts/seed_ubuntu.sh" "$@"
