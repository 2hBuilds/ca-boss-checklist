package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** The icon swap: the old button goes first; the panel is opened again only after the swap, if still current. */
public class IconSwapTest
{
	private static final String OLD = "old";
	private static final String NEW = "new";

	/** Records the toolbar's calls and holds what was handed to {@code later}. */
	private static final class Fake
	{
		final List<String> calls = new ArrayList<>();
		final List<Runnable> queued = new ArrayList<>();
		String current = NEW;

		IconSwap<String> swap()
		{
			return new IconSwap<>(
				b -> calls.add("remove " + b),
				b -> calls.add("add " + b),
				b -> calls.add("open " + b),
				() -> current,
				queued::add);
		}
	}

	@Test
	public void theOldButtonGoesBeforeTheNewOneComes()
	{
		final Fake fake = new Fake();
		fake.swap().swap(OLD, NEW, false);
		assertEquals(Arrays.asList("remove old", "add new"), fake.calls);
	}

	@Test
	public void aShowingPanelIsOpenedAgainOnlyAfterTheSwap()
	{
		final Fake fake = new Fake();
		fake.swap().swap(OLD, NEW, true);
		assertEquals(Arrays.asList("remove old", "add new"), fake.calls);
		assertEquals(1, fake.queued.size());
		fake.queued.get(0).run();
		assertEquals(Arrays.asList("remove old", "add new", "open new"), fake.calls);
	}

	@Test
	public void aHiddenPanelIsNotOpened()
	{
		final Fake fake = new Fake();
		fake.swap().swap(OLD, NEW, false);
		assertTrue(fake.queued.isEmpty());
		assertEquals(2, fake.calls.size());
	}

	@Test
	public void aButtonThatIsNoLongerCurrentIsNotOpened()
	{
		final Fake changed = new Fake();
		changed.swap().swap(OLD, NEW, true);
		changed.current = "another";
		changed.queued.get(0).run();
		assertEquals(Arrays.asList("remove old", "add new"), changed.calls);

		final Fake gone = new Fake();
		gone.swap().swap(OLD, NEW, true);
		gone.current = null;
		gone.queued.get(0).run();
		assertEquals(Arrays.asList("remove old", "add new"), gone.calls);
	}
}
