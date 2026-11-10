package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class CaCompletionTest
{
	private static Set<Integer> set(Integer... ids)
	{
		return new HashSet<>(Arrays.asList(ids));
	}

	/** The monsters shown green from the account: the complete ones of {@link CaCompletion#progress}. */
	private static Set<Integer> complete(List<CaCompletion.Task> tasks, int[] varps, boolean deadman, int groupSize)
	{
		return CaCompletion.completeIds(CaCompletion.progress(tasks, varps, deadman, groupSize));
	}

	private static int[] varps(int fill)
	{
		final int[] v = new int[21];
		Arrays.fill(v, fill);
		return v;
	}

	private static List<CaCompletion.Task> dkPrime()
	{
		return Arrays.asList(
			new CaCompletion.Task(197, 19, 0, 0),
			new CaCompletion.Task(198, 19, 0, 0),
			new CaCompletion.Task(199, 19, 0, 0),
			new CaCompletion.Task(200, 19, 0, 0));
	}

	private static List<CaCompletion.Task> tob()
	{
		return Arrays.asList(
			new CaCompletion.Task(255, 55, 2, 0),
			new CaCompletion.Task(237, 55, 0, 0));
	}

	/** Task 255 is varp 7 bit 31 and task 237 is varp 7 bit 13. */
	private static int[] tobVarps()
	{
		final int[] v = new int[21];
		v[237 / 32] |= 1 << (237 % 32);
		return v;
	}

	@Test
	public void taskIdPicksVarpAndBit()
	{
		final int[] v = new int[21];
		v[0] = 1;
		assertTrue(CaCompletion.isTaskDone(0, v));
		assertFalse(CaCompletion.isTaskDone(1, v));
		final int[] w = new int[21];
		w[6] = 1 << 5;
		assertTrue(CaCompletion.isTaskDone(197, w));
		final int[] x = new int[21];
		x[20] = 1 << 14;
		assertTrue(CaCompletion.isTaskDone(654, x));
		final int[] all = varps(-1);
		assertFalse(CaCompletion.isTaskDone(672, all));
		assertFalse(CaCompletion.isTaskDone(-1, all));
	}

	@Test
	public void dkPrimeCompleteOnlyWithAllFour()
	{
		final int[] v = new int[21];
		v[6] = 0x1E0;
		assertEquals(set(19), complete(dkPrime(), v, false, 0));
		v[6] = 0xE0;
		assertEquals(Collections.emptySet(), complete(dkPrime(), v, false, 0));
	}

	@Test
	public void monsterWithNoTasksNeverComplete()
	{
		assertFalse(complete(dkPrime(), varps(-1), false, 0).contains(26));
	}

	@Test
	public void gimGroupTooSmallSkipsTask()
	{
		assertEquals(set(55), complete(tob(), tobVarps(), false, 2));
		assertEquals(Collections.emptySet(), complete(tob(), tobVarps(), false, 3));
	}

	@Test
	public void gimRuleOnlyForGroupsOf2To5()
	{
		for (int size : new int[] {0, 1, 6})
		{
			assertEquals(Collections.emptySet(), complete(tob(), tobVarps(), false, size));
		}
	}

	@Test
	public void deadmanUsesOnlyItsFlag()
	{
		// Task 624 is done; task 625 is not done but skipped on Deadman, so the monster is complete.
		final List<CaCompletion.Task> skipped = Arrays.asList(new CaCompletion.Task(624, 51, 0, 0), new CaCompletion.Task(625, 51, 0, 1));
		final int[] done = new int[21];
		done[624 / 32] |= 1 << (624 % 32);
		assertEquals(set(51), complete(skipped, done, true, 0));
		final List<CaCompletion.Task> counted = Arrays.asList(new CaCompletion.Task(625, 51, 5, 0));
		assertEquals(Collections.emptySet(), complete(counted, new int[21], true, 3));
	}

	@Test
	public void allSkippedMeansNotComplete()
	{
		final List<CaCompletion.Task> only = Arrays.asList(new CaCompletion.Task(255, 55, 2, 0));
		assertEquals(Collections.emptySet(), complete(only, new int[21], false, 2));
	}

	@Test
	public void rowIndexToId()
	{
		assertEquals(-1, CaCompletion.idForRow(0));
		assertEquals(0, CaCompletion.idForRow(1));
		assertEquals(19, CaCompletion.idForRow(20));
		assertEquals(92, CaCompletion.idForRow(93));
	}

	@Test
	public void progressCountsCountedTasks()
	{
		final int[] v = new int[21];
		v[6] = 0xE0;
		final CaCompletion.Progress partial = CaCompletion.progress(dkPrime(), v, false, 0).get(19);
		assertEquals(4, partial.total);
		assertEquals(3, partial.done);
		assertFalse(partial.complete());
		v[6] = 0x1E0;
		assertTrue(CaCompletion.progress(dkPrime(), v, false, 0).get(19).complete());
		final CaCompletion.Progress raid = CaCompletion.progress(tob(), tobVarps(), false, 2).get(55);
		assertEquals(1, raid.total);
		assertEquals(1, raid.done);
		assertTrue(raid.complete());
		assertNull(CaCompletion.progress(dkPrime(), varps(-1), false, 0).get(26));
	}

	@Test
	public void headlineCounts()
	{
		assertEquals("27 of 92 done", CaCompletion.statusHeadline(27, 92));
		assertEquals("0 of 92 done", CaCompletion.statusHeadline(0, 92));
	}

	private static Map<Integer, CaCompletion.TaskText> primeTexts()
	{
		final Map<Integer, CaCompletion.TaskText> texts = new HashMap<>();
		texts.put(197, new CaCompletion.TaskText("Dagannoth Prime Champion", "Kill Dagannoth Prime 10 times.", 2, 3));
		texts.put(198, new CaCompletion.TaskText("Dagannoth Prime Adept", "Kill Dagannoth Prime 25 times.", 3, 3));
		texts.put(199, new CaCompletion.TaskText("Death to the Seer King",
			"Kill Dagannoth Prime whilst under attack by Dagannoth Supreme and Dagannoth Rex.", 4, 4));
		texts.put(200, new CaCompletion.TaskText("From One King to Another",
			"Kill Prime using a Rune Thrownaxe special attack, bounced off Dagannoth Rex.", 4, 4));
		return texts;
	}

	private static Map<Integer, String> tierWords()
	{
		final Map<Integer, String> words = new HashMap<>();
		words.put(2, "Medium");
		words.put(3, "Hard");
		words.put(4, "Elite");
		return words;
	}

	private static Map<Integer, String> typeWords()
	{
		final Map<Integer, String> words = new HashMap<>();
		words.put(3, "Kill Count");
		words.put(4, "Mechanical");
		return words;
	}

	@Test
	public void taskLinesInTierThenNameOrder()
	{
		final int[] v = new int[21];
		v[6] = 0xA0;
		final List<CaCompletion.TaskLine> lines =
			CaCompletion.taskLines(dkPrime(), primeTexts(), tierWords(), typeWords(), v, false, 0).get(19);
		assertEquals(4, lines.size());
		assertEquals("Dagannoth Prime Champion", lines.get(0).name);
		assertTrue(lines.get(0).done);
		assertEquals("Medium \u00b7 Kill Count", lines.get(0).detail());
		assertEquals("Dagannoth Prime Adept", lines.get(1).name);
		assertFalse(lines.get(1).done);
		assertEquals("Hard \u00b7 Kill Count", lines.get(1).detail());
		assertEquals("Death to the Seer King", lines.get(2).name);
		assertTrue(lines.get(2).done);
		assertEquals("Elite \u00b7 Mechanical", lines.get(2).detail());
		assertEquals("From One King to Another", lines.get(3).name);
		assertFalse(lines.get(3).done);
		assertEquals("Elite \u00b7 Mechanical", lines.get(3).detail());
		assertEquals("Kill Dagannoth Prime 10 times.", lines.get(0).description);
		assertEquals("Kill Dagannoth Prime 25 times.", lines.get(1).description);
		assertEquals("Kill Dagannoth Prime whilst under attack by Dagannoth Supreme and Dagannoth Rex.", lines.get(2).description);
		assertEquals("Kill Prime using a Rune Thrownaxe special attack, bounced off Dagannoth Rex.", lines.get(3).description);
	}

	@Test
	public void skippedTasksAreNotListed()
	{
		final Map<Integer, CaCompletion.TaskText> texts = new HashMap<>();
		texts.put(255, new CaCompletion.TaskText("Theatre (Trio) Speed-Chaser", "d", 5, 6));
		texts.put(237, new CaCompletion.TaskText("Theatre of Blood Veteran", "a\tb", 4, 3));
		final List<CaCompletion.TaskLine> lines =
			CaCompletion.taskLines(tob(), texts, tierWords(), typeWords(), tobVarps(), false, 2).get(55);
		assertEquals(1, lines.size());
		assertEquals("Theatre of Blood Veteran", lines.get(0).name);
		assertEquals("a b", lines.get(0).description);
	}

	@Test
	public void almostDoneMeansOneOrTwoLeftAfterOne()
	{
		assertTrue(CaCompletion.isAlmostDone(new CaCompletion.Progress(8, 6)));
		assertTrue(CaCompletion.isAlmostDone(new CaCompletion.Progress(8, 7)));
		assertFalse(CaCompletion.isAlmostDone(new CaCompletion.Progress(8, 8)));
		assertFalse(CaCompletion.isAlmostDone(new CaCompletion.Progress(8, 5)));
		assertFalse(CaCompletion.isAlmostDone(new CaCompletion.Progress(2, 0)));
		assertTrue(CaCompletion.isAlmostDone(new CaCompletion.Progress(3, 1)));
		assertFalse(CaCompletion.isAlmostDone(new CaCompletion.Progress(0, 0)));
		assertFalse(CaCompletion.isAlmostDone(null));
	}

	@Test
	public void shadeAllIsPlain()
	{
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(0, new CaCompletion.Progress(3, 3));
		final Map<Integer, Mark> marks = new HashMap<>();
		marks.put(0, Mark.DONE);
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(0, true, true, set(0), marks, progress));
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(-1, true, true, set(-1), marks, progress));
	}

	@Test
	public void shadeMarkWins()
	{
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(7, new CaCompletion.Progress(4, 2));
		final Map<Integer, Mark> done = new HashMap<>();
		done.put(7, Mark.DONE);
		assertEquals(CaCompletion.Shade.GREEN, CaCompletion.shadeOf(7, true, false, set(), done, progress));
		progress.put(8, new CaCompletion.Progress(2, 2));
		final Map<Integer, Mark> notDone = new HashMap<>();
		notDone.put(8, Mark.NOT_DONE);
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(8, true, true, set(8), notDone, progress));
	}

	@Test
	public void shadeFromAccount()
	{
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(7, new CaCompletion.Progress(2, 2));
		final Map<Integer, Mark> none = Collections.emptyMap();
		assertEquals(CaCompletion.Shade.GREEN, CaCompletion.shadeOf(7, true, true, set(7), none, progress));
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(7, false, true, set(7), none, progress));
	}

	@Test
	public void shadeAlmostDone()
	{
		final Map<Integer, Mark> none = Collections.emptyMap();
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(1, new CaCompletion.Progress(3, 2));
		progress.put(2, new CaCompletion.Progress(2, 0));
		progress.put(3, new CaCompletion.Progress(5, 2));
		assertEquals(CaCompletion.Shade.YELLOW, CaCompletion.shadeOf(1, true, true, set(), none, progress));
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(1, true, false, set(), none, progress));
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(2, true, true, set(), none, progress));
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(3, true, true, set(), none, progress));
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(1, true, true, set(), none, null));
	}

	@Test
	public void showsLabelInFullOrCut()
	{
		assertTrue(CaCompletion.showsLabel("Fire Giants", "Fire Giants"));
		assertTrue(CaCompletion.showsLabel("Corporeal Be...", "Corporeal Beast"));
		assertTrue(CaCompletion.showsLabel("Chambers of ...", "Chambers of Xeric"));
		assertFalse(CaCompletion.showsLabel("Monster:", "Fire Giants"));
		assertFalse(CaCompletion.showsLabel("Fire Giants", "Giant Mole"));
		assertFalse(CaCompletion.showsLabel("...", "Fire Giants"));
		assertFalse(CaCompletion.showsLabel("", "Fire Giants"));
		assertFalse(CaCompletion.showsLabel(null, "Fire Giants"));
	}
}
