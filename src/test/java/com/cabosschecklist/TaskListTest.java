package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.FontMetrics;
import java.util.Collections;
import java.util.List;
import javax.swing.JLabel;
import org.junit.Test;

/** {@link TaskList#lines}: a description broken into lines that fit the task row, every word kept. */
public class TaskListTest
{
	/** The game's longest task description, 342 characters: task 433, "Perfect Akkha" (osrs-dumps 44e1ff8). */
	private static final String LONGEST = "Complete Akkha in a group of two or more, without anyone taking any hits from the following: Akkha's attacks off-prayer, Akkha's special attacks (orbs, memory, detonate), exploding shadow timers, orbs in the enrage phase or attacking Akkha with the wrong style. Hits for zero damage still count. You must have all Akkha invocations activated.";

	/**
	 * Found in the lead's look at the opened task: a CSS width cut the description at the row's edge, so the lines are
	 * now broken by measuring them in the label's own font.
	 */
	@Test
	public void descriptionLinesFitTheirWidthAndKeepEveryWord()
	{
		final JLabel probe = new JLabel();
		probe.setFont(ViewParts.plain(11f));
		final FontMetrics metrics = probe.getFontMetrics(probe.getFont());
		final List<String> lines = TaskList.lines(LONGEST, metrics);
		assertTrue("the longest description takes several lines", lines.size() > 3);
		for (String line : lines)
		{
			assertTrue("'" + line + "' is " + metrics.stringWidth(line) + " px wide",
				metrics.stringWidth(line) <= TaskList.DESCRIPTION_WIDTH);
		}
		assertEquals(LONGEST, String.join(" ", lines));
		assertEquals(Collections.singletonList("Kill the Whisperer once."), TaskList.lines("  Kill the  Whisperer once. ", metrics));
	}
}
