package com.chattabhotkeys;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup(ChatTabHotkeysConfig.GROUP)
public interface ChatTabHotkeysConfig extends Config
{
	String GROUP = "chattabhotkeys";

	// ------------------------------------------------------------------
	// Sections
	// ------------------------------------------------------------------
	@ConfigSection(
		name = "Tab hotkeys",
		description = "One hotkey per chat tab. Pressing a tab's hotkey shows it; "
			+ "pressing the same one again closes the chat (see 'Close on repeat').",
		position = 0
	)
	String tabsSection = "tabsSection";

	@ConfigSection(
		name = "Close chat",
		description = "Collapse/expand the chatbox. Only meaningful in resizable mode.",
		position = 1
	)
	String closeSection = "closeSection";

	@ConfigSection(
		name = "Chat filters (current tab)",
		description = "Set the filter of the currently-shown tab, like the right-click menu. "
			+ "No-ops on tabs that don't offer the option (e.g. Game / All).",
		position = 2
	)
	String filtersSection = "filtersSection";

	@ConfigSection(
		name = "Clear history (current tab)",
		description = "Clear the currently-shown tab's history. Kept separate because it is destructive.",
		position = 3
	)
	String clearSection = "clearSection";

	// ------------------------------------------------------------------
	// Tab hotkeys — game order: All, Game, Public, Private, Channel, Clan, Trade.
	// Tip: bind function keys; plain letters fire while typing in chat.
	// ------------------------------------------------------------------
	@ConfigItem(keyName = "tabAll", name = "All", description = "Show the All tab.", position = 0, section = tabsSection)
	default Keybind tabAll()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "tabGame", name = "Game", description = "Show the Game tab.", position = 1, section = tabsSection)
	default Keybind tabGame()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "tabPublic", name = "Public", description = "Show the Public tab.", position = 2, section = tabsSection)
	default Keybind tabPublic()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "tabPrivate", name = "Private", description = "Show the Private tab.", position = 3, section = tabsSection)
	default Keybind tabPrivate()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "tabChannel", name = "Channel", description = "Show the Channel tab.", position = 4, section = tabsSection)
	default Keybind tabChannel()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "tabClan", name = "Clan", description = "Show the Clan tab.", position = 5, section = tabsSection)
	default Keybind tabClan()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "tabTrade", name = "Trade", description = "Show the Trade tab.", position = 6, section = tabsSection)
	default Keybind tabTrade()
	{
		return Keybind.NOT_SET;
	}

	// ------------------------------------------------------------------
	// Close chat
	// ------------------------------------------------------------------
	@ConfigItem(
		keyName = "closeChat",
		name = "Close chat",
		description = "Toggle the chat closed/open. Closed re-opens to the last tab.",
		position = 0,
		section = closeSection
	)
	default Keybind closeChat()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "closeOnRepeat",
		name = "Close on repeat",
		description = "Pressing the same tab's hotkey again closes the chat. "
			+ "When off, pressing again just re-shows the tab.",
		position = 1,
		section = closeSection
	)
	default boolean closeOnRepeat()
	{
		return true;
	}

	// ------------------------------------------------------------------
	// Chat filters (current tab)
	// ------------------------------------------------------------------
	@ConfigItem(
		keyName = "showAll",
		name = "Show all",
		description = "Set the currently-shown tab's filter to 'Show all'.",
		position = 0,
		section = filtersSection
	)
	default Keybind showAll()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "showFriends",
		name = "Show friends",
		description = "Set the currently-shown tab's filter to 'Show friends'.",
		position = 1,
		section = filtersSection
	)
	default Keybind showFriends()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "showNone",
		name = "Show none",
		description = "Set the currently-shown tab's filter to 'Show none'.",
		position = 2,
		section = filtersSection
	)
	default Keybind showNone()
	{
		return Keybind.NOT_SET;
	}

	// ------------------------------------------------------------------
	// Clear history (current tab)
	// ------------------------------------------------------------------
	@ConfigItem(
		keyName = "clearHistory",
		name = "Clear history",
		description = "Clear the currently-shown tab's history.",
		position = 0,
		section = clearSection
	)
	default Keybind clearHistory()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "confirmClearHistory",
		name = "Confirm first",
		description = "Ask for confirmation before clearing history (a mis-pressed hotkey is easy to hit).",
		position = 1,
		section = clearSection
	)
	default boolean confirmClearHistory()
	{
		return false;
	}
}
