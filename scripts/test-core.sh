#!/usr/bin/env bash
# Runs the JUnit tests of mikesstats.core locally without Loom/Minecraft
# (cloud sessions can't reach the Fabric/Mojang servers). CI runs the full build.
set -euo pipefail
cd "$(dirname "$0")/.."
lib=.gradle/core-test-lib
out=build/core-test
mkdir -p "$lib"
fetch() { [ -f "$lib/$2" ] || curl -sSfL -o "$lib/$2" "https://repo1.maven.org/maven2/$1/$2"; }
fetch org/junit/platform/junit-platform-console-standalone/1.11.4 junit-platform-console-standalone-1.11.4.jar
fetch com/google/code/gson/gson/2.11.0 gson-2.11.0.jar
rm -rf "$out" && mkdir -p "$out"
cp="$lib/*"
main=$(find src/main/java/mikesstats/core -name '*.java' 2>/dev/null || true)
tests=$(find src/test/java -name '*.java')
javac -d "$out" -cp "$cp" -encoding UTF-8 $main $tests
[ -d src/test/resources ] && cp -r src/test/resources/. "$out/"
java -jar "$lib/junit-platform-console-standalone-1.11.4.jar" execute -cp "$out:$lib/gson-2.11.0.jar" --scan-classpath "$out" --disable-banner --details=summary --fail-if-no-tests
