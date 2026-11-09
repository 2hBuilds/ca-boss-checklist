package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class ChecklistViewTest
{
	private static Map<Integer, String> labels()
	{
		final Map<Integer, String> labels = new HashMap<>();
		labels.put(19, "DK: Prime");
		labels.put(5, "Barrows");
		labels.put(26, "Fractured Archive, The");
		labels.put(72, "Zulrah");
		labels.put(1, "Abyssal Sire");
		return labels;
	}

	private static Map<Integer, CaCompletion.Progress> progress()
	{
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(19, new CaCompletion.Progress(4, 4));
		progress.put(5, new CaCompletion.Progress(6, 2));
		progress.put(72, new CaCompletion.Progress(8, 8));
		progress.put(1, new CaCompletion.Progress(5, 0));
		return progress;
	}

	private static Map<Integer, Mark> marks(Object... pairs)
	{
		final Map<Integer, Mark> marks = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			marks.put((Integer) pairs[i], (Mark) pairs[i + 1]);
		}
		return marks;
	}

	private static List<String> names(List<ChecklistView.Boss> bosses)
	{
		final List<String> names = new ArrayList<>();
		for (ChecklistView.Boss boss : bosses)
		{
			names.add(boss.label);
		}
		return names;
	}

	private static ChecklistView.Boss boss(ChecklistView view, String label)
	{
		for (ChecklistView.Boss boss : view.bosses)
		{
			if (boss.label.equals(label))
			{
				return boss;
			}
		}
		throw new AssertionError(label);
	}

	@Test
	public void rowsSortedByLabelWithStatus()
	{
		final ChecklistView view = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(72, Mark.NOT_DONE_KEPT), true);
		assertEquals(Arrays.asList("Abyssal Sire", "Barrows", "DK: Prime", "Fractured Archive, The", "Zulrah"), names(view.bosses));
		final List<String> statuses = new ArrayList<>();
		final List<Boolean> greens = new ArrayList<>();
		for (ChecklistView.Boss boss : view.bosses)
		{
			statuses.add(boss.status());
			greens.add(boss.green);
		}
		assertEquals(Arrays.asList("5 left", "4 left", "Done", "No tasks", "Marked"), statuses);
		assertEquals(Arrays.asList(false, false, true, false, false), greens);
	}

	@Test
	public void headlineAndHelper()
	{
		final ChecklistView on = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(72, Mark.NOT_DONE_KEPT), true);
		assertEquals("1 of 5 done", on.headline());
		assertEquals("Shown green in the Tasks menu", on.helper());
		assertFalse(on.showsReview());
		final ChecklistView off = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(72, Mark.NOT_DONE_KEPT), false);
		assertEquals("0 of 5 done", off.headline());
		assertEquals("Only marked bosses show green", off.helper());
	}

	@Test
	public void disagreementsAreCountedAndWorded()
	{
		final ChecklistView two = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(5, Mark.DONE, 72, Mark.NOT_DONE), true);
		assertEquals(2, two.disagreeCount);
		assertEquals("2 marks disagree", two.helper());
		assertTrue(two.showsReview());
		final ChecklistView one = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(5, Mark.DONE), true);
		assertEquals("1 mark disagrees", one.helper());
		final ChecklistView off = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(5, Mark.DONE, 72, Mark.NOT_DONE), false);
		assertEquals(0, off.disagreeCount);
		assertEquals("Only marked bosses show green", off.helper());
		assertFalse(off.showsReview());
	}

	private static ChecklistView groupsView()
	{
		return ChecklistView.build(labels(), Collections.emptyMap(), progress(),
			marks(5, Mark.DONE, 19, Mark.DONE, 26, Mark.DONE, 72, Mark.NOT_DONE_KEPT), true);
	}

	@Test
	public void manualGroups()
	{
		final ChecklistView view = groupsView();
		assertEquals(Arrays.asList("Barrows"), names(view.group(Marks.Group.DISAGREE)));
		assertEquals(Arrays.asList("DK: Prime"), names(view.group(Marks.Group.NOT_NEEDED)));
		assertEquals(Arrays.asList("Fractured Archive, The", "Zulrah"), names(view.group(Marks.Group.YOURS)));
		assertEquals(4, view.markCount);
		assertEquals("Manual (4)", view.manualWord());
	}

	@Test
	public void detailWords()
	{
		final ChecklistView view = groupsView();
		assertEquals("Marked done · 4 tasks left", boss(view, "Barrows").detail());
		assertEquals("Marked done · all tasks done", boss(view, "DK: Prime").detail());
		assertEquals("Marked done · no tasks", boss(view, "Fractured Archive, The").detail());
		assertEquals("Marked not done · all tasks done", boss(view, "Zulrah").detail());
		final Map<Integer, CaCompletion.Progress> nearly = progress();
		nearly.put(5, new CaCompletion.Progress(6, 5));
		final ChecklistView one = ChecklistView.build(labels(), Collections.emptyMap(), nearly, marks(5, Mark.DONE), true);
		assertEquals("Marked done · 1 task left", boss(one, "Barrows").detail());
	}

	@Test
	public void marksForUnknownIdsAreIgnored()
	{
		final ChecklistView view = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(99, Mark.DONE), true);
		assertEquals(0, view.markCount);
		for (Marks.Group group : Marks.Group.values())
		{
			assertEquals(Collections.emptyList(), view.group(group));
		}
		assertEquals(2, view.greenCount);
		assertEquals("Manual", view.manualWord());
	}

	@Test
	public void namesAndSearch()
	{
		final Map<Integer, String> names = new HashMap<>();
		names.put(19, "Dagannoth Prime");
		names.put(5, " ");
		final ChecklistView view = ChecklistView.build(labels(), names, progress(), marks(), true);
		final ChecklistView.Boss prime = boss(view, "DK: Prime");
		assertEquals("Dagannoth Prime", prime.name);
		final ChecklistView.Boss barrows = boss(view, "Barrows");
		assertEquals("Barrows", barrows.name);
		assertTrue(prime.matches("dagan"));
		assertTrue(prime.matches("DK"));
		assertTrue(prime.matches(" prime "));
		assertTrue(prime.matches(""));
		assertFalse(prime.matches("zulrah"));
		assertTrue(barrows.matches("BARR"));
		for (ChecklistView.Boss boss : view.bosses)
		{
			assertTrue(boss.matches(null));
		}
	}

	@Test
	public void cardWords()
	{
		assertEquals("5 of 8 tasks done", progressBoss(8, 5).progressLine());
		assertEquals("8 of 8 tasks done", progressBoss(8, 8).progressLine());
		assertEquals("0 of 1 task done", progressBoss(1, 0).progressLine());
		assertEquals("No tasks yet", progressBoss(0, 0).progressLine());
		final ChecklistView on = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(5, Mark.DONE), true);
		assertEquals("Marked by you", on.sourceLine(boss(on, "Barrows")));
		assertEquals("From your account", on.sourceLine(boss(on, "Zulrah")));
		final ChecklistView off = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(), false);
		assertEquals("Tick to show it green", off.sourceLine(boss(off, "Zulrah")));
	}

	/** A boss with {@code total} counted tasks of which {@code done} are done. */
	private static ChecklistView.Boss progressBoss(int total, int done)
	{
		final Map<Integer, String> one = new HashMap<>();
		one.put(5, "Barrows");
		final Map<Integer, CaCompletion.Progress> counts = new HashMap<>();
		counts.put(5, new CaCompletion.Progress(total, done));
		return ChecklistView.build(one, Collections.emptyMap(), counts, marks(), true).bosses.get(0);
	}

	@Test
	public void tasksBelongToTheirBoss()
	{
		final List<CaCompletion.TaskLine> four = Arrays.asList(
			new CaCompletion.TaskLine(197, "Dagannoth Prime Champion", "Kill Dagannoth Prime 10 times.", "Medium", "Kill Count", true),
			new CaCompletion.TaskLine(198, "Dagannoth Prime Adept", "Kill Dagannoth Prime 25 times.", "Hard", "Kill Count", false),
			new CaCompletion.TaskLine(199, "Death to the Seer King", "Kill Dagannoth Prime whilst under attack.", "Elite", "Mechanical", true),
			new CaCompletion.TaskLine(200, "From One King to Another", "Kill Prime using a Rune Thrownaxe.", "Elite", "Mechanical", false));
		final ChecklistView base = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(72, Mark.NOT_DONE_KEPT), true);
		assertTrue(base.tasksOf(19).isEmpty());
		final Map<Integer, List<CaCompletion.TaskLine>> byBoss = new HashMap<>();
		byBoss.put(19, four);
		final ChecklistView copy = base.withTasks(byBoss);
		assertEquals(four, copy.tasksOf(19));
		assertTrue(copy.tasksOf(5).isEmpty());
		assertEquals(names(base.bosses), names(copy.bosses));
		assertEquals(base.headline(), copy.headline());
		assertEquals(base.helper(), copy.helper());
		assertEquals(base.manualWord(), copy.manualWord());
		assertEquals(base.greenCount, copy.greenCount);
		assertEquals(base.markCount, copy.markCount);
	}
}
