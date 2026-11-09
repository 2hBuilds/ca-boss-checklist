package com.cabosschecklist;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Standalone runner: boots RuneLite in developer mode with 2h CA Boss Checklist injected. This is what {@code gradlew run}
 * starts. It lives in the test source set, so the packaged jar never holds it, and it has no {@code @Test} method,
 * so the test task skips it. {@code ExternalPluginManager.loadBuiltin} needs assertions on, which the run task passes.
 */
public class CaBossChecklistPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CaBossChecklistPlugin.class);
		RuneLite.main(args);
	}
}
