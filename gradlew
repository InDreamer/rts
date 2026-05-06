#!/bin/sh

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
GRADLE_VERSION=8.7
GRADLE_HOME="$APP_HOME/.gradle/bootstrap/gradle-$GRADLE_VERSION"

if [ -n "$JAVA_HOME" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$APP_HOME/.gradle/bootstrap"
  curl -fsSL -o "$APP_HOME/.gradle/bootstrap/gradle-$GRADLE_VERSION-bin.zip" \
    "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  unzip -q -o "$APP_HOME/.gradle/bootstrap/gradle-$GRADLE_VERSION-bin.zip" -d "$APP_HOME/.gradle/bootstrap"
fi

export JAVA_HOME=$(cd "$(dirname "$JAVA_CMD")/.." && pwd -P)
exec "$GRADLE_HOME/bin/gradle" "$@"
