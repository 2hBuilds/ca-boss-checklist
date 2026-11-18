package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.runelite.client.ui.ColorScheme;
import org.junit.After;
import org.junit.Test;

/** Lot 16: when a name turns yellow, the four colours, and how they reach the sidebar and the game's lists. */
public class ColourSettingsTest
{
	private static final int WIDTH = 242;
	private static final int HEIGHT = 700;
	private static final Color CYAN = new Color(0x00D0FF);
	private static final Color PINK = new Color(0xFF77D0);
	private static final Color GREY = new Color(0xC8C8C8);
	private static final Color BLUE = new Color(0x3C8CFF);

	@After
	public void putTheDefaultsBack()
	{
		ViewParts.useColours(null);
	}

	private static CaBossChecklistConfig.YellowWhen[] rules()
	{
		return CaBossChecklistConfig.YellowWhen.values();
	}

	private static boolean yellow(CaBossChecklistConfig.YellowWhen rule, int total, int done, int gmTotal, int gmDone)
	{
		return CaCompletion.isYellow(rule, new CaCompletion.Progress(total, done, gmTotal, gmDone));
	}

	// ---- the choices and their order -------------------------------------------------------------------------------

	@Test
	public void theChoicesAreInTheAgreedOrderWithTheirWords()
	{
		final CaBossChecklistConfig.YellowWhen[] all = rules();
		assertEquals(5, all.length);
		assertEquals("1 or 2 tasks left", all[0].toString());
		assertEquals("Started", all[1].toString());
		assertEquals("Half done", all[2].toString());
		assertEquals("Only Grandmaster left", all[3].toString());
		assertEquals("Off", all[4].toString());
		assertEquals(CaBossChecklistConfig.YellowWhen.ONE_OR_TWO, new CaBossChecklistConfig()
		{
		}.yellowWhen());
	}

	// ---- each rule on its boundaries -------------------------------------------------------------------------------

	@Test
	public void oneOrTwoLeftIsTodaysRule()
	{
		final CaBossChecklistConfig.YellowWhen rule = CaBossChecklistConfig.YellowWhen.ONE_OR_TWO;
		assertFalse("0 done", yellow(rule, 8, 0, 0, 0));
		assertTrue("1 done of 3", yellow(rule, 3, 1, 0, 0));
		assertTrue("1 done of 2", yellow(rule, 2, 1, 0, 0));
		assertFalse("0 done of 2", yellow(rule, 2, 0, 0, 0));
		assertTrue("6 of 8", yellow(rule, 8, 6, 0, 0));
		assertTrue("7 of 8", yellow(rule, 8, 7, 0, 0));
		assertFalse("5 of 8", yellow(rule, 8, 5, 0, 0));
		assertFalse("all done", yellow(rule, 8, 8, 0, 0));
		assertFalse("no tasks", yellow(rule, 0, 0, 0, 0));
		assertFalse("null", CaCompletion.isYellow(rule, null));
	}

	@Test
	public void startedMeansOneDoneButNotAll()
	{
		final CaBossChecklistConfig.YellowWhen rule = CaBossChecklistConfig.YellowWhen.STARTED;
		assertFalse("0 done", yellow(rule, 8, 0, 0, 0));
		assertTrue("1 done", yellow(rule, 8, 1, 0, 0));
		assertTrue("half", yellow(rule, 8, 4, 0, 0));
		assertTrue("all but one", yellow(rule, 8, 7, 0, 0));
		assertFalse("all done", yellow(rule, 8, 8, 0, 0));
		assertFalse("no tasks", yellow(rule, 0, 0, 0, 0));
		assertTrue("a Grandmaster-free boss", yellow(rule, 3, 1, 0, 0));
	}

	@Test
	public void halfDoneMeansDoneTimesTwoReachesTotalButNotAll()
	{
		final CaBossChecklistConfig.YellowWhen rule = CaBossChecklistConfig.YellowWhen.HALF;
		assertFalse("0 done", yellow(rule, 8, 0, 0, 0));
		assertFalse("1 of 8", yellow(rule, 8, 1, 0, 0));
		assertFalse("3 of 8", yellow(rule, 8, 3, 0, 0));
		assertTrue("4 of 8 is half", yellow(rule, 8, 4, 0, 0));
		assertFalse("2 of 5 is under half", yellow(rule, 5, 2, 0, 0));
		assertTrue("3 of 5 is over half", yellow(rule, 5, 3, 0, 0));
		assertTrue("7 of 8", yellow(rule, 8, 7, 0, 0));
		assertFalse("all done", yellow(rule, 8, 8, 0, 0));
		assertFalse("no tasks", yellow(rule, 0, 0, 0, 0));
		assertFalse("1 task, 0 done", yellow(rule, 1, 0, 0, 0));
	}

	@Test
	public void onlyGrandmasterLeftNeedsEverythingBelowDoneAndOneGrandmasterOpen()
	{
		final CaBossChecklistConfig.YellowWhen rule = CaBossChecklistConfig.YellowWhen.ONLY_GRANDMASTER;
		assertFalse("0 done", yellow(rule, 8, 0, 2, 0));
		assertTrue("all below done, no Grandmaster done", yellow(rule, 8, 6, 2, 0));
		assertTrue("all below done, one Grandmaster done", yellow(rule, 8, 7, 2, 1));
		assertFalse("one below Grandmaster still open", yellow(rule, 8, 5, 2, 0));
		assertFalse("a Grandmaster done but one below open", yellow(rule, 8, 6, 2, 1));
		assertFalse("all done", yellow(rule, 8, 8, 2, 2));
		assertFalse("a boss with no Grandmaster task", yellow(rule, 6, 3, 0, 0));
		assertFalse("no tasks", yellow(rule, 0, 0, 0, 0));
		assertTrue("only Grandmaster tasks, one done", yellow(rule, 2, 1, 2, 1));
		assertFalse("only Grandmaster tasks, none done", yellow(rule, 2, 0, 2, 0));
	}

	@Test
	public void offIsNeverYellow()
	{
		final CaBossChecklistConfig.YellowWhen rule = CaBossChecklistConfig.YellowWhen.OFF;
		assertFalse(yellow(rule, 8, 7, 2, 1));
		assertFalse(yellow(rule, 8, 4, 0, 0));
		assertFalse(yellow(rule, 3, 1, 0, 0));
	}

	@Test
	public void aFinishedBossIsNeverYellowUnderAnyRule()
	{
		for (CaBossChecklistConfig.YellowWhen rule : rules())
		{
			assertFalse(rule.name(), yellow(rule, 8, 8, 2, 2));
			assertFalse(rule.name(), yellow(rule, 0, 0, 0, 0));
		}
	}

	@Test
	public void theCountReadsGrandmasterTasksFromTheirTier()
	{
		final List<CaCompletion.Task> tasks = new ArrayList<>();
		tasks.add(new CaCompletion.Task(0, 7, 0, 0, 1));
		tasks.add(new CaCompletion.Task(1, 7, 0, 0, 5));
		tasks.add(new CaCompletion.Task(2, 7, 0, 0, 6));
		tasks.add(new CaCompletion.Task(3, 7, 0, 0, 6));
		tasks.add(new CaCompletion.Task(4, 7, 5, 0, 6));
		final int[] varps = new int[1];
		varps[0] = 0b00111;
		final CaCompletion.Progress alone = CaCompletion.progress(tasks, varps, false, 0).get(7);
		assertEquals(5, alone.total);
		assertEquals(3, alone.done);
		assertEquals(3, alone.grandmasterTotal);
		assertEquals(1, alone.grandmasterDone);
		// A group ironman of 2 leaves out the task the game turns off for groups of up to 5.
		final CaCompletion.Progress group = CaCompletion.progress(tasks, varps, false, 2).get(7);
		assertEquals(4, group.total);
		assertEquals(2, group.grandmasterTotal);
		assertEquals(1, group.grandmasterDone);
	}

	@Test
	public void marksStillWinOverTheYellowRule()
	{
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(1, new CaCompletion.Progress(8, 4));
		progress.put(2, new CaCompletion.Progress(8, 8));
		final Map<Integer, Mark> ticked = new HashMap<>();
		ticked.put(1, Mark.DONE);
		final Map<Integer, Mark> unticked = new HashMap<>();
		unticked.put(2, Mark.NOT_DONE);
		final Set<Integer> complete = new HashSet<>();
		complete.add(2);
		final CaBossChecklistConfig.YellowWhen half = CaBossChecklistConfig.YellowWhen.HALF;
		assertEquals(CaCompletion.Shade.YELLOW, CaCompletion.shadeOf(1, true, half, complete, new HashMap<>(), progress));
		assertEquals(CaCompletion.Shade.GREEN, CaCompletion.shadeOf(1, true, half, complete, ticked, progress));
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(2, true, half, complete, unticked, progress));
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(1, true, CaBossChecklistConfig.YellowWhen.OFF, complete,
			new HashMap<>(), progress));
		assertEquals(CaCompletion.Shade.PLAIN, CaCompletion.shadeOf(0, true, half, complete, new HashMap<>(), progress));
	}

	// ---- the migration of the old switch ---------------------------------------------------------------------------

	@Test
	public void anOldFalseBecomesOffWhenTheNewKeyWasNeverSet()
	{
		assertEquals(CaBossChecklistConfig.YellowWhen.OFF, YellowMigration.carryOver("false", null));
		assertEquals(CaBossChecklistConfig.YellowWhen.OFF, YellowMigration.carryOver("false", ""));
		assertEquals(CaBossChecklistConfig.YellowWhen.OFF, YellowMigration.carryOver(" FALSE ", "  "));
	}

	@Test
	public void anOldTrueOrNothingLeavesTheDefault()
	{
		assertNull(YellowMigration.carryOver("true", null));
		assertNull(YellowMigration.carryOver(null, null));
		assertNull(YellowMigration.carryOver("", null));
		assertNull(YellowMigration.carryOver("maybe", null));
	}

	@Test
	public void aNewKeyThatIsSetIsNeverTouched()
	{
		assertNull(YellowMigration.carryOver("false", "HALF"));
		assertNull(YellowMigration.carryOver("false", "ONE_OR_TWO"));
		assertNull(YellowMigration.carryOver("true", "OFF"));
		assertEquals("the old key is still the one the config used", "almostDone", CaBossChecklistConfig.ALMOST_DONE_KEY);
		assertEquals("yellowWhen", CaBossChecklistConfig.YELLOW_WHEN_KEY);
	}

	// ---- the colours and their defaults -----------------------------------------------------------------------------

	@Test
	public void theDefaultsAreExactlyTodaysColours()
	{
		final ColourSet defaults = ColourSet.DEFAULTS;
		assertEquals(0x0DC10D, defaults.green.getRGB() & 0xFFFFFF);
		assertEquals(0xFFFF00, defaults.yellow.getRGB() & 0xFFFFFF);
		assertEquals(0xFF981F, defaults.notDone.getRGB() & 0xFFFFFF);
		assertEquals(0x26716D, defaults.frame.getRGB() & 0xFFFFFF);
		final CaBossChecklistConfig config = new CaBossChecklistConfig()
		{
		};
		assertEquals(defaults.green, config.greenColour());
		assertEquals(defaults.yellow, config.yellowColour());
		assertEquals(defaults.notDone, config.notDoneColour());
		assertEquals(defaults.frame, config.frameColour());
		assertTrue(defaults.notDoneIsDefault());
		assertEquals(ViewParts.GREEN, ViewParts.colourOf(CaCompletion.Shade.GREEN));
		assertEquals(ViewParts.YELLOW, ViewParts.colourOf(CaCompletion.Shade.YELLOW));
		assertEquals(ViewParts.PLAIN, ViewParts.colourOf(CaCompletion.Shade.PLAIN));
		assertEquals(ViewParts.FRAME, ViewParts.frame());
		assertEquals("Green colour is the first row's key", "greenColour", ColourSet.Slot.GREEN.key);
		assertEquals("yellowColour", ColourSet.Slot.YELLOW.key);
		assertEquals("notDoneColour", ColourSet.Slot.NOT_DONE.key);
		assertEquals("frameColour", ColourSet.Slot.FRAME.key);
	}

	@Test
	public void aMissingColourTakesItsDefaultAndTransparencyIsDropped()
	{
		final ColourSet set = new ColourSet(null, new Color(0x80FF0000, true), null, null);
		assertEquals(ViewParts.GREEN, set.green);
		assertEquals(0xFF0000, set.yellow.getRGB() & 0xFFFFFF);
		assertEquals(255, set.yellow.getAlpha());
		assertEquals(ViewParts.PLAIN, set.notDone);
		assertEquals(ViewParts.FRAME, set.frame);
	}

	// ---- what the plugin sets in the game ---------------------------------------------------------------------------

	@Test
	public void theGameGetsTheConfiguredGreenAndYellow()
	{
		final ColourSet set = new ColourSet(CYAN, PINK, ViewParts.PLAIN, BLUE);
		assertEquals(Integer.valueOf(0x00D0FF), set.inGame(CaCompletion.Shade.GREEN, 7));
		assertEquals(Integer.valueOf(0xFF77D0), set.inGame(CaCompletion.Shade.YELLOW, 7));
		assertEquals(0x00D0FF, set.completeTier());
	}

	@Test
	public void notDoneIsOnlySetWhenItDiffersFromTheGamesOrange()
	{
		assertNull(ColourSet.DEFAULTS.inGame(CaCompletion.Shade.PLAIN, 7));
		assertEquals(Integer.valueOf(0x0DC10D), ColourSet.DEFAULTS.inGame(CaCompletion.Shade.GREEN, 7));
		assertEquals(Integer.valueOf(0xFFFF00), ColourSet.DEFAULTS.inGame(CaCompletion.Shade.YELLOW, 7));
		final ColourSet grey = new ColourSet(null, null, GREY, null);
		assertFalse(grey.notDoneIsDefault());
		assertEquals(Integer.valueOf(0xC8C8C8), grey.inGame(CaCompletion.Shade.PLAIN, 7));
		// Setting the orange itself, with any transparency, is still "the default".
		assertNull(new ColourSet(null, null, new Color(0xFF981F), null).inGame(CaCompletion.Shade.PLAIN, 7));
	}

	@Test
	public void allIsNeverColoured()
	{
		final ColourSet set = new ColourSet(CYAN, PINK, GREY, BLUE);
		for (CaCompletion.Shade shade : CaCompletion.Shade.values())
		{
			assertNull(shade.name() + " id 0", set.inGame(shade, 0));
			assertNull(shade.name() + " id -1", set.inGame(shade, -1));
		}
	}

	// ---- the sidebar, laid out ---------------------------------------------------------------------------------------

	private static final MarkActions NO_ACTIONS = new MarkActions()
	{
		@Override
		public void flip(int id)
		{
		}

		@Override
		public void remove(List<Integer> ids)
		{
		}

		@Override
		public void remember(Map<Integer, Mark> seen)
		{
		}
	};

	/** Zulrah finished (green), Araxxor 7 of 8 (yellow), Barrows 2 of 8 (not done); Araxxor is the opened one. */
	private static ChecklistView view()
	{
		final Map<Integer, String> labels = new HashMap<>();
		labels.put(4, "Araxxor");
		labels.put(5, "Barrows");
		labels.put(72, "Zulrah");
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(4, new CaCompletion.Progress(8, 7));
		progress.put(5, new CaCompletion.Progress(8, 2));
		progress.put(72, new CaCompletion.Progress(8, 8));
		return ChecklistView.build(labels, Collections.emptyMap(), progress, new HashMap<>(), true,
			CaBossChecklistConfig.YellowWhen.ONE_OR_TWO, Collections.emptyMap());
	}

	private static BossesView shown() throws Exception
	{
		final BossesView[] holder = new BossesView[1];
		SwingUtilities.invokeAndWait(() ->
		{
			final WikiPictures noWiki = new WikiPictures(request ->
			{
				throw new AssertionError("the wiki was asked: " + request.url());
			}, new Gson(), () ->
			{
			});
			final BossesView bosses = new BossesView(NO_ACTIONS, () -> false, noWiki);
			bosses.setSize(WIDTH, HEIGHT);
			bosses.show(view());
			holder[0] = bosses;
		});
		return holder[0];
	}

	private static void collect(Component root, List<Component> into)
	{
		into.add(root);
		if (root instanceof Container)
		{
			for (Component child : ((Container) root).getComponents())
			{
				collect(child, into);
			}
		}
	}

	private static JLabel label(Component root, String text)
	{
		final List<Component> every = new ArrayList<>();
		collect(root, every);
		for (Component component : every)
		{
			if (component instanceof JLabel && text.equals(((JLabel) component).getText()))
			{
				return (JLabel) component;
			}
		}
		throw new AssertionError("no label '" + text + "'");
	}

	private static void layOut(Component root)
	{
		for (int pass = 0; pass < 3; pass++)
		{
			final List<Component> every = new ArrayList<>();
			collect(root, every);
			for (Component component : every)
			{
				component.invalidate();
			}
			for (Component component : every)
			{
				if (component instanceof Container)
				{
					((Container) component).doLayout();
				}
			}
		}
	}

	private static void press(Component target)
	{
		target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 1, 1, 1, false,
			MouseEvent.BUTTON1));
	}

	private static BufferedImage paint(Component root)
	{
		final BufferedImage image = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_ARGB);
		root.printAll(image.getGraphics());
		return image;
	}

	private static Rectangle bounds(Component part, Component root)
	{
		final Point at = SwingUtilities.convertPoint(part.getParent(), part.getLocation(), root);
		return new Rectangle(at.x, at.y, part.getWidth(), part.getHeight());
	}

	private static boolean is(BufferedImage image, int x, int y, Color colour)
	{
		return (image.getRGB(x, y) & 0xFFFFFF) == (colour.getRGB() & 0xFFFFFF);
	}

	/** How many pixels of {@code area} are exactly {@code colour}. */
	private static int count(BufferedImage image, Rectangle area, Color colour)
	{
		int found = 0;
		for (int x = area.x; x < area.x + area.width; x++)
		{
			for (int y = area.y; y < area.y + area.height; y++)
			{
				found += is(image, x, y, colour) ? 1 : 0;
			}
		}
		return found;
	}

	@Test
	public void theNamesAndTheFrameFollowTheChosenColours() throws Exception
	{
		final BossesView bosses = shown();
		SwingUtilities.invokeAndWait(() ->
		{
			press(label(bosses, "Araxxor").getParent());
			layOut(bosses);
			// Today's colours first.
			assertEquals(ViewParts.GREEN, label(bosses, "Zulrah").getForeground());
			assertEquals(ViewParts.YELLOW, label(bosses, "Araxxor").getForeground());
			assertEquals(ViewParts.PLAIN, label(bosses, "Barrows").getForeground());
			final Rectangle defaultRow = bounds(label(bosses, "Araxxor").getParent(), bosses);
			assertTrue("the frame is the default colour",
				is(paint(bosses), defaultRow.x, defaultRow.y + defaultRow.height / 2, ViewParts.FRAME));

			// A new set of colours reaches the very next draw.
			ViewParts.useColours(new ColourSet(CYAN, PINK, GREY, BLUE));
			bosses.show(view());
			layOut(bosses);
			assertEquals(CYAN, label(bosses, "Zulrah").getForeground());
			assertEquals(PINK, label(bosses, "Araxxor").getForeground());
			assertEquals(GREY, label(bosses, "Barrows").getForeground());
			final BufferedImage image = paint(bosses);
			final Rectangle row = bounds(label(bosses, "Araxxor").getParent(), bosses);
			final Rectangle opened = bounds(openedAfter(bosses, "Araxxor"), bosses);
			for (int w = 0; w < 2; w++)
			{
				assertTrue("row top " + w, is(image, row.x + row.width / 2, row.y + w, BLUE));
				assertTrue("row left " + w, is(image, row.x + w, row.y + row.height / 2, BLUE));
				assertTrue("row right " + w, is(image, row.x + row.width - 1 - w, row.y + row.height / 2, BLUE));
				assertTrue("card left " + w, is(image, opened.x + w, opened.y + opened.height / 2, BLUE));
				assertTrue("card bottom " + w, is(image, opened.x + opened.width / 2, opened.y + opened.height - 1 - w, BLUE));
			}
			assertEquals("no default frame is left", 0, count(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()),
				ViewParts.FRAME));
			assertTrue("the name is drawn in pink", count(image, bounds(label(bosses, "Araxxor"), bosses), PINK) > 0);
			assertTrue("the name is drawn in cyan", count(image, bounds(label(bosses, "Zulrah"), bosses), CYAN) > 0);
			assertTrue("the name is drawn in grey", count(image, bounds(label(bosses, "Barrows"), bosses), GREY) > 0);

			// Back to the defaults is just as live.
			ViewParts.useColours(null);
			bosses.show(view());
			layOut(bosses);
			assertEquals(ViewParts.GREEN, label(bosses, "Zulrah").getForeground());
			assertTrue(is(paint(bosses), row.x, row.y + row.height / 2, ViewParts.FRAME));
		});
	}

	/** The opened block that follows {@code boss}'s row in its column. */
	private static Component openedAfter(BossesView bosses, String boss)
	{
		final Component row = label(bosses, boss).getParent();
		final Component[] parts = row.getParent().getComponents();
		for (int i = 0; i < parts.length; i++)
		{
			if (parts[i] == row)
			{
				return parts[i + 1];
			}
		}
		throw new AssertionError("row not in its column");
	}

	// ---- the sidebar's settings menu ---------------------------------------------------------------------------------

	/** Menu settings that record what the menu asks for. */
	private static final class Recorder implements MenuSettings
	{
		CaBossChecklistConfig.YellowWhen when = CaBossChecklistConfig.YellowWhen.HALF;
		ColourSet colours = new ColourSet(CYAN, PINK, GREY, BLUE);
		final List<ColourSet.Slot> picked = new ArrayList<>();
		Component owner;
		int resets;

		@Override
		public boolean autoDetect()
		{
			return true;
		}

		@Override
		public void setAutoDetect(boolean on)
		{
		}

		@Override
		public CaBossChecklistConfig.YellowWhen yellowWhen()
		{
			return when;
		}

		@Override
		public void setYellowWhen(CaBossChecklistConfig.YellowWhen next)
		{
			when = next;
		}

		@Override
		public ColourSet colours()
		{
			return colours;
		}

		@Override
		public void pickColour(ColourSet.Slot slot, Component from)
		{
			picked.add(slot);
			owner = from;
		}

		@Override
		public void resetColours()
		{
			resets++;
			colours = ColourSet.DEFAULTS;
		}

		@Override
		public boolean wikiPictures()
		{
			return false;
		}

		@Override
		public void setWikiPictures(boolean on)
		{
		}

		@Override
		public CaBossChecklistConfig.SidebarIcon sidebarIcon()
		{
			return CaBossChecklistConfig.SidebarIcon.COIN_AND_BOOK;
		}

		@Override
		public void setSidebarIcon(CaBossChecklistConfig.SidebarIcon icon)
		{
		}

		@Override
		public void hideSidebar()
		{
		}

		@Override
		public void resetToDefault()
		{
		}
	}

	private static CaBossChecklistPanel panelWith(Recorder settings) throws Exception
	{
		final CaBossChecklistPanel[] holder = new CaBossChecklistPanel[1];
		SwingUtilities.invokeAndWait(() ->
		{
			holder[0] = new CaBossChecklistPanel(settings, NO_ACTIONS, new WikiPictures(request ->
			{
				throw new AssertionError("the wiki was asked");
			}, new Gson(), () ->
			{
			}), (t, q, y) -> false);
			for (PopupMenuListener listener : holder[0].menu.getPopupMenuListeners())
			{
				listener.popupMenuWillBecomeVisible(new PopupMenuEvent(holder[0].menu));
			}
		});
		return holder[0];
	}

	private static int rgbOf(Icon icon)
	{
		final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		icon.paintIcon(null, image.getGraphics(), 0, 0);
		return image.getRGB(icon.getIconWidth() / 2, icon.getIconHeight() / 2) & 0xFFFFFF;
	}

	@Test
	public void theYellowGroupListsTheFiveChoicesAndWritesThePick() throws Exception
	{
		final Recorder settings = new Recorder();
		final CaBossChecklistPanel panel = panelWith(settings);
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals("Yellow when: Half done", panel.yellowMenu.getText());
			assertEquals(5, panel.yellowItems.size());
			final String[] words = {"1 or 2 tasks left", "Started", "Half done", "Only Grandmaster left", "Off"};
			for (int i = 0; i < words.length; i++)
			{
				assertEquals(words[i], panel.yellowItems.get(i).getText());
				assertEquals(i == 2, panel.yellowItems.get(i).isSelected());
			}
			panel.yellowItems.get(3).doClick(0);
			assertEquals(CaBossChecklistConfig.YellowWhen.ONLY_GRANDMASTER, settings.when);
			panel.yellowItems.get(4).doClick(0);
			assertEquals(CaBossChecklistConfig.YellowWhen.OFF, settings.when);
		});
	}

	@Test
	public void theColoursGroupHasFourRowsWithSwatchesAndAResetRow() throws Exception
	{
		final Recorder settings = new Recorder();
		final CaBossChecklistPanel panel = panelWith(settings);
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(5, panel.colourItems.size());
			final String[] words = {"Green...", "Yellow...", "Not done...", "Frame...", "Reset colours"};
			final Color[] swatches = {CYAN, PINK, GREY, BLUE};
			for (int i = 0; i < words.length; i++)
			{
				final JMenuItem item = panel.colourItems.get(i);
				assertEquals(words[i], item.getText());
				if (i < 4)
				{
					assertEquals("swatch " + i, swatches[i].getRGB() & 0xFFFFFF, rgbOf(item.getIcon()));
				}
			}
			final ColourSet.Slot[] slots = ColourSet.Slot.values();
			for (int i = 0; i < 4; i++)
			{
				panel.colourItems.get(i).doClick(0);
			}
			assertEquals(java.util.Arrays.asList(slots), settings.picked);
			assertEquals("the picker opens beside the panel", panel, settings.owner);
			panel.colourItems.get(4).doClick(0);
			assertEquals(1, settings.resets);
			assertEquals("the swatches follow the reset", ViewParts.GREEN.getRGB() & 0xFFFFFF,
				rgbOf(panel.colourItems.get(0).getIcon()));
		});
	}
}
