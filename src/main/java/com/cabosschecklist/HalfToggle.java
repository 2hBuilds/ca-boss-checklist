package com.cabosschecklist;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.function.IntConsumer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Two halves side by side in one bordered box, the lit half orange with its word in dark bold and the other dark with
 * its word in grey: the bank's toggle. The halves are equal and both words take one size, the largest up to 12 at which
 * each keeps {@link #TOGGLE_AIR} px a side. The box fills the width it is given.
 */
final class HalfToggle extends JPanel
{
	/** The toggle's height, in px, its 1 px border included. */
	static final int TOGGLE_HEIGHT = 20;
	/** The least air, in px, left and right of a half's word. */
	static final int TOGGLE_AIR = 2;
	/** The largest size, in px, the words are set in. */
	static final int TOGGLE_TEXT_MAX = 12;
	/** The smallest size, so a box far narrower than the sidebar still ends. */
	private static final int TOGGLE_TEXT_MIN = 6;

	/** The side of the dot, in px. */
	private static final int DOT_SIZE = 7;
	/** The space between a half's word and its dot, in px. */
	private static final int DOT_GAP = 5;
	/** The dot: a filled orange circle, one shared icon. */
	private static final Icon DOT = new Icon()
	{
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			final Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(ColorScheme.BRAND_ORANGE);
			g2.fill(new Ellipse2D.Float(x, y, DOT_SIZE, DOT_SIZE));
			g2.dispose();
		}

		@Override
		public int getIconWidth()
		{
			return DOT_SIZE;
		}

		@Override
		public int getIconHeight()
		{
			return DOT_SIZE;
		}
	};

	private final JLabel[] halves = new JLabel[2];
	/** Which half is lit: 0, 1, or -1 before the caller first says. */
	private int lit = -1;
	private int textSize = TOGGLE_TEXT_MAX;
	private Font litFont = bold(TOGGLE_TEXT_MAX);
	private Font unlitFont = plain(TOGGLE_TEXT_MAX);

	/** @param onPress called with the index of an unlit half that is pressed */
	HalfToggle(String first, String second, IntConsumer onPress)
	{
		super(null);
		setBorder(new MatteBorder(1, 1, 1, 1, ColorScheme.MEDIUM_GRAY_COLOR));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		final String[] words = {first, second};
		for (int i = 0; i < halves.length; i++)
		{
			final int index = i;
			final JLabel half = new JLabel(words[i]);
			half.setHorizontalAlignment(SwingConstants.CENTER);
			half.setVerticalAlignment(SwingConstants.CENTER);
			half.setOpaque(true);
			half.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (isPress(e) && !isLit(index))
					{
						onPress.accept(index);
					}
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					if (!isLit(index))
					{
						half.setForeground(ColorScheme.BRAND_ORANGE);
					}
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					paintHalf(index);
				}
			});
			halves[i] = half;
			add(half);
		}
		setPreferredSize(new Dimension(1, TOGGLE_HEIGHT));
		setMinimumSize(new Dimension(1, TOGGLE_HEIGHT));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, TOGGLE_HEIGHT));
		setLit(0);
	}

	private static Font plain(int size)
	{
		return FontManager.getDefaultFont().deriveFont((float) size);
	}

	private static Font bold(int size)
	{
		return FontManager.getDefaultBoldFont().deriveFont((float) size);
	}

	/** Lights half {@code index} (0 or 1) and unlights the other; the same half again repaints nothing. */
	void setLit(int index)
	{
		final int want = index == 1 ? 1 : 0;
		if (want == lit)
		{
			return;
		}
		lit = want;
		for (int i = 0; i < halves.length; i++)
		{
			paintHalf(i);
		}
	}

	/** Sets half {@code index}'s word and fits both words again. */
	void setWord(int index, String word)
	{
		halves[index].setText(word);
		revalidate();
		repaint();
	}

	/**
	 * Puts the dot after half {@code index}'s word and sets the half's tooltip to {@code tip}; a null {@code tip}
	 * removes both. Fits the words again.
	 */
	void setDot(int index, String tip)
	{
		final JLabel half = halves[index];
		half.setIcon(tip == null ? null : DOT);
		half.setHorizontalTextPosition(SwingConstants.LEADING);
		half.setIconTextGap(DOT_GAP);
		half.setToolTipText(tip);
		revalidate();
		repaint();
	}

	/**
	 * The halves are equal (the second takes the odd pixel) and both words are set in one size: the largest whole size up
	 * to {@link #TOGGLE_TEXT_MAX} at which each word, in the wider of the bold and plain faces, fits its half with
	 * {@link #TOGGLE_AIR} px on each side.
	 */
	@Override
	public void doLayout()
	{
		final Insets in = getInsets();
		final int inner = getWidth() - in.left - in.right;
		final int height = getHeight() - in.top - in.bottom;
		final int first = inner / 2;
		final int[] widths = {first, inner - first};
		int size = TOGGLE_TEXT_MAX;
		while (size > TOGGLE_TEXT_MIN && !fits(size, widths))
		{
			size--;
		}
		if (size != textSize)
		{
			textSize = size;
			litFont = bold(size);
			unlitFont = plain(size);
			for (int i = 0; i < halves.length; i++)
			{
				paintHalf(i);
			}
		}
		halves[0].setBounds(in.left, in.top, widths[0], height);
		halves[1].setBounds(in.left + widths[0], in.top, widths[1], height);
	}

	private boolean fits(int size, int[] widths)
	{
		final FontMetrics boldMetrics = getFontMetrics(bold(size));
		final FontMetrics plainMetrics = getFontMetrics(plain(size));
		for (int i = 0; i < halves.length; i++)
		{
			final String word = halves[i].getText();
			final int text = Math.max(boldMetrics.stringWidth(word), plainMetrics.stringWidth(word));
			final int dot = halves[i].getIcon() == null ? 0 : DOT_SIZE + DOT_GAP;
			if (text + dot + 2 * TOGGLE_AIR > widths[i])
			{
				return false;
			}
		}
		return true;
	}

	/** Whether half {@code index} is the lit one; pressing the lit half changes nothing. */
	boolean isLit(int index)
	{
		return index == lit;
	}

	private void paintHalf(int index)
	{
		final JLabel half = halves[index];
		final boolean on = isLit(index);
		half.setBackground(on ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR);
		half.setForeground(on ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
		half.setFont(on ? litFont : unlitFont);
		// A hand only where a press does something.
		half.setCursor(on ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	/**
	 * Whether a mouse event is a press that should act: the left button, and not a popup trigger (macOS raises one on a
	 * ctrl-click of the left button).
	 */
	static boolean isPress(MouseEvent e)
	{
		return SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger();
	}
}
