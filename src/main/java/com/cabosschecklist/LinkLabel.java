package com.cabosschecklist;

import java.awt.Cursor;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import net.runelite.client.ui.ColorScheme;

/** A link word: grey, white under the mouse, a hand cursor, acting on a left press. */
final class LinkLabel extends JLabel
{
	LinkLabel(String word, float size, Runnable action)
	{
		super(word);
		setFont(ViewParts.plain(size));
		setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (HalfToggle.isPress(e))
				{
					action.run();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				setForeground(Color.WHITE);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
		});
	}
}
