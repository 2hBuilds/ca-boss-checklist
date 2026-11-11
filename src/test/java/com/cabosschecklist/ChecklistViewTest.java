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
		final ChecklistView view = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(72, Mark.NOT_DONE), true, false, Collections.emptyMap());
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
		final ChecklistView on = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(72, Mark.NOT_DONE), true, false, Collections.emptyMap());
		assertEquals("1 of 5 done", on.headline());
		assertEquals("Shown green in the Tasks menu", on.helper());
		final ChecklistView off = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(72, Mark.NOT_DONE), false, false, Collections.emptyMap());
		assertEquals("0 of 5 done", off.headline());
		assertEquals("Only marked bosses show green", off.helper());
	}

	@Test
	public void aMarkNeverChangesTheHelper()
	{
		final Map<Integer, Mark> some = marks(5, Mark.DONE, 72, Mark.NOT_DONE);
		final ChecklistView on = ChecklistView.build(labels(), Collections.emptyMap(), progress(), some, true, false, Collections.emptyMap());
		assertEquals("Shown green in the Tasks menu", on.helper());
		final ChecklistView off = ChecklistView.build(labels(), Collections.emptyMap(), progress(), some, false, false, Collections.emptyMap());
		assertEquals("Only marked bosses show green", off.helper());
	}

	private static ChecklistView groupsView()
	{
		return ChecklistView.build(labels(), Collections.emptyMap(), progress(),
			marks(5, Mark.DONE, 19, Mark.DONE, 26, Mark.DONE, 72, Mark.NOT_DONE), true, false, Collections.emptyMap());
	}

	@Test
	public void manualGroups()
	{
		final ChecklistView view = groupsView();
		assertEquals(Arrays.asList("Barrows", "Fractured Archive, The", "Zulrah"),
			names(view.group(Marks.Group.DIFFERENT)));
		assertEquals(Arrays.asList("DK: Prime"), names(view.group(Marks.Group.SAME)));
		assertEquals(Arrays.asList("Barrows", "DK: Prime", "Fractured Archive, The", "Zulrah"), names(view.marked()));
		assertEquals(4, view.markCount);
		assertEquals("Manual (4)", view.manualWord());
	}

	@Test
	public void accountWords()
	{
		final ChecklistView view = groupsView();
		assertEquals("2 of 6 done", boss(view, "Barrows").accountText());
		assertEquals("Your account: 4 tasks left", boss(view, "Barrows").accountLine());
		assertEquals("4 of 4 done", boss(view, "DK: Prime").accountText());
		assertEquals("Your account: all tasks done", boss(view, "DK: Prime").accountLine());
		assertEquals("no tasks yet", boss(view, "Fractured Archive, The").accountText());
		assertEquals("Your account: no tasks yet", boss(view, "Fractured Archive, The").accountLine());
		final Map<Integer, CaCompletion.Progress> nearly = progress();
		nearly.put(5, new CaCompletion.Progress(6, 5));
		final ChecklistView one = ChecklistView.build(labels(), Collections.emptyMap(), nearly,
			marks(5, Mark.DONE), true, false, Collections.emptyMap());
		assertEquals("5 of 6 done", boss(one, "Barrows").accountText());
		assertEquals("Your account: 1 task left", boss(one, "Barrows").accountLine());
	}

	@Test
	public void unmarkedShadeFollowsTheSwitch()
	{
		final Map<Integer, CaCompletion.Progress> nearly = progress();
		nearly.put(5, new CaCompletion.Progress(6, 5));
		final ChecklistView on = ChecklistView.build(labels(), Collections.emptyMap(), nearly,
			marks(5, Mark.DONE, 72, Mark.NOT_DONE), true, true, Collections.emptyMap());
		assertEquals(CaCompletion.Shade.GREEN, boss(on, "Barrows").shade);
		assertEquals(CaCompletion.Shade.YELLOW, boss(on, "Barrows").unmarkedShade);
		assertEquals(CaCompletion.Shade.PLAIN, boss(on, "Zulrah").shade);
		assertEquals(CaCompletion.Shade.GREEN, boss(on, "Zulrah").unmarkedShade);
		assertEquals(CaCompletion.Shade.PLAIN, boss(on, "Abyssal Sire").shade);
		assertEquals(CaCompletion.Shade.PLAIN, boss(on, "Abyssal Sire").unmarkedShade);
		final ChecklistView off = ChecklistView.build(labels(), Collections.emptyMap(), nearly,
			marks(5, Mark.DONE, 72, Mark.NOT_DONE), true, false, Collections.emptyMap());
		assertEquals(CaCompletion.Shade.PLAIN, boss(off, "Barrows").unmarkedShade);
		final ChecklistView unfollowed = ChecklistView.build(labels(), Collections.emptyMap(), nearly,
			marks(19, Mark.DONE), false, true, Collections.emptyMap());
		assertEquals(CaCompletion.Shade.GREEN, boss(unfollowed, "DK: Prime").shade);
		assertEquals(CaCompletion.Shade.PLAIN, boss(unfollowed, "DK: Prime").unmarkedShade);
		assertEquals(CaCompletion.Shade.PLAIN, boss(unfollowed, "Zulrah").shade);
		assertEquals(CaCompletion.Shade.PLAIN, boss(unfollowed, "Zulrah").unmarkedShade);
	}

	@Test
	public void noteWords()
	{
		final Map<Integer, Mark> some = marks(5, Mark.DONE, 19, Mark.DONE, 72, Mark.NOT_DONE);
		final ChecklistView on = ChecklistView.build(labels(), Collections.emptyMap(), progress(), some, true, false,
			Collections.emptyMap());
		assertTrue(boss(on, "Barrows").overrides());
		assertEquals("Green", boss(on, "Barrows").shownWord());
		assertEquals("orange", boss(on, "Barrows").unmarkedWord());
		assertTrue(boss(on, "Zulrah").overrides());
		assertEquals("Orange", boss(on, "Zulrah").shownWord());
		assertEquals("green", boss(on, "Zulrah").unmarkedWord());
		assertFalse(boss(on, "DK: Prime").overrides());
		assertEquals("Green", boss(on, "DK: Prime").shownWord());
		final Map<Integer, CaCompletion.Progress> nearly = progress();
		nearly.put(5, new CaCompletion.Progress(6, 5));
		final ChecklistView yellow = ChecklistView.build(labels(), Collections.emptyMap(), nearly, some, true, true,
			Collections.emptyMap());
		assertEquals("yellow", boss(yellow, "Barrows").unmarkedWord());
		final ChecklistView off = ChecklistView.build(labels(), Collections.emptyMap(), progress(), some, false, false,
			Collections.emptyMap());
		assertTrue(boss(off, "DK: Prime").overrides());
		assertEquals("orange", boss(off, "DK: Prime").unmarkedWord());
		assertFalse(boss(off, "Zulrah").overrides());
		assertEquals("Orange", boss(off, "Zulrah").shownWord());
	}

	@Test
	public void overridingAndSeen()
	{
		final Map<Integer, Mark> some = marks(5, Mark.DONE, 19, Mark.DONE, 26, Mark.DONE, 72, Mark.NOT_DONE);
		final Map<Integer, Mark> three = marks(5, Mark.DONE, 26, Mark.DONE, 72, Mark.NOT_DONE);
		final ChecklistView fresh = ChecklistView.build(labels(), Collections.emptyMap(), progress(), some, true, false,
			Collections.emptyMap());
		assertEquals(three, fresh.overriding());
		assertTrue(fresh.unseen());
		assertTrue(fresh.seenAfter(false).isEmpty());
		assertEquals(three, fresh.seenAfter(true));
		final ChecklistView part = ChecklistView.build(labels(), Collections.emptyMap(), progress(), some, true, false,
			marks(5, Mark.DONE, 19, Mark.DONE));
		assertTrue(part.unseen());
		assertEquals(marks(5, Mark.DONE), part.seenAfter(false));
		final ChecklistView all = ChecklistView.build(labels(), Collections.emptyMap(), progress(), some, true, false,
			three);
		assertFalse(all.unseen());
		assertEquals(all.seen, all.seenAfter(false));
		final ChecklistView off = ChecklistView.build(labels(), Collections.emptyMap(), progress(), some, false, false,
			marks(5, Mark.DONE, 19, Mark.DONE));
		assertTrue(off.overriding().isEmpty());
		assertFalse(off.unseen());
		assertEquals(off.seen, off.seenAfter(true));
		assertEquals(off.seen, off.seenAfter(false));
		assertTrue(fresh.withTasks(Collections.emptyMap()).unseen());
		assertFalse(all.withTasks(Collections.emptyMap()).unseen());
	}

	@Test
	public void marksForUnknownIdsAreIgnored()
	{
		final ChecklistView view = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(99, Mark.DONE), true, false, Collections.emptyMap());
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
		final ChecklistView view = ChecklistView.build(labels(), names, progress(), marks(), true, false, Collections.emptyMap());
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
		final ChecklistView on = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(5, Mark.DONE), true, false, Collections.emptyMap());
		assertEquals("Marked by you", on.sourceLine(boss(on, "Barrows")));
		assertEquals("From your account", on.sourceLine(boss(on, "Zulrah")));
		final ChecklistView off = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(), false, false, Collections.emptyMap());
		assertEquals("Tick to show it green", off.sourceLine(boss(off, "Zulrah")));
	}

	/** A boss with {@code total} counted tasks of which {@code done} are done. */
	private static ChecklistView.Boss progressBoss(int total, int done)
	{
		final Map<Integer, String> one = new HashMap<>();
		one.put(5, "Barrows");
		final Map<Integer, CaCompletion.Progress> counts = new HashMap<>();
		counts.put(5, new CaCompletion.Progress(total, done));
		return ChecklistView.build(one, Collections.emptyMap(), counts, marks(), true, false, Collections.emptyMap()).bosses.get(0);
	}

	@Test
	public void tasksBelongToTheirBoss()
	{
		final List<CaCompletion.TaskLine> four = Arrays.asList(
			new CaCompletion.TaskLine(197, "Dagannoth Prime Champion", "Kill Dagannoth Prime 10 times.", "Medium", "Kill Count", true),
			new CaCompletion.TaskLine(198, "Dagannoth Prime Adept", "Kill Dagannoth Prime 25 times.", "Hard", "Kill Count", false),
			new CaCompletion.TaskLine(199, "Death to the Seer King", "Kill Dagannoth Prime whilst under attack.", "Elite", "Mechanical", true),
			new CaCompletion.TaskLine(200, "From One King to Another", "Kill Prime using a Rune Thrownaxe.", "Elite", "Mechanical", false));
		final ChecklistView base = ChecklistView.build(labels(), Collections.emptyMap(), progress(), marks(72, Mark.NOT_DONE), true, false, Collections.emptyMap());
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

	@Test
	public void bossShadeFollowsTheRules()
	{
		final Map<Integer, String> labels = new HashMap<>();
		labels.put(1, "Done Boss");
		labels.put(2, "Close Boss");
		labels.put(3, "Far Boss");
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(1, new CaCompletion.Progress(4, 4));
		progress.put(2, new CaCompletion.Progress(4, 3));
		progress.put(3, new CaCompletion.Progress(8, 3));
		final Map<Integer, Mark> none = Collections.emptyMap();
		final ChecklistView on = ChecklistView.build(labels, Collections.emptyMap(), progress, none, true, true, Collections.emptyMap());
		assertEquals(CaCompletion.Shade.GREEN, boss(on, "Done Boss").shade);
		assertEquals(CaCompletion.Shade.YELLOW, boss(on, "Close Boss").shade);
		assertEquals(CaCompletion.Shade.PLAIN, boss(on, "Far Boss").shade);
		final ChecklistView old = ChecklistView.build(labels, Collections.emptyMap(), progress, none, true, false, Collections.emptyMap());
		assertEquals(CaCompletion.Shade.PLAIN, boss(old, "Close Boss").shade);
	}
}
