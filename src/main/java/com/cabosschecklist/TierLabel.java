package com.cabosschecklist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JLabel;
import net.runelite.client.ui.ColorScheme;

/**
 * A task's detail line ("Elite · Speed") in which only the tier word is coloured. The label's text stays the plain line;
 * it is drawn twice, the tier word's part of the label in the tier's colour and the rest in grey, so the label keeps the
 * size and the cutting of any other grey label.
 */
final class TierLabel extends JLabel
{
	private final String tier;
	private final Color tierColour;
	/** The colour the text is drawn in while a part is painted; null at any other time. */
	private Color painting;

	TierLabel(String tier, String text)
	{
		super(text);
		this.tier = tier == null ? "" : tier;
		this.tierColour = ViewParts.tierColour(tier);
		setFont(ViewParts.plain(11f));
		setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		setMinimumSize(new Dimension(0, 0));
	}

	@Override
	public Color getForeground()
	{
		return painting != null ? painting : super.getForeground();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		if (tierColour == null || tier.isEmpty() || !getText().startsWith(tier))
		{
			super.paintComponent(graphics);
			return;
		}
		final int split = Math.min(getWidth(), getFontMetrics(getFont()).stringWidth(tier));
		final Graphics word = graphics.create();
		final Graphics rest = graphics.create();
		try
		{
			word.clipRect(0, 0, split, getHeight());
			painting = tierColour;
			super.paintComponent(word);
			rest.clipRect(split, 0, Math.max(0, getWidth() - split), getHeight());
			painting = null;
			super.paintComponent(rest);
		}
		finally
		{
			painting = null;
			word.dispose();
			rest.dispose();
		}
	}
}
