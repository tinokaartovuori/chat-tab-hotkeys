# Chat Tab Hotkeys

Configurable hotkeys for navigating and filtering the Old School RuneScape chat. Every action maps to
something you can already do with the mouse — switching chat tabs, setting a tab's filter, clearing a
tab's history, and collapsing the chatbox. No gameplay automation, no overlays.

## Features

- **Tab hotkeys** — one per chat tab (All, Game, Public, Private, Channel, Clan, Trade). Default binds
  are **Ctrl+1–7**. Pressing a tab's hotkey shows it; pressing the same one again closes the chat
  (toggle, in resizable mode).
- **Close chat** — a hotkey to collapse/expand the chatbox; reopens to the last tab. *(Resizable mode
  only — no-op in fixed mode.)*
- **Chat filters** — *Show all / Show friends / Show none* applied to the currently-shown tab, exactly
  like the right-click menu. No-ops on tabs that don't offer them (Game/All).
- **Clear history** — clears the currently-shown tab's history. Implemented natively (no dependency on
  the Chat History plugin).

All binds except the tab hotkeys are unbound by default — set them in the plugin settings.

## Usage

Enable **Chat Tab Hotkeys** in the RuneLite configuration panel and (optionally) rebind any keys.
Settings are grouped into two sections: *Tab hotkeys & close chat* and *Chat filters & clear history*.

Tip: bind modifier combos or function keys. The `Ctrl+1–7` defaults are typing-safe — they won't fire
into a chat message.

## Behaviour notes

- State is read from the game (current tab, collapsed state, text-entry), so hotkeys stay in sync when
  you click tabs with the mouse.
- Hotkeys are suppressed while a chat input/dialog is open.
- Collapsing the chat only applies in **resizable** mode; in fixed mode the close actions no-op while
  tab switching, filters, and clear-history still work.
- If RuneLite's Chat History plugin is enabled with "retain chat history", a tab cleared via this
  plugin's hotkey may repopulate on relog/world-hop.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
