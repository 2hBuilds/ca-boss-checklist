package com.cabosschecklist;

/**
 * Which wiki page's picture stands for a boss of the game's Monster list. Checked on 2026-11-08: the game's long names
 * (enum 3971) give a PNG picture for 86 of the 92 bosses. The rest are the table in {@link #titleFor}; "The Mad Angel"
 * has only a WebP picture and "Giants" none, so their cards show none.
 */
final class BossPictures
{
	private BossPictures()
	{
	}

	/**
	 * The wiki page whose picture stands for the boss, or null for none. Id 14 (Chambers of Xeric: CM) is the page
	 * "Chambers of Xeric"; ids 54 and 56 (Theatre of Blood: Entry Mode and Hard Mode, whose own pages have no picture)
	 * are "Theatre of Blood"; id 76 ("Other") has none. Any other boss is the page of its {@code name}, or none when the
	 * name is null or blank.
	 */
	static String titleFor(int id, String name)
	{
		if (id == 14)
		{
			return "Chambers of Xeric";
		}
		if (id == 54 || id == 56)
		{
			return "Theatre of Blood";
		}
		if (id == 76)
		{
			return null;
		}
		return name == null || name.trim().isEmpty() ? null : name;
	}
}
