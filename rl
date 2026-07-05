#!/usr/bin/env bash
#
# Runs the project's Gradle wrapper inside a throwaway JDK 11 container, so the
# toolchain never has to be installed on the host.
#
# Why this exists: RuneLite pins Gradle 8.10, which refuses to run on JDK 24+.
# This machine's only JDK-with-compiler is Java 26, so `./gradlew` fails on the
# host. A temurin:11-jdk container sidesteps that without installing anything.
#
# Usage:
#   ./rl compileJava            # compile main sources
#   ./rl build                  # full build
#   ./rl run                    # launch RuneLite (needs an X display; see below)
#   ./rl <any gradle task/args>
#
# Nothing is installed on the host. The container runs as your uid, so build
# outputs are owned by you (no root-owned files). Gradle's distribution and
# dependency cache persist in $RUNELITE_GRADLE_CACHE (default ~/.cache/runelite-gradle)
# so only the first run downloads.
#
# `run` opens a GUI: it forwards $DISPLAY and the X11 socket. On some setups you
# must first allow container access with:  xhost +local:
set -euo pipefail

IMAGE="${RUNELITE_JDK_IMAGE:-eclipse-temurin:11-jdk}"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CACHE_DIR="${RUNELITE_GRADLE_CACHE:-$HOME/.cache/runelite-gradle}"
mkdir -p "$CACHE_DIR"

args=(
	--rm --init
	--user "$(id -u):$(id -g)"
	-e HOME=/gradle
	-e GRADLE_USER_HOME=/gradle
	-v "$PROJECT_DIR":/work -w /work
	-v "$CACHE_DIR":/gradle
)

# Allocate a TTY when attached to one, for readable Gradle progress output.
[ -t 1 ] && args+=(-t)

# Forward X11 for GUI tasks (e.g. `run`) so the RuneLite client can open a window.
case "${1:-}" in
	run|*:run)
		if [ -n "${DISPLAY:-}" ]; then
			args+=(-e "DISPLAY=$DISPLAY" --network host)
			[ -d /tmp/.X11-unix ] && args+=(-v /tmp/.X11-unix:/tmp/.X11-unix:ro)
		else
			echo "rl: warning: DISPLAY is unset; the RuneLite window cannot open." >&2
		fi
		;;
esac

exec docker run "${args[@]}" "$IMAGE" ./gradlew --no-daemon "$@"
