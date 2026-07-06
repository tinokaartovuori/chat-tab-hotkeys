#!/usr/bin/env bash
#
# Bolt "RuneLite launch command" → launches THIS plugin's dev client, logged in.
#
# Point Bolt at this file: Settings → RuneLite launch command = the absolute path
# to this script (or set "runelite_launch_command" in ~/.config/bolt-launcher/launcher.json).
# Because the command has no `%command%` token, Bolt runs it INSTEAD of the stock
# runelite.jar — but still in a child process that has the Jagex session env
# (JX_SESSION_ID / JX_CHARACTER_ID / JX_DISPLAY_NAME) and HOME redirected to Bolt's
# data dir. `rl run` forwards those vars into the Docker dev client, which reads them
# and auto-logs-in to your account. RuneLite auth is purely env-based here (Bolt uses
# no LD_PRELOAD shim for RuneLite), so it works the same inside the container.
set -euo pipefail

# Absolute project dir (this script lives in <project>/tools/).
PROJECT_DIR="$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)"

# Bolt redirects HOME to its own data dir, so resolve the real login home for a
# stable, warm Gradle cache (avoids re-downloading into Bolt's data dir each launch).
REAL_HOME="$(getent passwd "$(id -un)" | cut -d: -f6)"
export RUNELITE_GRADLE_CACHE="${RUNELITE_GRADLE_CACHE:-$REAL_HOME/.cache/runelite-gradle}"

cd "$PROJECT_DIR"
exec ./rl run
