#!/bin/sh

# Lightweight Gradle launcher for this repository. It uses a locally installed
# Gradle when available and otherwise bootstraps Gradle 8.8 into the user's
# Gradle cache, without putting the distribution in the repository.
set -eu

GRADLE_VERSION="8.8"

if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

if ! command -v curl >/dev/null 2>&1 || ! command -v unzip >/dev/null 2>&1; then
    echo "Gradle is not installed, and curl/unzip are required to bootstrap it." >&2
    exit 1
fi

CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/votlistva-wrapper"
INSTALL_DIR="$CACHE_DIR/gradle-$GRADLE_VERSION"
GRADLE_BIN="$INSTALL_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$CACHE_DIR"
    archive="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"
    if [ ! -f "$archive" ]; then
        curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$archive"
    fi
    rm -rf "$CACHE_DIR/unpacked"
    mkdir -p "$CACHE_DIR/unpacked"
    unzip -q "$archive" -d "$CACHE_DIR/unpacked"
    rm -rf "$INSTALL_DIR"
    mv "$CACHE_DIR/unpacked/gradle-$GRADLE_VERSION" "$INSTALL_DIR"
    rm -rf "$CACHE_DIR/unpacked"
fi

exec "$GRADLE_BIN" "$@"
