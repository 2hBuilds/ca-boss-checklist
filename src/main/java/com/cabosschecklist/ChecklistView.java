package com.cabosschecklist;

import java.util.ArrayList;
import java.util.Collections;
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
		/** Null when the boss has no mark. */
		final Marks.Group group;

		Boss(int id, String label, String name, int total, int done, Mark mark, boolean green, Marks.Group group)
		{
			this.id = id;
			this.label = label;
			this.name = name;
			this.total = total;
			this.done = done;
			this.mark = mark;
			this.green = green;
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

		/** The second line of a Manual row; only for a marked boss. */
		String detail()
		{
			final String answer = mark.done() ? "Marked done" : "Marked not done";
			final int left = total - done;
			final String tasks;
			if (total == 0)
			{
				tasks = "no tasks";
			}
			else if (left == 0)
			{
				tasks = "all tasks done";
			}
			else
			{
				tasks = left == 1 ? "1 task left" : left + " tasks left";
			}
			return answer + " · " + tasks;
		}
	}

	/** The bosses in row order: by label ignoring case, then id. */
	final List<Boss> bosses;
	final boolean autoDetect;
	final int greenCount;
	final int disagreeCount;
	final int markCount;
	/** Each boss's task rows by boss id; empty from {@link #build}. */
	final Map<Integer, List<CaCompletion.TaskLine>> tasks;

	private ChecklistView(List<Boss> bosses, boolean autoDetect, int greenCount, int disagreeCount, int markCount,
		Map<Integer, List<CaCompletion.TaskLine>> tasks)
	{
		this.bosses = Collections.unmodifiableList(bosses);
		this.autoDetect = autoDetect;
		this.greenCount = greenCount;
		this.disagreeCount = disagreeCount;
		this.markCount = markCount;
		this.tasks = tasks;
	}

	/**
	 * One boss per entry of {@code labels}; marks for ids not in {@code labels} are ignored everywhere. {@code names} are
	 * the game's long names; a boss with none (or a blank one) takes its label for its name.
	 */
	static ChecklistView build(Map<Integer, String> labels, Map<Integer, String> names,
		Map<Integer, CaCompletion.Progress> progress, Map<Integer, Mark> marks, boolean autoDetect)
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
		int disagrees = 0;
		int marked = 0;
		for (int id : ids)
		{
			final CaCompletion.Progress counts = progress.get(id);
			final int total = counts == null ? 0 : counts.total;
			final int done = counts == null ? 0 : counts.done;
			final Mark mark = marks.get(id);
			final Marks.Group group = mark == null ? null : Marks.groupOf(mark, total, done, autoDetect);
			final boolean green = Marks.isGreen(id, autoDetect, complete, marks);
			final String longName = names.get(id);
			final String name = longName == null || longName.trim().isEmpty() ? labels.get(id) : longName;
			bosses.add(new Boss(id, labels.get(id), name, total, done, mark, green, group));
			greens += green ? 1 : 0;
			disagrees += group == Marks.Group.DISAGREE ? 1 : 0;
			marked += mark == null ? 0 : 1;
		}
		return new ChecklistView(bosses, autoDetect, greens, disagrees, marked, Collections.emptyMap());
	}

	/** A copy of this view that holds {@code lines} as the bosses' task rows. */
	ChecklistView withTasks(Map<Integer, List<CaCompletion.TaskLine>> lines)
	{
		return new ChecklistView(bosses, autoDetect, greenCount, disagreeCount, markCount, lines);
	}

	/** The boss's task rows in list order; empty when it has none. */
	List<CaCompletion.TaskLine> tasksOf(int id)
	{
		final List<CaCompletion.TaskLine> lines = tasks.get(id);
		return lines == null ? Collections.emptyList() : lines;
	}

	/** The card's source line: where the boss's green comes from. */
	String sourceLine(Boss boss)
	{
		if (boss.mark != null)
		{
			return "Marked by you";
		}
		return autoDetect ? "From your account" : "Tick to show it green";
	}

	/** The status card's bold line. */
	String headline()
	{
		return CaCompletion.statusHeadline(greenCount, bosses.size());
	}

	/** The status card's grey line, without the Review link. */
	String helper()
	{
		if (disagreeCount > 0)
		{
			return disagreeCount == 1 ? "1 mark disagrees" : disagreeCount + " marks disagree";
		}
		return autoDetect ? "Shown green in the Tasks menu" : "Only marked bosses show green";
	}

	/** Whether the card offers the Review link. */
	boolean showsReview()
	{
		return disagreeCount > 0;
	}

	/** The toggle's second word. */
	String manualWord()
	{
		return markCount == 0 ? "Manual" : "Manual (" + markCount + ")";
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
}
