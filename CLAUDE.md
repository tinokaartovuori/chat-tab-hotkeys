# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status: v1 implemented (needs an in-game smoke test)

The plugin is implemented and compiles/tests clean against the RuneLite client API. `spec.md` is the
behaviour source of truth; `handoff.md` is the original background/discovery brief (now largely
superseded — the ids it wanted from the Var Inspector were found as named `gameval` constants). No
mandatory in-game discovery remains; a run-through only *confirms* the runtime assumptions listed at
the bottom of this section.

Source layout (package `com.chattabhotkeys`):
- `ChatTabHotkeysConfig.java` — 4 `@ConfigSection`s (lower three `closedByDefault`), 11 keybinds. Tab
  binds default to `Ctrl+1..7`; close/filters/clear default to `Keybind.NOT_SET`.
- `ChatTabHotkeysPlugin.java` — hotkeys via `HotkeyListener`/`KeyManager`, all press-time logic on the
  client thread, real behaviour (no placeholders).
- `ChatTabs.java` — `ChatTab` enum (widgetId/tabIndex/supports*/messageTypes) + `FilterOp` labels. All
  ids are named constants; nothing to "fill in".

## What this plugin is

**Chat Tab Hotkeys** (repo `chat-tab-hotkeys`) — configurable hotkeys for navigating and filtering
the OSRS chat: 7 tab binds (All, Game, Public, Private, Channel, Clan, Trade), 1 close-chat toggle,
3 filter binds (Show all/friends/none) acting on the *currently-viewed* tab, and 1 clear-history
bind. It only automates UI actions the player can already do by mouse — no gameplay automation.

## Build & run

**Use the `./rl` wrapper, not `./gradlew` directly.** RuneLite pins Gradle 8.10, which refuses to
run on JDK 24+. This host's only JDK-with-compiler is Java 26, so bare `./gradlew` fails with
"Unsupported class file major version 70". `./rl` runs the same Gradle wrapper inside a throwaway
`eclipse-temurin:11-jdk` Docker container (as your uid, so no root-owned build output; cache persists
in `~/.cache/runelite-gradle`). Nothing is installed on the host.

```bash
./rl compileJava          # compile main sources
./rl build                # compile + test + jar
./rl run                  # launch RuneLite with the plugin (task already passes --developer-mode --debug)
./rl <task>               # any gradle task/args
```

`./rl run` opens a GUI. It uses a GUI-capable image built from `docker/Dockerfile` (the slim JDK
lacks X11/AWT libs — `libXext` etc. — so RuneLite's Toolkit would otherwise die with
`UnsatisfiedLinkError`); `rl` builds it on first `run` and caches it. It forwards `$DISPLAY`, the X11
socket, and `$XAUTHORITY` into the container. On some setups you must first run `xhost +local:` to
allow the connection. This is the path for the in-game discovery pass (Widget Inspector + Var
Inspector) — there is no viable host-side `run` while the host JDK is 26.

**Login.** The dev client's login box only accepts *legacy* (email+password) accounts. Two ways in for
a Jagex account:
- **Bolt launcher (set up on this machine):** Bolt's `runelite_launch_command` points at
  `tools/bolt-run.sh`, so clicking *Play* in Bolt runs our Docker dev client with Bolt's injected
  Jagex session env (`JX_SESSION_ID`/`JX_CHARACTER_ID`/`JX_DISPLAY_NAME`), which `rl run` forwards into
  the container → auto-login. (RuneLite auth here is env-based; Bolt uses no LD_PRELOAD shim for it.)
- **Official launcher:** run it as `RuneLite --insecure-write-credentials` to write
  `~/.runelite/credentials.properties`, which the dev client reads. `rl` bind-mounts the host
  `~/.runelite` into the container so credentials/config are shared.

If a JDK 11–23 (with `javac`) is ever installed on the host, plain `./gradlew` works and `./rl`
becomes unnecessary.

Constraints (non-negotiable for Plugin Hub review): `runeLiteVersion = 'latest.release'`, Java 11
language level, **no third-party dependencies**.

## Architecture that isn't obvious from the code

- **Read state from game vars/widgets, not internal tracking.** At press time: current tab =
  `client.getVarcIntValue(VarClientID.CHAT_VIEW)` mapped by `ChatTab.tabIndex`; chat closed =
  `client.isResized() && Chatbox.CHATAREA` hidden; typing = `getVarcIntValue(VarClientID.MESLAYERMODE)
  != InputType.NONE`. This keeps the plugin in sync when the player clicks tabs with the mouse. An
  internal `lastTab` is kept only for close→reopen.

- **Thread discipline.** `HotkeyListener`s fire on the key-event thread; the shared dispatch wraps
  every action in `clientThread.invoke(...)` (and applies the typing gate there). Never touch the
  client off that thread.

- **Two mechanisms for tab actions (both ID-stable — no hardcoded script/op numbers):**
  - *Switch / open / close a tab* → replay the tab button's own left-click op (`SWITCH_TAB_OP = 1`):
    `client.createScriptEventBuilder(button.getOnOpListener()).setOp(1).setSource(button).build().run()`.
    In resizable mode, op-1 on the *active* tab natively collapses the chat, which is how close/toggle
    works; in fixed mode it just re-selects, so close actions no-op.
  - *Filters* → resolve the op index at runtime by matching the tab button's `getActions()` by label
    suffix, then replay `.setOp(i+1)`. Individual binds match one label (`FilterOp`); `Cycle filter`
    detects all filter ops on the button (label starts with "show" or is "hide") and rotates among
    them, so it adapts per tab (3 on most, 5 on Public). Absent label → silent no-op.

- **Chat input mode** (which channel you type into) is *not* a widget op — set it the game's way:
  `client.setVarcIntValue(VarClientID.CHATBOX_MODE /*945*/, mode)` (0=Public,1=Channel,2=Clan,3=Guest,
  4=Group) then `client.runScript(ScriptID.CHAT_PROMPT_INIT /*223*/)` to redraw the input line.

- **Clear history is native (no Chat History plugin dependency).** For the active tab's
  `ChatMessageType`s, drop every `MessageNode` from `client.getChatLineMap()` then
  `client.runScript(ScriptID.SPLITPM_CHANGED)`. Ported from core `ChatHistoryPlugin` +
  `ChatboxTab` (the message-type arrays live in `ChatTabs.ChatTab.messageTypes`).

- **Silent no-ops, not errors.** Unsupported action on the active tab (Game/All don't offer filters or
  clear) → no-op via the `supportsFilters`/`supportsClear` flags. Fixed mode → close actions no-op.

- **Runtime assumptions to confirm in a smoke test** (graceful no-op if wrong): op index `1` is the tab
  left-click op; the "Show …" labels appear in each channel tab's `getActions()`; `CHATAREA.isHidden()`
  tracks the collapse. Fallback if any differs: Toggle Chat's `client.runScript(175, 1, tabIndex)` and
  a one-time `MenuOptionClicked` log for filter op ids.

## Releasing

The Plugin Hub distributes plugins by **pinning a commit hash**, not by git tags or GitHub releases:
the hub repo holds `plugins/chat-tab-hotkeys` with `repository=` + `commit=<40-char hash>`, and a
"release" is a PR there that bumps `commit=`. The `version=` in `runelite-plugin.properties` is
optional (the commit is used if absent) — treat it as the user-visible version label. Because
`build=standard`, the hub **replaces `build.gradle`/`settings.gradle`** at build time, so their
contents (and the `rl` wrapper) matter only for local dev, never for the hub build.

Tags/releases/CHANGELOG are for our own tracking, not a hub requirement (many hub plugins skip them).
Model to follow: [`melkypie/resource-packs`](https://github.com/melkypie/resource-packs) — SemVer
tags + GitHub Releases + `CHANGELOG.md`. Commit style is Conventional Commits; **no** Claude
co-author trailer.

Release steps (SemVer: MAJOR breaking / MINOR feature / PATCH fix):
1. Move `CHANGELOG.md` `[Unreleased]` items under a new `## [x.y.z]` heading; set `version=x.y.z` in
   `runelite-plugin.properties`.
2. `git commit -m "chore: release vX.Y.Z"`, then `git tag -a vX.Y.Z -m "vX.Y.Z"`, `git push --follow-tags`.
   Optionally create a GitHub Release from the tag with the changelog section as notes.
3. Update the hub manifest's `commit=` to that tagged commit via a branch + PR to `runelite/plugin-hub`
   (see `handoff.md` → Plugin Hub submission).
