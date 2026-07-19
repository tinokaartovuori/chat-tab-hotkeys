# Chat Tab Hotkeys — Specification

Product spec for the plugin: **what it does and how it behaves**. Build and architecture guidance,
along with Plugin Hub submission steps, live in [`CLAUDE.md`](CLAUDE.md).

## Summary

A RuneLite plugin that adds configurable hotkeys for navigating the chat: switching chat tabs,
clearing a tab's history, collapsing/expanding the chatbox, and choosing the chat input channel. No
gameplay automation, no overlays, no auto-reply. Everything is packet-free and client-side — the
plugin never sends anything to the game server.

## Actions

Hotkeys in a few groups. The **7 tab binds default to `Ctrl+1..7`** (typing-safe) for immediate
usability; the close, clear-history, cycle, and chat-input-mode binds default to `Keybind.NOT_SET`
(opt-in).

### 1. Tab hotkeys — one per chat tab
Tabs, in game order: **All, Game, Public, Private, Channel, Clan, Trade.**

Pressing a tab's hotkey, given `targetTab`:

| State when pressed | Result |
| --- | --- |
| Chat is closed (collapsed) | Open the chat and show `targetTab` |
| Chat open, a different tab shown | Show `targetTab` |
| Chat open, `targetTab` already shown, `closeOnRepeat` **on** | Close the chat |
| Chat open, `targetTab` already shown, `closeOnRepeat` **off** | Re-show `targetTab` (no-op visually) |

### 2. Cycle tab — one hotkey, steps through the chosen tabs
One bind advances to the next tab in a configurable set (game order, wrapping). The set is a
multi-select list, **"Tabs to cycle"** (a `Set<ChatTab>` config item like the World Hopper region
filter), defaulting to all seven selected. Behaviour:

- Reads the shown tab from the game var, so it stays in sync with mouse clicks.
- If the shown tab is in the set, it goes to the next one in the set; wraps from the last back to the
  first.
- If the shown tab is **not** in the set, it goes to the first selected tab.
- If the chat is collapsed, it resumes from the last-shown tab and opens the chat on the next one.
- Empty selection → no-op. Cycle tab never closes the chat (unlike a repeated single-tab bind).

### 3. Close chat — one hotkey, toggles the chatbox
- Closed → open (to the last shown tab).
- Open → close.
- Respects the OSRS "Split friends private chat" view: collapsing or reopening the chatbox rebuilds the
  split private-chat overlay (via `SPLITPM_CHANGED`), so it hides and returns with the chatbox under the
  "Hide private chat when the chatbox is hidden" setting, the same as clicking the chatbox toggle. It no
  longer waits for a later chat message to refresh.

### 4. Clear history — one bind for the current tab, plus one per tab
Clears a tab's history, like the right-click "Clear history" entry. Implemented natively (drops the
tab's chat lines and rebuilds the chatbox) — no dependency on the Chat History plugin. Two ways to bind:

- **Clear current tab** — clears the **currently-viewed** tab.
- **Clear: Public / Private / Channel / Clan / Trade** — one bind per channel-type tab; clears that
  tab's history **without switching to it** (so you can clear the friends chat while viewing another
  tab). No tab switch, no chat open/close.

All clear binds work only on the channel-type tabs (Public, Private, Channel, Clan, Trade); Game and
All have no history to clear, so "Clear current tab" no-ops there (the per-tab binds only exist for the
five clearable tabs). No confirmation (see out of scope).

### 5. Chat input mode — sets the channel you type into
Hotkeys for `Public`, `Channel`, `Clan`, `Guest clan`, `Group`, matching the game's right-click
"Set chat mode" on the All tab. Applied by writing the chat-mode client var and rerunning the
chatbox-input build script. Group only takes effect while in a group ironman group (the game resets it
otherwise).

**Ordering invariant (load-bearing — do not reorder).** In `applyChatMode` the mode var
(`VarClientID.CHATBOX_MODE`, 945) is written **before** `runScript(ScriptID.CHAT_PROMPT_INIT)`, both on
the client thread. Rerunning that script redraws the input line, and — because a plugin-invoked
`runScript` fires the same `ScriptPostFired` the game's own script does — it is also what lets input-colour
plugins such as **Smart Chat Input Color** recolour the input to the new channel: SCIC recolours on
`ScriptPostFired(CHAT_PROMPT_INIT)` and reads mode var 945, which we have already set by then. Reversing
the two calls, or moving either off the client thread, would make SCIC read the *previous* mode and leave
the input showing the old channel's colour. This cross-plugin recolour needs no code of our own; it works
purely by keeping this order (issue #4).

### 6. Cycle mode — one hotkey, steps through the chosen input modes
A `Cycle mode` bind advances to the next input mode in a configurable set (game order, wrapping),
reading the current mode from the game var. The set is a multi-select list, **"Modes to cycle"** (a
`Set<ChatMode>` config item), defaulting to Public / Channel / Clan / Guest clan. **Group is left out
of the default** because the game self-resets it to the current mode when you are not in a group
ironman group; if you do select it while not in a group, the cycle steps past it (the plugin remembers
it just tried Group, so the reverted var doesn't trap the cycle). Empty selection → no-op.

## Configuration

Settings panel, three always-visible `@ConfigSection`s:

| Section | Items |
| --- | --- |
| **Tab hotkeys & close** | 7 × `Keybind` — All, Game, Public, Private, Channel, Clan, Trade (default `Ctrl+1..7`); `boolean closeOnRepeat` (default **true**); 1 × `Keybind` "Close chat"; 1 × `Keybind` "Cycle tab"; `Set<ChatTab>` "Tabs to cycle" (default all seven) |
| **Clear history** | 1 × `Keybind` "Clear current tab"; 5 × `Keybind` "Clear: Public / Private / Channel / Clan / Trade" (all unbound) |
| **Chat input mode** | 5 × `Keybind` — Set mode: Public, Channel, Clan, Guest clan, Group; 1 × `Keybind` "Cycle mode"; `Set<ChatMode>` "Modes to cycle" (default Public/Channel/Clan/Guest clan) |

Both sections render expanded (no `closedByDefault`). The two "to cycle" items render as multi-select
dropdown lists (the RuneLite `Set<Enum>` widget, as used by World Hopper's filters).

## Behaviour rules & edge cases

- **State is read from game vars at press time** (current tab, collapsed state, text-entry mode), not
  from internal tracking — so the plugin stays correct after the player clicks tabs with the mouse. An
  internal "last tab" is kept only as a fallback for the close→reopen and cycle-tab-when-collapsed
  resume paths.
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

- [ ] 7 tab binds (default `Ctrl+1..7`) + 1 close bind + a "Clear current tab" bind + 5 per-tab "Clear: X" binds (rest unbound), each working.
- [ ] Same-tab-twice closes the chat when `closeOnRepeat` on; re-shows (no-op) when off.
- [ ] Close bind toggles closed/open; reopens to the last tab.
- [ ] With "Split friends private chat" on and "Hide private chat when the chatbox is hidden" on, the close bind hides the split overlay instantly on collapse and restores it instantly on reopen (no delay, no waiting for a message); no-op when either setting is off.
- [ ] Tab bind while chat closed opens it on that tab.
- [ ] "Clear current tab" applies to the active tab; the per-tab "Clear: X" binds clear their tab without switching to it; both no-op on tabs that don't offer it (Game/All).
- [ ] Chat input mode binds set the channel you type into; Cycle mode steps through the selected modes.
- [ ] Cycle tab steps through the selected tabs (wrapping); opens the chat on the next tab when collapsed; no-ops with an empty selection.
- [ ] The "Tabs to cycle" / "Modes to cycle" lists actually narrow their respective cycles.
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

A clear-history confirmation prompt (`confirmClearHistory`). (Setting the chat **input** channel and a
cycle-tabs bind, once future ideas, are now implemented — see "Cycle tab" and "Chat input mode".)
