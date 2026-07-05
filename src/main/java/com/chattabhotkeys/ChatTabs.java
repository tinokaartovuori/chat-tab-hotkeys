package com.chattabhotkeys;

/**
 * Single holder for every value that must be discovered in-game with the Widget
 * Inspector / Var Inspector (launch via {@code ./gradlew run}, which passes
 * {@code --developer-mode}). Cite the source of each value in its comment so a
 * future game update is cheap to re-verify.
 *
 * Everything below is {@code UNSET} (-1) until confirmed in-game. The plugin
 * treats -1 as "not discovered yet" and no-ops rather than throwing, so the
 * scaffold runs safely before these are filled in.
 *
 * Discovery checklist (see handoff.md):
 *  - Chatbox interface group id (expected 162) and the child id of each of the 7 tab buttons.
 *  - For a tab button: which listener is populated (onOp vs onClick) + fallback script id/arg.
 *  - Right-click menu action / op id / params / option text for Show all / friends / none / Clear history.
 *  - The collapse/expand button widget id and its listener.
 *  - Vars for: current tab, collapsed state, and text-entry mode.
 */
final class ChatTabs
{
	private ChatTabs()
	{
	}

	static final int UNSET = -1;

	// --- Chatbox interface -------------------------------------------------
	// TODO(discovery): confirm the chatbox group id (expected 162) in the Widget Inspector.
	static final int CHATBOX_GROUP = UNSET;

	// TODO(discovery): collapse/expand button child id within CHATBOX_GROUP.
	static final int COLLAPSE_BUTTON_CHILD = UNSET;

	// --- Client vars -------------------------------------------------------
	// TODO(discovery): varc/varbit ids. Watch them change in the Var Inspector.
	// Replace the get* calls in the plugin once the type (VarClientInt vs Varbit) is confirmed.
	static final int VAR_CURRENT_TAB = UNSET;   // which tab is displayed
	static final int VAR_CHAT_CLOSED = UNSET;   // whether the chatbox is collapsed
	static final int VAR_TEXT_ENTRY = UNSET;    // whether the player is typing in chat

	/**
	 * The seven chat tabs, in game order. {@link #buttonChild} is the tab button's
	 * child id within {@link ChatTabs#CHATBOX_GROUP}; {@link #varValue} is the value
	 * {@link ChatTabs#VAR_CURRENT_TAB} holds when this tab is shown.
	 *
	 * TODO(discovery): fill buttonChild and varValue for each tab from the inspector.
	 * {@code supportsFilters} marks the channel-type tabs that offer Show all/friends/none
	 * and Clear history; Game and All do not (filter/clear hotkeys no-op there).
	 */
	enum ChatTab
	{
		ALL(UNSET, UNSET, false),
		GAME(UNSET, UNSET, false),
		PUBLIC(UNSET, UNSET, true),
		PRIVATE(UNSET, UNSET, true),
		CHANNEL(UNSET, UNSET, true),
		CLAN(UNSET, UNSET, true),
		TRADE(UNSET, UNSET, true);

		final int buttonChild;
		final int varValue;
		final boolean supportsFilters;

		ChatTab(int buttonChild, int varValue, boolean supportsFilters)
		{
			this.buttonChild = buttonChild;
			this.varValue = varValue;
			this.supportsFilters = supportsFilters;
		}
	}

	/**
	 * A right-click menu op on a tab button. The menu action / op id / option text are
	 * consistent across the channel-type tabs; only param1 (the packed widget id of the
	 * active tab button) changes.
	 *
	 * TODO(discovery): fill opId and optionText from a temporary MenuOptionClicked log
	 * (record getMenuAction(), getId(), getMenuOption()).
	 */
	enum FilterOp
	{
		SHOW_ALL(UNSET, ""),
		SHOW_FRIENDS(UNSET, ""),
		SHOW_NONE(UNSET, ""),
		CLEAR_HISTORY(UNSET, "");

		final int opId;
		final String optionText;

		FilterOp(int opId, String optionText)
		{
			this.opId = opId;
			this.optionText = optionText;
		}
	}

	/** Packs a (group, child) pair into the param1 form used by {@code client.menuAction}. */
	static int packComponentId(int group, int child)
	{
		return (group << 16) | child;
	}
}
