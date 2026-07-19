# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.1] - 2026-07-20

### Fixed
- **Close chat now respects the split friends private chat view.** The Close-chat hotkey (and
  Close-on-repeat) rebuilds the split private-chat overlay when it collapses or reopens the chatbox, so
  the overlay hides and returns with the chatbox under the OSRS "Hide private chat when the chatbox is
  hidden" setting, the same as clicking the chatbox toggle. Before, the overlay kept its old state until
  something else refreshed the chat, such as the next incoming message, so it hid late or stayed visible.
  ([#6](https://github.com/tinokaartovuori/chat-tab-hotkeys/issues/6))

## [1.1.0] - 2026-07-09

### Added
- **Per-tab clear-history hotkeys** — a separate **Clear: Public / Private / Channel / Clan / Trade**
  bind for each clearable tab, clearing that tab's history without switching to it (e.g. clear the
  friends chat while viewing another tab). No tab switch, no chat open/close.

### Changed
- The existing clear-history hotkey is renamed **Clear current tab** and moved into a new **Clear
  history** settings section (its saved bind is preserved). The former "Tab hotkeys, close & clear"
  section is now **Tab hotkeys & close**.

## [1.0.0] - 2026-07-08

Initial Plugin Hub release.

### Added
- Configurable hotkeys to switch to each chat tab (All, Game, Public, Private, Channel, Clan, Trade),
  defaulting to `Ctrl+1..7`.
- **Cycle tab** hotkey — steps through a configurable set of tabs (wraps around; opens the chat if it
  is collapsed).
- **Close chat** hotkey that toggles the chatbox collapsed/open in resizable mode (reopens to the last
  tab), plus a `Close on repeat` option so pressing a tab's own hotkey again closes the chat.
- Native **clear history** hotkey for the currently-viewed tab (no Chat History plugin dependency);
  no-ops on the Game and All tabs.
- **Chat input mode** hotkeys — set the channel your typed messages go to (Public, Channel, Clan,
  Guest clan, Group).
- **Cycle mode** hotkey — steps through a configurable set of input modes.
- Configurable cycle sets: **"Tabs to cycle"** and **"Modes to cycle"** multi-select lists (the
  RuneLite `Set<Enum>` widget, like World Hopper's filters). Tabs default to all seven; modes default
  to Public / Channel / Clan / Guest clan (Group left out, since the game resets it when you are not in
  a group ironman group). If you add Group to the mode list while not in a group, the cycle steps past
  it rather than getting stuck.
- Settings grouped into two always-visible sections: **"Tab hotkeys, close & clear"** and
  **"Chat input mode"**.

Everything is packet-free and client-side: tab switching writes `VarClientID.CHAT_VIEW` and runs benign
redraw procedures; no server-persisted state, no gameplay automation.

[unreleased]: https://github.com/tinokaartovuori/chat-tab-hotkeys/compare/v1.1.1...HEAD
[1.1.1]: https://github.com/tinokaartovuori/chat-tab-hotkeys/releases/tag/v1.1.1
[1.1.0]: https://github.com/tinokaartovuori/chat-tab-hotkeys/releases/tag/v1.1.0
[1.0.0]: https://github.com/tinokaartovuori/chat-tab-hotkeys/releases/tag/v1.0.0
