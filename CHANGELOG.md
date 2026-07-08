# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.0] - 2026-07-08

### Removed
- **The chat filter feature has been removed entirely.** All filter hotkeys (Show all /
  Show friends / Show none), the Cycle-filter hotkey, and the Public-tab filter hotkeys
  (Show autochat / Show standard / Hide) are gone, along with their config section. The
  plugin no longer touches chat filtering in any form.
- **The generic client-script click primitive is gone.** The previous approach that
  replayed an arbitrary widget's on-op handler (`replayWidgetOp` / `getOnOpListener` with
  `createScriptEventBuilder`) — the primitive the Plugin Hub rejected (PR #13356) — has
  been removed, as has the interim `runScript(175, ...)` approach.

### Changed
- **Tab switch / open / close** now use a plain `setVarcIntValue(VarClientID.CHAT_VIEW, tab)`
  followed by benign redraw procedures (`runScript(923 toplevel_chatbox_background)`,
  `runScript(178 redraw_chat_buttons)`, `runScript(ScriptID.BUILD_CHATBOX)`), plus
  `runScript(183 chat_alert_set, tab, 0)` to clear the tab's alert. Closing the chat sets
  `CHAT_VIEW` to the sentinel `1337` and redraws; it is a toggle in resizable mode only
  (fixed mode is a no-op) and reopens to the last tab. Everything remains packet-free and
  client-side, with no server-persisted state.
- Config sections reduced from three to two: **"Tab hotkeys, close & clear"** (7 tab binds,
  Close on repeat, Close chat, Clear history) and **"Chat input mode"** (5 mode binds +
  Cycle mode).
- Hotkeys now fire whenever you are logged in; the previous typing gate was removed (it only ever
  caught modal chat inputs, never normal typing, so it was invisible in practice). The `Ctrl+1..7`
  default binds keep keys from leaking into a typed message.

## [1.0.0] - 2026-07-06

### Added
- Configurable hotkeys to switch to each chat tab (All, Game, Public, Private, Channel, Clan, Trade),
  defaulting to `Ctrl+1..7`.
- Close-chat hotkey that toggles the chatbox collapsed/open (reopens to the last tab).
- Filter hotkeys (Show all / Show friends / Show none) acting on the currently-viewed tab.
- Cycle-filter hotkey that rotates the current tab through the options it offers (3 on most tabs,
  5 on the Public tab).
- Public-tab filter hotkeys: Show autochat, Show standard, Hide.
- Chat input mode hotkeys: set the channel you type into (Public, Channel, Clan, Guest clan, Group),
  plus a Cycle mode hotkey that rotates Public / Channel / Clan / Guest clan.
- Native clear-history hotkey for the currently-viewed tab (no Chat History plugin dependency).
- `closeOnRepeat` option: pressing a tab's hotkey again closes the chat.
- Settings grouped into three always-visible sections: "Tab hotkeys & close chat",
  "Chat filters & clear history", and "Chat input mode".

> **Note:** The filter features listed above were removed in [1.1.0]; they remain here as a
> historical record of what shipped in 1.0.0.

[Unreleased]: https://github.com/tinokaartovuori/chat-tab-hotkeys/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/tinokaartovuori/chat-tab-hotkeys/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/tinokaartovuori/chat-tab-hotkeys/releases/tag/v1.0.0
