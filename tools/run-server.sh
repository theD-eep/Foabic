#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$(pwd)"
SERVER_JAR="${SERVER_JAR:-server.jar}"
AGENT_JAR="${AGENT_JAR:-$ROOT/dist/foabic-0.1.0.jar}"

if [[ ! -f "$AGENT_JAR" ]]; then
  echo "Foabic agent not found: $AGENT_JAR (run build.sh first)" >&2
  exit 1
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  JAVA="$JAVA_HOME/bin/java"
  JAR="$JAVA_HOME/bin/jar"
else
  JAVA="$(command -v java)"
  JAR="$(command -v jar)"
fi
if [[ ! -x "$JAVA" || ! -x "$JAR" ]]; then
  echo "Java 25 JDK not found. Set JAVA_HOME." >&2
  exit 1
fi

SERVER="$(cd "$(dirname "$SERVER_JAR")" && pwd)/$(basename "$SERVER_JAR")"
RUNTIME="$WORK/.foabic/runtime"
MARKER="$RUNTIME/.source"
SOURCE_ID="$(stat -c '%s:%Y' "$SERVER" 2>/dev/null || stat -f '%z:%m' "$SERVER")"
CURRENT_ID="$(cat "$MARKER" 2>/dev/null || true)"

if [[ "$CURRENT_ID" != "$SOURCE_ID" ]]; then
  rm -rf "$RUNTIME"
  mkdir -p "$RUNTIME"
  (cd "$RUNTIME" && "$JAR" xf "$SERVER")
  printf '%s' "$SOURCE_ID" > "$MARKER"
fi

NESTED="$(find "$RUNTIME/META-INF/versions" -name 'server-*.jar' -print -quit 2>/dev/null || true)"
if [[ -z "$NESTED" ]]; then
  echo "No nested server jar in $SERVER; use the official server.jar bundle." >&2
  exit 1
fi

CP="$NESTED"
while IFS= read -r -d '' lib; do
  CP="$CP:$lib"
done < <(find "$RUNTIME/META-INF/libraries" -name '*.jar' -print0 2>/dev/null || true)

exec "$JAVA" \
  "-Dfoabic.gameDir=$WORK" \
  "-javaagent:$AGENT_JAR" \
  -cp "$CP" \
  net.minecraft.server.Main "$@"
