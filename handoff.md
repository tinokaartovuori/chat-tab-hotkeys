# Handoff — cross-plugin menu-event fixes (issues #4 and #5)

> **Temporary file. Delete it before the v1.1.1 release commit.** It exists only to carry context
> into the next session while issues #4 and #5 are fixed. It is not part of the plugin and must not
> ship in a release or a hub commit.

## Branch and how this fits the release

- Working branch: **`fix/cross-plugin-menu-events`**, cut from `release/v1.1.1`.
- Both fixes target the **v1.1.1** release, which is on hold and collecting changes. When #4 and #5
  are done, merge this branch back into `release/v1.1.1` (each fix as its own commit with a
  `## [Unreleased]` CHANGELOG entry), delete this file, then finalize v1.1.1 (see below).

## Project status (2026-07-19)

- **v1.1.0** is tagged and the current user-visible version (`runelite-plugin.properties` `version=1.1.0`).
  Hub publication of the plugin is still pending; the first hub submission PR to `runelite/plugin-hub`
  has not been reviewed yet.
- **v1.1.1 is in progress and held open on `release/v1.1.1`.** It currently has one change:
  - `fix:` **split friends chat toggle** (issue #6, fixed and smoke-tested). `redrawChat()` now runs
    `ScriptID.SPLITPM_CHANGED` instead of `ScriptID.BUILD_CHATBOX`, so collapsing/reopening the chatbox
    rebuilds the split private-chat overlay right away. Its CHANGELOG entry sits under `## [Unreleased]`.
  - **PR #7** (`release/v1.1.1 -> main`, titled "Release v1.1.1 (integration)") is **open** and holds
    the release. Do not merge it to `main` until #4 and #5 are in.
  - There is **no `v1.1.1` tag yet** and **no `chore: release` commit yet**; both were removed on
    purpose so the release can keep collecting changes. `version=` is still `1.1.0`.
- `main` is untouched (last commit `d474f12`).

## The two issues to fix

Both are the same root shape: the plugin performs a UI action the game's own way (game vars + redraw
scripts), which never fires the `MenuOptionClicked` / menu event that other plugins listen for. So a
plugin that mirrors or recolors chat off the mouse menu never hears our keyboard-driven action.

### Issue #4 — chat-mode switch doesn't recolor with Smart Chat Input Color

- Symptom: with Smart Chat Input Color installed, pressing a chat-mode keybind (or Cycle mode) switches
  the channel you type into, but the input line keeps the previous mode's color instead of recoloring.
- Our code: `applyChatMode(ChatMode)` in `ChatTabHotkeysPlugin.java` (~line 260) writes
  `VarClientID.CHATBOX_MODE` then runs `ScriptID.CHAT_PROMPT_INIT` to redraw the input line. A mouse
  click on the mode button fires an extra event that Smart Chat Input Color hooks for its recolor; the
  var write alone doesn't reach it.
- Next step: find which event Smart Chat Input Color recolors on (read its source), then fire that (or
  its equivalent) after the mode change. Confirm the exact trigger before coding.

### Issue #5 — keyboard clear-history doesn't clear the Chat Widgets overlay

- Symptom: with the Chat Widgets plugin (`cnnnr/chat-widgets`) installed, clearing a tab by mouse
  (right-click tab -> Clear history) also clears that tab from the Chat Widgets overlay, but clearing
  with our keybind leaves the overlay text in place.
- Our code: `clearTab(ChatTab)` in `ChatTabHotkeysPlugin.java` (~line 218) removes the tab's message
  nodes from `client.getChatLineMap()` then runs `ScriptID.SPLITPM_CHANGED`. It fires no
  `MenuOptionClicked`, so Chat Widgets (which clears its private list only when it catches the mouse
  clear via `onMenuOptionClicked`, matching option text like "Clear" + a tab label) never hears it.
- Options to weigh (still undecided; see issue #5):
  1. Post the equivalent menu event on our side so Chat-Widgets-style plugins clear in step. Check
     whether a synthetic `MenuOptionClicked` is clean and feasible.
  2. File it upstream on Chat Widgets to clear off a non-menu signal (hard, no such signal today).
  3. Document it as a known cross-plugin limitation, like the Chat History retain-history caveat
     already in `spec.md`.

### Shared angle

If a synthetic `MenuOptionClicked` (or the specific event each plugin hooks) is the chosen path, both
issues may share one helper. Verify each plugin's actual listener first; do not assume they hook the
same event.

## Constraints (Plugin Hub, non-negotiable)

- Packet-free / client-side only. No server-persisted state.
- **Do not** reintroduce a generic "click any widget" primitive. The rejected build used
  `runScript(175)` and `replayWidgetOp`/`replayTabOp` (`getOnOpListener()` + `createScriptEventBuilder`);
  those are gone and must stay gone.
- Java 11 language level. No third-party dependencies. `runeLiteVersion = 'latest.release'`.

## Build, test, run

- Use the **`./rl`** wrapper, not `./gradlew` (host JDK is 26; `rl` runs Gradle in a JDK-11 container).
  - `./rl compileJava`, `./rl build` (compile + test + jar), `./rl run` (GUI).
- In-game testing goes through Bolt's dev client. Toggle Bolt to launch the dev build by setting
  `runelite_launch_command` in `~/.config/bolt-launcher/launcher.json` to
  `/mnt/x/Github/chat-tab-hotkeys/tools/bolt-run.sh` (set to `null` for stock RuneLite). Bolt must not
  be running while editing that file. It is currently set to **stock** (`null`).

## Finalizing v1.1.1 (after #4 and #5 land, and this file is deleted)

1. On `release/v1.1.1`, ensure each fix has a `## [Unreleased]` CHANGELOG entry.
2. **Delete `handoff.md`.**
3. Final `chore: release v1.1.1` commit: move `[Unreleased]` items under `## [1.1.1] - <date>` (+ link
   ref) and set `version=1.1.1` in `runelite-plugin.properties`.
4. Tag `v1.1.1` on that commit; push branch + tag.
5. Merge PR #7 to `main` **with a merge commit** (never squash/rebase — the hub pins a commit hash).
6. Point the hub manifest `commit=` (and `version=`) at the tagged commit. While the plugin's first
   hub submission PR is still unreviewed, update that same open PR rather than opening a new one.

Commit style: Conventional Commits, **no** Claude co-author trailer.
