package com.cabosschecklist;

/**
 * A player's mark on one boss: it shows green ({@code DONE}) or never green ({@code NOT_DONE}) whatever the account
 * says. A KEPT mark was made while the account was being followed, so the player saw the account and chose otherwise;
 * it is never flagged for review.
 */
enum Mark
{
	DONE(true, false, "done"),
	NOT_DONE(false, false, "not-done"),
	DONE_KEPT(true, true, "done-kept"),
	NOT_DONE_KEPT(false, true, "not-done-kept");

	private final boolean done;
	private final boolean kept;
	private final String token;

	Mark(boolean done, boolean kept, String token)
	{
		this.done = done;
		this.kept = kept;
		this.token = token;
	}

	/** Whether the mark says the boss is done (green). */
	boolean done()
	{
		return done;
	}

	/** Whether the mark is a KEPT one. */
	boolean kept()
	{
		return kept;
	}

	/** The word stored in the profile for this mark. */
	String token()
	{
		return token;
	}

	/** The mark with the given answer and kept flag. */
	static Mark of(boolean done, boolean kept)
	{
		if (done)
		{
			return kept ? DONE_KEPT : DONE;
		}
		return kept ? NOT_DONE_KEPT : NOT_DONE;
	}

	/** The KEPT mark with the same answer. */
	Mark asKept()
	{
		return of(done, true);
	}
}
