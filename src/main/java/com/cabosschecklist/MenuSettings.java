package com.cabosschecklist;

/** What the sidebar's settings menu reads and writes: the plugin's settings, kept apart from RuneLite's config classes. */
interface MenuSettings
{
	boolean autoDetect();

	void setAutoDetect(boolean on);

	boolean almostDone();

	void setAlmostDone(boolean on);

	boolean wikiPictures();

	void setWikiPictures(boolean on);

	CaBossChecklistConfig.SidebarIcon sidebarIcon();

	void setSidebarIcon(CaBossChecklistConfig.SidebarIcon icon);

	/** Turns "Show the sidebar button" off. */
	void hideSidebar();

	/** Puts every setting back to its default. */
	void resetToDefault();
}
