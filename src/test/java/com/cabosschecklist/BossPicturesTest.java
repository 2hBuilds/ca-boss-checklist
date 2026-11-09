package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BossPicturesTest
{
	@Test
	public void titlesFollowTheTable()
	{
		assertEquals("Chambers of Xeric", BossPictures.titleFor(14, "Chambers of Xeric: CM"));
		assertEquals("Theatre of Blood", BossPictures.titleFor(54, "Theatre of Blood: Entry Mode"));
		assertEquals("Theatre of Blood", BossPictures.titleFor(56, "Theatre of Blood: Hard Mode"));
		assertNull(BossPictures.titleFor(76, "Other"));
		assertEquals("Vorkath", BossPictures.titleFor(67, "Vorkath"));
		assertNull(BossPictures.titleFor(93, ""));
		assertNull(BossPictures.titleFor(93, "  "));
		assertNull(BossPictures.titleFor(93, null));
	}
}
