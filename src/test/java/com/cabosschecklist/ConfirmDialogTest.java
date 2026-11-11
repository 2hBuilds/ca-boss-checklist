package com.cabosschecklist;

import java.awt.Color;
import javax.swing.Action;
import javax.swing.JLabel;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** The question window's controls, built without a window; no test opens a real window. */
public class ConfirmDialogTest
{
	private static final String FOUR = "One.\nTwo.\nThree.\nBack.";

	@Test
	public void contentHoldsThePictureTheLinesAndTwoButtons()
	{
		final ConfirmDialog dialog = new ConfirmDialog(NavIcon.create(), FOUR, "Hide");
		assertEquals(4, dialog.lines.size());
		assertEquals("One.", dialog.lines.get(0).getText());
		assertEquals("Two.", dialog.lines.get(1).getText());
		assertEquals("Three.", dialog.lines.get(2).getText());
		assertEquals("Back.", dialog.lines.get(3).getText());
		assertTrue(dialog.lines.get(0).getFont().isBold());
		assertEquals(Color.WHITE, dialog.lines.get(0).getForeground());
		for (int i = 1; i <= 2; i++)
		{
			assertFalse(dialog.lines.get(i).getFont().isBold());
			assertEquals(new Color(0xD6D6D6), dialog.lines.get(i).getForeground());
		}
		assertFalse(dialog.lines.get(3).getFont().isBold());
		assertEquals(ColorScheme.LIGHT_GRAY_COLOR, dialog.lines.get(3).getForeground());
		assertNotNull(dialog.pictureLabel);
		assertEquals(32, dialog.pictureLabel.getIcon().getIconWidth());
		assertEquals(32, dialog.pictureLabel.getIcon().getIconHeight());
		assertEquals("Hide", dialog.yesButton.getText());
		assertEquals("Cancel", dialog.cancelButton.getText());
		for (final JLabel line : dialog.lines)
		{
			assertTrue(dialog.content.isAncestorOf(line));
		}
		assertTrue(dialog.content.isAncestorOf(dialog.pictureLabel));
		assertTrue(dialog.content.isAncestorOf(dialog.yesButton));
		assertTrue(dialog.content.isAncestorOf(dialog.cancelButton));
	}

	@Test
	public void noPictureMeansNoTile()
	{
		final ConfirmDialog dialog = new ConfirmDialog(null, FOUR, "Hide");
		assertNull(dialog.pictureLabel);
		assertEquals(4, dialog.lines.size());
	}

	@Test
	public void onlyTheYesButtonAnswersYes()
	{
		assertFalse(new ConfirmDialog(null, "Q", "Hide").answeredYes);

		final ConfirmDialog yes = new ConfirmDialog(null, "Q", "Hide");
		yes.yesButton.doClick(0);
		assertTrue(yes.answeredYes);

		final ConfirmDialog cancel = new ConfirmDialog(null, "Q", "Hide");
		cancel.cancelButton.doClick(0);
		assertFalse(cancel.answeredYes);

		final ConfirmDialog escape = new ConfirmDialog(null, "Q", "Hide");
		escape.yesButton.doClick(0);
		final Action action = escape.content.getActionMap().get(ConfirmDialog.CANCEL_KEY);
		assertNotNull(action);
		action.actionPerformed(null);
		assertFalse(escape.answeredYes);
		assertSame(action, escape.content.getActionMap().get(ConfirmDialog.CANCEL_KEY));
	}
}
