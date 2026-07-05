# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status: scaffolded, awaiting in-game discovery

The project is scaffolded from `runelite/example-plugin` and compiles cleanly against the RuneLite
client API. What's left is the in-game discovery pass: every widget/var/menu id lives in
`ChatTabs.java` as `UNSET` (-1) placeholders, and the plugin no-ops rather than throwing until they
are filled in. `handoff.md` is the source of truth for behaviour — read it before implementing. It
holds the feature spec, config layout, core press-time logic, the discovery checklist, and the v1
definition of done. This file captures the build/architecture context that document assumes.

Source layout (package `com.chattabhotkeys`):
- `ChatTabHotkeysConfig.java` — fully implemented; all 4 `@ConfigSection`s and 12 keybinds per spec.
- `ChatTabHotkeysPlugin.java` — hotkeys wired end-to-end; core logic done; action/state calls guarded
  on the `UNSET` placeholders.
- `ChatTabs.java` — the single holder for all discovered ids (widget group/children, vars, menu ops).
  **Fill this in during discovery**; grep for `TODO(discovery)`.

## What this plugin is

**Chat Tab Hotkeys** (repo `chat-tab-hotkeys`) — configurable hotkeys for navigating and filtering
the OSRS chat: 7 tab binds (All, Game, Public, Private, Channel, Clan, Trade), 1 close-chat toggle,
3 filter binds (Show all/friends/none) acting on the *currently-viewed* tab, and 1 clear-history
bind. All binds default to `Keybind.NOT_SET`. It only automates UI actions the player can already
do by mouse — no gameplay automation.

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

`./rl run` opens a GUI: it forwards `$DISPLAY` and the X11 socket into the container. On some setups
you must first run `xhost +local:` to allow it. This is the path for the in-game discovery pass
(Widget Inspector + Var Inspector) — there is no viable host-side `run` while the host JDK is 26.

If a JDK 11–23 (with `javac`) is ever installed on the host, plain `./gradlew` works and `./rl`
becomes unnecessary.

Constraints (non-negotiable for Plugin Hub review): `runeLiteVersion = 'latest.release'`, Java 11
language level, **no third-party dependencies**.

## Architecture that isn't obvious from the code

- **Read state from game vars, not internal tracking.** At press time, resolve `currentTab`,
  `chatClosed`, and text-entry mode via `client.getVarcIntValue(...)` / `client.getVarbitValue(...)`
  so the plugin stays in sync when the player clicks tabs with the mouse. Keep an internal "last tab"
  only as a fallback for close→reopen.

- **Thread discipline.** `HotkeyListener`s fire on the key-event thread. All client-touching work
  (script events, menu actions, var reads that mutate) must be wrapped in `clientThread.invoke(...)`.
  Never call `runScript`/menu actions directly from the key handler.

- **Two distinct mechanisms for tab actions:**
  - *Left-click behavior* (switch tab, collapse/expand chat) → replay the tab button widget's own CS2
    listener: `client.createScriptEvent(tab.getOnOpListener()).setSource(tab).run()` (fall back to
    `getOnClickListener()`). This is ID-stable across Jagex script reshuffles; prefer it over
    `runScript(scriptId, arg)`.
  - *Right-click menu ops* (Show all/friends/none, Clear history) → replay a menu action via
    `client.menuAction(param0, param1, menuAction, id, -1, option, target)`, where `param1` is the
    packed widget id of the currently-active tab's button. These op ids are consistent across the
    channel-type tabs.

- **Discovery-first workflow.** The widget child ids (chatbox group is expected to be 162), the menu
  action/op-id/params for each filter, the collapse button id, and the vars for current-tab /
  collapsed / text-entry are **unknown until observed in-game** with the inspectors. Follow the
  discovery checklist in `handoff.md`, then record every value in a single `Constants`/`ChatTabs`
  holder with a comment citing where each came from, so future game updates are cheap to re-verify.

- **Silent no-ops, not errors.** If the active tab doesn't support a pressed action (Game/All don't
  offer Show all/friends/none), no-op. In fixed mode, chat collapse can't happen, so the close bind
  and same-tab-twice-closes must no-op rather than throw. Suppress hotkeys entirely while the user is
  typing in chat.
