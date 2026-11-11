package com.cabosschecklist;

import okhttp3.HttpUrl;

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

	/**
	 * The wiki address of the boss's Combat Achievements: the page is "Chambers of Xeric/Challenge Mode" for id 14,
	 * "Combat Achievements" for id 76 (Other), else {@code name}; every id but 76 ends at the page's table, the
	 * fragment "Combat_Achievements". Checked on 2026-11-11: all 91 long names against the wiki's API, where only
	 * "Chambers of Xeric: CM" and "Other" are not pages; the table's anchor on 11 of 13 sampled pages (a page
	 * without it opens at its top).
	 */
	static String wikiUrl(int id, String name)
	{
		final String page = id == 14 ? "Chambers of Xeric/Challenge Mode" : id == 76 ? "Combat Achievements" : name;
		final HttpUrl.Builder url = new HttpUrl.Builder()
			.scheme("https")
			.host("oldschool.runescape.wiki")
			.addPathSegment("w");
		for (String part : page.split("/"))
		{
			url.addPathSegment(part.replace(' ', '_'));
		}
		if (id != 76)
		{
			url.fragment("Combat_Achievements");
		}
		return url.build().toString();
	}
}
