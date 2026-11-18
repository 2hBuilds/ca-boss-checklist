package com.cabosschecklist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** The small pieces the two views are made of: type, labels that Swing cuts with "...", and rows that keep their height. */
final class ViewParts
{
	/** The game's green for a finished boss's name. */
	static final Color GREEN = new Color(0x0DC10D);
	/** The game's yellow for a boss with one or two tasks left. */
	static final Color YELLOW = new Color(0xFFFF00);
	/** The game's own orange in its Monster list. */
	static final Color PLAIN = new Color(0xFF981F);

	/** The frame's default colour: a deep teal no name or tier word uses (the user's pick), for the frame round the chosen boss. */
	static final Color FRAME = new Color(0x26716D);
	/** The frame's line width, in px. */
	static final int FRAME_WIDTH = 2;
	/** The ground of the chosen boss's card, a touch darker than the rows. */
	static final Color CARD_GROUND = new Color(36, 36, 36);
	/** The ground of the box the tasks sit in. */
	static final Color TASKS_GROUND = new Color(27, 27, 27);
	/** The 1 px border of the tasks box. */
	static final Color TASKS_BORDER = new Color(58, 58, 58);
	/** The 1 px line between two tasks. */
	static final Color TASK_LINE = new Color(42, 42, 42);
	/** The lighter band behind an open task. */
	static final Color OPEN_BAND = new Color(38, 38, 38);

	/** The colours the player chose; null until the plugin sets them, which means {@link ColourSet#DEFAULTS}. */
	private static volatile ColourSet chosen;

	private ViewParts()
	{
	}

	/** The colours the sidebar draws in now. */
	static ColourSet colours()
	{
		final ColourSet set = chosen;
		return set == null ? ColourSet.DEFAULTS : set;
	}

	/** Sets the colours the sidebar draws in from the next draw on (null: the defaults). */
	static void useColours(ColourSet set)
	{
		chosen = set;
	}

	/** The frame round the opened boss, in the colour the player chose. */
	static Color frame()
	{
		return colours().frame;
	}

	/** The colour of a tier word ("Easy" to "Grandmaster"), or null for any other word. */
	static Color tierColour(String word)
	{
		switch (word == null ? "" : word)
		{
			case "Easy":
				return new Color(168, 216, 168);
			case "Medium":
				return new Color(164, 200, 232);
			case "Hard":
				return new Color(143, 214, 207);
			case "Elite":
				return new Color(176, 164, 232);
			case "Master":
				return new Color(232, 164, 164);
			case "Grandmaster":
				return new Color(232, 210, 122);
			default:
				return null;
		}
	}

	/** The colour a name is drawn in for {@code shade}: the game's own unless the player chose another. */
	static Color colourOf(CaCompletion.Shade shade)
	{
		return colours().colourOf(shade);
	}

	/** The default face, plain, at {@code size} px. */
	static Font plain(float size)
	{
		return FontManager.getDefaultFont().deriveFont(size);
	}

	/** The default face, bold, at {@code size} px. */
	static Font bold(float size)
	{
		return FontManager.getDefaultBoldFont().deriveFont(size);
	}

	/** A label that may be squeezed to nothing, so Swing cuts a long text with "..." instead of widening its row. */
	static JLabel label(String text, Font font, Color colour)
	{
		final JLabel label = new JLabel(text);
		label.setFont(font);
		label.setForeground(colour);
		label.setMinimumSize(new Dimension(0, 0));
		return label;
	}

	/**
	 * One centred line, for a view's empty states and the boss card: as wide as its column, its text centred in it.
	 * The full width matters: in a column whose parts may all shrink to nothing, Swing's {@code BoxLayout} would give a
	 * line that keeps its own width only half of it, at the column's left edge.
	 */
	static JLabel centred(String text, Font font, Color colour)
	{
		final JLabel label = new JLabel(text, SwingConstants.CENTER)
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		label.setFont(font);
		label.setForeground(colour);
		label.setMinimumSize(new Dimension(0, 0));
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		return label;
	}

	/** A grey 11 px caption or detail. */
	static JLabel quiet(String text)
	{
		return label(text, plain(11f), ColorScheme.LIGHT_GRAY_COLOR);
	}

	/** A panel for a column of rows: as wide as the column, never taller than its content. */
	static JPanel capped(LayoutManager layout)
	{
		return new JPanel(layout)
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
	}
}
