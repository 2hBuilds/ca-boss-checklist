package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class MarksTest
{
	private static Set<Integer> set(Integer... ids)
	{
		return new HashSet<>(Arrays.asList(ids));
	}

	private static Map<Integer, Mark> map(Object... pairs)
	{
		final Map<Integer, Mark> result = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			result.put((Integer) pairs[i], (Mark) pairs[i + 1]);
		}
		return result;
	}

	@Test
	public void parseAndFormatRoundTrip()
	{
		final Map<Integer, Mark> parsed = Marks.parse("3=done,7=not-done");
		assertEquals(map(3, Mark.DONE, 7, Mark.NOT_DONE), parsed);
		assertEquals("3=done,7=not-done", Marks.format(parsed));
	}

	@Test
	public void oldKeptWordsReadAsPlain()
	{
		final Map<Integer, Mark> parsed = Marks.parse("3=done-kept,4=not-done-kept");
		assertEquals(map(3, Mark.DONE, 4, Mark.NOT_DONE), parsed);
		assertEquals("3=done,4=not-done", Marks.format(parsed));
	}

	@Test
	public void parseSkipsJunk()
	{
		assertTrue(Marks.parse(null).isEmpty());
		assertTrue(Marks.parse("").isEmpty());
		assertEquals(map(20, Mark.DONE), Marks.parse("x=done,0=done,-3=done,19=maybe,19,=done, 20 = done "));
		assertEquals(map(19, Mark.NOT_DONE), Marks.parse("19=done,19=not-done"));
	}

	@Test
	public void formatEmptyIsEmpty()
	{
		assertEquals("", Marks.format(Collections.<Integer, Mark>emptyMap()));
	}

	@Test
	public void flipFollowsTheRule()
	{
		assertEquals(Mark.NOT_DONE, Marks.flip(true, true, true));
		assertNull(Marks.flip(false, true, true));
		assertEquals(Mark.DONE, Marks.flip(false, false, true));
		assertNull(Marks.flip(true, false, true));
		assertEquals(Mark.DONE, Marks.flip(false, true, false));
		assertNull(Marks.flip(true, true, false));
	}

	@Test
	public void greenFromAutoOrMark()
	{
		final Map<Integer, Mark> none = Collections.emptyMap();
		assertTrue(Marks.isGreen(19, true, set(19), none));
		assertFalse(Marks.isGreen(19, false, set(19), none));
		assertTrue(Marks.isGreen(19, false, set(), map(19, Mark.DONE)));
		assertFalse(Marks.isGreen(19, true, set(19), map(19, Mark.NOT_DONE)));
		assertFalse(Marks.isGreen(0, true, set(0), map(0, Mark.DONE)));
		assertFalse(Marks.isGreen(-1, true, set(), none));
	}

	@Test
	public void groupsFollowTheRule()
	{
		assertEquals(Marks.Group.DIFFERENT, Marks.groupOf(Mark.DONE, 8, 5));
		assertEquals(Marks.Group.SAME, Marks.groupOf(Mark.DONE, 8, 8));
		assertEquals(Marks.Group.DIFFERENT, Marks.groupOf(Mark.NOT_DONE, 8, 8));
		assertEquals(Marks.Group.SAME, Marks.groupOf(Mark.NOT_DONE, 8, 5));
		assertEquals(Marks.Group.DIFFERENT, Marks.groupOf(Mark.DONE, 0, 0));
		assertEquals(Marks.Group.SAME, Marks.groupOf(Mark.NOT_DONE, 0, 0));
	}

	@Test
	public void tokensAndOf()
	{
		assertEquals("done", Mark.DONE.token());
		assertEquals("not-done", Mark.NOT_DONE.token());
		assertEquals(Mark.DONE, Mark.of(true));
		assertEquals(Mark.NOT_DONE, Mark.of(false));
		assertEquals(2, Mark.values().length);
	}

	@Test
	public void marksAreIgnoredWhileTheSidebarIsHidden()
	{
		final Map<Integer, Mark> marks = map(19, Mark.DONE);
		assertEquals(marks, Marks.inUse(marks, true));
		assertTrue(Marks.inUse(marks, false).isEmpty());
		assertFalse(Marks.isGreen(19, true, set(), Marks.inUse(map(19, Mark.DONE), false)));
		assertTrue(Marks.isGreen(19, true, set(19), Marks.inUse(map(19, Mark.NOT_DONE), false)));
	}

	@Test
	public void seenAfterFollowsTheRule()
	{
		final Map<Integer, Mark> seen = map(5, Mark.DONE, 9, Mark.DONE);
		final Map<Integer, Mark> overriding = map(5, Mark.DONE, 72, Mark.NOT_DONE);
		assertEquals(map(5, Mark.DONE), Marks.seenAfter(seen, overriding, false));
		assertEquals(overriding, Marks.seenAfter(seen, overriding, true));
		assertTrue(Marks.seenAfter(map(5, Mark.DONE), map(5, Mark.NOT_DONE), false).isEmpty());
		assertTrue(Marks.seenAfter(map(), map(), true).isEmpty());
	}

	@Test
	public void unseenFollowsTheRule()
	{
		assertFalse(Marks.hasUnseen(map(5, Mark.DONE), map(5, Mark.DONE)));
		assertTrue(Marks.hasUnseen(map(5, Mark.DONE), map(5, Mark.DONE, 72, Mark.NOT_DONE)));
		assertFalse(Marks.hasUnseen(map(), map()));
		assertTrue(Marks.hasUnseen(map(5, Mark.DONE), map(5, Mark.NOT_DONE)));
		assertFalse(Marks.hasUnseen(map(5, Mark.DONE, 9, Mark.DONE), map(5, Mark.DONE)));
	}
}
