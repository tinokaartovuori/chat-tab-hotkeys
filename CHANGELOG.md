# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/tinokaartovuori/chat-tab-hotkeys/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/tinokaartovuori/chat-tab-hotkeys/releases/tag/v1.0.0
