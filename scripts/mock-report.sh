#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
OUT="${1:-target/mock-profile-report}"

mvn -q -DskipTests test-compile dependency:build-classpath \
  -DincludeScope=test \
  -Dmdep.outputFile=target/mock-classpath.txt

SEP=":"
case "$(uname -s 2>/dev/null || true)" in
  MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
esac
CP="$(cat target/mock-classpath.txt)"

java -cp "target/test-classes${SEP}target/classes${SEP}${CP}" \
  com.initialneko.qualityanalysis.MockReportMain "$OUT"
