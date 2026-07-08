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

	@ConfigSection(
		name = "Tab hotkeys, close & clear",
		description = "One hotkey per chat tab, plus closing the chat and clearing a tab's history. Defaults are "
			+ "Ctrl+1–7; press a tab's hotkey again to close the chat. Bind function keys or modifier combos so "
			+ "binds don't fire while typing.",
		position = 0
	)
	String tabsSection = "tabsSection";

	@ConfigSection(
		name = "Chat input mode",
		description = "Set which channel your typed messages go to, like the right-click 'Set chat mode' on the All tab. "
			+ "Group only works while you are in a group ironman group.",
		position = 1
	)
	String modeSection = "modeSection";

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
	// Close chat + clear history
	// ------------------------------------------------------------------
	@ConfigItem(
		keyName = "closeOnRepeat",
		name = "Close on repeat",
		description = "Pressing the same tab's hotkey again closes the chat. "
			+ "When off, pressing again just re-shows the tab. "
			+ "Closing only works in resizable mode (no-op in fixed mode).",
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
		description = "Toggle the chat closed/open. Closed re-opens to the last tab. "
			+ "Only works in resizable mode (no-op in fixed mode).",
		position = 8,
		section = tabsSection
	)
	default Keybind closeChat()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "clearHistory",
		name = "Clear history",
		description = "Clear the currently-shown tab's history (no-op on the Game and All tabs).",
		position = 9,
		section = tabsSection
	)
	default Keybind clearHistory()
	{
		return Keybind.NOT_SET;
	}

	// ------------------------------------------------------------------
	// Chat input mode — which channel typed messages go to.
	// ------------------------------------------------------------------
	@ConfigItem(
		keyName = "setModePublic",
		name = "Set mode: Public",
		description = "Type into Public chat.",
		position = 0,
		section = modeSection
	)
	default Keybind setModePublic()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "setModeChannel",
		name = "Set mode: Channel",
		description = "Type into your friends chat channel.",
		position = 1,
		section = modeSection
	)
	default Keybind setModeChannel()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "setModeClan",
		name = "Set mode: Clan",
		description = "Type into your clan chat.",
		position = 2,
		section = modeSection
	)
	default Keybind setModeClan()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "setModeGuest",
		name = "Set mode: Guest clan",
		description = "Type into a guest clan chat.",
		position = 3,
		section = modeSection
	)
	default Keybind setModeGuest()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "setModeGroup",
		name = "Set mode: Group",
		description = "Type into group ironman chat. Only works while in a group.",
		position = 4,
		section = modeSection
	)
	default Keybind setModeGroup()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "cycleMode",
		name = "Cycle mode",
		description = "Cycle the chat input mode: Public, then Channel, then Clan, then Guest clan. "
			+ "Group is excluded from the cycle (use its own bind).",
		position = 5,
		section = modeSection
	)
	default Keybind cycleMode()
	{
		return Keybind.NOT_SET;
	}
}
