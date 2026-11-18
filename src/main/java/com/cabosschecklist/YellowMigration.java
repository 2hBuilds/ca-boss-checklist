package com.cabosschecklist;

/**
 * Carries the old "Yellow when almost done" switch (the boolean key {@link CaBossChecklistConfig#ALMOST_DONE_KEY}) over to
 * the "Yellow when" choice. Only a player who had switched yellow off needs anything: the choice's default is today's rule.
 */
final class YellowMigration
{
	private YellowMigration()
	{
	}

	/**
	 * The choice to store at start-up, or null for none: "Off" when the old key reads false and the new key was never set;
	 * null when the old key is true, unset or anything else, or when the new key already holds a value (a blank value
	 * counts as never set).
	 */
	static CaBossChecklistConfig.YellowWhen carryOver(String oldValue, String newValue)
	{
		final boolean newSet = newValue != null && !newValue.trim().isEmpty();
		if (newSet || oldValue == null || !"false".equalsIgnoreCase(oldValue.trim()))
		{
			return null;
		}
		return CaBossChecklistConfig.YellowWhen.OFF;
	}
}
