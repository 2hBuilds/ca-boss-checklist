package com.cabosschecklist;

import java.awt.Color;

/**
 * The four colours the player can choose: green for finished names, yellow for almost done, "not done" for the rest, and
 * the frame round the opened boss. Immutable, with no RuneLite types so it is tested without a client. The defaults are
 * the game's own colours and the sidebar's orange, so a player who never touches a setting sees what they always saw.
 */
final class ColourSet
{
	/** The colours of a player who has changed nothing. */
	static final ColourSet DEFAULTS = new ColourSet(ViewParts.GREEN, ViewParts.YELLOW, ViewParts.PLAIN, ViewParts.FRAME);

	/** One of the four colours: its config key, its row in the sidebar's menu and its picker's title. */
	enum Slot
	{
		GREEN(CaBossChecklistConfig.GREEN_COLOUR_KEY, "Green...", "Green colour"),
		YELLOW(CaBossChecklistConfig.YELLOW_COLOUR_KEY, "Yellow...", "Yellow colour"),
		NOT_DONE(CaBossChecklistConfig.NOT_DONE_COLOUR_KEY, "Not done...", "Not done colour"),
		FRAME(CaBossChecklistConfig.FRAME_COLOUR_KEY, "Frame...", "Frame colour");

		/** The config key the colour is stored under. */
		final String key;
		/** The menu row's text. */
		final String menuText;
		/** The colour picker's title. */
		final String title;

		Slot(String key, String menuText, String title)
		{
			this.key = key;
			this.menuText = menuText;
			this.title = title;
		}
	}

	final Color green;
	final Color yellow;
	final Color notDone;
	final Color frame;

	/** The four colours; a null one takes its default, and any transparency is dropped (the picker hides it). */
	ColourSet(Color green, Color yellow, Color notDone, Color frame)
	{
		this.green = opaque(green, ViewParts.GREEN);
		this.yellow = opaque(yellow, ViewParts.YELLOW);
		this.notDone = opaque(notDone, ViewParts.PLAIN);
		this.frame = opaque(frame, ViewParts.FRAME);
	}

	private static Color opaque(Color colour, Color fallback)
	{
		return colour == null ? fallback : new Color(colour.getRGB() & 0xFFFFFF);
	}

	/** The colour of {@code slot}. */
	Color get(Slot slot)
	{
		switch (slot)
		{
			case GREEN:
				return green;
			case YELLOW:
				return yellow;
			case NOT_DONE:
				return notDone;
			default:
				return frame;
		}
	}

	/** The colour a name has in the sidebar for {@code shade}. */
	Color colourOf(CaCompletion.Shade shade)
	{
		return shade == CaCompletion.Shade.GREEN ? green : shade == CaCompletion.Shade.YELLOW ? yellow : notDone;
	}

	/** Whether the "not done" colour is the game's own orange, which the plugin then leaves to the game. */
	boolean notDoneIsDefault()
	{
		return same(notDone, ViewParts.PLAIN);
	}

	private static boolean same(Color a, Color b)
	{
		return (a.getRGB() & 0xFFFFFF) == (b.getRGB() & 0xFFFFFF);
	}

	/**
	 * The colour the plugin sets on a name in the game's lists, as 0xRRGGBB, or null to leave the game's own colour alone:
	 * green and yellow always, "not done" only when it differs from the game's orange, and nothing for "All" (id 0) or
	 * any id below 1.
	 */
	Integer inGame(CaCompletion.Shade shade, int id)
	{
		if (id < 1)
		{
			return null;
		}
		if (shade == CaCompletion.Shade.PLAIN)
		{
			return notDoneIsDefault() ? null : Integer.valueOf(notDone.getRGB() & 0xFFFFFF);
		}
		return colourOf(shade).getRGB() & 0xFFFFFF;
	}

	/** The colour a complete tier has in the game's Tier list, as 0xRRGGBB: the green. */
	int completeTier()
	{
		return green.getRGB() & 0xFFFFFF;
	}
}
