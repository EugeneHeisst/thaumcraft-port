#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
exec ./gradlew "${@:-runClient}"
