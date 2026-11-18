package com.cabosschecklist;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * The plugin's settings. Flat on purpose: every item is declared here and this interface extends {@link Config}
 * alone, because RuneLite stores defaults only for the methods declared on the interface it is given. The group and
 * the keys are frozen at first release; renaming one silently discards every player's setting.
 *
 * <p>The player's per-boss marks are not an item: they live in the RuneScape profile under {@link #MARKS_KEY}, one
 * string per account, written by the sidebar, and so are the marks the player has seen, under {@link #SEEN_KEY}.
 */
@ConfigGroup(CaBossChecklistConfig.GROUP)
public interface CaBossChecklistConfig extends Config
{
	String GROUP = "ca-boss-checklist";

	/** The switch that turns a name green once the account has done every task for it. */
	String AUTO_DETECT_KEY = "autoDetect";

	/**
	 * The old switch for yellow names, a boolean of 0.0.5 and before. It is no longer a config item: {@link #YELLOW_WHEN_KEY}
	 * replaced it, and the plugin reads this key once at start-up to carry a "false" over (see {@link YellowMigration}).
	 */
	String ALMOST_DONE_KEY = "almostDone";

	/** The choice of when a name turns yellow. */
	String YELLOW_WHEN_KEY = "yellowWhen";

	/** The colour of a finished (or ticked) name. */
	String GREEN_COLOUR_KEY = "greenColour";

	/** The colour of a name that is almost done. */
	String YELLOW_COLOUR_KEY = "yellowColour";

	/** The colour of an unfinished name. */
	String NOT_DONE_COLOUR_KEY = "notDoneColour";

	/** The colour of the frame round the opened boss. */
	String FRAME_COLOUR_KEY = "frameColour";

	/** The switch that shows the wiki's picture of a boss on its card. */
	String WIKI_PICTURES_KEY = "wikiPictures";

	/** The switch that shows the plugin's sidebar button. */
	String SHOW_SIDEBAR_KEY = "showSidebar";

	/** The drop-down that picks the sidebar button's picture. */
	String SIDEBAR_ICON_KEY = "sidebarIcon";

	/** The profile key holding the account's marks, "id=token" entries joined by commas (see {@link Marks}). */
	String MARKS_KEY = "marks";

	/** The profile key holding the overriding marks the player has looked at, in the same form as the marks. */
	String SEEN_KEY = "seenMarks";

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
		keyName = YELLOW_WHEN_KEY,
		name = "Yellow when",
		description = "When a name turns yellow in the Tasks menu and the sidebar. Tasks the game turns off for your account are left out.",
		position = 1
	)
	default YellowWhen yellowWhen()
	{
		return YellowWhen.ONE_OR_TWO;
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
		keyName = SHOW_SIDEBAR_KEY,
		name = "Show the sidebar button",
		description = "Off: the button is hidden and names follow your account only. "
			+ "Your marks are kept for when you turn it back on.",
		position = 3
	)
	default boolean showSidebar()
	{
		return true;
	}

	@ConfigItem(
		keyName = SIDEBAR_ICON_KEY,
		name = "Sidebar icon",
		description = "Which picture the plugin's sidebar button shows.",
		position = 4
	)
	default SidebarIcon sidebarIcon()
	{
		return SidebarIcon.COIN_AND_BOOK;
	}

	@ConfigItem(
		keyName = GREEN_COLOUR_KEY,
		name = "Green colour",
		description = "The colour of finished names, in the Tasks menu and the sidebar.",
		position = 5
	)
	default Color greenColour()
	{
		return ViewParts.GREEN;
	}

	@ConfigItem(
		keyName = YELLOW_COLOUR_KEY,
		name = "Yellow colour",
		description = "The colour of names that are almost done (see Yellow when).",
		position = 6
	)
	default Color yellowColour()
	{
		return ViewParts.YELLOW;
	}

	@ConfigItem(
		keyName = NOT_DONE_COLOUR_KEY,
		name = "Not done colour",
		description = "The colour of unfinished names. The game's own orange is left to the game; any other colour is also set on the names in the Tasks menu.",
		position = 7
	)
	default Color notDoneColour()
	{
		return ViewParts.PLAIN;
	}

	@ConfigItem(
		keyName = FRAME_COLOUR_KEY,
		name = "Frame colour",
		description = "The colour of the frame round the boss you opened in the sidebar.",
		position = 8
	)
	default Color frameColour()
	{
		return ViewParts.FRAME;
	}

	/** When a name turns yellow; each is shown as its words. */
	enum YellowWhen
	{
		ONE_OR_TWO("1 or 2 tasks left"),
		STARTED("Started"),
		HALF("Half done"),
		ONLY_GRANDMASTER("Only Grandmaster left"),
		OFF("Off");

		private final String words;

		YellowWhen(String words)
		{
			this.words = words;
		}

		@Override
		public String toString()
		{
			return words;
		}
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
