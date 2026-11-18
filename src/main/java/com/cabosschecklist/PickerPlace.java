package com.cabosschecklist;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Where RuneLite's colour picker opens: beside the sidebar, level with the mouse, so it never covers the sidebar whose
 * colours it changes. Left of the sidebar when there is room on that screen, else right of it; always kept on the screen.
 */
final class PickerPlace
{
	/** The space between the sidebar and the picker, in px. */
	static final int GAP = 8;

	private PickerPlace()
	{
	}

	/**
	 * The picker's top-left corner on the screen.
	 *
	 * @param screen  the usable part of the screen the sidebar is on (its bounds less the task bar)
	 * @param sidebar the sidebar's bounds on the screen
	 * @param mouse   the mouse on the screen, or null when not known (the picker is then level with the sidebar's top)
	 * @param picker  the picker's size
	 */
	static Point place(Rectangle screen, Rectangle sidebar, Point mouse, Dimension picker)
	{
		int x = sidebar.x - GAP - picker.width;
		if (x < screen.x)
		{
			x = sidebar.x + sidebar.width + GAP;
		}
		final int wanted = mouse == null ? sidebar.y : mouse.y - picker.height / 2;
		return new Point(clamp(x, screen.x, screen.x + screen.width - picker.width),
			clamp(wanted, screen.y, screen.y + screen.height - picker.height));
	}

	private static int clamp(int value, int low, int high)
	{
		return Math.max(low, Math.min(value, Math.max(low, high)));
	}
}
