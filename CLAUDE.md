# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status: v1.1.0 tagged on `release/v1.1.0` (per-tab clear hotkeys — smoke-tested in-game via Bolt); hub manifest bump pending

The plugin compiles/builds clean against the RuneLite client API. An earlier build was rejected by the
Plugin Hub for a generic client-script click primitive; it has been reworked so tab switching is a plain
`setVarcIntValue(VarClientID.CHAT_VIEW)` write plus benign redraw procs (no widget-op replay, no
`runScript(175)`), on branch `fix/remove-rejected-clientscript-primitive`. The rejected build was never
published, so the first hub release is labelled **v1.0.0**. The chat-filter feature was **removed
entirely** this round. `spec.md` is the behaviour source of truth. No mandatory in-game discovery
remains; a run-through only *confirms* the runtime assumptions listed at the bottom of this section.

Source layout (package `com.chattabhotkeys`):
- `ChatTabHotkeysConfig.java` — three `@ConfigSection`s ("Tab hotkeys & close", "Clear history", and
  "Chat input mode"), keybinds. Tab binds default to `Ctrl+1..7`; `closeOnRepeat` (bool, default true)
  is a toggle; close/clear/cycle and all mode binds default to `Keybind.NOT_SET`. The "Clear history"
  section holds `clearHistory` ("Clear current tab") plus one bind per clearable tab (`clearPublic`,
  `clearPrivate`, `clearChannel`, `clearClan`, `clearTrade` → "Clear: X"). `clearHistory` keeps its
  keyName from v1.0.0 so saved binds survive the rename/move. Cycle membership is a `Set<ChatTab>`
  ("Tabs to cycle", default all seven) and `Set<ChatMode>` ("Modes to cycle", default
  Public/Channel/Clan/Guest — Group left out) — the RuneLite multi-select `Set<Enum>` widget, same as
  World Hopper's region filter. The enums override `toString()` for their list labels.
- `ChatTabHotkeysPlugin.java` — hotkeys via `HotkeyListener`/`KeyManager`, all press-time logic on the
  client thread, real behaviour (no placeholders).
- `ChatTabs.java` — `ChatTab` enum (tabIndex 0..6, per-tab `supportsClear` + `messageTypes`, and a
  `byTabIndex(int)` lookup) + `ChatMode` enum (input channels: Public/Channel/Clan/Guest/Group).

## What this plugin is

**Chat Tab Hotkeys** (repo `chat-tab-hotkeys`) — configurable hotkeys for navigating the OSRS chat:
7 tab binds (All, Game, Public, Private, Channel, Clan, Trade), a cycle-tab bind (steps through a
configurable subset of tabs), a close-chat toggle (resizable mode), clear-history binds (a "current tab"
bind plus one per channel-type tab, which clears that tab without switching to it), and chat-input-mode
binds (Public/Channel/Clan/Guest clan/Group, plus a Cycle-mode bind
over a configurable subset of modes). It only automates UI actions the player can already do by mouse —
no gameplay automation. Everything is packet-free / client-side.

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

- **Read state from game vars, not internal tracking.** At press time: current tab =
  `client.getVarcIntValue(VarClientID.CHAT_VIEW)` mapped by `ChatTab.byTabIndex`; chat closed =
  `client.isResized() && CHAT_VIEW == 1337` (the collapse sentinel). This keeps the plugin in sync when
  the player clicks tabs with the mouse. An internal `lastTab` is kept only for close→reopen.

- **Thread discipline.** `HotkeyListener`s fire on the key-event thread; the shared dispatch wraps
  every action in `clientThread.invoke(...)` (and applies the logged-in gate there). Never touch the
  client off that thread.

- **Tab switch / open is a varc write + redraw (packet-free), not a widget op.** Switch / open a tab
  calls `client.setVarcIntValue(VarClientID.CHAT_VIEW, tab.tabIndex)` (the varc the game itself holds
  for the active tab), then `client.runScript(183 /*chat_alert_set*/, tab.tabIndex, 0)` to clear the
  tab's unread flash, then `redrawChat()`. `redrawChat()` runs three benign client-side repaint procs —
  `runScript(923 /*toplevel_chatbox_background*/)`, `runScript(178 /*redraw_chat_buttons*/)`, and
  `runScript(ScriptID.SPLITPM_CHANGED)` — because `setVarcIntValue` alone doesn't repaint. The third
  proc is SPLITPM_CHANGED (not `BUILD_CHATBOX`) because it runs both the chatbox rebuild
  (`~rebuildchatbox`, a superset of BUILD_CHATBOX) **and** the split private-chat overlay rebuild
  (`~rebuildpmbox`), which BUILD_CHATBOX alone never runs. Only that pmbox rebuild re-evaluates the
  split-chat hide/show guard (`CHAT_VIEW == 1337` + the "hide private chat when the chatbox is hidden"
  setting), so without it a collapse/reopen left the split overlay stale until a later event (an
  incoming message, the redraw timer) rebuilt it. The native mouse toggle runs the pmbox rebuild, so it
  never lagged (issue #6). None of 178 / 923 / 183 have a `ScriptID` constant, hence the literals;
  all are pure redraw procs (no packet, no server-persisted state). The earlier `runScript(175)`
  leaf-script call and the
  `replayWidgetOp`/`replayTabOp` primitive (`getOnOpListener()` + `createScriptEventBuilder`) — the
  generic "click any widget" call the Plugin Hub rejected (PR #13356) — are both **removed**.

- **Close / collapse (resizable mode only).** `client.setVarcIntValue(VarClientID.CHAT_VIEW, 1337)`
  (the collapse sentinel) + `redrawChat()`. `isChatClosed` = `isResized() && CHAT_VIEW == 1337`. In
  fixed mode there is no collapse, so close actions no-op. Reopen restores `lastTab`.

- **Chat input mode** (which channel you type into) is *not* a widget op — set it the game's way:
  `client.setVarcIntValue(VarClientID.CHATBOX_MODE /*945*/, mode)` (0=Public,1=Channel,2=Clan,3=Guest,
  4=Group) then `client.runScript(ScriptID.CHAT_PROMPT_INIT /*223*/)` to redraw the input line.
  `Cycle mode` and `Cycle tab` step through a configurable membership set (the "Tabs to cycle" /
  "Modes to cycle" `Set<Enum>` lists): read the current value from the game var, advance to the next
  selected entry in game order (wrapping), no-op if the set is empty. Cyclability is config-driven, not
  hardcoded on the enum. Group is left out of the default mode set; if selected while not in a GIM group
  the game reverts the mode var, so the cycle keeps `lastCycledMode` and steps *past* Group (rather than
  re-reading the reverted var and retrying Group forever).

- **Clear history is native (no Chat History plugin dependency).** `clearTab(ChatTab)` takes the target
  tab: for its `ChatMessageType`s, drop every `MessageNode` from `client.getChatLineMap()` then
  `client.runScript(ScriptID.SPLITPM_CHANGED)`. Ported from core `ChatHistoryPlugin` + `ChatboxTab` (the
  message-type arrays live in `ChatTabs.ChatTab.messageTypes`). The "Clear current tab" bind passes
  `currentTab()`; the per-tab binds pass a fixed constant, so they clear without touching `CHAT_VIEW`
  (no switch, works while the chat is collapsed). The single `tab == null || !supportsClear` guard in
  `clearTab` covers both paths.

- **Silent no-ops, not errors.** Clear on a tab without history (Game/All) → no-op via the
  `supportsClear` flag. Fixed mode → close actions no-op. Not logged in → all actions no-op.

- **Runtime assumptions to confirm in a smoke test** (graceful no-op if wrong): writing
  `VarClientID.CHAT_VIEW = N` + the redraw procs switches to tab N; `CHAT_VIEW == 1337` collapses the
  chat in resizable mode; clearing `getChatLineMap()` nodes + `SPLITPM_CHANGED` removes the tab's
  history; setting `CHATBOX_MODE` + `CHAT_PROMPT_INIT` changes the input channel. None of these persist
  across relog/world-hop (confirms client-side, no packet).

## Releasing

The Plugin Hub distributes plugins by **pinning a commit hash**, not by git tags or GitHub releases:
the hub repo holds `plugins/chat-tab-hotkeys` with `repository=` + `commit=<40-char hash>` (+ our
`authors=`), and a "release" is a PR there that bumps `commit=`. **The hub commit descriptor accepts
only `repository` / `commit` / `authors` — never a `version=` key.** The packager reads the version
from the plugin's own `runelite-plugin.properties`; adding `version=` to the hub descriptor fails the
build with `unexpected key in commit descriptor` (this sank hub PR #14056 for v1.1.1). The `version=`
in `runelite-plugin.properties` is optional (the commit is used if absent) — treat it as the
user-visible version label. Because
`build=standard`, the hub **replaces `build.gradle`/`settings.gradle`** at build time, so their
contents (and the `rl` wrapper) matter only for local dev, never for the hub build.

Tags/releases/CHANGELOG are for our own tracking, not a hub requirement (many hub plugins skip them).
Model to follow: [`melkypie/resource-packs`](https://github.com/melkypie/resource-packs) — SemVer
tags + GitHub Releases + `CHANGELOG.md`. Commit style is Conventional Commits; **no** Claude
co-author trailer.

### Version-management model (standing policy)

Each version is prepared on a **`release/vX.Y.Z` branch off `main`**, then merged to `main` via a PR.

> **Never squash- or rebase-merge a release branch.** The hub pins a commit *hash*; squash/rebase
> rewrites it, orphaning the tagged commit so `commit=` would point at a hash that isn't in `main`'s
> history. Merge with a **merge commit** (GitHub "Create a merge commit") or a **fast-forward** only —
> both keep the tagged release commit reachable on `main`.

The hub `commit=` always pins the **`vX.Y.Z`-tagged release commit**, which lives on `main`'s history
after the (non-squash) merge. So: merge to `main` first, then point the hub at that tag's hash.

Release steps (SemVer: MAJOR breaking / MINOR feature / PATCH fix):
1. On `release/vX.Y.Z`: implement the change (Conventional Commits, **no** Claude co-author trailer),
   then in a final `chore: release vX.Y.Z` commit move `CHANGELOG.md` `[Unreleased]` items under a new
   `## [x.y.z] - <date>` heading (+ link ref) and set `version=x.y.z` in `runelite-plugin.properties`.
2. `git tag -a vX.Y.Z -m "vX.Y.Z"` on that release commit; push the branch + tag
   (`git push -u origin release/vX.Y.Z --follow-tags`).
3. Open a PR `release/vX.Y.Z` → `main` and merge it **with a merge commit (not squash/rebase)**, so the
   tagged commit stays reachable on `main`. Optionally create a GitHub Release from the tag with the
   changelog section as notes.
4. Point the hub manifest's `commit=` at the tagged release commit via a PR to
   `runelite/plugin-hub` (only `commit=` changes — do **not** add a `version=` line; it fails the
   build). **While the plugin's first submission PR is still unreviewed, update that same
   open PR** (push to its head branch) instead of opening a new one — the plugin then first publishes at
   the newer version, skipping the unpublished earlier one. Once the plugin is live on the hub, each
   later version is its own hub PR that bumps `commit=`.
