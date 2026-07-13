#!/usr/bin/env bash

set -euo pipefail

no_build=false
case "${1:-}" in
  "")
    ;;
  --no-build)
    no_build=true
    ;;
  *)
    echo "Usage: bash scripts/test.sh [--no-build]" >&2
    exit 2
    ;;
esac

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "$script_directory/.." && pwd)"
main_output="$project_root/out/main"
test_output="$project_root/out/test"
report_directory="$project_root/out/test-reports"
temporary_directory="$project_root/out/test-tmp"
junit_jar="$project_root/lib/junit-platform-console-standalone-6.1.1.jar"

if ! command -v java >/dev/null 2>&1; then
  echo "java was not found. Install a JDK and make sure java is on PATH." >&2
  exit 1
fi

if [[ "$no_build" != true ]]; then
  bash "$script_directory/compile.sh" all
fi

for path in "$junit_jar" "$main_output" "$test_output"; do
  if [[ ! -e "$path" ]]; then
    echo "Required test input was not found: $path" >&2
    exit 1
  fi
done

rm -rf "$report_directory" "$temporary_directory"
mkdir -p "$report_directory" "$temporary_directory"

class_path="$main_output:$test_output"
echo "Running JUnit tests. Reports will be written to $report_directory"
java -Djava.io.tmpdir="$temporary_directory" -jar "$junit_jar" execute \
  --class-path "$class_path" \
  --scan-class-path \
  --fail-if-no-tests \
  --reports-dir "$report_directory"