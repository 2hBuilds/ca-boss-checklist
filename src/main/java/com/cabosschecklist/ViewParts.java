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

	private ViewParts()
	{
	}

	/** The colour the game draws a name in for {@code shade}. */
	static Color colourOf(CaCompletion.Shade shade)
	{
		return shade == CaCompletion.Shade.GREEN ? GREEN : shade == CaCompletion.Shade.YELLOW ? YELLOW : PLAIN;
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
