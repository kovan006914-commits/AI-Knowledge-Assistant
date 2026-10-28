#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if [ ! -f target/AI-Knowledge-Assistant-1.0.0.jar ]; then
  sh mvnw -DskipTests package
fi
exec java -jar target/AI-Knowledge-Assistant-1.0.0.jar
