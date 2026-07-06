# Chat Tab Hotkeys

Keyboard control for the chat tabs. Switch tab, set a tab's filter, clear its history, and open or
close the chatbox without reaching for the mouse. Everything it does is something you can already do
by clicking — no automation, no overlays.

## Keys

- **Tab keys** — one per chat tab (All, Game, Public, Private, Channel, Clan, Trade), bound to
  **Ctrl+1–7** by default. Press a tab's key to show it; press it again to close the chat. (The
  "press again to close" behaviour can be turned off.)
- **Close chat** — toggles the chatbox and reopens to the last tab. Resizable mode only.
- **Show all / friends / none** — sets the current tab's filter, exactly like the right-click menu.
- **Clear history** — clears the current tab's messages. Done directly, so no other plugin is needed.

Only the tab keys are bound out of the box; bind the rest in the settings if you want them.

## Settings

<img src="docs/settings.png" alt="Chat Tab Hotkeys settings panel" width="242">

Two groups: the tab keys and close-chat, then the filters and clear-history. The `Ctrl`+number
defaults are safe to leave on — modifier combos don't leak into a chat message.

## Notes

- The current tab and open/closed state are read from the game, so the keys stay correct after you
  click tabs with the mouse.
- Keys are ignored while a chat input box is open.
- Closing the chat only works in resizable mode. In fixed mode those two actions do nothing, while
  tab switching, filters and clear still work.
- Filters and clear-history only apply to the tabs that offer them (not Game or All).
- If you also run the Chat History plugin with *retain chat history* on, a tab you clear with this
  plugin can reappear after a world-hop or relog.

## License

BSD 2-Clause — see [LICENSE](LICENSE).
