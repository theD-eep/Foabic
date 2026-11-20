#!/usr/bin/env bash
set -euo pipefail

# Builds release artifacts and writes dist/SHA256SUMS.
# The GitHub Release workflow does the same on a v* tag; this script is for
# maintainers who need to build the full adapter locally.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/dist"

"$ROOT/build.sh"

cd "$DIST"
sha256sum ./*.jar > SHA256SUMS
cat SHA256SUMS

echo
echo "Next steps"
echo "  1. git tag -a v<version> -m \"v<version>\""
echo "  2. git push origin v<version>"
echo "  3. the Release workflow rebuilds and attaches the jars"
echo "     (set repository variable MC_SERVER_JAR_URL first)"
