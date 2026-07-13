#!/usr/bin/env bash

set -euo pipefail

target="${1:-all}"
case "$target" in
  main|test|all)
    ;;
  *)
    echo "Usage: bash scripts/compile.sh [main|test|all]" >&2
    exit 2
    ;;
esac

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
main_source_root="$project_root/src/main/java"
test_source_root="$project_root/src/test/java"
main_output="$project_root/out/main"
test_output="$project_root/out/test"
junit_jar="$project_root/lib/junit-platform-console-standalone-6.1.1.jar"

if ! command -v javac >/dev/null 2>&1; then
  echo "javac was not found. Install a JDK and make sure javac is on PATH." >&2
  exit 1
fi

main_sources=()
if [[ -d "$main_source_root" ]]; then
  while IFS= read -r -d '' source; do
    main_sources+=("$source")
  done < <(find "$main_source_root" -type f -name '*.java' -print0)
fi

test_sources=()
if [[ -d "$test_source_root" ]]; then
  while IFS= read -r -d '' source; do
    test_sources+=("$source")
  done < <(find "$test_source_root" -type f -name '*.java' -print0)
fi

compile_main() {
  if ((${#main_sources[@]} == 0)); then
    echo "No production Java sources were found under src/main/java." >&2
    exit 1
  fi

  rm -rf "$main_output"
  mkdir -p "$main_output"
  echo "Compiling production sources to $main_output"
  javac --release 17 -encoding UTF-8 -d "$main_output" "${main_sources[@]}"
}

compile_tests() {
  if [[ ! -f "$junit_jar" ]]; then
    echo "JUnit launcher was not found at $junit_jar. Download it before compiling tests." >&2
    exit 1
  fi
  if [[ ! -d "$main_output" ]]; then
    echo "Production classes were not found at $main_output. Compile production sources first." >&2
    exit 1
  fi
  if ((${#test_sources[@]} == 0)); then
    echo "No test Java sources were found under src/test/java." >&2
    exit 1
  fi

  rm -rf "$test_output"
  mkdir -p "$test_output"
  echo "Compiling test sources to $test_output"
  javac --release 17 \
    -encoding UTF-8 \
    -classpath "$main_output:$junit_jar" \
    -d "$test_output" \
    "${test_sources[@]}"
}

if [[ "$target" == "main" || "$target" == "all" ]]; then
  compile_main
fi

if [[ "$target" == "test" || "$target" == "all" ]]; then
  compile_tests
fi

echo "Compilation completed."
