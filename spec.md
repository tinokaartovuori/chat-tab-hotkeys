# Chat Tab Hotkeys — Specification

Product spec for the plugin: **what it does and how it behaves**. Implementation notes, the in-game
discovery checklist, and Plugin Hub submission steps live in [`handoff.md`](handoff.md); build and
architecture guidance lives in [`CLAUDE.md`](CLAUDE.md).

## Summary

A RuneLite plugin that adds configurable hotkeys for navigating the chat: switching chat tabs,
clearing a tab's history, collapsing/expanding the chatbox, and choosing the chat input channel. No
gameplay automation, no overlays, no auto-reply. Everything is packet-free and client-side — the
plugin never sends anything to the game server.

## Actions

Four groups of hotkeys. The **7 tab binds default to `Ctrl+1..7`** (typing-safe) for immediate
usability; the close, clear-history, and chat-input-mode binds default to `Keybind.NOT_SET` (opt-in).

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

### 3. Clear history — one hotkey
Clears the **currently-viewed** tab's history, like the right-click "Clear history" entry. Implemented
natively (drops the tab's chat lines and rebuilds the chatbox) — no dependency on the Chat History
plugin. Works on the channel-type tabs (Public, Private, Channel, Clan, Trade); no-ops on Game/All. No
confirmation in v1 (see out of scope).

### 4. Chat input mode — sets the channel you type into
Hotkeys for `Public`, `Channel`, `Clan`, `Guest clan`, `Group`, matching the game's right-click
"Set chat mode" on the All tab. Applied by writing the chat-mode client var and rerunning the
chatbox-input build script. Group only takes effect while in a group ironman group (the game resets it
otherwise). A `Cycle mode` hotkey rotates Public → Channel → Clan → Guest clan (reading the current
mode from the game). Group is excluded from the cycle because the game self-resets it when not in a
group, which would trap the cycle; it remains available on its own bind.

## Configuration

Settings panel, two always-visible `@ConfigSection`s:

| Section | Items |
| --- | --- |
| **Tab hotkeys, close & clear** | 7 × `Keybind` — All, Game, Public, Private, Channel, Clan, Trade (default `Ctrl+1..7`); `boolean closeOnRepeat` (default **true**); 1 × `Keybind` "Close chat"; 1 × `Keybind` "Clear history" |
| **Chat input mode** | 5 × `Keybind` — Set mode: Public, Channel, Clan, Guest clan, Group; 1 × `Keybind` "Cycle mode" |

Both sections render expanded (no `closedByDefault`).

## Behaviour rules & edge cases

- **State is read from game vars at press time** (current tab, collapsed state, text-entry mode), not
  from internal tracking — so the plugin stays correct after the player clicks tabs with the mouse. An
  internal "last tab" is kept only as a fallback for close→reopen.
- **Unsupported actions no-op silently.** Clear-history exists only on the channel-type tabs (Public,
  Private, Channel, Clan, Trade). Game and All don't offer it; pressing the clear hotkey while those
  are active does nothing (no error, no guessed target).
- **Fixed vs resizable mode.** Collapsing the chat only applies in resizable mode. In fixed mode, tab
  switching and clear still work; the close action (and same-tab-twice-closes) no-ops rather than
  erroring.
- **Hotkeys fire whenever logged in.** There is no typing gate — the always-present chat input can't be
  detected reliably, so the plugin doesn't try. Typing safety relies on the bind choice: the `Ctrl+1..7`
  defaults (and recommended modifier/function-key binds) don't leak into a chat message.

## Definition of done (v1)

- [ ] 7 tab binds (default `Ctrl+1..7`) + 1 close bind + 1 clear-history bind (rest unbound), each working.
- [ ] Same-tab-twice closes the chat when `closeOnRepeat` on; re-shows (no-op) when off.
- [ ] Close bind toggles closed/open; reopens to the last tab.
- [ ] Tab bind while chat closed opens it on that tab.
- [ ] Clear history applies to the active tab; no-ops on tabs that don't offer it (Game/All).
- [ ] Chat input mode binds set the channel you type into; Cycle mode rotates Public → Channel → Clan → Guest clan.
- [ ] State stays correct after manual mouse clicks (reads game vars).
- [ ] No-ops gracefully in fixed mode; no exceptions.
- [ ] No third-party dependencies; `runeLiteVersion = 'latest.release'`.

## Known limitations (v1)

- **Clear history + RuneLite's Chat History plugin.** Clear-history removes the tab's chat lines
  directly; it doesn't fire the menu event RuneLite's Chat History plugin listens for. So if that
  plugin is also enabled with "retain chat history" on, a hotkey-cleared tab may repopulate on
  relog/world-hop. Standalone (the common case) it works as expected.
- **Group chat input mode is conditional.** Setting the `Group` chat mode only sticks while you are in
  a group ironman group; the game resets it otherwise, which is why it is excluded from the mode cycle.
- **No typing detection.** The plugin can't reliably tell when you are typing a normal chat message
  (the always-present main message bar isn't exposed via `MESLAYERMODE`), so it doesn't try to suppress
  the hotkeys while typing. Typing safety relies on the `Ctrl`-modifier default binds.

## Explicitly out of scope for v1

A clear-history confirmation prompt (`confirmClearHistory`) and a cycle-tabs bind. Listed in
`handoff.md` under future ideas so they aren't reinvented. (Setting the chat **input** channel, once a
future idea, is now implemented — see "Chat input mode".)
