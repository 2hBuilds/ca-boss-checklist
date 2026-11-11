package com.cabosschecklist;

import java.util.List;
import java.util.Map;

/** What the sidebar's boxes and links do to the player's marks; every call is made on the EDT. */
interface MarkActions
{
	/** Flips the green of boss {@code id}, keeping a mark only while the choice differs from the account. */
	void flip(int id);

	/** Removes the marks of the given bosses. */
	void remove(List<Integer> ids);

	/** Stores the overriding marks the player has looked at. */
	void remember(Map<Integer, Mark> seen);
}
