# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Configurable hotkeys to switch to each chat tab (All, Game, Public, Private, Channel, Clan, Trade),
  defaulting to `Ctrl+1..7`.
- Close-chat hotkey that toggles the chatbox collapsed/open (reopens to the last tab).
- Filter hotkeys (Show all / Show friends / Show none) acting on the currently-viewed tab.
- Native clear-history hotkey for the currently-viewed tab (no Chat History plugin dependency).
- `closeOnRepeat` option: pressing a tab's hotkey again closes the chat.
- Tidy settings sidebar: the Close/Filters/Clear sections start collapsed.

_Note: pre-1.0.0 and not yet published to the Plugin Hub; behaviour is defined in [`spec.md`](spec.md).
Needs an in-game smoke test before tagging v1._

[Unreleased]: https://github.com/tinokaartovuori/chat-tab-hotkeys/commits/main
