#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/target-manual"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/test-classes"
find "$ROOT/src/main/java" -name '*.java' \
  ! -path '*/persistence/*' \
  ! -path '*/report/*' \
  ! -path '*/run/*' \
  ! -path '*/cli/*' > "$OUT/main-sources.txt"
javac -source 8 -target 8 -encoding UTF-8 -d "$OUT/classes" @"$OUT/main-sources.txt"
javac -source 8 -target 8 -encoding UTF-8 -cp "$OUT/classes" -d "$OUT/test-classes" \
  "$ROOT/src/test/java/com/initialneko/qualityanalysis/MockDatasets.java" \
  "$ROOT/src/test/java/com/initialneko/qualityanalysis/ManualRegressionMain.java"
java -cp "$OUT/classes:$OUT/test-classes" com.initialneko.qualityanalysis.ManualRegressionMain
