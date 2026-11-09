package com.cabosschecklist;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link NavIcon}, in memory: the size, the coin exactly as {@link CoinGlyph} draws it, the picture clear of it. */
public class NavIconTest
{
	/** The coin's centre and the farthest its jittered rim reaches from it, with a pixel of margin. */
	private static final double COIN_CENTRE = 4.5;
	private static final double CLEAR_DISTANCE = 6.0;

	private static BufferedImage coinAlone()
	{
		final BufferedImage img = new BufferedImage(NavIcon.SIZE, NavIcon.SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			CoinGlyph.paint(g);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	@Test
	public void theIconIsSixteenBySixteenArgb()
	{
		final BufferedImage icon = NavIcon.create();
		assertEquals(16, icon.getWidth());
		assertEquals(16, icon.getHeight());
		assertEquals(BufferedImage.TYPE_INT_ARGB, icon.getType());
	}

	@Test
	public void theCoinIsDrawnPixelForPixelAsTheCoinGlyphDrawsIt()
	{
		final BufferedImage icon = NavIcon.create();
		final BufferedImage coin = coinAlone();
		int coinPixels = 0;
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				if ((coin.getRGB(x, y) >>> 24) != 0)
				{
					coinPixels++;
					assertEquals("coin pixel " + x + "," + y, coin.getRGB(x, y), icon.getRGB(x, y));
				}
			}
		}
		assertTrue("the coin drew something", coinPixels > 0);
	}

	@Test
	public void thePictureLiesClearOfTheCoinsCircle()
	{
		final BufferedImage icon = NavIcon.create();
		final BufferedImage coin = coinAlone();
		int pictureOnly = 0;
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				if (icon.getRGB(x, y) != coin.getRGB(x, y))
				{
					pictureOnly++;
					final double dx = x + 0.5 - COIN_CENTRE;
					final double dy = y + 0.5 - COIN_CENTRE;
					assertTrue("picture pixel " + x + "," + y + " is too near the coin",
						Math.sqrt(dx * dx + dy * dy) >= CLEAR_DISTANCE);
				}
			}
		}
		assertTrue("the picture drew something", pictureOnly > 0);
	}

	/**
	 * A stand-in for RuneLite's picture of the book: 36 x 32 and clear, but for a solid 30 x 27 block at (3, 2), the size
	 * the game's book has inside its picture. No picture of the game's is used in the tests.
	 */
	private static BufferedImage itemPicture()
	{
		final BufferedImage item = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		for (int y = 2; y < 29; y++)
		{
			for (int x = 3; x < 33; x++)
			{
				item.setRGB(x, y, 0xFF5C3F3D);
			}
		}
		return item;
	}

	@Test
	public void theBookIconKeepsTheCoinWhole()
	{
		final BufferedImage icon = NavIcon.withBook(itemPicture());
		final BufferedImage coin = coinAlone();
		assertEquals(16, icon.getWidth());
		assertEquals(16, icon.getHeight());
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				if ((coin.getRGB(x, y) >>> 24) == 255)
				{
					assertEquals("coin pixel " + x + "," + y, coin.getRGB(x, y), icon.getRGB(x, y));
				}
			}
		}
	}

	/** Cut to its outline first, the 30 x 27 book fills its 12 x 11 box (uncut, its clear margins would leave it 10 wide). */
	@Test
	public void theBookIsCutToItsOutlineAndFillsItsBox()
	{
		final BufferedImage icon = NavIcon.withBook(itemPicture());
		final BufferedImage coin = coinAlone();
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				final boolean inBox = x >= 4 && y >= 5;
				if (!inBox && (coin.getRGB(x, y) >>> 24) == 0)
				{
					assertEquals("pixel " + x + "," + y + " is outside the book's box and stays clear", 0, icon.getRGB(x, y) >>> 24);
				}
			}
		}
		assertEquals("the box's left column holds book", 255, icon.getRGB(4, 15) >>> 24);
		assertEquals("the box's right column holds book", 255, icon.getRGB(15, 15) >>> 24);
		assertEquals("the box's top row holds book", 255, icon.getRGB(15, 5) >>> 24);
	}

	@Test
	public void anEmptyPictureGivesTheCoinAlone()
	{
		final BufferedImage icon = NavIcon.withBook(new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB));
		final BufferedImage coin = coinAlone();
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				assertEquals("pixel " + x + "," + y, coin.getRGB(x, y), icon.getRGB(x, y));
			}
		}
	}

	/** The chosen picture (2026-11-08, number 1 of 20): CA in the brand orange, at x 9 to 15 and y 11 to 15, nothing else. */
	@Test
	public void thePictureIsTheLettersCa()
	{
		final String[] letters = {"###.###", "#...#.#", "#...###", "#...#.#", "###.#.#"};
		final BufferedImage icon = NavIcon.create();
		final BufferedImage coin = coinAlone();
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				final boolean letter = x >= 9 && y >= 11 && letters[y - 11].charAt(x - 9) == '#';
				if (letter)
				{
					assertEquals("letter pixel " + x + "," + y, ColorScheme.BRAND_ORANGE.getRGB(), icon.getRGB(x, y));
				}
				else if ((coin.getRGB(x, y) >>> 24) == 0)
				{
					assertEquals("pixel " + x + "," + y + " stays clear", 0, icon.getRGB(x, y) >>> 24);
				}
			}
		}
	}

	@Test
	public void theBookAloneFillsTheIcon()
	{
		final BufferedImage icon = NavIcon.bookOnly(itemPicture());
		assertEquals(16, icon.getWidth());
		assertEquals(16, icon.getHeight());
		for (int x = 0; x < NavIcon.SIZE; x++)
		{
			assertEquals("row 0, column " + x, 0, icon.getRGB(x, 0) >>> 24);
			assertEquals("row 15, column " + x, 0, icon.getRGB(x, 15) >>> 24);
		}
		assertEquals(255, icon.getRGB(0, 1) >>> 24);
		assertEquals(255, icon.getRGB(15, 1) >>> 24);
		assertEquals(255, icon.getRGB(0, 14) >>> 24);
		assertEquals(255, icon.getRGB(15, 14) >>> 24);
		final BufferedImage clear = NavIcon.bookOnly(new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB));
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				assertEquals("empty picture pixel " + x + "," + y, 0, clear.getRGB(x, y) >>> 24);
			}
		}
	}

	private static void assertSame(String what, BufferedImage expected, BufferedImage actual)
	{
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				assertEquals(what + " pixel " + x + "," + y, expected.getRGB(x, y), actual.getRGB(x, y));
			}
		}
	}

	@Test
	public void eachChoiceDrawsItsIcon()
	{
		final BufferedImage book = itemPicture();
		assertSame("coin and book", NavIcon.withBook(book), NavIcon.icon(CaBossChecklistConfig.SidebarIcon.COIN_AND_BOOK, book));
		assertSame("book only", NavIcon.bookOnly(book), NavIcon.icon(CaBossChecklistConfig.SidebarIcon.BOOK, book));
		assertSame("coin and letters", NavIcon.create(), NavIcon.icon(CaBossChecklistConfig.SidebarIcon.COIN_AND_LETTERS, book));
		for (CaBossChecklistConfig.SidebarIcon choice : CaBossChecklistConfig.SidebarIcon.values())
		{
			assertSame("no book, " + choice, NavIcon.create(), NavIcon.icon(choice, null));
		}
	}
}
