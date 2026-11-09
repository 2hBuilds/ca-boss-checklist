package com.cabosschecklist;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.ColorScheme;

/**
 * The sidebar button's icon, 16 x 16 px, with the 2hBuilds coin top-left ({@link CoinGlyph}). The player picks one of
 * three pictures in the settings ({@link #icon}):
 * <ul>
 * <li>{@link #withBook}, the default (chosen 2026-11-08, number 43 of the candidates): the game's own
 * Combat Achievements book, which RuneLite draws from the player's game cache, tucked under the coin's lower right.
 * Nothing of the game's is shipped with the plugin.</li>
 * <li>{@link #bookOnly}: the same book alone, fitted to the whole icon.</li>
 * <li>{@link #create}, drawn entirely in code, shown for the coin and letters choice, and for either book choice until
 * RuneLite has drawn the book (and kept if it never does): the
 * letters CA in the coin's own 3 x 5 letter shapes and the brand orange, clear of the coin, so it reads "2h CA".</li>
 * </ul>
 */
public final class NavIcon
{
	/** The sidebar icon size. */
	public static final int SIZE = 16;

	/** The letters C and A, 3 x 5 each like the coin's own, a 1 px gap between them. */
	private static final String[] LETTERS = {
		"###.###",
		"#...#.#",
		"#...###",
		"#...#.#",
		"###.#.#"
	};
	/** The letters' top-left pixel: right of the coin, the letters standing on the bottom row. */
	private static final int LETTERS_X = 9;
	private static final int LETTERS_Y = 11;
	/** The box the book is fitted into, under the coin's lower right: its top-left pixel and its size. */
	private static final int BOOK_X = 4;
	private static final int BOOK_Y = 5;
	private static final int BOOK_WIDTH = 12;
	private static final int BOOK_HEIGHT = 11;

	private NavIcon()
	{
	}

	/** A new 16 x 16 ARGB image of the icon. */
	public static BufferedImage create()
	{
		final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			CoinGlyph.paint(g);
			g.setColor(ColorScheme.BRAND_ORANGE);
			for (int row = 0; row < LETTERS.length; row++)
			{
				for (int column = 0; column < LETTERS[row].length(); column++)
				{
					if (LETTERS[row].charAt(column) == '#')
					{
						g.fillRect(LETTERS_X + column, LETTERS_Y + row, 1, 1);
					}
				}
			}
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/**
	 * A new 16 x 16 ARGB image of the icon with the book: {@code item} (the game's picture of the book, as RuneLite's item
	 * manager draws it, 36 x 32 with clear margins) cut to its own outline, shrunk to fit a 12 x 11 box with its
	 * proportions kept, centred in that box at (4, 5), and the coin drawn over the book's top-left corner. A picture
	 * with nothing in it gives the coin alone.
	 */
	public static BufferedImage withBook(BufferedImage item)
	{
		final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			final BufferedImage outline = trim(item);
			if (outline != null)
			{
				final BufferedImage book = shrink(outline, BOOK_WIDTH, BOOK_HEIGHT);
				g.drawImage(book, BOOK_X + (BOOK_WIDTH - book.getWidth()) / 2, BOOK_Y + (BOOK_HEIGHT - book.getHeight()) / 2, null);
			}
			CoinGlyph.paint(g);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/**
	 * A new 16 x 16 ARGB image of the book alone: {@code item} cut to its own outline, shrunk to fit the whole icon with its
	 * proportions kept, and centred. A picture with nothing in it gives a fully clear icon.
	 */
	public static BufferedImage bookOnly(BufferedImage item)
	{
		final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			final BufferedImage outline = trim(item);
			if (outline != null)
			{
				final BufferedImage book = shrink(outline, SIZE, SIZE);
				g.drawImage(book, (SIZE - book.getWidth()) / 2, (SIZE - book.getHeight()) / 2, null);
			}
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/** The icon for {@code choice}: the drawn one for the letters or while {@code book} is null, else the book's. */
	static BufferedImage icon(CaBossChecklistConfig.SidebarIcon choice, BufferedImage book)
	{
		if (choice == CaBossChecklistConfig.SidebarIcon.COIN_AND_LETTERS || book == null)
		{
			return create();
		}
		return choice == CaBossChecklistConfig.SidebarIcon.BOOK ? bookOnly(book) : withBook(book);
	}

	/** The smallest part of {@code picture} holding every pixel that is not fully clear, or null when there is none. */
	static BufferedImage trim(BufferedImage picture)
	{
		int minX = picture.getWidth();
		int minY = picture.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < picture.getHeight(); y++)
		{
			for (int x = 0; x < picture.getWidth(); x++)
			{
				if ((picture.getRGB(x, y) >>> 24) != 0)
				{
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
		return maxX < 0 ? null : picture.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}

	/**
	 * {@code picture} shrunk, never grown, to fit {@code width} x {@code height} with its proportions kept. Choice: the
	 * shrink averages area by area, so a detail blends into its neighbours instead of dropping out.
	 */
	static BufferedImage shrink(BufferedImage picture, int width, int height)
	{
		final double scale = Math.min(1.0, Math.min((double) width / picture.getWidth(), (double) height / picture.getHeight()));
		final int w = Math.max(1, (int) Math.round(picture.getWidth() * scale));
		final int h = Math.max(1, (int) Math.round(picture.getHeight() * scale));
		final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = out.createGraphics();
		try
		{
			g.drawImage(picture.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING), 0, 0, null);
		}
		finally
		{
			g.dispose();
		}
		return out;
	}
}
