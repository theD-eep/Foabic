#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="0.1.0"
BUILD="$ROOT/build"
CLASSES="$BUILD/classes"
DIST="$ROOT/dist"

if [[ -n "${JAVA_HOME:-}" ]]; then
  JAVAC="$JAVA_HOME/bin/javac"
  JAR="$JAVA_HOME/bin/jar"
  JAVA="$JAVA_HOME/bin/java"
else
  JAVAC="$(command -v javac || true)"
  JAR="$(command -v jar || true)"
  JAVA="$(command -v java || true)"
fi
if [[ -z "$JAVAC" || -z "$JAR" || -z "$JAVA" ]]; then
  echo "Java 25 JDK not found. Set JAVA_HOME." >&2
  exit 1
fi
if ! "$JAVAC" -version 2>&1 | grep -q '25\.'; then
  echo "Foabic targets Java 25; found $("$JAVAC" -version 2>&1)." >&2
  exit 1
fi

compile() {
  local source="$1" output="$2" classpath="${3:-}"
  mkdir -p "$output"
  local files=()
  while IFS= read -r -d '' file; do files+=("$file"); done < <(find "$source" -name '*.java' -print0)
  if [[ ${#files[@]} -eq 0 ]]; then
    echo "No sources under $source" >&2
    exit 1
  fi
  local args=("--release" "25" "-g:none" "-Xlint:all,-classfile" "-d" "$output")
  if [[ -n "$classpath" ]]; then
    args+=("-cp" "$classpath")
  fi
  "$JAVAC" "${args[@]}" "${files[@]}"
}

echo "Foabic $VERSION build"
rm -rf "$CLASSES"
mkdir -p "$CLASSES" "$DIST"

API="$CLASSES/api"
LOADER="$CLASSES/loader"
MC26="$CLASSES/mc26"
TEST="$CLASSES/test"

echo "[1/5] API"
compile "$ROOT/src/api/java" "$API"

echo "[2/5] loader"
compile "$ROOT/src/loader/java" "$LOADER" "$API"

MC_BUILT=0
MC_JAR="${FOABIC_MC_JAR:-}"
if [[ -z "$MC_JAR" && -f "$ROOT/server.jar" ]]; then
  MC_JAR="$ROOT/server.jar"
fi

if [[ -n "$MC_JAR" ]]; then
  echo "[3/5] Minecraft 26.x adapter"
  MC_JAR="$(cd "$(dirname "$MC_JAR")" && pwd)/$(basename "$MC_JAR")"
  BUNDLE="$BUILD/mc-bundle"
  MARKER="$BUNDLE/.source"
  SOURCE_ID="$(stat -c '%s:%Y' "$MC_JAR" 2>/dev/null || stat -f '%z:%m' "$MC_JAR")"
  CURRENT_ID="$(cat "$MARKER" 2>/dev/null || true)"
  if [[ "$CURRENT_ID" != "$SOURCE_ID" ]]; then
    rm -rf "$BUNDLE"
    mkdir -p "$BUNDLE"
    (cd "$BUNDLE" && "$JAR" xf "$MC_JAR")
    printf '%s' "$SOURCE_ID" > "$MARKER"
  fi
  NESTED="$(find "$BUNDLE/META-INF/versions" -name 'server-*.jar' -print -quit 2>/dev/null || true)"
  if [[ -z "$NESTED" ]]; then
    echo "No nested server jar in $MC_JAR; use the official server.jar bundle." >&2
    exit 1
  fi
  MC_CP="$NESTED"
  while IFS= read -r -d '' lib; do MC_CP="$MC_CP:$lib"; done < <(find "$BUNDLE/META-INF/libraries" -name '*.jar' -print0)
  compile "$ROOT/src/mc26/java" "$MC26" "$MC_CP:$API:$LOADER"
  MC_BUILT=1
else
  echo "[3/5] FOABIC_MC_JAR not set: building core only (no 26.x adapter)."
fi

echo "[4/5] jars"
cat > "$BUILD/API-MANIFEST.MF" <<EOF
Manifest-Version: 1.0
Implementation-Title: Foabic API
Implementation-Version: $VERSION
Automatic-Module-Name: dev.foabic.api
EOF
cat > "$BUILD/AGENT-MANIFEST.MF" <<EOF
Manifest-Version: 1.0
Premain-Class: dev.foabic.loader.agent.FoabicAgent
Agent-Class: dev.foabic.loader.agent.FoabicAgent
Can-Retransform-Classes: false
Implementation-Title: Foabic
Implementation-Version: $VERSION
Implementation-Vendor: Foabic
EOF

API_JAR="$DIST/foabic-api-$VERSION.jar"
AGENT_JAR="$DIST/foabic-$VERSION.jar"
rm -f "$API_JAR" "$AGENT_JAR"
(cd "$API" && "$JAR" --create --file "$API_JAR" --manifest "$BUILD/API-MANIFEST.MF" .)

ALL="$CLASSES/all"
mkdir -p "$ALL"
cp -R "$API/." "$ALL/"
cp -R "$LOADER/." "$ALL/"
if [[ -d "$MC26" ]]; then cp -R "$MC26/." "$ALL/"; fi
(cd "$ALL" && "$JAR" --create --file "$AGENT_JAR" --manifest "$BUILD/AGENT-MANIFEST.MF" .)

API_SOURCES_JAR="$DIST/foabic-api-$VERSION-sources.jar"
AGENT_SOURCES_JAR="$DIST/foabic-$VERSION-sources.jar"
rm -f "$API_SOURCES_JAR" "$AGENT_SOURCES_JAR"
(cd "$ROOT/src/api/java" && "$JAR" --create --file "$API_SOURCES_JAR" .)

SOURCES="$BUILD/sources"
rm -rf "$SOURCES"
mkdir -p "$SOURCES"
cp -R "$ROOT/src/api/java/." "$SOURCES/"
cp -R "$ROOT/src/loader/java/." "$SOURCES/"
if [[ -d "$ROOT/src/mc26/java" ]]; then cp -R "$ROOT/src/mc26/java/." "$SOURCES/"; fi
(cd "$SOURCES" && "$JAR" --create --file "$AGENT_SOURCES_JAR" .)
rm -rf "$SOURCES"

if [[ "${SKIP_TESTS:-0}" != "1" ]]; then
  echo "[5/5] tests"
  compile "$ROOT/src/test/java" "$TEST" "$API:$LOADER"
  "$JAVA" -cp "$TEST:$API:$LOADER" dev.foabic.test.AllTests
else
  echo "[5/5] tests skipped"
fi

echo
echo "Artifacts"
ls -lh "$DIST"/*.jar | awk '{print "  " $9 "  " $5}'
if [[ "$MC_BUILT" == "1" ]]; then
  echo "Full 26.x adapter included."
else
  echo "Core only. Set FOABIC_MC_JAR to build the 26.x adapter."
fi
