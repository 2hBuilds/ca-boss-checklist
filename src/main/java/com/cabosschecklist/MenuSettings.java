package com.cabosschecklist;

import java.awt.Component;

/**
 * What the sidebar's settings menu reads and writes: the plugin's settings, kept apart from RuneLite's config classes.
 */
interface MenuSettings
{
	boolean autoDetect();

	void setAutoDetect(boolean on);

	/** When a name turns yellow. */
	CaBossChecklistConfig.YellowWhen yellowWhen();

	/** Chooses when a name turns yellow. */
	void setYellowWhen(CaBossChecklistConfig.YellowWhen when);

	/** The colours chosen now. */
	ColourSet colours();

	/** Opens RuneLite's colour picker for {@code slot} beside {@code owner}; every change in it is stored at once. */
	void pickColour(ColourSet.Slot slot, Component owner);

	/** Puts the four colours back to their defaults. */
	void resetColours();

	boolean wikiPictures();

	void setWikiPictures(boolean on);

	CaBossChecklistConfig.SidebarIcon sidebarIcon();

	void setSidebarIcon(CaBossChecklistConfig.SidebarIcon icon);

	/** Turns "Show the sidebar button" off. */
	void hideSidebar();

	/** Puts every setting back to its default. */
	void resetToDefault();
}
