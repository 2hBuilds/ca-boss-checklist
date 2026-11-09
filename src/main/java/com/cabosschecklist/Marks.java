package com.cabosschecklist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The rules for a player's marks, with no RuneLite types so they are tested without a client: how they are stored, when
 * a boss shows green, what a click on its box does, and which Manual group a mark falls in.
 */
final class Marks
{
	private Marks()
	{
	}

	/** Where a mark is listed in the Manual view. */
	enum Group
	{
		/** A mark that differs from the account and was made while the switch was off. */
		DISAGREE,
		/** A mark the account now gives by itself. */
		NOT_NEEDED,
		/** Every other mark. */
		YOURS
	}

	/**
	 * Reads the stored marks, "id=token" entries split on commas. An entry whose id is not an int of 1 or more, or whose
	 * word is not a mark, is skipped; a later entry for the same id wins.
	 */
	static Map<Integer, Mark> parse(String stored)
	{
		final Map<Integer, Mark> marks = new HashMap<>();
		if (stored != null && !stored.trim().isEmpty())
		{
			for (String entry : stored.split(",", -1))
			{
				final int at = entry.indexOf('=');
				if (at < 0)
				{
					continue;
				}
				final Mark mark = byToken(entry.substring(at + 1).trim());
				final Integer id = parseId(entry.substring(0, at).trim());
				if (mark != null && id != null)
				{
					marks.put(id, mark);
				}
			}
		}
		return Collections.unmodifiableMap(marks);
	}

	private static Integer parseId(String text)
	{
		try
		{
			final int id = Integer.parseInt(text);
			return id >= 1 ? id : null;
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private static Mark byToken(String token)
	{
		for (Mark mark : Mark.values())
		{
			if (mark.token().equals(token))
			{
				return mark;
			}
		}
		return null;
	}

	/** The stored form of {@code marks}: "id=token" joined by commas, ids ascending; "" when there are none. */
	static String format(Map<Integer, Mark> marks)
	{
		final List<Integer> ids = new ArrayList<>(marks.keySet());
		Collections.sort(ids);
		final StringBuilder out = new StringBuilder();
		for (int id : ids)
		{
			if (out.length() > 0)
			{
				out.append(',');
			}
			out.append(id).append('=').append(marks.get(id).token());
		}
		return out.toString();
	}

	/** Whether a Monster name shows green: "All" (0) never does; a mark wins; else the account alone, if followed. */
	static boolean isGreen(int id, boolean autoDetect, Set<Integer> complete, Map<Integer, Mark> marks)
	{
		if (id < 1)
		{
			return false;
		}
		final Mark mark = marks.get(id);
		if (mark != null)
		{
			return mark.done();
		}
		return autoDetect && complete.contains(id);
	}

	/**
	 * What a click on a boss's box leaves: null for no mark (the choice equals what the account alone gives), else the
	 * mark. A mark made while the switch is on is born kept.
	 */
	static Mark flip(boolean greenNow, boolean accountGreen, boolean autoDetect)
	{
		final boolean want = !greenNow;
		final boolean baseline = autoDetect && accountGreen;
		return want == baseline ? null : Mark.of(want, autoDetect);
	}

	/** Which Manual group {@code mark} is listed in, given the boss's counted tasks and its done tasks. */
	static Group groupOf(Mark mark, int total, int done, boolean autoDetect)
	{
		if (!autoDetect)
		{
			return Group.YOURS;
		}
		final boolean accountGreen = total > 0 && done == total;
		if (mark.done() == accountGreen)
		{
			return Group.NOT_NEEDED;
		}
		return total == 0 || mark.kept() ? Group.YOURS : Group.DISAGREE;
	}
}
