# Handoff: Chat Tab Hotkeys — current state

Short doc for whoever picks this up next (future me or Kart5a later). The product spec is in
`spec.md`, build and release detail in `CLAUDE.md`. This file is about where things actually stand.

## Where things stand

The plugin was rejected from the RuneLite Plugin Hub (PR #13356) and has since been **reworked** on
branch `fix/remove-rejected-clientscript-primitive`. The current working tree is **not yet committed**
and builds clean (`./rl build`).

Big change this round: the **chat FILTER feature was removed entirely** (see "Why filters were
removed"). The plugin now does four things, all packet-free / client-side and confirmed working
in-game via the Bolt dev client:

- **7 tab hotkeys** (All, Game, Public, Private, Channel, Clan, Trade; default `Ctrl+1..7`)
- **Close / collapse** the chat (toggle; resizable only)
- **Clear history** for the current tab (moved into the first config section)
- **Chat input mode** (Public/Channel/Clan/Guest/Group + cycle)

Config now has **two** sections: "Tab hotkeys, close & clear" and "Chat input mode".

**Repo dir was renamed** `chat-hotkeys` → `chat-tab-hotkeys` (2026-07-08); `tools/bolt-run.sh` is
path-agnostic but Bolt's `runelite_launch_command` must point at the current absolute path (see
`[[dev-mode-bolt]]`).

## The rejection + the Discord ruling (why the mechanisms are what they are)

riktenx rejected v1.0.0 for *"clientscript execution for the purpose of switching active interfaces,
even when behind keypresses"* — our old `replayWidgetOp` (a generic `getOnOpListener()` +
`createScriptEventBuilder` "click any widget" primitive). Discord follow-up (riktenx, ron, Shaggy):

- Clientscript execution is **not blanket banned**; "it depends what the script does."
- The real rule is: **no packets, no server-persisted state changes.**
- **Do not** use existing hub plugins (e.g. Toggle Chat) as precedent — rules change, not applied
  retroactively.
- On tab switching: *"the fact that this just bubbles down to a varc set may mean it's ok, i don't
  know"* (riktenx); *"just use the var if it works"* (Shaggy).

So tab switching was rebuilt to set the varc directly (the switch **is** the varc; scripts only
redraw).

## Final feature set + mechanisms (all packet-free / client-side)

- **Tab switch / open** — `setVarcIntValue(VarClientID.CHAT_VIEW /*41*/, tab.tabIndex)`, then
  `runScript(183 chat_alert_set, tab, 0)` (clears that tab's unread flash), then `redrawChat()`:
  `runScript(923 toplevel_chatbox_background)` (frame), `runScript(178 redraw_chat_buttons)` (highlight),
  `runScript(ScriptID.BUILD_CHATBOX /*216*/)` (content).
- **Close / collapse** — `setVarcIntValue(CHAT_VIEW, 1337)` + `redrawChat()`. Resizable only; fixed-mode
  no-op. `isChatClosed()` = `isResized() && CHAT_VIEW == 1337`.
- **Clear history** — drop every `MessageNode` of the tab's `messageTypes` from `getChatLineMap()` then
  `runScript(ScriptID.SPLITPM_CHANGED)`. Ported from core `ChatHistoryPlugin`.
- **Chat input mode** — `setVarcIntValue(VarClientID.CHATBOX_MODE /*945*/, mode)` +
  `runScript(ScriptID.CHAT_PROMPT_INIT /*223*/)`. Sets the channel only; inserts no text (not
  autotyping).
- State is read from game vars at press time (CHAT_VIEW, MESLAYERMODE), so mouse clicks stay in sync.
  `lastTab` is kept only for close→reopen.

**PR note:** scripts `178 / 923 / 183` are procs with no `ScriptID` constant, called by literal id.
They are benign redraws / client-varc writes (no packet, no interface switch). Explain each in the
resubmission, plus that the tab switch is a plain `setVarcIntValue(CHAT_VIEW)` per the Discord
discussion (riktenx's "may be ok" is reviewer-discretion, not a guaranteed pass).

## Why filters were removed (the big research finding)

Extensively researched (see the workflow transcripts). The conclusion:

- **Client-side filtering is subtractive-only.** The game's own filter runs first (`invoke 193` /
  `filtertest` in `ChatBuilder.rs2asm` gates the `chatFilterCheck` callback), so the callback can
  **hide** a line but never **reveal** one the game already hid. "On/Show all/Standard/Autochat"
  (reveal directions) can't work; only "Friends/None/Hide" (hide directions) do.
- **Setting the game's real filter is banned.** Public/Private/Trade go through the `chat_setfilter`
  engine opcode (a packet); Channel/Clan/Game are server varbits (928/929/26). Both = packet /
  server-persisted.
- **Local varbit override** (`client.setVarbit`, no packet, reverts on relog — legal, precedented by
  core `banktags` `TabInterface.java:357`) **could** drive the native filter (reveal+hide+label) — but
  **only** for the three varbit-backed tabs (Channel 928 / Clan 929 / Game 26). Public/Private/Trade are
  engine-internal (no varbit) → impossible legally.

So a faithful "all filters work like the game" was only reachable for 3 of 6 tabs. Rather than ship a
partial/confusing feature, **the filter feature was dropped** and clear-history kept.

## Reusable technical facts (for anyone revisiting filters or the chat UI)

- `CHAT_VIEW` = VarClientID **41** (active tab; **1337** = collapsed). `CHATBOX_MODE` = **945**.
- `chat_alert_set` = script **183**; writes `CHAT_ALERT_*` varcs (game=44, public=45, private=46,
  clan=47, trade=48, channel=438). tab 0 = clear all.
- Filter storage: channel=varbit **928**, clan=varbit **929**, game(spam)=varbit **26**;
  public/private/trade = engine opcode (`chat_getfilter_*` / `chat_setfilter`, packet).
  `CHAT_FILTER_PRIVATE=13674` is a **red herring** (not read by `filtertest`).
- `client.setVarbit()` writes only the local copy, **no packet** (`getServerVarbitValue` javadoc);
  diverges until the server resyncs (relog/hop).
- Native filter sublabel strings (cache enum 3844), if ever repainting the buttons: `"<br>On"`,
  `"<br><col=ffff00>Friends</col>"`, `"<br><col=ff0000>Off</col>"`, `"<br><col=00ffff>Hide</col>"`,
  `"<br><col=00afff>Autochat</col>"` (the leading `<br>` drops the label to the button's 2nd line).
  Sublabel components: `InterfaceID.Chatbox.CHAT_{PUBLIC,PRIVATE,FRIENDSCHAT,CLAN,TRADE}_FILTER`.

## Next steps

1. **Update docs to drop filters** — `spec.md`, `README.md`, `CHANGELOG.md`,
   `runelite-plugin.properties` (description/tags), `CLAUDE.md` still mention the filter feature.
2. **Final in-game smoke test** of the cleaned build (tabs / close / clear / mode).
3. **Commit** the rework (Conventional Commits; **no** Claude co-author trailer per CLAUDE.md).
4. **Version bump + resubmit** the hub PR (bump `commit=` in `runelite/plugin-hub`). In the PR reply,
   note the rejected primitive is fully gone, tab switch is a plain `setVarcIntValue(CHAT_VIEW)` +
   benign redraws, and clear/mode are packet-free core-ported patterns.

## Build / run / release (detail in CLAUDE.md)

- Build: `./rl build` (Docker JDK 11 wrapper). **Gotcha:** if a build fails with a gradle journal
  "Timeout waiting to lock" error, a stale build container is holding the lock — `docker stop` the
  `chat-tab-hotkeys/runelite-jdk:11` container and `find ~/.cache/runelite-gradle -name '*.lock'
  -delete`, then rebuild.
- Dev client: Bolt → Play (see `[[dev-mode-bolt]]`). Logs at `~/.runelite-chattabhotkeys/logs/client.log`.
- Release + hub PR steps are in CLAUDE.md under "Releasing".
