#!/usr/bin/env bash
# The artifacts `mvn verify` built carry what a release needs: the classes (runtime and generated),
# the sources and the javadoc, at the version the SDK reports.
set -euo pipefail
cd "$(dirname "$0")/.."
VERSION="$(sed -n 's/.*public static final String VERSION = "\(.*\)";/\1/p' src/main/java/com/oblodai/Oblodai.java)"
POM_VERSION="$(sed -n '0,/<version>/s/.*<version>\(.*\)<\/version>.*/\1/p' pom.xml)"
if [ "$VERSION" != "$POM_VERSION" ]; then
  echo "check_package: Oblodai.VERSION $VERSION != pom.xml $POM_VERSION" >&2
  exit 1
fi
JAR="target/oblodai-sdk-$VERSION.jar"
for f in "$JAR" "target/oblodai-sdk-$VERSION-sources.jar" "target/oblodai-sdk-$VERSION-javadoc.jar"; do
  [ -s "$f" ] || { echo "check_package: missing $f" >&2; exit 1; }
done
LIST="$(unzip -l "$JAR")"
for cls in com/oblodai/Oblodai.class com/oblodai/core/Transport.class \
           com/oblodai/generated/Routes.class com/oblodai/generated/resources/Payments.class \
           com/oblodai/generated/resources/async/Payments.class com/oblodai/kotlin/CoroutinesKt.class; do
  grep -q "$cls" <<<"$LIST" || { echo "check_package: $JAR lacks $cls" >&2; exit 1; }
done
echo "package: $JAR with sources and javadoc, version $VERSION"
