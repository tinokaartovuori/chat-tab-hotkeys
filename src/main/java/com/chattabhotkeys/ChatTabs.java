package com.chattabhotkeys;

import net.runelite.api.ChatMessageType;
import net.runelite.api.gameval.InterfaceID;

/**
 * Static data for the seven chat tabs. Every id here is a named RuneLite
 * {@code gameval} constant, so nothing needs in-game "Var Inspector" discovery
 * and the values survive Jagex re-shuffling raw widget/script ids.
 *
 * The tab widgets live under {@link InterfaceID.Chatbox} (group 162). The
 * {@link ChatTab#tabIndex} matches the value the game stores in
 * {@code VarClientID.CHAT_VIEW} while that tab is shown. The per-tab
 * {@link ChatTab#messageTypes} are ported from RuneLite's own
 * {@code net.runelite.client.plugins.chathistory.ChatboxTab} and drive native
 * clear-history.
 */
final class ChatTabs
{
	private ChatTabs()
	{
	}

	/**
	 * The seven chat tabs in game order. {@code supportsFilters}/{@code supportsClear}
	 * mark the channel-type tabs that offer Show all/friends/none and clear-history;
	 * Game and All do not (those actions no-op there).
	 */
	enum ChatTab
	{
		ALL(InterfaceID.Chatbox.CHAT_ALL, 0, false, false),
		GAME(InterfaceID.Chatbox.CHAT_GAME, 1, false, false),
		PUBLIC(InterfaceID.Chatbox.CHAT_PUBLIC, 2, true, true,
			ChatMessageType.PUBLICCHAT, ChatMessageType.AUTOTYPER,
			ChatMessageType.MODCHAT, ChatMessageType.MODAUTOTYPER),
		PRIVATE(InterfaceID.Chatbox.CHAT_PRIVATE, 3, true, true,
			ChatMessageType.PRIVATECHAT, ChatMessageType.PRIVATECHATOUT,
			ChatMessageType.MODPRIVATECHAT, ChatMessageType.LOGINLOGOUTNOTIFICATION),
		CHANNEL(InterfaceID.Chatbox.CHAT_FRIENDSCHAT, 4, true, true,
			ChatMessageType.FRIENDSCHATNOTIFICATION, ChatMessageType.FRIENDSCHAT,
			ChatMessageType.CHALREQ_FRIENDSCHAT),
		CLAN(InterfaceID.Chatbox.CHAT_CLAN, 5, true, true,
			ChatMessageType.CLAN_CHAT, ChatMessageType.CLAN_MESSAGE,
			ChatMessageType.CLAN_GUEST_CHAT, ChatMessageType.CLAN_GUEST_MESSAGE),
		TRADE(InterfaceID.Chatbox.CHAT_TRADE, 6, true, true,
			ChatMessageType.TRADE_SENT, ChatMessageType.TRADEREQ, ChatMessageType.TRADE,
			ChatMessageType.CHALREQ_TRADE, ChatMessageType.CLAN_GIM_CHAT, ChatMessageType.CLAN_GIM_MESSAGE);

		/** Packed component id of the tab button (from {@link InterfaceID.Chatbox}). */
		final int widgetId;
		/** Value held by {@code VarClientID.CHAT_VIEW} when this tab is shown. */
		final int tabIndex;
		final boolean supportsFilters;
		final boolean supportsClear;
		/** Chat line types cleared for this tab (empty for tabs without clear-history). */
		final ChatMessageType[] messageTypes;

		ChatTab(int widgetId, int tabIndex, boolean supportsFilters, boolean supportsClear,
			ChatMessageType... messageTypes)
		{
			this.widgetId = widgetId;
			this.tabIndex = tabIndex;
			this.supportsFilters = supportsFilters;
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
	 * A tab filter menu op. {@link #label} is matched against the tab button's
	 * {@code Widget.getActions()} at runtime to find the op index to replay — so we
	 * never hardcode a numeric op id.
	 */
	enum FilterOp
	{
		SHOW_ALL("Show all"),
		SHOW_FRIENDS("Show friends"),
		SHOW_NONE("Show none");

		final String label;

		FilterOp(String label)
		{
			this.label = label;
		}

		/** Next filter in the cycle: all -> friends -> none -> all. */
		FilterOp next()
		{
			FilterOp[] values = values();
			return values[(ordinal() + 1) % values.length];
		}
	}
}
