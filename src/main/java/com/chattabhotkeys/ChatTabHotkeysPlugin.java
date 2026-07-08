package com.chattabhotkeys;

import com.chattabhotkeys.ChatTabs.ChatMode;
import com.chattabhotkeys.ChatTabs.ChatTab;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.inject.Inject;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.ScriptID;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;

@PluginDescriptor(
	name = "Chat Tab Hotkeys",
	description = "Configurable hotkeys to switch and close chat tabs, clear history, and set chat mode",
	tags = {"chat", "tab", "hotkey", "keybind", "clear", "mode"}
)
public class ChatTabHotkeysPlugin extends Plugin
{
	/** Value {@code VarClientID.CHAT_VIEW} holds when the chat is collapsed (resizable mode). */
	private static final int CHAT_VIEW_CLOSED = 1337;

	/**
	 * Chat-UI redraw procs run after writing CHAT_VIEW, to repaint what {@code setVarcIntValue} alone
	 * doesn't (the game only reacts to the varc when its own scripts run). All are benign, packet-free
	 * redraws with no {@code ScriptID} constant, called by literal id; none switches an interface or
	 * replays a widget listener.
	 */
	private static final int REDRAW_CHAT_BUTTONS = 178;          // [proc,redraw_chat_buttons]: tab-button highlight
	private static final int TOPLEVEL_CHATBOX_BACKGROUND = 923;  // [proc,toplevel_chatbox_background]: shows/hides the resizable chat frame
	private static final int CHAT_ALERT_SET = 183;              // [proc,chat_alert_set](tab,state): clears a tab's unread/flash (client varcs 44-48/438 only)

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatTabHotkeysConfig config;

	private final List<HotkeyListener> hotkeys = new ArrayList<>();

	/** Which tab to re-open to when the close hotkey expands a collapsed chat. */
	private ChatTab lastTab = ChatTab.ALL;

	/**
	 * The mode the mode-cycle last tried to set, used to step past Group. Group (GIM) reverts the mode
	 * var when the player isn't in a group, so reading the var back would land us before Group again and
	 * trap the cycle; when our last step was Group we advance from Group's slot instead.
	 */
	private ChatMode lastCycledMode = null;

	@Override
	protected void startUp()
	{
		register(config::tabAll, () -> onTabHotkey(ChatTab.ALL));
		register(config::tabGame, () -> onTabHotkey(ChatTab.GAME));
		register(config::tabPublic, () -> onTabHotkey(ChatTab.PUBLIC));
		register(config::tabPrivate, () -> onTabHotkey(ChatTab.PRIVATE));
		register(config::tabChannel, () -> onTabHotkey(ChatTab.CHANNEL));
		register(config::tabClan, () -> onTabHotkey(ChatTab.CLAN));
		register(config::tabTrade, () -> onTabHotkey(ChatTab.TRADE));

		register(config::cycleTab, this::onCycleTabHotkey);

		register(config::closeChat, this::onCloseHotkey);
		register(config::clearHistory, this::onClearHistoryHotkey);

		register(config::setModePublic, () -> applyChatMode(ChatMode.PUBLIC));
		register(config::setModeChannel, () -> applyChatMode(ChatMode.CHANNEL));
		register(config::setModeClan, () -> applyChatMode(ChatMode.CLAN));
		register(config::setModeGuest, () -> applyChatMode(ChatMode.GUEST));
		register(config::setModeGroup, () -> applyChatMode(ChatMode.GROUP));
		register(config::cycleMode, this::onCycleModeHotkey);
	}

	@Override
	protected void shutDown()
	{
		for (HotkeyListener listener : hotkeys)
		{
			keyManager.unregisterKeyListener(listener);
		}
		hotkeys.clear();
	}

	@Provides
	ChatTabHotkeysConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChatTabHotkeysConfig.class);
	}

	/**
	 * Tracks the last-shown tab from the game var, so mouse-driven tab switches keep
	 * {@link #lastTab} correct for the close→reopen path. When the chat collapses,
	 * CHAT_VIEW no longer maps to a tab, so we keep the previous value.
	 */
	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		if (event.getIndex() == VarClientID.CHAT_VIEW)
		{
			rememberCurrentTab();
		}
	}

	/** Remembers the currently-shown tab (if any) for the close -> reopen path. */
	private void rememberCurrentTab()
	{
		ChatTab tab = currentTab();
		if (tab != null)
		{
			lastTab = tab;
		}
	}

	// ----------------------------------------------------------------------
	// Actions (client thread)
	// ----------------------------------------------------------------------

	private void onTabHotkey(ChatTab target)
	{
		if (!isChatClosed() && currentTab() == target)
		{
			// Target already shown. A repeat collapses the chat when closeOnRepeat is on (resizable
			// only; fixed mode can't collapse). Otherwise it's a visual no-op.
			if (config.closeOnRepeat() && client.isResized())
			{
				lastTab = target;
				collapseChat();
			}
			return;
		}

		switchTab(target);
		rememberCurrentTab();
	}

	/**
	 * Steps to the next selected chat tab (game order, wrapping). Reads the shown tab from the game var so
	 * mouse clicks stay in sync; when the chat is collapsed it resumes from {@link #lastTab}. If the shown
	 * tab isn't in the selection it starts at the first selected tab. No-ops when the selection is empty.
	 */
	private void onCycleTabHotkey()
	{
		List<ChatTab> tabs = cyclableTabs();
		if (tabs.isEmpty())
		{
			return;
		}

		ChatTab current = isChatClosed() ? lastTab : currentTab();
		int idx = current == null ? -1 : tabs.indexOf(current);
		ChatTab next = tabs.get((idx + 1) % tabs.size());
		switchTab(next);
		rememberCurrentTab();
	}

	private void onCloseHotkey()
	{
		if (isChatClosed())
		{
			switchTab(lastTab);
		}
		else if (client.isResized())
		{
			rememberCurrentTab();
			collapseChat();
		}
		// Fixed mode: chat can't collapse, so this is a no-op.
	}

	private void onClearHistoryHotkey()
	{
		ChatTab tab = currentTab();
		if (tab == null || !tab.supportsClear)
		{
			return;
		}

		// Native clear, ported from RuneLite's ChatHistoryPlugin: drop every message node
		// of the tab's chat types from the client's line buffers, then rebuild the chatbox.
		// Note: this doesn't fire the "Clear history" menu event, so if RuneLite's own Chat
		// History plugin is enabled with "retain chat history", a hotkey-cleared tab can
		// repopulate on relog/world-hop. Acceptable (see spec.md → Known limitations).
		boolean removed = false;
		for (ChatMessageType type : tab.messageTypes)
		{
			ChatLineBuffer buffer = client.getChatLineMap().get(type.getType());
			if (buffer == null)
			{
				continue;
			}

			for (MessageNode line : buffer.getLines().clone())
			{
				if (line != null)
				{
					buffer.removeMessageNode(line);
					removed = true;
				}
			}
		}

		if (removed)
		{
			client.runScript(ScriptID.SPLITPM_CHANGED);
		}
	}

	/**
	 * Sets which channel typed messages go to, the game's own way: write the mode var and rerun the
	 * chatbox-input build script so the input line redraws. Group (GIM) auto-resets to the current
	 * mode if the player isn't in a group. Only sets the target channel — never inserts message text.
	 */
	private void applyChatMode(ChatMode mode)
	{
		// Clear the cycle's Group-step tracker: a direct mode change (the Set-mode binds) resets where
		// the cycle resumes from. onCycleModeHotkey re-sets it right after calling this.
		lastCycledMode = null;
		client.setVarcIntValue(VarClientID.CHATBOX_MODE, mode.value);
		client.runScript(ScriptID.CHAT_PROMPT_INIT);
	}

	/**
	 * Steps to the next selected chat input mode (game order, wrapping). Normally advances from the mode
	 * the game var reports, so manual (right-click) changes stay in sync. The exception is Group: it
	 * reverts the var when the player isn't in a GIM group, so if our last step set Group and the var no
	 * longer shows it, we advance from Group's slot — stepping past it instead of retrying it forever.
	 * If the resolved current mode isn't selected, it starts at the first selected mode. No-ops when the
	 * selection is empty.
	 */
	private void onCycleModeHotkey()
	{
		List<ChatMode> modes = cyclableModes();
		if (modes.isEmpty())
		{
			return;
		}

		int currentValue = client.getVarcIntValue(VarClientID.CHATBOX_MODE);
		ChatMode current = lastCycledMode == ChatMode.GROUP && currentValue != ChatMode.GROUP.value
			? ChatMode.GROUP
			: ChatMode.byValue(currentValue);

		int idx = current == null ? -1 : modes.indexOf(current);
		ChatMode next = modes.get((idx + 1) % modes.size());
		applyChatMode(next);
		lastCycledMode = next;
	}

	/** The selected tabs for the tab cycle, always in game order (All..Trade) regardless of pick order. */
	private List<ChatTab> cyclableTabs()
	{
		Set<ChatTab> selected = config.cycleTabs();
		List<ChatTab> tabs = new ArrayList<>();
		for (ChatTab tab : ChatTab.values())
		{
			if (selected.contains(tab))
			{
				tabs.add(tab);
			}
		}
		return tabs;
	}

	/** The selected input modes for the mode cycle, always in game order (Public..Group). */
	private List<ChatMode> cyclableModes()
	{
		Set<ChatMode> selected = config.cycleModes();
		List<ChatMode> modes = new ArrayList<>();
		for (ChatMode mode : ChatMode.values())
		{
			if (selected.contains(mode))
			{
				modes.add(mode);
			}
		}
		return modes;
	}

	// ----------------------------------------------------------------------
	// Tab switch / close: write the CHAT_VIEW varc + a benign redraw. No packet,
	// no interface-switch clientscript — the switch is the varc; the procs only repaint.
	// ----------------------------------------------------------------------

	/** Switches to / opens {@code tab} by writing CHAT_VIEW (the game's active-tab varc) + a rebuild. */
	private void switchTab(ChatTab tab)
	{
		client.setVarcIntValue(VarClientID.CHAT_VIEW, tab.tabIndex);
		// Clear the switched-to tab's unread/flash, exactly as the native switch does (must run
		// before REDRAW_CHAT_BUTTONS, which reads the alert varc to pick the flash graphic).
		client.runScript(CHAT_ALERT_SET, tab.tabIndex, 0);
		redrawChat();
	}

	/**
	 * Collapses the chat by setting CHAT_VIEW to the collapse sentinel and rebuilding. Only meaningful
	 * in resizable mode; the caller must ensure {@code client.isResized()} (fixed mode never collapses).
	 */
	private void collapseChat()
	{
		client.setVarcIntValue(VarClientID.CHAT_VIEW, CHAT_VIEW_CLOSED);
		redrawChat();
	}

	/**
	 * Repaints the chat UI to reflect the CHAT_VIEW varc just written: frame collapse/expand + background
	 * ({@link #TOPLEVEL_CHATBOX_BACKGROUND}), the tab-button highlight ({@link #REDRAW_CHAT_BUTTONS}), then
	 * the chat content ({@code ScriptID.BUILD_CHATBOX}). All three are packet-free client-side redraws.
	 */
	private void redrawChat()
	{
		client.runScript(TOPLEVEL_CHATBOX_BACKGROUND);
		client.runScript(REDRAW_CHAT_BUTTONS);
		client.runScript(ScriptID.BUILD_CHATBOX);
	}

	private void register(Supplier<Keybind> keybind, Runnable action)
	{
		HotkeyListener listener = new HotkeyListener(keybind)
		{
			@Override
			public void hotkeyPressed()
			{
				clientThread.invoke(() ->
				{
					if (client.getGameState() == GameState.LOGGED_IN)
					{
						action.run();
					}
				});
			}
		};
		keyManager.registerKeyListener(listener);
		hotkeys.add(listener);
	}

	// ----------------------------------------------------------------------
	// State reads (from game vars, so mouse clicks stay in sync)
	// ----------------------------------------------------------------------

	private ChatTab currentTab()
	{
		return ChatTab.byTabIndex(client.getVarcIntValue(VarClientID.CHAT_VIEW));
	}

	private boolean isChatClosed()
	{
		// Collapsing only exists in resizable mode; in fixed mode the chat is never "closed".
		return client.isResized()
			&& client.getVarcIntValue(VarClientID.CHAT_VIEW) == CHAT_VIEW_CLOSED;
	}
}
