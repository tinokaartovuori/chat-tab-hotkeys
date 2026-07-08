package com.chattabhotkeys;

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
	}

	/**
	 * Chat input channel, i.e. which channel typed messages go to (the game's right-click
	 * "Set chat mode" on the All tab). Set by writing {@code VarClientID.CHATBOX_MODE} to
	 * {@link #value} and rerunning the chatbox-input build script. Group (GIM) auto-resets
	 * to the current mode if the player is not in a group.
	 */
	enum ChatMode
	{
		PUBLIC(0, true),
		CHANNEL(1, true),
		CLAN(2, true),
		GUEST(3, true),
		// Excluded from the cycle: the game auto-resets it to the current mode when not in a
		// group ironman group, which would trap the cycle. Still available as its own bind.
		GROUP(4, false);

		final int value;
		final boolean cyclable;

		ChatMode(int value, boolean cyclable)
		{
			this.value = value;
			this.cyclable = cyclable;
		}
	}
}
