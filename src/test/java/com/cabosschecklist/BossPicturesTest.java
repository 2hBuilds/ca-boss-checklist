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

	@Test
	public void wikiUrlsFollowTheTable()
	{
		final String base = "https://oldschool.runescape.wiki/w/";
		assertEquals(base + "Abyssal_Sire#Combat_Achievements", BossPictures.wikiUrl(1, "Abyssal Sire"));
		assertEquals(base + "Chambers_of_Xeric/Challenge_Mode#Combat_Achievements",
			BossPictures.wikiUrl(14, "Chambers of Xeric: CM"));
		assertEquals(base + "K'ril_Tsutsaroth#Combat_Achievements", BossPictures.wikiUrl(36, "K'ril Tsutsaroth"));
		assertEquals(base + "Theatre_of_Blood:_Hard_Mode#Combat_Achievements",
			BossPictures.wikiUrl(56, "Theatre of Blood: Hard Mode"));
		assertEquals(base + "Combat_Achievements", BossPictures.wikiUrl(76, "Other"));
	}
}
