#!/usr/bin/env sh
set -eu
GRADLE_VERSION=8.7
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/manual-dists/gradle-$GRADLE_VERSION"
BIN="$BASE/gradle-$GRADLE_VERSION/bin/gradle"
if [ ! -x "$BIN" ]; then
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  else
    echo "Se requiere curl para descargar Gradle $GRADLE_VERSION" >&2; exit 1
  fi
  unzip -q -o "$ZIP" -d "$BASE"
fi
exec "$BIN" "$@"
