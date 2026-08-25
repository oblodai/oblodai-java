#!/usr/bin/env bash
# Regenerates src/main/java/com/oblodai/contract from contract/contract.json.
#   codegen/run.sh           write the generated sources
#   codegen/run.sh --check   fail when the committed sources differ (the drift gate in `mvn verify`)
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
out="$root/target/codegen"
mkdir -p "$out"
javac -d "$out" "$root"/codegen/*.java
exec java -Doblodai.root="$root" -cp "$out" codegen.Codegen "$@"
