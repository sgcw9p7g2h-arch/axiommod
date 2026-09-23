#!/usr/bin/env sh
set -eu
GRADLE_VERSION=9.2.0
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
DIST="$ROOT/.gradle-dist/gradle-$GRADLE_VERSION"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$ROOT/.gradle-dist"
  curl -fL --retry 3 -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  tar -xf /tmp/gradle.zip -C "$ROOT/.gradle-dist"
fi
exec "$DIST/bin/gradle" "$@"
