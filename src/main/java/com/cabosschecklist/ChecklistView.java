package com.cabosschecklist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What the sidebar shows, built from immutable inputs on any thread, with no RuneLite or Swing types: the panel only
 * draws it.
 */
final class ChecklistView
{
	/** One boss of the game's Monster list, with its progress, its mark and whether it shows green. */
	static final class Boss
	{
		final int id;
		final String label;
		/** The game's long name for the boss (enum 3971), or the label when the game has none. */
		final String name;
		final int total;
		final int done;
		/** Null when the boss has no mark. */
		final Mark mark;
		final boolean green;
		/** The colour of the boss's name in the game's Monster list. */
		final CaCompletion.Shade shade;
		/** The colour the boss would have now without its mark. */
		final CaCompletion.Shade unmarkedShade;
		/** Null when the boss has no mark. */
		final Marks.Group group;

		Boss(int id, String label, String name, int total, int done, Mark mark, boolean green, CaCompletion.Shade shade,
			CaCompletion.Shade unmarkedShade, Marks.Group group)
		{
			this.id = id;
			this.label = label;
			this.name = name;
			this.total = total;
			this.done = done;
			this.mark = mark;
			this.green = green;
			this.shade = shade;
			this.unmarkedShade = unmarkedShade;
			this.group = group;
		}

		/**
		 * Whether the boss stays in the list for the search text: a blank or null query matches every boss, else the
		 * label or the name must contain the trimmed text, ignoring case.
		 */
		boolean matches(String query)
		{
			if (query == null || query.trim().isEmpty())
			{
				return true;
			}
			final String wanted = query.trim().toLowerCase(Locale.ROOT);
			return label.toLowerCase(Locale.ROOT).contains(wanted) || name.toLowerCase(Locale.ROOT).contains(wanted);
		}

		/** The card's progress line. */
		String progressLine()
		{
			if (total == 0)
			{
				return "No tasks yet";
			}
			return done + " of " + total + (total == 1 ? " task" : " tasks") + " done";
		}

		/** The word at the right of a Bosses row. */
		String status()
		{
			if (mark != null)
			{
				return "Marked";
			}
			if (total == 0)
			{
				return "No tasks";
			}
			return done == total ? "Done" : (total - done) + " left";
		}

		/** The account's progress in a Manual row: "done of total done", or "no tasks yet" for a boss with none. */
		String accountText()
		{
			return total == 0 ? "no tasks yet" : done + " of " + total + " done";
		}

		/** Whether the mark changes the colour the game shows: the shade differs from the unmarked one. */
		boolean overrides()
		{
			return shade != unmarkedShade;
		}

		/** The colour the game shows now, as a capitalised word. */
		String shownWord()
		{
			return word(shade, true);
		}

		/** The colour the boss would show without its mark, in lower case. */
		String unmarkedWord()
		{
			return word(unmarkedShade, false);
		}

		private static String word(CaCompletion.Shade shade, boolean capital)
		{
			final String word = shade == CaCompletion.Shade.GREEN ? "green"
				: shade == CaCompletion.Shade.YELLOW ? "yellow" : "orange";
			return capital ? Character.toUpperCase(word.charAt(0)) + word.substring(1) : word;
		}

		/** The tooltip of the account's progress in a Manual row. */
		String accountLine()
		{
			if (total == 0)
			{
				return "Your account: no tasks yet";
			}
			final int left = total - done;
			if (left == 0)
			{
				return "Your account: all tasks done";
			}
			return "Your account: " + (left == 1 ? "1 task left" : left + " tasks left");
		}
	}

	/** The bosses in row order: by label ignoring case, then id. */
	final List<Boss> bosses;
	final boolean autoDetect;
	final int greenCount;
	final int markCount;
	/** Each boss's task rows by boss id; empty from {@link #build}. */
	final Map<Integer, List<CaCompletion.TaskLine>> tasks;
	/** The overriding marks the player has looked at in the Manual tab. */
	final Map<Integer, Mark> seen;

	private ChecklistView(List<Boss> bosses, boolean autoDetect, int greenCount, int markCount,
		Map<Integer, List<CaCompletion.TaskLine>> tasks, Map<Integer, Mark> seen)
	{
		this.bosses = Collections.unmodifiableList(bosses);
		this.autoDetect = autoDetect;
		this.greenCount = greenCount;
		this.markCount = markCount;
		this.tasks = tasks;
		this.seen = seen;
	}

	/**
	 * One boss per entry of {@code labels}; marks for ids not in {@code labels} are ignored everywhere. {@code names} are
	 * the game's long names; a boss with none (or a blank one) takes its label for its name. {@code almostDone} lets a boss
	 * with one or two tasks left take the yellow shade. {@code seen} are the overriding marks the player has looked at.
	 */
	static ChecklistView build(Map<Integer, String> labels, Map<Integer, String> names,
		Map<Integer, CaCompletion.Progress> progress, Map<Integer, Mark> marks, boolean autoDetect, boolean almostDone,
		Map<Integer, Mark> seen)
	{
		final Set<Integer> complete = CaCompletion.completeIds(progress);
		final List<Integer> ids = new ArrayList<>(labels.keySet());
		ids.sort((a, b) ->
		{
			final int byLabel = String.CASE_INSENSITIVE_ORDER.compare(labels.get(a), labels.get(b));
			return byLabel != 0 ? byLabel : Integer.compare(a, b);
		});
		final List<Boss> bosses = new ArrayList<>();
		int greens = 0;
		int marked = 0;
		for (int id : ids)
		{
			final CaCompletion.Progress counts = progress.get(id);
			final int total = counts == null ? 0 : counts.total;
			final int done = counts == null ? 0 : counts.done;
			final Mark mark = marks.get(id);
			final Marks.Group group = mark == null ? null : Marks.groupOf(mark, total, done);
			final boolean green = Marks.isGreen(id, autoDetect, complete, marks);
			final String longName = names.get(id);
			final String name = longName == null || longName.trim().isEmpty() ? labels.get(id) : longName;
			final CaCompletion.Shade shade = CaCompletion.shadeOf(id, autoDetect, almostDone, complete, marks, progress);
			final CaCompletion.Shade unmarkedShade = CaCompletion.shadeOf(id, autoDetect, almostDone, complete,
				Collections.emptyMap(), progress);
			bosses.add(new Boss(id, labels.get(id), name, total, done, mark, green, shade, unmarkedShade, group));
			greens += green ? 1 : 0;
			marked += mark == null ? 0 : 1;
		}
		return new ChecklistView(bosses, autoDetect, greens, marked, Collections.emptyMap(),
			Collections.unmodifiableMap(new HashMap<>(seen)));
	}

	/** A copy of this view that holds {@code lines} as the bosses' task rows. */
	ChecklistView withTasks(Map<Integer, List<CaCompletion.TaskLine>> lines)
	{
		return new ChecklistView(bosses, autoDetect, greenCount, markCount, lines, seen);
	}

	/** The boss's task rows in list order; empty when it has none. */
	List<CaCompletion.TaskLine> tasksOf(int id)
	{
		final List<CaCompletion.TaskLine> lines = tasks.get(id);
		return lines == null ? Collections.emptyList() : lines;
	}

	/** What the boss card says under its box: a tag, some words, both or nothing. */
	enum CardNote
	{
		NONE(false, false, ""),
		HINT(false, false, "Tick to show it green"),
		OVERRIDES(true, true, "overrides your progress"),
		SAME(true, false, "same as your progress"),
		MARK(true, false, "");

		/** Whether the tag (see {@link MarkTag#TEXT}) shows. */
		final boolean tag;
		/** Whether the tag is drawn strong, in orange (the mark overrides), rather than in grey. */
		final boolean strong;
		/** The words after the tag; empty for none. */
		final String text;

		CardNote(boolean tag, boolean strong, String text)
		{
			this.tag = tag;
			this.strong = strong;
			this.text = text;
		}
	}

	/** What the card says about {@code boss}: nothing for an unmarked boss while the account is followed. */
	CardNote cardNote(Boss boss)
	{
		if (boss.mark == null)
		{
			return autoDetect ? CardNote.NONE : CardNote.HINT;
		}
		if (!autoDetect)
		{
			return CardNote.MARK;
		}
		return boss.group == Marks.Group.DIFFERENT ? CardNote.OVERRIDES : CardNote.SAME;
	}

	/** The status card's bold line. */
	String headline()
	{
		return CaCompletion.statusHeadline(greenCount, bosses.size());
	}

	/** The status card's grey line: it never changes with the marks. */
	String helper()
	{
		return autoDetect ? "Shown green in the Tasks menu" : "Only marked bosses show green";
	}

	/** The toggle's second word. */
	String manualWord()
	{
		return markCount == 0 ? "Manual" : "Manual (" + markCount + ")";
	}

	/** The marks of the group DIFFERENT by boss id while the account is followed; none otherwise. */
	Map<Integer, Mark> overriding()
	{
		final Map<Integer, Mark> result = new HashMap<>();
		if (autoDetect)
		{
			for (Boss boss : group(Marks.Group.DIFFERENT))
			{
				result.put(boss.id, boss.mark);
			}
		}
		return Collections.unmodifiableMap(result);
	}

	/** Whether some overriding mark has not been seen yet. */
	boolean unseen()
	{
		return autoDetect && Marks.hasUnseen(seen, overriding());
	}

	/** The seen marks after the player has ({@code looking}) or has not looked at the Manual tab. */
	Map<Integer, Mark> seenAfter(boolean looking)
	{
		return autoDetect ? Marks.seenAfter(seen, overriding(), looking) : seen;
	}

	/** The marked bosses in {@code group}, in row order. */
	List<Boss> group(Marks.Group group)
	{
		final List<Boss> result = new ArrayList<>();
		for (Boss boss : bosses)
		{
			if (boss.group == group)
			{
				result.add(boss);
			}
		}
		return result;
	}

	/** Every marked boss, in row order. */
	List<Boss> marked()
	{
		final List<Boss> result = new ArrayList<>();
		for (Boss boss : bosses)
		{
			if (boss.mark != null)
			{
				result.add(boss);
			}
		}
		return result;
	}
}
