package com.cabosschecklist;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import org.junit.Test;

/** Where the colour picker opens: beside the sidebar, level with the mouse, on the screen. */
public class PickerPlaceTest
{
	private static final Rectangle SCREEN = new Rectangle(0, 0, 1920, 1040);
	private static final Dimension PICKER = new Dimension(400, 300);

	@Test
	public void leftOfTheSidebarLevelWithTheMouse()
	{
		final Rectangle sidebar = new Rectangle(1670, 30, 242, 1000);
		assertEquals(new Point(1670 - 8 - 400, 500 - 150),
			PickerPlace.place(SCREEN, sidebar, new Point(1700, 500), PICKER));
	}

	@Test
	public void rightOfTheSidebarWhenTheLeftHasNoRoom()
	{
		final Rectangle sidebar = new Rectangle(100, 30, 242, 1000);
		assertEquals(new Point(100 + 242 + 8, 350), PickerPlace.place(SCREEN, sidebar, new Point(200, 500), PICKER));
	}

	@Test
	public void keptOnTheScreenNearItsEdges()
	{
		final Rectangle sidebar = new Rectangle(1670, 30, 242, 1000);
		assertEquals(0, PickerPlace.place(SCREEN, sidebar, new Point(1700, 20), PICKER).y);
		assertEquals(1040 - 300, PickerPlace.place(SCREEN, sidebar, new Point(1700, 1035), PICKER).y);
	}

	@Test
	public void aSecondScreenToTheRightIsRespected()
	{
		final Rectangle second = new Rectangle(1920, 0, 2560, 1400);
		final Rectangle sidebar = new Rectangle(1920 + 2300, 40, 242, 1300);
		assertEquals(new Point(1920 + 2300 - 8 - 400, 650), PickerPlace.place(second, sidebar, new Point(4300, 800), PICKER));
	}

	@Test
	public void withNoMouseItIsLevelWithTheSidebarTop()
	{
		final Rectangle sidebar = new Rectangle(1670, 30, 242, 1000);
		assertEquals(30, PickerPlace.place(SCREEN, sidebar, null, PICKER).y);
	}
}
