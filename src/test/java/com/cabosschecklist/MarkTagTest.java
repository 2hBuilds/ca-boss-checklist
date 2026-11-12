package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class MarkTagTest
{
	/** The tag painted at its preferred size into a transparent image. */
	private static BufferedImage paint(MarkTag tag)
	{
		final Dimension size = tag.getPreferredSize();
		tag.setSize(size);
		final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		tag.paint(g);
		g.dispose();
		return image;
	}

	private static boolean isOrange(Color c)
	{
		return c.getRed() > 200 && c.getGreen() >= 110 && c.getGreen() <= 170 && c.getBlue() < 60;
	}

	private static boolean isGrey(Color c)
	{
		final int high = Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue()));
		final int low = Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()));
		return high - low <= 12;
	}

	@Test
	public void theTagIsAnOutlineOrangeWhenStrong()
	{
		final MarkTag strong = new MarkTag();
		final Dimension size = strong.getPreferredSize();
		assertTrue("height " + size.height, size.height >= 12 && size.height <= 18);
		assertTrue("width " + size.width, size.width >= 30 && size.width <= 70);
		strong.setStrong(true);
		final BufferedImage orange = paint(strong);
		final Color top = new Color(orange.getRGB(size.width / 2, 0), true);
		assertTrue("alpha " + top.getAlpha(), top.getAlpha() >= 200);
		assertTrue("orange " + top, isOrange(top));
		assertEquals(0, new Color(orange.getRGB(3, size.height / 2), true).getAlpha());

		final MarkTag quiet = new MarkTag();
		quiet.setStrong(false);
		final BufferedImage grey = paint(quiet);
		final Color edge = new Color(grey.getRGB(size.width / 2, 0), true);
		assertTrue("alpha " + edge.getAlpha(), edge.getAlpha() >= 200);
		assertFalse("not orange " + edge, isOrange(edge));
		assertTrue("grey " + edge, isGrey(edge));
		assertEquals(0, new Color(grey.getRGB(3, size.height / 2), true).getAlpha());
	}
}
