package com.chattabhotkeys;

import com.chattabhotkeys.ChatTabs.ChatTab;
import com.chattabhotkeys.ChatTabs.FilterOp;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.ScriptID;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Chat Tab Hotkeys",
	description = "Configurable hotkeys to switch, filter, and close chat tabs",
	tags = {"chat", "tab", "hotkey", "keybind", "filter"}
)
public class ChatTabHotkeysPlugin extends Plugin
{
	/** The left-click op on a chat tab button ("Switch tab" / collapse when re-selected). */
	private static final int SWITCH_TAB_OP = 1;

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

		register(config::closeChat, this::onCloseHotkey);

		register(config::showAll, () -> onFilterHotkey(FilterOp.SHOW_ALL));
		register(config::showFriends, () -> onFilterHotkey(FilterOp.SHOW_FRIENDS));
		register(config::showNone, () -> onFilterHotkey(FilterOp.SHOW_NONE));

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
	 * Tracks the last-shown tab from the game var, so mouse-driven tab switches keep
	 * {@link #lastTab} correct for the close→reopen path. When the chat collapses,
	 * CHAT_VIEW no longer maps to a tab, so we keep the previous value.
	 */
	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		if (event.getIndex() == VarClientID.CHAT_VIEW)
		{
			ChatTab tab = currentTab();
			if (tab != null)
			{
				lastTab = tab;
			}
		}
	}

	/**
	 * Registers a hotkey for {@code keybind} that runs {@code action} on the client thread,
	 * suppressed while the player is typing in a chat input.
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
					if (!isTyping())
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
	// Actions (client thread)
	// ----------------------------------------------------------------------

	private void onTabHotkey(ChatTab target)
	{
		// Re-pressing the shown tab with closeOnRepeat off should not re-fire (that would collapse it).
		if (!isChatClosed() && currentTab() == target && !config.closeOnRepeat())
		{
			return;
		}

		replayTabOp(target, SWITCH_TAB_OP);

		// Keep lastTab pointing at whatever is now shown, for close -> reopen.
		if (!isChatClosed())
		{
			ChatTab now = currentTab();
			if (now != null)
			{
				lastTab = now;
			}
		}
	}

	private void onCloseHotkey()
	{
		if (isChatClosed())
		{
			replayTabOp(lastTab, SWITCH_TAB_OP);
		}
		else
		{
			ChatTab cur = currentTab();
			if (cur != null)
			{
				lastTab = cur;
				replayTabOp(cur, SWITCH_TAB_OP);
			}
		}
	}

	private void onFilterHotkey(FilterOp op)
	{
		ChatTab tab = currentTab();
		if (tab == null || !tab.supportsFilters)
		{
			return;
		}

		Widget button = client.getWidget(tab.widgetId);
		if (button == null)
		{
			return;
		}

		String[] actions = button.getActions();
		if (actions == null)
		{
			return;
		}

		String want = op.label.toLowerCase(Locale.ROOT);
		for (int i = 0; i < actions.length; i++)
		{
			String action = actions[i];
			if (action == null)
			{
				continue;
			}
			// The op text can carry a coloured tab-name prefix (e.g. "<col=..>Trade:</col> Show none"),
			// so match by suffix — none of the three labels is a suffix of another.
			if (Text.removeTags(action).toLowerCase(Locale.ROOT).endsWith(want))
			{
				// Widget ops are 1-based; getActions()[i] is op i+1.
				replayWidgetOp(button, i + 1);
				return;
			}
		}
		// Tab doesn't offer this filter (e.g. Game/All): no-op.
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
		// repopulate on relog/world-hop. Acceptable for v1 (see spec.md → Known limitations).
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

	// ----------------------------------------------------------------------
	// Widget-op replay (ID-stable: run the tab button's own CS2 listener)
	// ----------------------------------------------------------------------

	private void replayTabOp(ChatTab tab, int op)
	{
		if (tab == null)
		{
			return;
		}
		Widget button = client.getWidget(tab.widgetId);
		if (button != null)
		{
			replayWidgetOp(button, op);
		}
	}

	private void replayWidgetOp(Widget widget, int op)
	{
		Object[] listener = widget.getOnOpListener();
		if (listener == null)
		{
			return;
		}
		client.createScriptEventBuilder(listener)
			.setOp(op)
			.setSource(widget)
			.build()
			.run();
	}

	// ----------------------------------------------------------------------
	// State reads (from game vars/widgets, so mouse clicks stay in sync)
	// ----------------------------------------------------------------------

	private ChatTab currentTab()
	{
		return ChatTab.byTabIndex(client.getVarcIntValue(VarClientID.CHAT_VIEW));
	}

	private boolean isChatClosed()
	{
		// Collapsing only exists in resizable mode; in fixed mode the chat is never "closed".
		if (!client.isResized())
		{
			return false;
		}
		Widget chatArea = client.getWidget(InterfaceID.Chatbox.CHATAREA);
		return chatArea == null || chatArea.isHidden();
	}

	private boolean isTyping()
	{
		return client.getVarcIntValue(VarClientID.MESLAYERMODE) != InputType.NONE.getType();
	}
}
