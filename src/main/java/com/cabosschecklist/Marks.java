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
		/** A mark whose answer differs from what the account alone gives. */
		DIFFERENT,
		/** A mark whose answer is what the account alone gives. */
		SAME
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
		// 0.0.1 and 0.0.2 also wrote "done-kept" and "not-done-kept"; they read as the plain marks.
		if (token.endsWith("-kept"))
		{
			return byToken(token.substring(0, token.length() - "-kept".length()));
		}
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

	/** What a click on a boss's box leaves: null for no mark (the choice equals the account alone), else the mark. */
	static Mark flip(boolean greenNow, boolean accountGreen, boolean autoDetect)
	{
		final boolean want = !greenNow;
		final boolean baseline = autoDetect && accountGreen;
		return want == baseline ? null : Mark.of(want);
	}

	/** Which Manual group {@code mark} is listed in, given the boss's counted tasks and its done tasks. */
	static Group groupOf(Mark mark, int total, int done)
	{
		final boolean accountGreen = total > 0 && done == total;
		return mark.done() == accountGreen ? Group.SAME : Group.DIFFERENT;
	}

	/** The marks that take effect: {@code marks} itself while the sidebar button is shown, else none. */
	static Map<Integer, Mark> inUse(Map<Integer, Mark> marks, boolean sidebarShown)
	{
		return sidebarShown ? marks : Collections.emptyMap();
	}

	/**
	 * The seen marks after the player has (looking) or has not looked at the Manual tab: a copy of {@code overriding}
	 * when looking, else the entries of {@code seen} that are still the same mark in {@code overriding}.
	 */
	static Map<Integer, Mark> seenAfter(Map<Integer, Mark> seen, Map<Integer, Mark> overriding, boolean looking)
	{
		final Map<Integer, Mark> next = new HashMap<>();
		if (looking)
		{
			next.putAll(overriding);
		}
		else
		{
			for (Map.Entry<Integer, Mark> entry : seen.entrySet())
			{
				if (entry.getValue() == overriding.get(entry.getKey()))
				{
					next.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return Collections.unmodifiableMap(next);
	}

	/** Whether some overriding mark has not been seen: it has no equal entry in {@code seen}. */
	static boolean hasUnseen(Map<Integer, Mark> seen, Map<Integer, Mark> overriding)
	{
		for (Map.Entry<Integer, Mark> entry : overriding.entrySet())
		{
			if (seen.get(entry.getKey()) != entry.getValue())
			{
				return true;
			}
		}
		return false;
	}
}
