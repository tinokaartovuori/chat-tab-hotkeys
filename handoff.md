# Handoff: Chat Tab Hotkeys — current state

Short doc for whoever picks this up next (future me or Kart5a later). The product spec is in
`spec.md`, build and release detail in `CLAUDE.md`. This file is about where things actually stand.

## Where things stand

The plugin is fully implemented, tagged **v1.0.0**, and works in game (tested via the Bolt dev client).
It was submitted to the RuneLite Plugin Hub as PR #13356 and **rejected**. A policy question has been
posted to the RuneLite Discord and we are waiting on the answer before deciding the next step. The
plugin works locally right now no matter what the hub decides.

## Why it was rejected

Reviewer riktenx, word for word:

> we don't allow clientscript execution for the purpose of switching active interfaces, even when
> behind keypresses, because this is liable to trigger anticheat and get users banned

Our mechanism was `replayWidgetOp()`, which fires the tab button's own onop CS2 handler via
`client.createScriptEventBuilder(button.getOnOpListener()).setOp(n).build().run()` to switch tabs and
apply filters. That is a generic "click any widget" primitive, which is what the reviewer objects to.

## What the research established (the reusable facts)

- Script **175** is `[clientscript,chat_button_onop]`, the chat tab button's own onop handler.
  `runScript(175, 1, N)` with N a tab index 0..6:
  - N not equal to the current tab, it sets `CHAT_VIEW` (varc 41) to N and redraws, so it switches to tab N.
  - resizable and N equal to the current tab, it sets varc 41 to 1337, which collapses the chat. This
    is exactly how Toggle Chat closes.
  - It is packet free. It only writes client side varcs and redraws. No server op.
- **Toggle Chat** (approved, hub commit `8bc61d2`) uses exactly `runScript(175, 1, tab)` to open the
  chat to a chosen tab. So the same call is already accepted for one plugin.
- **Filters** (Show all/friends/none and the Public tab set) call `chat_set_filter`, which is
  server persisted, so setting a filter **sends a packet**. Filters are dead regardless of the outcome.
- `client.menuAction` is **not** a loophole. It is worse, it sends an `if_button` packet without a real
  click, which is the classic banned automation pattern.
- Tab switch, close/collapse, clear history, and chat input mode are all packet free.
- The open question is whether the ban is about the **mechanism** (the generic click synthesis) or the
  **purpose** (switching the active tab by any means). Toggle Chat suggests the specific leaf script 175
  might be fine. Only RuneLite can confirm.

## The question posted to RuneLite

Asked on the RuneLite Discord (plain summary):
1. Is `runScript(175, 1, N)` on a keypress to switch the current tab ok, since Toggle Chat already uses
   it to open to a chosen tab, or is the problem the purpose regardless of the call.
2. If that script is off limits, is a pure `setVarcIntValue(CHAT_VIEW, N)` ok, or is tab switching not
   allowed by any method.
3. Separate from tabs, would a close/open toggle plus clear history plus a chat input mode bind be ok on
   their own.

Links:
- Rejected PR (closed): https://github.com/runelite/plugin-hub/pull/13356
- Toggle Chat reference: https://github.com/MoreBuchus/buchus-plugins/blob/toggle-chat/src/main/java/com/togglechat/ToggleChatPlugin.java#L152

## Next steps, decided by the Discord answer

- **A) If `runScript(175, 1, N)` tab switching is allowed.** Replace `replayWidgetOp` and `replayTabOp`
  with `client.runScript(175, 1, targetTab.tabIndex)`. For a plain switch, if the target equals the
  current tab do nothing (in resizable mode that call would collapse). Keep filters dropped. Rebuild,
  tag a new version, open a new hub PR that bumps `commit=`.
- **B) If only close, clear, and mode are allowed (no tab switching at all).** Drop the 7 tab binds, the
  3 filter binds, and cycle filter. Keep the close toggle (via runScript 175), clear history, chat input
  mode and cycle mode. Rename and rescope honestly since it then overlaps Toggle Chat. Update
  displayName, README, spec. Resubmit.
- **C) If nothing programmatic is allowed.** Do not resubmit. Keep it as a personal sideloaded plugin.
  It already works via the Bolt dev client.

## Where the code to change is

- `ChatTabHotkeysPlugin.java`: `replayWidgetOp()` and `replayTabOp()` are the single choke point (the
  rejected primitive). `onFilterHotkey` / `applyFilter` / `onCycleFilterHotkey` / `filterOpIndices` are
  the filter paths to drop. `applyChatMode()` and `onClearHistoryHotkey()` are already packet free and in
  the accepted style, so they stay.
- `ChatTabs.java`: `ChatTab` enum (widgetId, tabIndex 0..6), `FilterOp` labels.
- `ChatTabHotkeysConfig.java`: three config sections and the keybinds.

## Build, run, release (detail in CLAUDE.md)

- Build with `./rl` (Docker JDK 11) because the host JDK is too new for RuneLite's Gradle.
  `RUNELITE_GRADLE_CACHE=~/.cache/runelite-gradle ./rl build`.
- You cannot build while the dev client is running (they share the project `.gradle` lock). Build in an
  isolated copy or close the client first.
- In game via Bolt dev mode. Turn it on or off by setting or nulling `runelite_launch_command` in Bolt's
  `launcher.json` with Bolt closed. See the dev-mode notes.
- Release plus hub PR steps are in CLAUDE.md under "Releasing".

## Note

This file replaced the original pre-build brief. That content is still in git history (commit
`a541a4a`). The product spec now lives in `spec.md`.
