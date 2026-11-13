package com.cabosschecklist;

/**
 * The one place the plugin's version is written. The descriptor's description, the settings menu's header and the
 * start-up log line read it; the frame test checks it against {@code version=} in runelite-plugin.properties.
 */
public final class Version
{
	/** The version this build ships as, {@code major.minor.patch}. */
	public static final String CURRENT = "0.0.5";

	private Version()
	{
	}
}
