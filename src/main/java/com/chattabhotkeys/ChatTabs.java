package com.chattabhotkeys;

import java.util.Locale;
import net.runelite.api.ChatMessageType;

/**
 * Static data for the seven chat tabs.
 *
 * The {@link ChatTab#tabIndex} matches the value the game stores in {@code VarClientID.CHAT_VIEW}
 * while that tab is shown; it is the value written to switch to that tab. The per-tab
 * {@link ChatTab#messageTypes} are ported from RuneLite's own
 * {@code net.runelite.client.plugins.chathistory.ChatboxTab} and drive native clear-history.
 */
final class ChatTabs
{
	private ChatTabs()
	{
	}

	/**
	 * {@code supportsClear} marks the channel-type tabs whose history can be cleared; Game and All
	 * cannot (that action no-ops there).
	 */
	enum ChatTab
	{
		ALL(0, false),
		GAME(1, false),
		PUBLIC(2, true,
			ChatMessageType.PUBLICCHAT, ChatMessageType.AUTOTYPER,
			ChatMessageType.MODCHAT, ChatMessageType.MODAUTOTYPER),
		PRIVATE(3, true,
			ChatMessageType.PRIVATECHAT, ChatMessageType.PRIVATECHATOUT,
			ChatMessageType.MODPRIVATECHAT, ChatMessageType.LOGINLOGOUTNOTIFICATION),
		CHANNEL(4, true,
			ChatMessageType.FRIENDSCHATNOTIFICATION, ChatMessageType.FRIENDSCHAT,
			ChatMessageType.CHALREQ_FRIENDSCHAT),
		CLAN(5, true,
			ChatMessageType.CLAN_CHAT, ChatMessageType.CLAN_MESSAGE,
			ChatMessageType.CLAN_GUEST_CHAT, ChatMessageType.CLAN_GUEST_MESSAGE),
		TRADE(6, true,
			ChatMessageType.TRADE_SENT, ChatMessageType.TRADEREQ, ChatMessageType.TRADE,
			ChatMessageType.CHALREQ_TRADE, ChatMessageType.CLAN_GIM_CHAT, ChatMessageType.CLAN_GIM_MESSAGE);

		final int tabIndex;
		final boolean supportsClear;
		/** Empty for tabs without clear-history. */
		final ChatMessageType[] messageTypes;

		ChatTab(int tabIndex, boolean supportsClear, ChatMessageType... messageTypes)
		{
			this.tabIndex = tabIndex;
			this.supportsClear = supportsClear;
			this.messageTypes = messageTypes;
		}

		/** The tab whose {@link #tabIndex} equals {@code index}, or null if none (e.g. chat closed). */
		static ChatTab byTabIndex(int index)
		{
			for (ChatTab tab : values())
			{
				if (tab.tabIndex == index)
				{
					return tab;
				}
			}
			return null;
		}

		/** Title-cased label for the "Tabs to cycle" multi-select list (e.g. PUBLIC -> "Public"). */
		@Override
		public String toString()
		{
			String n = name();
			return n.charAt(0) + n.substring(1).toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * Chat input channel, i.e. which channel typed messages go to (the game's right-click
	 * "Set chat mode" on the All tab). Set by writing {@code VarClientID.CHATBOX_MODE} to
	 * {@link #value} and rerunning the chatbox-input build script. Group (GIM) auto-resets
	 * to the current mode if the player is not in a group, so it is left out of the default
	 * "Modes to cycle" selection.
	 */
	enum ChatMode
	{
		PUBLIC(0, "Public"),
		CHANNEL(1, "Channel"),
		CLAN(2, "Clan"),
		GUEST(3, "Guest clan"),
		GROUP(4, "Group");

		final int value;
		private final String label;

		ChatMode(int value, String label)
		{
			this.value = value;
			this.label = label;
		}

		/** The mode whose {@link #value} equals {@code value}, or null if none. */
		static ChatMode byValue(int value)
		{
			for (ChatMode mode : values())
			{
				if (mode.value == value)
				{
					return mode;
				}
			}
			return null;
		}

		/** Label for the "Modes to cycle" multi-select list. */
		@Override
		public String toString()
		{
			return label;
		}
	}
}
