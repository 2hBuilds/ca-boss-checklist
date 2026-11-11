package com.cabosschecklist;

/**
 * A player's mark on one boss: it shows green ({@code DONE}) or never green ({@code NOT_DONE}) whatever the account
 * says.
 */
enum Mark
{
	DONE(true, "done"),
	NOT_DONE(false, "not-done");

	private final boolean done;
	private final String token;

	Mark(boolean done, String token)
	{
		this.done = done;
		this.token = token;
	}

	/** Whether the mark says the boss is done (green). */
	boolean done()
	{
		return done;
	}

	/** The word stored in the profile for this mark. */
	String token()
	{
		return token;
	}

	/** The mark with the given answer. */
	static Mark of(boolean done)
	{
		return done ? DONE : NOT_DONE;
	}
}
