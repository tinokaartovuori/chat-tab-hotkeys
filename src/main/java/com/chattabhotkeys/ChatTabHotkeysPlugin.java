package com.chattabhotkeys;

import com.chattabhotkeys.ChatTabs.ChatTab;
import com.chattabhotkeys.ChatTabs.FilterOp;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Chat Tab Hotkeys",
	description = "Configurable hotkeys to switch, filter, and close chat tabs",
	tags = {"chat", "tab", "hotkey", "keybind", "filter"}
)
public class ChatTabHotkeysPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatTabHotkeysConfig config;

	private final List<HotkeyListener> hotkeys = new ArrayList<>();

	/** Fallback for close -> reopen when the game var can't tell us the last tab. */
	private ChatTab lastTab = ChatTab.ALL;

	@Override
	protected void startUp()
	{
		// Tab hotkeys, in game order.
		register(config::tabAll, () -> onTabHotkey(ChatTab.ALL));
		register(config::tabGame, () -> onTabHotkey(ChatTab.GAME));
		register(config::tabPublic, () -> onTabHotkey(ChatTab.PUBLIC));
		register(config::tabPrivate, () -> onTabHotkey(ChatTab.PRIVATE));
		register(config::tabChannel, () -> onTabHotkey(ChatTab.CHANNEL));
		register(config::tabClan, () -> onTabHotkey(ChatTab.CLAN));
		register(config::tabTrade, () -> onTabHotkey(ChatTab.TRADE));

		// Close chat.
		register(config::closeChat, this::onCloseHotkey);

		// Filters (current tab).
		register(config::showAll, () -> onFilterHotkey(FilterOp.SHOW_ALL));
		register(config::showFriends, () -> onFilterHotkey(FilterOp.SHOW_FRIENDS));
		register(config::showNone, () -> onFilterHotkey(FilterOp.SHOW_NONE));

		// Clear history (current tab).
		register(config::clearHistory, this::onClearHistoryHotkey);
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
	 * Builds a {@link HotkeyListener} for {@code keybind}, registers it, and hands the
	 * key press off to {@code action} on the client thread. Hotkeys are suppressed while
	 * the player is typing in chat.
	 */
	private void register(Supplier<Keybind> keybind, Runnable action)
	{
		HotkeyListener listener = new HotkeyListener(keybind)
		{
			@Override
			public void hotkeyPressed()
			{
				clientThread.invoke(() ->
				{
					if (isTypingInChat())
					{
						return;
					}
					action.run();
				});
			}
		};
		keyManager.registerKeyListener(listener);
		hotkeys.add(listener);
	}

	// ----------------------------------------------------------------------
	// Core logic (runs on the client thread)
	// ----------------------------------------------------------------------

	private void onTabHotkey(ChatTab target)
	{
		if (isChatClosed())
		{
			openChat();
			showTab(target);
		}
		else if (currentTab() == target && config.closeOnRepeat())
		{
			closeChat();
		}
		else
		{
			showTab(target);
		}
	}

	private void onCloseHotkey()
	{
		if (isChatClosed())
		{
			openChat();
			showTab(lastTab);
		}
		else
		{
			closeChat();
		}
	}

	private void onFilterHotkey(FilterOp op)
	{
		ChatTab tab = currentTab();
		if (tab == null || !tab.supportsFilters)
		{
			// Game / All / unknown tab: silently no-op.
			return;
		}
		invokeTabMenuOp(tab, op);
	}

	private void onClearHistoryHotkey()
	{
		ChatTab tab = currentTab();
		if (tab == null || !tab.supportsFilters)
		{
			return;
		}
		if (config.confirmClearHistory())
		{
			// TODO(discovery): show a confirmation (e.g. chatboxPanelManager confirm) before clearing.
			// Until wired up, honour the setting by refusing to clear silently.
			log.debug("Clear history requested but confirmation is not yet implemented; skipping.");
			return;
		}
		invokeTabMenuOp(tab, FilterOp.CLEAR_HISTORY);
	}

	// ----------------------------------------------------------------------
	// Actions — replay the widget's own behaviour (ID-stable across script reshuffles)
	// ----------------------------------------------------------------------

	private void showTab(ChatTab tab)
	{
		if (tab != null && runWidgetOp(tab.buttonChild))
		{
			lastTab = tab;
		}
	}

	private void openChat()
	{
		// Opening is the same collapse/expand button as closing.
		runWidgetOp(ChatTabs.COLLAPSE_BUTTON_CHILD);
	}

	private void closeChat()
	{
		runWidgetOp(ChatTabs.COLLAPSE_BUTTON_CHILD);
	}

	/**
	 * Replays a chatbox widget's left-click by running its own CS2 listener.
	 * Returns false (no-op) when the child id hasn't been discovered yet or the widget is absent.
	 */
	private boolean runWidgetOp(int child)
	{
		if (ChatTabs.CHATBOX_GROUP == ChatTabs.UNSET || child == ChatTabs.UNSET)
		{
			return false;
		}
		Widget widget = client.getWidget(ChatTabs.CHATBOX_GROUP, child);
		if (widget == null)
		{
			return false;
		}
		Object[] listener = widget.getOnOpListener();
		// Note: Widget exposes no getOnClickListener() getter, only getOnOpListener().
		// If the tab buttons turn out to use onClick, discover the script id/arg and
		// fire via client.runScript(...) instead.
		if (listener == null)
		{
			return false;
		}
		client.createScriptEventBuilder(listener).setSource(widget).build().run();
		return true;
	}

	/**
	 * Replays a tab button's right-click menu op (filter / clear history) via menuAction,
	 * targeting the packed widget id of {@code tab}'s button.
	 */
	private void invokeTabMenuOp(ChatTab tab, FilterOp op)
	{
		if (ChatTabs.CHATBOX_GROUP == ChatTabs.UNSET
			|| tab.buttonChild == ChatTabs.UNSET
			|| op.opId == ChatTabs.UNSET)
		{
			return;
		}
		int param1 = ChatTabs.packComponentId(ChatTabs.CHATBOX_GROUP, tab.buttonChild);
		// TODO(discovery): confirm the MenuAction from the MenuOptionClicked log (expected CC_OP).
		client.menuAction(-1, param1, MenuAction.CC_OP, op.opId, -1, op.optionText, "");
	}

	// ----------------------------------------------------------------------
	// State reads (from game vars, so mouse clicks stay in sync)
	// ----------------------------------------------------------------------

	private ChatTab currentTab()
	{
		if (ChatTabs.VAR_CURRENT_TAB == ChatTabs.UNSET)
		{
			return null;
		}
		int value = client.getVarcIntValue(ChatTabs.VAR_CURRENT_TAB);
		for (ChatTab tab : ChatTab.values())
		{
			if (tab.varValue == value)
			{
				return tab;
			}
		}
		return null;
	}

	private boolean isChatClosed()
	{
		if (ChatTabs.VAR_CHAT_CLOSED == ChatTabs.UNSET)
		{
			return false;
		}
		// TODO(discovery): confirm whether this is a varbit or varc int and adjust the getter.
		return client.getVarbitValue(ChatTabs.VAR_CHAT_CLOSED) != 0;
	}

	private boolean isTypingInChat()
	{
		if (ChatTabs.VAR_TEXT_ENTRY == ChatTabs.UNSET)
		{
			return false;
		}
		return client.getVarcIntValue(ChatTabs.VAR_TEXT_ENTRY) != 0;
	}
}
