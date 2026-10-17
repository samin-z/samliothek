#!/usr/bin/env bash
# Point this repo at .githooks/ so pre-commit runs detekt/ktlint/compile before each commit.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

git config core.hooksPath .githooks
chmod +x .githooks/pre-commit

echo "Git hooks installed (core.hooksPath=.githooks)."
echo "Pre-commit will run: ./gradlew ciLocal"
