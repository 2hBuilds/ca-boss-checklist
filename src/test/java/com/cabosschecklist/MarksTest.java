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
		final Map<Integer, Mark> parsed = Marks.parse("19=done,55=not-done-kept,72=done-kept,5=not-done");
		assertEquals(map(5, Mark.NOT_DONE, 19, Mark.DONE, 55, Mark.NOT_DONE_KEPT, 72, Mark.DONE_KEPT), parsed);
		assertEquals("5=not-done,19=done,55=not-done-kept,72=done-kept", Marks.format(parsed));
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
		assertEquals(Mark.NOT_DONE_KEPT, Marks.flip(true, true, true));
		assertNull(Marks.flip(false, true, true));
		assertEquals(Mark.DONE_KEPT, Marks.flip(false, false, true));
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
		assertFalse(Marks.isGreen(19, true, set(19), map(19, Mark.NOT_DONE_KEPT)));
		assertFalse(Marks.isGreen(0, true, set(0), map(0, Mark.DONE)));
		assertFalse(Marks.isGreen(-1, true, set(), none));
	}

	@Test
	public void groupsFollowTheRule()
	{
		assertEquals(Marks.Group.YOURS, Marks.groupOf(Mark.DONE, 8, 5, false));
		assertEquals(Marks.Group.YOURS, Marks.groupOf(Mark.NOT_DONE, 8, 8, false));
		assertEquals(Marks.Group.DISAGREE, Marks.groupOf(Mark.DONE, 8, 5, true));
		assertEquals(Marks.Group.YOURS, Marks.groupOf(Mark.DONE_KEPT, 8, 5, true));
		assertEquals(Marks.Group.NOT_NEEDED, Marks.groupOf(Mark.DONE, 8, 8, true));
		assertEquals(Marks.Group.DISAGREE, Marks.groupOf(Mark.NOT_DONE, 8, 8, true));
		assertEquals(Marks.Group.YOURS, Marks.groupOf(Mark.NOT_DONE_KEPT, 8, 8, true));
		assertEquals(Marks.Group.NOT_NEEDED, Marks.groupOf(Mark.NOT_DONE, 8, 5, true));
		assertEquals(Marks.Group.YOURS, Marks.groupOf(Mark.DONE, 0, 0, true));
		assertEquals(Marks.Group.NOT_NEEDED, Marks.groupOf(Mark.NOT_DONE, 0, 0, true));
	}

	@Test
	public void keptAndTokens()
	{
		assertEquals(Mark.DONE_KEPT, Mark.DONE.asKept());
		assertEquals(Mark.NOT_DONE_KEPT, Mark.NOT_DONE.asKept());
		assertEquals(Mark.DONE_KEPT, Mark.DONE_KEPT.asKept());
		assertEquals(Mark.NOT_DONE_KEPT, Mark.of(false, true));
		assertEquals("done", Mark.DONE.token());
		assertEquals("not-done", Mark.NOT_DONE.token());
		assertEquals("done-kept", Mark.DONE_KEPT.token());
		assertEquals("not-done-kept", Mark.NOT_DONE_KEPT.token());
	}
}
