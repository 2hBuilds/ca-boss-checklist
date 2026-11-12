package com.cabosschecklist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import javax.swing.JComponent;
import net.runelite.client.ui.ColorScheme;

/**
 * The small pill "MANUAL" on the boss card: bold 9 px capitals with a little letter spacing, fully rounded ends.
 * Never filled: a 1 px outline and letters, orange when strong (the mark overrides the account's progress), grey when
 * not. Not opaque.
 */
final class MarkTag extends JComponent
{
	/** The words on the pill. */
	static final String TEXT = "MANUAL";
	/** Air left and right of the letters, in px. */
	private static final int SIDE = 6;
	/** Air above and below the letters, in px. */
	private static final int UPDOWN = 2;
	private static final Color OUTLINE = new Color(0x6A6A6A);
	private static final Color QUIET_LETTERS = new Color(0xC8C8C8);

	/** Whether the outline and letters are orange (strong) rather than grey; read by tests. */
	boolean strong;

	MarkTag()
	{
		setOpaque(false);
		setFont(ViewParts.bold(9f).deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.08f)));
		final Dimension size = pillSize();
		setPreferredSize(size);
		setMinimumSize(size);
		setMaximumSize(size);
	}

	/** Sets the orange (strong) or grey look and draws the pill again. */
	void setStrong(boolean strong)
	{
		this.strong = strong;
		repaint();
	}

	private Dimension pillSize()
	{
		final FontMetrics metrics = getFontMetrics(getFont());
		final int width = (int) Math.ceil(getFont().getStringBounds(TEXT, metrics.getFontRenderContext()).getWidth());
		return new Dimension(width + 2 * SIDE, metrics.getHeight() + 2 * UPDOWN);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		final Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			final int width = getWidth();
			final int height = getHeight();
			g.setColor(strong ? ColorScheme.BRAND_ORANGE : OUTLINE);
			g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, height - 1, height - 1));
			g.setColor(strong ? ColorScheme.BRAND_ORANGE : QUIET_LETTERS);
			final Font font = getFont();
			g.setFont(font);
			final FontMetrics metrics = g.getFontMetrics();
			final double textWidth = font.getStringBounds(TEXT, g.getFontRenderContext()).getWidth();
			g.drawString(TEXT, (float) ((width - textWidth) / 2), (height - metrics.getHeight()) / 2f + metrics.getAscent());
		}
		finally
		{
			g.dispose();
		}
	}
}
