package com.cabosschecklist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The game's Combat Achievement completion rules, with no RuneLite types so they are tested without a client. Each rule
 * names the game script it copies (Joshua-F/osrs-dumps 44e1ff8).
 */
final class CaCompletion
{
	private CaCompletion()
	{
	}

	/** One Combat Achievement task, with the four params the rules read. */
	static final class Task
	{
		final int taskId;
		final int monsterId;
		final int gimMaxGroupSize;
		final int deadmanFlag;
		/** The task's tier, 1 Easy to 6 Grandmaster; 0 when not known. */
		final int tier;

		Task(int taskId, int monsterId, int gimMaxGroupSize, int deadmanFlag)
		{
			this(taskId, monsterId, gimMaxGroupSize, deadmanFlag, 0);
		}

		Task(int taskId, int monsterId, int gimMaxGroupSize, int deadmanFlag, int tier)
		{
			this.taskId = taskId;
			this.monsterId = monsterId;
			this.gimMaxGroupSize = gimMaxGroupSize;
			this.deadmanFlag = deadmanFlag;
			this.tier = tier;
		}
	}

	/** Whether the task's bit is set: 32 tasks to a varp (proc script4834). */
	static boolean isTaskDone(int taskId, int[] completedVarps)
	{
		if (taskId < 0 || taskId / 32 >= completedVarps.length)
		{
			return false;
		}
		return ((completedVarps[taskId / 32] >>> (taskId % 32)) & 1) == 1;
	}

	/** Whether the game leaves the task out of the count (proc ca_task_is_disabled, 5243). */
	static boolean isSkipped(Task task, boolean deadmanWorld, int gimGroupSize)
	{
		if (deadmanWorld)
		{
			return task.deadmanFlag == 1;
		}
		if (gimGroupSize < 2 || gimGroupSize > 5)
		{
			return false;
		}
		return gimGroupSize <= task.gimMaxGroupSize;
	}

	/** The tier number of Grandmaster tasks. */
	static final int GRANDMASTER = 6;

	/** One monster's counted tasks and how many of them are done, and how many of each are Grandmaster tasks. */
	static final class Progress
	{
		final int total;
		final int done;
		/** How many of the counted tasks are Grandmaster tasks. */
		final int grandmasterTotal;
		/** How many of the Grandmaster tasks are done. */
		final int grandmasterDone;

		Progress(int total, int done)
		{
			this(total, done, 0, 0);
		}

		Progress(int total, int done, int grandmasterTotal, int grandmasterDone)
		{
			this.total = total;
			this.done = done;
			this.grandmasterTotal = grandmasterTotal;
			this.grandmasterDone = grandmasterDone;
		}

		/** Whether the monster has counted tasks and all are done. */
		boolean complete()
		{
			return total > 0 && done == total;
		}
	}

	/**
	 * Each monster that has a task in the list, with the count of its tasks the game does not skip and how many of those
	 * are done (proc ca_bosses_draw_list, 4800).
	 */
	static Map<Integer, Progress> progress(List<Task> tasks, int[] completedVarps, boolean deadmanWorld, int gimGroupSize)
	{
		final Map<Integer, int[]> counts = new HashMap<>();
		for (Task task : tasks)
		{
			final int[] count = counts.computeIfAbsent(task.monsterId, k -> new int[4]);
			if (isSkipped(task, deadmanWorld, gimGroupSize))
			{
				continue;
			}
			final boolean done = isTaskDone(task.taskId, completedVarps);
			count[0]++;
			count[1] += done ? 1 : 0;
			if (task.tier == GRANDMASTER)
			{
				count[2]++;
				count[3] += done ? 1 : 0;
			}
		}
		final Map<Integer, Progress> result = new HashMap<>();
		for (Map.Entry<Integer, int[]> entry : counts.entrySet())
		{
			final int[] count = entry.getValue();
			result.put(entry.getKey(), new Progress(count[0], count[1], count[2], count[3]));
		}
		return result;
	}

	/** One task's words from the game's task struct: name, description, tier number (1 to 6) and type number (1 to 6). */
	static final class TaskText
	{
		final String name;
		final String description;
		final int tier;
		final int type;

		TaskText(String name, String description, int tier, int type)
		{
			this.name = name;
			this.description = description;
			this.tier = tier;
			this.type = type;
		}
	}

	/** One row of a boss's task list: the task's words, its tier and type in words, and whether the account did it. */
	static final class TaskLine
	{
		final int taskId;
		final String name;
		final String description;
		final String tier;
		final String type;
		final boolean done;

		TaskLine(int taskId, String name, String description, String tier, String type, boolean done)
		{
			this.taskId = taskId;
			this.name = name;
			this.description = description;
			this.tier = tier;
			this.type = type;
			this.done = done;
		}

		/** The row's second line: the tier word, a dot, the type word. */
		String detail()
		{
			return tier + " \u00b7 " + type;
		}
	}

	/** A task's tier number and line, kept together while a monster's lines are sorted. */
	private static final class Sortable
	{
		final int tier;
		final TaskLine line;

		Sortable(int tier, TaskLine line)
		{
			this.tier = tier;
			this.line = line;
		}
	}

	/**
	 * Each monster's counted tasks (the ones {@link #isSkipped} leaves in) as rows, ordered by tier number, then name
	 * ignoring case, then task id. A task with no text gets an empty name and description; a missing tier or type word is
	 * "". Tabs in the texts become spaces. Choice: a monster whose tasks are all skipped is in the map with an empty list.
	 */
	static Map<Integer, List<TaskLine>> taskLines(List<Task> tasks, Map<Integer, TaskText> texts,
		Map<Integer, String> tierWords, Map<Integer, String> typeWords, int[] completedVarps, boolean deadmanWorld,
		int gimGroupSize)
	{
		final Map<Integer, List<Sortable>> byMonster = new HashMap<>();
		for (Task task : tasks)
		{
			final List<Sortable> list = byMonster.computeIfAbsent(task.monsterId, k -> new ArrayList<>());
			if (isSkipped(task, deadmanWorld, gimGroupSize))
			{
				continue;
			}
			final TaskText text = texts.get(task.taskId);
			final String name = text == null ? "" : spaced(text.name);
			final String description = text == null ? "" : spaced(text.description);
			final int tier = text == null ? 0 : text.tier;
			final String tierWord = text == null ? null : tierWords.get(text.tier);
			final String typeWord = text == null ? null : typeWords.get(text.type);
			list.add(new Sortable(tier, new TaskLine(task.taskId, name, description, tierWord == null ? "" : tierWord,
				typeWord == null ? "" : typeWord, isTaskDone(task.taskId, completedVarps))));
		}
		final Map<Integer, List<TaskLine>> result = new HashMap<>();
		for (Map.Entry<Integer, List<Sortable>> entry : byMonster.entrySet())
		{
			final List<Sortable> sorted = entry.getValue();
			sorted.sort((a, b) ->
			{
				if (a.tier != b.tier)
				{
					return Integer.compare(a.tier, b.tier);
				}
				final int byName = String.CASE_INSENSITIVE_ORDER.compare(a.line.name, b.line.name);
				return byName != 0 ? byName : Integer.compare(a.line.taskId, b.line.taskId);
			});
			final List<TaskLine> lines = new ArrayList<>();
			for (Sortable item : sorted)
			{
				lines.add(item.line);
			}
			result.put(entry.getKey(), Collections.unmodifiableList(lines));
		}
		return result;
	}

	private static String spaced(String text)
	{
		return text == null ? "" : text.replace('\t', ' ');
	}

	/** Whether the monster has done at least one counted task and has one or two left. */
	static boolean isAlmostDone(Progress progress)
	{
		if (progress == null)
		{
			return false;
		}
		final int left = progress.total - progress.done;
		return progress.done >= 1 && (left == 1 || left == 2);
	}

	/**
	 * Whether {@code progress} makes a name yellow under {@code rule}. Marks are not looked at here, and a monster that is
	 * complete (or has no counted task) is never yellow under any rule:
	 * <ul>
	 * <li>ONE_OR_TWO: at least one task done, one or two left;
	 * <li>STARTED: at least one task done, not all;
	 * <li>HALF: at least half the tasks done (done * 2 >= total), not all;
	 * <li>ONLY_GRANDMASTER: at least one task done, every task below Grandmaster done, at least one Grandmaster task not;
	 * <li>OFF: never.
	 * </ul>
	 */
	static boolean isYellow(CaBossChecklistConfig.YellowWhen rule, Progress progress)
	{
		if (rule == null || rule == CaBossChecklistConfig.YellowWhen.OFF || progress == null || progress.total < 1
			|| progress.done >= progress.total)
		{
			return false;
		}
		switch (rule)
		{
			case ONE_OR_TWO:
				return isAlmostDone(progress);
			case STARTED:
				return progress.done >= 1;
			case HALF:
				return progress.done * 2 >= progress.total;
			case ONLY_GRANDMASTER:
				return progress.done >= 1 && progress.grandmasterDone < progress.grandmasterTotal
					&& progress.done - progress.grandmasterDone == progress.total - progress.grandmasterTotal;
			default:
				return false;
		}
	}

	/**
	 * The monsters whose counted tasks are all done and number more than none (proc ca_bosses_draw_list, 4800, which
	 * colours a Bosses-tab name green).
	 */
	static Set<Integer> completeIds(Map<Integer, Progress> progress)
	{
		final Set<Integer> complete = new HashSet<>();
		for (Map.Entry<Integer, Progress> entry : progress.entrySet())
		{
			if (entry.getValue().complete())
			{
				complete.add(entry.getKey());
			}
		}
		return complete;
	}

	/** How a monster's name is drawn: the game's own colour, green, or yellow. */
	enum Shade
	{
		PLAIN, GREEN, YELLOW
	}

	/**
	 * The shade of a monster's name: green when {@link Marks#isGreen}; else yellow when {@link #isYellow} says so for
	 * {@code rule}; else plain. Id 0 ("All") and below are always plain; {@code progress} may be null.
	 */
	static Shade shadeOf(int id, boolean autoDetect, CaBossChecklistConfig.YellowWhen rule, Set<Integer> complete,
		Map<Integer, Mark> marks, Map<Integer, Progress> progress)
	{
		if (id < 1)
		{
			return Shade.PLAIN;
		}
		if (Marks.isGreen(id, autoDetect, complete, marks))
		{
			return Shade.GREEN;
		}
		if (progress != null && isYellow(rule, progress.get(id)))
		{
			return Shade.YELLOW;
		}
		return Shade.PLAIN;
	}

	/**
	 * Whether a box's text shows {@code label}: the label itself, or the label cut short with "..." (the game's
	 * ca_task_filter_updatetext cuts it to the box's width). A blank text, or a lone "...", never does.
	 */
	static boolean showsLabel(String text, String label)
	{
		if (text == null || label == null || text.trim().isEmpty())
		{
			return false;
		}
		if (text.equals(label))
		{
			return true;
		}
		if (text.endsWith("...") && text.length() > 3)
		{
			final String start = text.substring(0, text.length() - 3).trim();
			return !start.isEmpty() && label.startsWith(start);
		}
		return false;
	}

	/**
	 * The entry id of a dropdown row (clientscript ca_task_filter_open, 4826): child 0 is the hover box and the entry for
	 * key k is child k + 1; sorting moves rows on screen and never changes an index.
	 */
	static int idForRow(int childIndex)
	{
		return childIndex >= 1 ? childIndex - 1 : -1;
	}

	/** The sidebar card's bold line. */
	static String statusHeadline(int done, int total)
	{
		return done + " of " + total + " done";
	}
}
