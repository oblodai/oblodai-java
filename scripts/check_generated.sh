#!/usr/bin/env bash
# Fail when src/main/java/com/oblodai/generated or the methods table of README.md/README.ru.md is
# not what the generator makes of the gateway's contract. Regenerates into a temporary directory
# with the backend's tools/sdkgen (from services/core/api/openapi.json, checked against names.lock
# with -frozen-lock - a lost or renamed public name fails as a breaking change, a new one as a lock
# that lags the contract) and compares. The backend checkout is $OBLODAI_BACKEND, else
# ../oblodai-backend next to this repository. Without a backend that has tools/sdkgen the check is
# skipped, loudly; with --require (or $OBLODAI_BACKEND set) it fails instead.
# Fix drift by regenerating (`make sdk` in the backend), never by hand.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="${OBLODAI_BACKEND:-$ROOT/../oblodai-backend}"
SDKGEN="$BACKEND/tools/sdkgen"
SPEC="$BACKEND/services/core/api/openapi.json"
GENERATED="src/main/java/com/oblodai/generated"

if [ ! -d "$SDKGEN/cmd/sdkgen" ] || [ ! -f "$SPEC" ]; then
  message="no generator at $SDKGEN (set OBLODAI_BACKEND to the backend checkout)"
  if [ "${1:-}" = "--require" ] || [ -n "${OBLODAI_BACKEND:-}" ]; then
    echo "check_generated: $message" >&2
    exit 1
  fi
  echo "  (skipped: $message)"
  exit 0
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
SPEC_ABS="$(cd "$(dirname "$SPEC")" && pwd)/$(basename "$SPEC")"
mkdir -p "$TMP/out"
cp "$ROOT/README.md" "$ROOT/README.ru.md" "$TMP/out/"
if ! (cd "$SDKGEN" && GOTOOLCHAIN="${GOTOOLCHAIN:-go1.26.6}" go run ./cmd/sdkgen \
      -spec "$SPEC_ABS" -lang java -out "$TMP/out" -lock "$ROOT/names.lock" -frozen-lock) >"$TMP/sdkgen.log" 2>&1; then
  echo "check_generated: sdkgen failed:" >&2
  cat "$TMP/sdkgen.log" >&2
  exit 1
fi
if ! diff -r -q "$TMP/out/$GENERATED" "$ROOT/$GENERATED"; then
  echo "check_generated: $GENERATED is stale; regenerate with \`make sdk\` in the backend" >&2
  exit 1
fi
for readme in README.md README.ru.md; do
  if ! diff -q "$TMP/out/$readme" "$ROOT/$readme" >/dev/null; then
    echo "check_generated: the methods table of $readme is stale; regenerate with \`make sdk\` in the backend" >&2
    exit 1
  fi
done
echo "generated code matches $SPEC_ABS"
