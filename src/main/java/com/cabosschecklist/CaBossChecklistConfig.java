package com.cabosschecklist;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * The plugin's settings. Flat on purpose: every item is declared here and this interface extends {@link Config}
 * alone, because RuneLite stores defaults only for the methods declared on the interface it is given. The group and
 * the keys are frozen at first release; renaming one silently discards every player's setting.
 *
 * <p>The player's per-boss marks are not an item: they live in the RuneScape profile under {@link #MARKS_KEY}, one
 * string per account, written by the sidebar.
 */
@ConfigGroup(CaBossChecklistConfig.GROUP)
public interface CaBossChecklistConfig extends Config
{
	String GROUP = "ca-boss-checklist";

	/** The switch that turns a name green once the account has done every task for it. */
	String AUTO_DETECT_KEY = "autoDetect";

	/** The switch that turns a name yellow when one or two of its tasks are left. */
	String ALMOST_DONE_KEY = "almostDone";

	/** The switch that shows the wiki's picture of a boss on its card. */
	String WIKI_PICTURES_KEY = "wikiPictures";

	/** The drop-down that picks the sidebar button's picture. */
	String SIDEBAR_ICON_KEY = "sidebarIcon";

	/** The profile key holding the account's marks, "id=token" entries joined by commas (see {@link Marks}). */
	String MARKS_KEY = "marks";

	@ConfigItem(
		keyName = AUTO_DETECT_KEY,
		name = "Green when all tasks done",
		description = "Show a name green once every task for it is done.",
		position = 0
	)
	default boolean autoDetect()
	{
		return true;
	}

	@ConfigItem(
		keyName = ALMOST_DONE_KEY,
		name = "Yellow when almost done",
		description = "Show a name yellow in the Tasks menu when one or two of its tasks are left.",
		position = 1
	)
	default boolean almostDone()
	{
		return true;
	}

	@ConfigItem(
		keyName = WIKI_PICTURES_KEY,
		name = "Wiki pictures",
		description = "Show the wiki's own picture of a boss on its card. Sends only the boss's name to oldschool.runescape.wiki, then fetches the picture the wiki names.",
		position = 2
	)
	default boolean wikiPictures()
	{
		return true;
	}

	@ConfigItem(
		keyName = SIDEBAR_ICON_KEY,
		name = "Sidebar icon",
		description = "Which picture the plugin's sidebar button shows.",
		position = 3
	)
	default SidebarIcon sidebarIcon()
	{
		return SidebarIcon.COIN_AND_BOOK;
	}

	/** The pictures the sidebar button can show; each is shown as its words. */
	enum SidebarIcon
	{
		COIN_AND_BOOK("2h coin and book"),
		BOOK("Book only"),
		COIN_AND_LETTERS("2h coin and CA letters");

		private final String words;

		SidebarIcon(String words)
		{
			this.words = words;
		}

		@Override
		public String toString()
		{
			return words;
		}
	}
}
