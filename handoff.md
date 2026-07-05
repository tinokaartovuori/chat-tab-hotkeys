# Handoff: "Chat Tab Hotkeys" — RuneLite Plugin

## What this is
A background/spec doc for building a small RuneLite Plugin Hub plugin. Read this first, then scaffold the project and implement to the acceptance criteria at the bottom. Still a small plugin — everything acts on chat tabs the player can already control by mouse; we're just adding hotkeys.

## Name
**Recommended:** `Chat Tab Hotkeys` (repo: `chat-tab-hotkeys`).
It's plain and searchable, which is the RuneLite convention. Alternatives if you prefer: `Chat Hotkeys` (broader, also covers the close-chat and filter binds) or `Chat Tab Keybinds`. Avoid "Quick Chat" — that's an unrelated RS3 feature name and will confuse users.

---

## Feature spec (the whole plugin)
Configurable hotkeys for navigating and filtering the chat. Four groups of actions:

**1. Tab hotkeys** — one per chat tab. Tabs in game order: **All, Game, Public, Private, Channel, Clan, Trade.**
- Tab hotkey → show that tab. If chat is closed (collapsed), open it and show that tab.
- Same tab hotkey twice → close the chat. Default ON; governed by `closeOnRepeat`. With it off, pressing again just re-shows the tab.

**2. Close chat** — one hotkey that toggles the chat closed/open. Closed → open (to last tab). Open → close.

**3. Filter hotkeys** — act on the **currently-viewed tab**: `Show all`, `Show friends`, `Show none`. These set that tab's filter, exactly like the right-click menu.

**4. Clear history** — one hotkey that clears the **currently-viewed tab's** history, like the right-click "Clear history" option.

For groups 3 and 4: these options only exist on the channel-type tabs (Public, Private, Channel, Clan, Trade). The Game tab has On/Filtered instead, and the All tab differs. If the active tab doesn't support the pressed action, **no-op silently** — don't error, don't guess a target.

Nothing beyond this. No auto-reply, no input-channel switching, no overlays.

---

## Config (settings panel) — use @ConfigSection for each group
**Section: Tab hotkeys**
- 7 × `Keybind` — All, Game, Public, Private, Channel, Clan, Trade.

**Section: Close chat**
- 1 × `Keybind` — "Close chat".
- 1 × `boolean closeOnRepeat` — default `true`. "Pressing the same tab's hotkey again closes the chat."

**Section: Chat filters (current tab)**
- 3 × `Keybind` — "Show all", "Show friends", "Show none". Description should state these apply to the currently-shown tab.

**Section: Clear history (current tab)**
- 1 × `Keybind` — "Clear history". Applies to the currently-shown tab.
- Optional `boolean confirmClearHistory` — default `false` (mirrors the game, which clears immediately). Consider offering it since a mis-pressed hotkey is easier to hit than a right-click menu, but keep it off by default. If you skip it in v1, note it under future ideas.

**All keybinds default to `Keybind.NOT_SET`.** The user opts in. This is also the main defense against a bound key firing while typing (see gotchas). Kept the sections split (filters vs clear history) because clear history is destructive and shouldn't sit next to the harmless filter toggles.

---

## Core logic
State the plugin reads at press-time (read from game vars, not just internal tracking, so mouse clicks stay in sync):
- `currentTab` — which tab is displayed.
- `chatClosed` — whether the chatbox is collapsed/hidden.

On a **tab** hotkey for `targetTab`:
```
if (chatClosed):            open chat; show targetTab
else if (currentTab == targetTab && closeOnRepeat):  close chat
else:                       show targetTab
```

On the **close-chat** hotkey:
```
if (chatClosed): open chat (to last tab)
else:            close chat
```

On a **filter** hotkey (Show all/friends/none) or **clear history**:
```
resolve the active tab's button widget
if that tab supports the action:  invoke the corresponding menu op on it
else:                             no-op
(clear history: if confirmClearHistory, gate behind a confirm first)
```

Use the **Var Inspector** (dev tools) to find the client var holding the current chat tab and the one holding the collapsed state — watch which var changes as you click tabs / collapse the chat, then read via `client.getVarcIntValue(...)` / `client.getVarbitValue(...)`. Keep an internal "last tab" as a fallback for close→reopen.

---

## RuneLite implementation notes

### Scaffolding
- Base it on the official example plugin template (generate a repo from it, or clone `runelite/example-plugin` and rename). Standard Gradle Java plugin project.
- Also clone `runelite/runelite` (core client) into the workspace **for reference only** — grepping core plugins is the fastest way to confirm correct API usage and to find the chatbox widget/script/menu details below. Don't depend on it beyond the normal `runeLiteVersion`.
- `build.gradle`: `runeLiteVersion = 'latest.release'`, Java 11 language level, **no third-party dependencies** (this plugin needs none, and new deps massively slow Plugin Hub review).
- Standard pieces: `Plugin` subclass with `@PluginDescriptor`; `Config` interface; injected `Client`, `ClientThread`, `KeyManager`.

### Hotkeys
- Use `net.runelite.client.input.HotkeyListener` (one per bind, constructed with a `Supplier<Keybind>` pointing at the config getter). Register with `keyManager.registerKeyListener(...)` in `startUp()`, unregister in `shutDown()`.
- All client-touching work runs on the client thread: `clientThread.invoke(() -> ...)`. Never call `runScript` / menu actions from the key event thread.

### Switching tabs (left-click behavior)
Replicate the tab button's click by running the widget's own CS2 listener — ID-stable, survives Jagex reshuffling script IDs:
```java
Widget tab = client.getWidget(CHATBOX_GROUP, tabChildId); // group 162 = chatbox; verify child IDs in inspector
if (tab != null) {
    Object[] listener = tab.getOnOpListener();      // fall back to getOnClickListener() if onOp is null
    if (listener != null) {
        client.createScriptEvent(listener).setSource(tab).run();
    }
}
```
You only need the **widget IDs of the 7 tab buttons** from the inspector. Fallback: `client.runScript(scriptId, tabIndex)` with the inspector-shown script + arg (less resilient — prefer the listener route).

### Filter options + clear history (right-click menu ops)
These are the tab button's right-click menu entries, not its left-click op — so replay a menu action rather than the onOp listener. Most reliable method:
1. **Discovery:** temporarily log `MenuOptionClicked` events. Right-click a tab and pick each of Show all / Show friends / Show none / Clear history, recording `getMenuAction()`, `getId()` (the op identifier), `getParam0()`, `getParam1()` (packed widget id), and `getMenuOption()`. The op identifier for each filter should be consistent across the channel-type tabs.
2. **Trigger** on the client thread: `client.menuAction(param0, param1, menuAction, id, -1, option, target)`, where `param1` is the **packed widget id of the currently-active tab's button** (compute it for whichever tab is showing) and `id`/`option` select the filter or clear action.
3. Before firing, check the active tab actually offers that option (Game/All don't offer Show all/friends/none) and no-op otherwise.

### Opening / closing (collapsing) the chat
"Close the chat" = collapse the chatbox (meaningful in **resizable** mode). Same technique as tab switching: find the collapse/expand button widget in the inspector and run its `onOp`/`onClick` listener via `createScriptEvent`. Read collapsed state from the var you identified.

### Don't fire while the user is typing
A bound plain letter would otherwise trigger mid-message. Mitigate: defaults are unbound (already covered) + suggest function keys in descriptions + suppress hotkey handling when the chatbox is in text-entry mode (find that input var in the Var Inspector). Should-have, not just polish.

### Fixed vs resizable mode
Collapsing only applies in resizable mode. In fixed mode, degrade gracefully: tab switching and filter/clear actions still work; the close action (and same-tab-twice-closes) should no-op rather than error. Note this in the plugin description.

---

## Discovery checklist (do this first, in-game)
Launch with dev tools: `./gradlew run --args="--developer-mode"`. With the **Widget Inspector** and **Var Inspector** open:
- [ ] Confirm the chatbox interface group id (expected 162) and record the **child id of each of the 7 tab buttons**.
- [ ] For one tab button, confirm which listener is populated (`onOp` vs `onClick`); note the fallback script id + arg it fires.
- [ ] Right-click a channel-type tab and, via a temporary `MenuOptionClicked` log, record the menu action / op id / params / option text for **Show all, Show friends, Show none, Clear history**.
- [ ] Record the **collapse/expand button** widget id and its listener.
- [ ] Find the vars for **current tab**, **collapsed state**, and **text-entry mode** (watch them change).

Put all of these in one `Constants`/`ChatTabs` holder with comments citing where each came from, so future game updates are easy to re-verify.

---

## Build & run
- `./gradlew run` to launch RuneLite with the plugin loaded; iterate.
- Verify all binds, `closeOnRepeat` on/off, filters + clear-history on each channel-type tab, no-op on Game/All, manual-click sync, and both display modes.

## Plugin Hub submission (later)
- Host the plugin in its own public GitHub repo (BSD 2-Clause).
- Fork `runelite/plugin-hub`, add a file under `plugins/` named after the plugin with `repository=<repo>.git` and `commit=<40-char hash>`, open a PR, get CI green.
- Compliance is a non-issue: this only automates UI actions (switching/filtering/closing tabs) the player can already do by hand — same category as Chat Filter / Chat History / Toggle Chat. No gameplay automation, no unfair info.

---

## Optional future ideas (NOT v1 — listed so they aren't reinvented)
- Per-tab filter binds (bind "set Public to friends" directly) — powerful but 28 combos; only if requested.
- A single "cycle tabs" bind (next/previous).
- A modifier-based scheme (hold Alt + 1–7) as an alternative to dedicated keys.
- Optionally set the chat **input** channel when switching (off by default; changes game behavior).
- `confirmClearHistory` guard, if not done in v1.

## Definition of done (v1)
- [ ] 7 tab binds + 1 close bind + 3 filter binds + 1 clear-history bind, all unbound by default, each working.
- [ ] Same-tab-twice closes chat when `closeOnRepeat` on; no-op re-show when off.
- [ ] Close bind toggles closed/open; reopens to last tab.
- [ ] Tab bind while chat closed opens it on that tab.
- [ ] Show all/friends/none and Clear history apply to the active tab; no-op on tabs that don't offer them (Game/All).
- [ ] State stays correct after manual mouse clicks (reads game vars).
- [ ] Doesn't fire while typing in chat.
- [ ] No-ops gracefully in fixed mode; no exceptions.
- [ ] No third-party dependencies; `runeLiteVersion = 'latest.release'`.
