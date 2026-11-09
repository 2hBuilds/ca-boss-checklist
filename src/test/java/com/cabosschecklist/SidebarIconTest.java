package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** {@link CaBossChecklistConfig.SidebarIcon}: the words the player reads, and the default. */
public class SidebarIconTest
{
	@Test
	public void wordsAndDefault()
	{
		final CaBossChecklistConfig.SidebarIcon[] all = CaBossChecklistConfig.SidebarIcon.values();
		assertEquals(3, all.length);
		assertEquals("2h coin and book", all[0].toString());
		assertEquals("Book only", all[1].toString());
		assertEquals("2h coin and CA letters", all[2].toString());
		assertEquals(CaBossChecklistConfig.SidebarIcon.COIN_AND_BOOK, new CaBossChecklistConfig()
		{
		}.sidebarIcon());
	}

	@Test
	public void almostDoneIsOnByDefault()
	{
		assertTrue(new CaBossChecklistConfig()
		{
		}.almostDone());
	}
}
