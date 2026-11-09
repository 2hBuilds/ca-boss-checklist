package com.cabosschecklist;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import net.runelite.client.ui.ColorScheme;

/** A column of rows that follows the width of the scroll pane it sits in, so a long label is cut and never pushes a status out. */
final class ScrollColumn extends JPanel implements Scrollable
{
	/** One wheel step or bar arrow, in px. */
	private static final int UNIT_INCREMENT = 16;

	ScrollColumn()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
	}

	/** The scroll pane over this column: no border, no sideways bar, the vertical bar always shown. */
	JScrollPane pane()
	{
		final JScrollPane pane = new JScrollPane(this);
		pane.setBorder(null);
		pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		pane.getVerticalScrollBar().setUnitIncrement(UNIT_INCREMENT);
		pane.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		return pane;
	}

	@Override
	public Dimension getPreferredScrollableViewportSize()
	{
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return UNIT_INCREMENT;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return Math.max(UNIT_INCREMENT, visibleRect.height - UNIT_INCREMENT);
	}

	@Override
	public boolean getScrollableTracksViewportWidth()
	{
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}
}
