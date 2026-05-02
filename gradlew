#!/bin/sh
set -eu

APP_HOME=$(cd "$(dirname "$0")" && pwd)

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi

WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  exec gradle "$@"
fi

exec "$JAVA_CMD" -Dfile.encoding=UTF-8 -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
