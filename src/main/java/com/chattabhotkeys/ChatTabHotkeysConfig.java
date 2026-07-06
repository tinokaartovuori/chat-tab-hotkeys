package com.chattabhotkeys;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
		name = "Tab hotkeys & close chat",
		description = "One hotkey per chat tab, plus closing the chat. Defaults are Ctrl+1–7; press a tab's hotkey again to close the chat. "
			+ "Bind function keys or modifier combos so binds don't fire while typing.",
		position = 0
	)
	String tabsSection = "tabsSection";

	@ConfigSection(
		name = "Chat filters & clear history",
		description = "Set the currently-shown tab's filter (like the right-click menu; no-ops on tabs that don't offer it) "
			+ "or clear its history.",
		position = 1
	)
	String filtersSection = "filtersSection";

	// ------------------------------------------------------------------
	// Tab hotkeys — game order: All, Game, Public, Private, Channel, Clan, Trade.
	// Default to Ctrl+1..7 (typing-safe: Ctrl combos never leak into chat).
	// ------------------------------------------------------------------
	@ConfigItem(keyName = "tabAll", name = "All", description = "Show the All tab.", position = 0, section = tabsSection)
	default Keybind tabAll()
	{
		return new Keybind(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(keyName = "tabGame", name = "Game", description = "Show the Game tab.", position = 1, section = tabsSection)
	default Keybind tabGame()
	{
		return new Keybind(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(keyName = "tabPublic", name = "Public", description = "Show the Public tab.", position = 2, section = tabsSection)
	default Keybind tabPublic()
	{
		return new Keybind(KeyEvent.VK_3, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(keyName = "tabPrivate", name = "Private", description = "Show the Private tab.", position = 3, section = tabsSection)
	default Keybind tabPrivate()
	{
		return new Keybind(KeyEvent.VK_4, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(keyName = "tabChannel", name = "Channel", description = "Show the Channel tab.", position = 4, section = tabsSection)
	default Keybind tabChannel()
	{
		return new Keybind(KeyEvent.VK_5, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(keyName = "tabClan", name = "Clan", description = "Show the Clan tab.", position = 5, section = tabsSection)
	default Keybind tabClan()
	{
		return new Keybind(KeyEvent.VK_6, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(keyName = "tabTrade", name = "Trade", description = "Show the Trade tab.", position = 6, section = tabsSection)
	default Keybind tabTrade()
	{
		return new Keybind(KeyEvent.VK_7, InputEvent.CTRL_DOWN_MASK);
	}

	// ------------------------------------------------------------------
	// Close chat
	// ------------------------------------------------------------------
	@ConfigItem(
		keyName = "closeOnRepeat",
		name = "Close on repeat",
		description = "Pressing the same tab's hotkey again closes the chat. "
			+ "When off, pressing again just re-shows the tab.",
		position = 7,
		section = tabsSection
	)
	default boolean closeOnRepeat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "closeChat",
		name = "Close chat",
		description = "Toggle the chat closed/open. Closed re-opens to the last tab.",
		position = 8,
		section = tabsSection
	)
	default Keybind closeChat()
	{
		return Keybind.NOT_SET;
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
		position = 3,
		section = filtersSection
	)
	default Keybind clearHistory()
	{
		return Keybind.NOT_SET;
	}
}
