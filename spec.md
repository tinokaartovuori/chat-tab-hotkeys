# Chat Tab Hotkeys — Specification

Product spec for the plugin: **what it does and how it behaves**. Implementation notes, the in-game
discovery checklist, and Plugin Hub submission steps live in [`handoff.md`](handoff.md); build and
architecture guidance lives in [`CLAUDE.md`](CLAUDE.md).

## Summary

A RuneLite plugin that adds configurable hotkeys for navigating and filtering the chat. Every action
maps to something the player can already do by mouse — switching chat tabs, setting a tab's filter,
clearing a tab's history, and collapsing/expanding the chatbox. No gameplay automation, no overlays,
no auto-reply, no input-channel switching.

## Actions

Four groups of hotkeys. The **7 tab binds default to `Ctrl+1..7`** (typing-safe) for immediate
usability; close, filters, and clear-history default to `Keybind.NOT_SET` (opt-in).

### 1. Tab hotkeys — one per chat tab
Tabs, in game order: **All, Game, Public, Private, Channel, Clan, Trade.**

Pressing a tab's hotkey, given `targetTab`:

| State when pressed | Result |
| --- | --- |
| Chat is closed (collapsed) | Open the chat and show `targetTab` |
| Chat open, a different tab shown | Show `targetTab` |
| Chat open, `targetTab` already shown, `closeOnRepeat` **on** | Close the chat |
| Chat open, `targetTab` already shown, `closeOnRepeat` **off** | Re-show `targetTab` (no-op visually) |

### 2. Close chat — one hotkey, toggles the chatbox
- Closed → open (to the last shown tab).
- Open → close.

### 3. Filter hotkeys — act on the **currently-viewed** tab
`Show all`, `Show friends`, `Show none`. Each sets the current tab's filter, exactly like the
right-click menu entry.

### 4. Clear history — one hotkey
Clears the **currently-viewed** tab's history, like the right-click "Clear history" entry. Implemented
natively (drops the tab's chat lines and rebuilds the chatbox) — no dependency on the Chat History
plugin. No confirmation in v1 (see out of scope).

## Configuration

Settings panel, one `@ConfigSection` per group:

| Section | Items |
| --- | --- |
| **Tab hotkeys** | 7 × `Keybind` — All, Game, Public, Private, Channel, Clan, Trade (default `Ctrl+1..7`) |
| **Close chat** | `boolean closeOnRepeat` (default **true**); 1 × `Keybind` "Close chat" |
| **Chat filters (current tab)** | 3 × `Keybind` — Show all, Show friends, Show none |
| **Clear history (current tab)** | 1 × `Keybind` "Clear history" |

The lower three sections start collapsed (`closedByDefault`) to keep the sidebar tidy.

Filters and clear-history are kept in separate sections because clear-history is destructive and
should not sit next to the harmless filter toggles.

## Behaviour rules & edge cases

- **State is read from game vars at press time** (current tab, collapsed state, text-entry mode), not
  from internal tracking — so the plugin stays correct after the player clicks tabs with the mouse. An
  internal "last tab" is kept only as a fallback for close→reopen.
- **Unsupported actions no-op silently.** Filters and clear-history exist only on the channel-type
  tabs (Public, Private, Channel, Clan, Trade). Game and All don't offer them; pressing a filter/clear
  hotkey while those are active does nothing (no error, no guessed target).
- **Fixed vs resizable mode.** Collapsing the chat only applies in resizable mode. In fixed mode, tab
  switching and filter/clear still work; the close action (and same-tab-twice-closes) no-ops rather
  than erroring.
- **Don't fire while typing.** Hotkeys are suppressed while the chatbox is in an input/text-entry mode
  (`MESLAYERMODE != NONE`). The primary mitigation is the bind choice: the `Ctrl+1..7` defaults (and
  recommended modifier/function-key binds) don't leak into a chat message.

## Definition of done (v1)

- [ ] 7 tab binds (default `Ctrl+1..7`) + 1 close bind + 3 filter binds + 1 clear-history bind (rest unbound), each working.
- [ ] Same-tab-twice closes the chat when `closeOnRepeat` on; re-shows (no-op) when off.
- [ ] Close bind toggles closed/open; reopens to the last tab.
- [ ] Tab bind while chat closed opens it on that tab.
- [ ] Show all/friends/none and Clear history apply to the active tab; no-op on tabs that don't offer them (Game/All).
- [ ] State stays correct after manual mouse clicks (reads game vars).
- [ ] Doesn't fire while typing in chat.
- [ ] No-ops gracefully in fixed mode; no exceptions.
- [ ] No third-party dependencies; `runeLiteVersion = 'latest.release'`.

## Known limitations (v1)

- **Clear history + RuneLite's Chat History plugin.** Clear-history removes the tab's chat lines
  directly; it doesn't fire the menu event RuneLite's Chat History plugin listens for. So if that
  plugin is also enabled with "retain chat history" on, a hotkey-cleared tab may repopulate on
  relog/world-hop. Standalone (the common case) it works as expected.
- **Typing suppression** covers input/dialog modes (`MESLAYERMODE`); it can't detect the always-present
  main message bar, so typing safety relies on the `Ctrl`-modifier default binds.

## Explicitly out of scope for v1

A clear-history confirmation prompt (`confirmClearHistory`), per-tab filter binds, a cycle-tabs bind,
and setting the chat **input** channel on switch. Listed in `handoff.md` under future ideas so they
aren't reinvented.
