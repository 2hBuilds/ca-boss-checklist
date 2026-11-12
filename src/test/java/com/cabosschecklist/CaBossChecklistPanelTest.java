package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.google.gson.Gson;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

public class CaBossChecklistPanelTest
{
	/** Records every call the panel makes, in order. */
	private static final class Recorder implements MarkActions
	{
		final List<String> calls = new ArrayList<>();

		@Override
		public void flip(int id)
		{
			calls.add("flip " + id);
		}

		@Override
		public void remove(List<Integer> ids)
		{
			calls.add("remove " + ids);
		}

		@Override
		public void remember(Map<Integer, Mark> seen)
		{
			calls.add("remember [" + Marks.format(seen) + "]");
		}
	}

	private static ChecklistView view(Map<Integer, Mark> marks)
	{
		final Map<Integer, String> labels = new HashMap<>();
		labels.put(5, "Barrows");
		labels.put(19, "DK: Prime");
		labels.put(72, "Zulrah");
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(5, new CaCompletion.Progress(6, 2));
		progress.put(19, new CaCompletion.Progress(4, 4));
		progress.put(72, new CaCompletion.Progress(8, 8));
		return ChecklistView.build(labels, Collections.emptyMap(), progress, marks, true, false, Collections.emptyMap());
	}

	/** The wiki's pictures over a factory that must never be called: for tests with the wiki switch off. */
	private static WikiPictures noWiki()
	{
		return new WikiPictures(request ->
		{
			throw new AssertionError("the wiki was asked: " + request.url());
		}, new Gson(), () ->
		{
		});
	}

	/** Menu settings for a test: auto on, the icon at its default, setters and reset doing nothing. */
	private static final class TestSettings implements MenuSettings
	{
		private final boolean wiki;
		int hidden;

		TestSettings(boolean wiki)
		{
			this.wiki = wiki;
		}

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
		public boolean almostDone()
		{
			return true;
		}

		@Override
		public void setAlmostDone(boolean on)
		{
		}

		@Override
		public boolean wikiPictures()
		{
			return wiki;
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
			hidden++;
		}

		@Override
		public void resetToDefault()
		{
		}
	}

	private static CaBossChecklistPanel panel(Recorder recorder, ChecklistView view)
	{
		final CaBossChecklistPanel panel = new CaBossChecklistPanel(new TestSettings(false), recorder, noWiki(),
			(t, q, y) -> false);
		panel.show(view);
		return panel;
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

	private static List<JLabel> labelsWithText(CaBossChecklistPanel panel, String text)
	{
		final List<Component> all = new ArrayList<>();
		collect(panel, all);
		final List<JLabel> found = new ArrayList<>();
		for (Component component : all)
		{
			if (component instanceof JLabel && text.equals(((JLabel) component).getText()))
			{
				found.add((JLabel) component);
			}
		}
		return found;
	}

	/** Lays the panel out as a shown sidebar would: every part made invalid, then each container laid out, top down. */
	private static void layOut(Component root)
	{
		for (int pass = 0; pass < 2; pass++)
		{
			final List<Component> all = new ArrayList<>();
			collect(root, all);
			for (Component component : all)
			{
				component.invalidate();
			}
			for (Component component : all)
			{
				if (component instanceof Container)
				{
					((Container) component).doLayout();
				}
			}
		}
	}

	/**
	 * Found in the lead's look at the laid-out panel: a lone line was given half its width, cut with "...". Run on the
	 * EDT, as the plugin runs the panel: the search box refuses any other thread.
	 */
	@Test
	public void aLoneLineKeepsItsWidth() throws Exception
	{
		final List<JLabel> lines = new ArrayList<>();
		SwingUtilities.invokeAndWait(() ->
		{
			final CaBossChecklistPanel loggedOut = panel(new Recorder(), view(Collections.emptyMap()));
			loggedOut.showLoggedOut();
			loggedOut.setSize(242, 600);
			layOut(loggedOut);
			final CaBossChecklistPanel noMarks = panel(new Recorder(), view(Collections.emptyMap()));
			noMarks.setSize(242, 600);
			layOut(noMarks);
			lines.addAll(labelsWithText(loggedOut, "Log in to see your bosses"));
			lines.addAll(labelsWithText(loggedOut, "Log in to see your marks"));
			lines.addAll(labelsWithText(noMarks, "No marks yet"));
			lines.addAll(labelsWithText(noMarks, "Tick or untick a boss to add a mark"));
		});
		assertEquals(4, lines.size());
		for (JLabel line : lines)
		{
			assertTrue("'" + line.getText() + "' is " + line.getWidth() + " px wide, its text needs "
				+ line.getPreferredSize().width, line.getWidth() >= line.getPreferredSize().width);
		}
	}

	private static void press(JLabel label)
	{
		label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 1, 1, 1, false,
			MouseEvent.BUTTON1));
	}

	@Test
	public void boxFlipsTheRightBoss()
	{
		final Recorder recorder = new Recorder();
		final CaBossChecklistPanel panel = panel(recorder, view(new HashMap<>()));
		final List<Component> all = new ArrayList<>();
		collect(panel, all);
		final List<JCheckBox> boxes = new ArrayList<>();
		for (Component component : all)
		{
			if (component instanceof JCheckBox)
			{
				boxes.add((JCheckBox) component);
			}
		}
		assertEquals(3, boxes.size());
		assertFalse(boxes.get(0).isSelected());
		assertTrue(boxes.get(1).isSelected());
		assertTrue(boxes.get(2).isSelected());
		boxes.get(1).doClick(0);
		assertEquals(Arrays.asList("flip 19"), recorder.calls);
	}

	/** The marks of the Manual tests: Barrows and Zulrah differ from the account, DK: Prime does not. */
	private static Map<Integer, Mark> threeMarks()
	{
		final Map<Integer, Mark> marks = new HashMap<>();
		marks.put(5, Mark.DONE);
		marks.put(19, Mark.DONE);
		marks.put(72, Mark.NOT_DONE);
		return marks;
	}

	@Test
	public void manualLinksCallActions()
	{
		final Recorder recorder = new Recorder();
		final CaBossChecklistPanel panel = panel(recorder, view(threeMarks()));

		assertTrue(labelsWithText(panel, "Keep").isEmpty());
		assertTrue(labelsWithText(panel, "Remove").isEmpty());
		final List<JLabel> remove = labelsWithText(panel, "\u00d7");
		assertEquals(3, remove.size());
		press(remove.get(0));
		final List<JLabel> removeAll = labelsWithText(panel, "Remove all");
		assertEquals(1, removeAll.size());
		press(removeAll.get(0));
		assertEquals(Arrays.asList("remove [5]", "remove [19]"), recorder.calls);
	}

	/** The name label of {@code boss} in the row that also holds a label {@code word}. */
	private static JLabel nameInRowWith(CaBossChecklistPanel panel, String boss, String word)
	{
		for (JLabel label : labelsWithText(panel, boss))
		{
			final Component lines = label.getParent().getParent();
			if (lines != null && !labelsIn(lines, word).isEmpty())
			{
				return label;
			}
		}
		throw new AssertionError(boss);
	}

	private static void assertCount(CaBossChecklistPanel panel, String text, int count, Color colour)
	{
		final List<JLabel> found = labelsWithText(panel, text);
		assertEquals(text, count, found.size());
		for (JLabel label : found)
		{
			assertEquals(text, colour, label.getForeground());
		}
	}

	@Test
	public void manualRowsSayWhatIsOverridden()
	{
		final CaBossChecklistPanel panel = panel(new Recorder(), view(threeMarks()));
		for (String word : new String[] {"Your marks override your account (2)", "Same as your account (1)",
			" \u00b7 2 of 6 done", " \u00b7 8 of 8 done", " \u00b7 4 of 4 done"})
		{
			assertEquals(word, 1, labelsWithText(panel, word).size());
		}
		for (String word : new String[] {"Boss", "You", "Account"})
		{
			assertTrue(word, labelsWithText(panel, word).isEmpty());
		}
		final Color green = new Color(0x0DC10D);
		final Color orange = new Color(0xFF981F);
		assertCount(panel, "Green", 2, green);
		assertCount(panel, "Orange", 1, orange);
		assertCount(panel, "orange", 1, orange);
		assertCount(panel, "green", 1, green);
		assertCount(panel, " instead of ", 2, ColorScheme.LIGHT_GRAY_COLOR);
		assertCount(panel, " either way", 1, ColorScheme.LIGHT_GRAY_COLOR);
		assertEquals("Your account: 4 tasks left", labelsWithText(panel,
			" \u00b7 2 of 6 done").get(0).getToolTipText());
		assertEquals("Barrows", nameInRowWith(panel, "Barrows", "orange").getToolTipText());
	}

	@Test
	public void manualWithTheSwitchOffIsOneGroup()
	{
		final Map<Integer, String> labels = new HashMap<>();
		labels.put(5, "Barrows");
		labels.put(19, "DK: Prime");
		labels.put(72, "Zulrah");
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(5, new CaCompletion.Progress(6, 2));
		progress.put(19, new CaCompletion.Progress(4, 4));
		progress.put(72, new CaCompletion.Progress(8, 8));
		final CaBossChecklistPanel panel = panel(new Recorder(),
			ChecklistView.build(labels, Collections.emptyMap(), progress, threeMarks(), false, false, Collections.emptyMap()));
		assertEquals(1, labelsWithText(panel, "Your marks (3)").size());
		for (JLabel label : all(panel, JLabel.class))
		{
			final String text = String.valueOf(label.getText());
			assertFalse(text, text.startsWith("Different") || text.startsWith("Same")
				|| text.startsWith("Your marks override"));
		}
		assertTrue(labelsWithText(panel, "Remove all").isEmpty());
		assertEquals(3, labelsWithText(panel, "\u00d7").size());
		assertEquals(2, labelsWithText(panel, " instead of ").size());
		assertEquals(1, labelsWithText(panel, " either way").size());
	}

	/** The view of lot 3's tests: a long name for DK: Prime only. */
	private static ChecklistView searchView()
	{
		final Map<Integer, String> labels = new HashMap<>();
		labels.put(5, "Barrows");
		labels.put(19, "DK: Prime");
		labels.put(72, "Zulrah");
		final Map<Integer, String> names = new HashMap<>();
		names.put(19, "Dagannoth Prime");
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(5, new CaCompletion.Progress(6, 2));
		progress.put(19, new CaCompletion.Progress(4, 4));
		progress.put(72, new CaCompletion.Progress(8, 8));
		return ChecklistView.build(labels, names, progress, new HashMap<>(), true, false, Collections.emptyMap());
	}

	private static <T> List<T> all(Component root, Class<T> type)
	{
		final List<Component> every = new ArrayList<>();
		collect(root, every);
		final List<T> found = new ArrayList<>();
		for (Component component : every)
		{
			if (type.isInstance(component))
			{
				found.add(type.cast(component));
			}
		}
		return found;
	}

	private static List<JLabel> labelsIn(Component root, String text)
	{
		final List<JLabel> found = new ArrayList<>();
		for (JLabel label : all(root, JLabel.class))
		{
			if (text.equals(label.getText()))
			{
				found.add(label);
			}
		}
		return found;
	}

	/** The card, or null while it is hidden (out of the view, or not visible). */
	private static BossCard card(CaBossChecklistPanel panel)
	{
		for (BossCard card : all(panel, BossCard.class))
		{
			if (card.isVisible())
			{
				return card;
			}
		}
		return null;
	}

	/** Types into the search box through its document, as setText insists on the EDT and these tests run without one. */
	private static void type(CaBossChecklistPanel panel, String text) throws BadLocationException
	{
		final List<IconTextField> boxes = all(panel, IconTextField.class);
		assertEquals(1, boxes.size());
		final AbstractDocument document = (AbstractDocument) boxes.get(0).getDocument();
		document.replace(0, document.getLength(), text, null);
	}

	private static void pressLeft(Component target)
	{
		target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 1, 1, 1, false,
			MouseEvent.BUTTON1));
	}

	@Test
	public void searchFiltersAndChoosesTheFirst() throws Exception
	{
		final Recorder recorder = new Recorder();
		final CaBossChecklistPanel panel = panel(recorder, searchView());
		assertNull("no text, no card", card(panel));

		type(panel, "dagan");
		assertEquals(1, labelsWithText(panel, "DK: Prime").size());
		assertEquals(0, labelsWithText(panel, "Barrows").size());
		assertEquals(0, labelsWithText(panel, "Zulrah").size());
		final BossCard card = card(panel);
		assertNotNull(card);
		assertEquals(1, labelsIn(card, "Dagannoth Prime").size());
		assertEquals(1, labelsIn(card, "4 of 4 tasks done").size());
		JCheckBox green = null;
		for (JCheckBox box : all(card, JCheckBox.class))
		{
			if ("Green in the Tasks menu".equals(box.getText()))
			{
				green = box;
			}
		}
		assertNotNull(green);
		assertTrue(green.isSelected());
		green.doClick(0);
		assertEquals(Arrays.asList("flip 19"), recorder.calls);

		type(panel, "qqq");
		assertEquals(1, labelsWithText(panel, "No boss matches").size());
		assertNull(card(panel));

		type(panel, "");
		assertEquals(1, labelsWithText(panel, "Barrows").size());
		assertEquals(1, labelsWithText(panel, "DK: Prime").size());
		assertEquals(1, labelsWithText(panel, "Zulrah").size());
		assertNull(card(panel));
	}

	@Test
	public void rowPressChoosesAndClears()
	{
		final Recorder recorder = new Recorder();
		final CaBossChecklistPanel panel = panel(recorder, searchView());
		final List<JLabel> barrows = labelsWithText(panel, "Barrows");
		assertEquals(1, barrows.size());
		final Component row = barrows.get(0).getParent();

		pressLeft(row);
		final BossCard card = card(panel);
		assertNotNull(card);
		assertEquals(1, labelsIn(card, "Barrows").size());

		pressLeft(row);
		assertNull(card(panel));
		assertEquals(Collections.emptyList(), recorder.calls);
	}

	@Test
	public void theCardHasAWikiLink()
	{
		final CaBossChecklistPanel panel = panel(new Recorder(), searchView());
		pressLeft(labelsWithText(panel, "Barrows").get(0).getParent());
		assertNotNull(card(panel));
		final List<JLabel> wiki = labelsWithText(panel, "Wiki");
		assertEquals(1, wiki.size());
		assertTrue(wiki.get(0) instanceof LinkLabel);
	}

	/** Whether the component and each of its parents up to the card answer isVisible(): the tests have no window. */
	private static boolean shown(Component component, BossCard card)
	{
		for (Component c = component; c != null; c = c.getParent())
		{
			if (!c.isVisible())
			{
				return false;
			}
			if (c == card)
			{
				return true;
			}
		}
		return false;
	}

	/** The labels in the card that are shown and read {@code text}. */
	private static List<JLabel> shownLabels(BossCard card, String text)
	{
		final List<JLabel> found = new ArrayList<>();
		for (JLabel label : labelsIn(card, text))
		{
			if (shown(label, card))
			{
				found.add(label);
			}
		}
		return found;
	}

	/** The card of {@code panel} after the row of Barrows (or {@code name}) is pressed. */
	private static BossCard cardOf(CaBossChecklistPanel panel, String name)
	{
		for (JLabel label : labelsWithText(panel, name))
		{
			pressLeft(label.getParent());
			final BossCard card = card(panel);
			if (card != null)
			{
				return card;
			}
		}
		throw new AssertionError(name);
	}

	private static MarkTag onlyTag(BossCard card)
	{
		final List<MarkTag> tags = all(card, MarkTag.class);
		assertEquals(1, tags.size());
		return tags.get(0);
	}

	@Test
	public void anUntouchedCardSaysNothingUnderItsBox()
	{
		final CaBossChecklistPanel panel = panel(new Recorder(), searchView());
		pressLeft(labelsWithText(panel, "Barrows").get(0).getParent());
		final BossCard card = card(panel);
		assertNotNull(card);
		assertFalse(shown(onlyTag(card), card));
		for (ChecklistView.CardNote note : ChecklistView.CardNote.values())
		{
			if (!note.text.isEmpty())
			{
				assertTrue(note.text, shownLabels(card, note.text).isEmpty());
			}
		}
		assertTrue(labelsWithText(panel, "From your account").isEmpty());
		assertTrue(labelsWithText(panel, "Marked by you").isEmpty());
	}

	@Test
	public void aMarkThatOverridesShowsTheOrangeTag()
	{
		final BossCard barrows = cardOf(panel(new Recorder(), view(only(5, Mark.DONE))), "Barrows");
		assertTrue(shown(onlyTag(barrows), barrows));
		assertTrue(onlyTag(barrows).strong);
		assertEquals(1, shownLabels(barrows, ChecklistView.CardNote.OVERRIDES.text).size());
		final BossCard prime = cardOf(panel(new Recorder(), view(only(19, Mark.DONE))), "DK: Prime");
		assertTrue(shown(onlyTag(prime), prime));
		assertFalse(onlyTag(prime).strong);
		assertEquals(1, shownLabels(prime, ChecklistView.CardNote.SAME.text).size());
	}

	@Test
	public void withAutoOffTheCardKeepsItsHintAndAQuietTag()
	{
		final BossCard hint = cardOf(panel(new Recorder(), viewSeen(new HashMap<>(), false, Collections.emptyMap())),
			"Barrows");
		assertFalse(shown(onlyTag(hint), hint));
		assertEquals(1, shownLabels(hint, ChecklistView.CardNote.HINT.text).size());
		final BossCard marked = cardOf(panel(new Recorder(), viewSeen(only(5, Mark.DONE), false, Collections.emptyMap())),
			"Barrows");
		assertTrue(shown(onlyTag(marked), marked));
		assertFalse(onlyTag(marked).strong);
		for (ChecklistView.CardNote note : ChecklistView.CardNote.values())
		{
			if (!note.text.isEmpty())
			{
				assertTrue(note.text, shownLabels(marked, note.text).isEmpty());
			}
		}
	}

	@Test
	public void theNoteTakesNoRoomWhenThereIsNothingToSay()
	{
		final BossCard untouched = cardOf(panel(new Recorder(), view(new HashMap<>())), "Barrows");
		final BossCard marked = cardOf(panel(new Recorder(), view(only(5, Mark.DONE))), "Barrows");
		final int more = marked.getPreferredSize().height - untouched.getPreferredSize().height;
		assertTrue("the marked card is " + more + " px taller", more >= 12 && more <= 30);
		assertTrue(onlyTag(marked).getParent().getPreferredSize().width <= 203);
	}

	@Test
	public void theHintAloneKeepsItsSizeAndCentre() throws Exception
	{
		final List<String> problems = new ArrayList<>();
		SwingUtilities.invokeAndWait(() ->
		{
			final CaBossChecklistPanel panel = panel(new Recorder(),
				viewSeen(new HashMap<>(), false, Collections.emptyMap()));
			panel.setSize(242, 560);
			final BossCard card = cardOf(panel, "Barrows");
			layOut(panel);
			final List<JLabel> hints = shownLabels(card, ChecklistView.CardNote.HINT.text);
			if (hints.size() != 1)
			{
				problems.add("hint labels shown: " + hints.size());
				return;
			}
			final JLabel hint = hints.get(0);
			if (hint.getWidth() < hint.getPreferredSize().width)
			{
				problems.add("width " + hint.getWidth() + " of " + hint.getPreferredSize().width);
			}
			if (hint.getHeight() < hint.getPreferredSize().height)
			{
				problems.add("height " + hint.getHeight() + " of " + hint.getPreferredSize().height);
			}
			int centre = hint.getWidth() / 2;
			for (Component c = hint; c != card; c = c.getParent())
			{
				centre += c.getX();
			}
			if (Math.abs(centre - card.getWidth() / 2.0) > 2)
			{
				problems.add("centre " + centre + " of a card " + card.getWidth() + " wide");
			}
		});
		assertTrue(problems.toString(), problems.isEmpty());
	}

	/** The page answer naming a PNG on the wiki's host. */
	private static final String PAGE_WITH_PICTURE = "{\"query\":{\"pages\":[{\"title\":\"Zulrah\",\"thumbnail\":{\"source\":"
		+ "\"https://oldschool.runescape.wiki/images/thumb/Zulrah.png/128px-Zulrah.png?abc\",\"width\":128,\"height\":40}}]}}";
	/** The page answer for a page with no thumbnail. */
	private static final String PAGE_WITHOUT_PICTURE = "{\"query\":{\"pages\":[{\"title\":\"Zulrah\"}]}}";

	private static byte[] fixtureBytes() throws IOException
	{
		try (InputStream in = CaBossChecklistPanelTest.class.getResourceAsStream("/com/cabosschecklist/picture-fixture.png"))
		{
			assertNotNull("the fixture is on the test classpath", in);
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final byte[] chunk = new byte[4096];
			for (int read = in.read(chunk); read >= 0; read = in.read(chunk))
			{
				out.write(chunk, 0, read);
			}
			return out.toByteArray();
		}
	}

	/** A stand-in factory whose calls answer at once: {@code page} for the wiki's api.php, the fixture for a picture. */
	private static Call.Factory answering(String page, byte[] picture)
	{
		return request ->
		{
			final Call call = mock(Call.class);
			doAnswer(invocation ->
			{
				final Callback callback = invocation.getArgument(0);
				final boolean isPage = request.url().encodedPath().equals("/api.php");
				callback.onResponse(call, new Response.Builder()
					.request(request)
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(ResponseBody.create(MediaType.parse(isPage ? "application/json" : "image/png"),
						isPage ? page.getBytes(StandardCharsets.UTF_8) : picture))
					.build());
				return null;
			}).when(call).enqueue(any(Callback.class));
			return call;
		};
	}

	/** The card's 64 x 64 picture box, found by its size. */
	private static JLabel pictureBox(BossCard card)
	{
		for (JLabel label : all(card, JLabel.class))
		{
			if (label.getPreferredSize().width == 64 && label.getPreferredSize().height == 64)
			{
				return label;
			}
		}
		throw new AssertionError("no 64 x 64 box");
	}

	/** A panel with the wiki switch on over the given stand-in, showing {@link #searchView()}. */
	private static CaBossChecklistPanel wikiPanel(Call.Factory factory)
	{
		final CaBossChecklistPanel[] self = new CaBossChecklistPanel[1];
		final WikiPictures pictures = new WikiPictures(factory, new Gson(), () -> self[0].pictureArrived());
		self[0] = new CaBossChecklistPanel(new TestSettings(true), new Recorder(), pictures,
			(t, q, y) -> false);
		self[0].show(searchView());
		return self[0];
	}

	@Test
	public void theCardShowsThePictureWhenItArrives() throws Exception
	{
		final CaBossChecklistPanel with = wikiPanel(answering(PAGE_WITH_PICTURE, fixtureBytes()));
		type(with, "zulrah");
		final BossCard card = card(with);
		assertNotNull(card);
		final JLabel box = pictureBox(card);
		assertTrue(box.isVisible());
		final Icon icon = box.getIcon();
		assertNotNull(icon);
		assertEquals(64, icon.getIconWidth());
		assertEquals(20, icon.getIconHeight());

		final CaBossChecklistPanel without = wikiPanel(answering(PAGE_WITHOUT_PICTURE, fixtureBytes()));
		type(without, "zulrah");
		final BossCard bare = card(without);
		assertNotNull(bare);
		assertFalse(pictureBox(bare).isVisible());
	}

	/** Dagannoth Prime's four real tasks (monster 19), as the game's words give them. */
	private static List<CaCompletion.TaskLine> primeLines()
	{
		return Arrays.asList(
			new CaCompletion.TaskLine(197, "Dagannoth Prime Champion", "Kill Dagannoth Prime 10 times.", "Medium", "Kill Count", true),
			new CaCompletion.TaskLine(198, "Dagannoth Prime Adept", "Kill Dagannoth Prime 25 times.", "Hard", "Kill Count", false),
			new CaCompletion.TaskLine(199, "Death to the Seer King",
				"Kill Dagannoth Prime whilst under attack by Dagannoth Supreme and Dagannoth Rex.", "Elite", "Mechanical", true),
			new CaCompletion.TaskLine(200, "From One King to Another",
				"Kill Prime using a Rune Thrownaxe special attack, bounced off Dagannoth Rex.", "Elite", "Mechanical", false));
	}

	/** Lot 3's three-boss view with Dagannoth Prime's four task rows. */
	private static ChecklistView tasksView()
	{
		final Map<Integer, List<CaCompletion.TaskLine>> lines = new HashMap<>();
		lines.put(19, primeLines());
		return searchView().withTasks(lines);
	}

	/** The label with {@code text}, or an error when there is none. */
	private static JLabel oneLabel(Component root, String text)
	{
		final List<JLabel> found = labelsIn(root, text);
		assertEquals(text, 1, found.size());
		return found.get(0);
	}

	@Test
	public void theChosenBossOpensUnderItsRow()
	{
		final CaBossChecklistPanel panel = panel(new Recorder(), tasksView());
		assertEquals(0, labelsWithText(panel, "Dagannoth Prime Champion").size());
		assertNull(card(panel));

		final JLabel prime = oneLabel(panel, "DK: Prime");
		final Component row = prime.getParent();
		final Container column = row.getParent();
		pressLeft(row);

		final Component[] parts = column.getComponents();
		int at = -1;
		for (int i = 0; i < parts.length; i++)
		{
			if (parts[i] == row)
			{
				at = i;
			}
		}
		assertTrue(at >= 0 && at + 1 < parts.length);
		final Container opened = (Container) parts[at + 1];
		final BossCard card = card(panel);
		assertNotNull(card);
		assertEquals(1, labelsIn(opened, "4 of 4 tasks done").size());
		final JLabel champion = oneLabel(opened, "Dagannoth Prime Champion");
		final JLabel adept = oneLabel(opened, "Dagannoth Prime Adept");
		assertEquals(ColorScheme.LIGHT_GRAY_COLOR, champion.getForeground());
		assertEquals(Color.WHITE, adept.getForeground());
		assertEquals(2, labelsIn(opened, "Elite \u00b7 Mechanical").size());
		final List<String> names = Arrays.asList("Dagannoth Prime Champion", "Dagannoth Prime Adept", "Death to the Seer King",
			"From One King to Another");
		final List<String> order = new ArrayList<>();
		for (JLabel label : all(opened, JLabel.class))
		{
			final String text = label.getText();
			if (names.contains(text))
			{
				order.add(text);
			}
		}
		assertEquals(Arrays.asList("Dagannoth Prime Champion", "Dagannoth Prime Adept", "Death to the Seer King",
			"From One King to Another"), order);

		pressLeft(row);
		assertEquals(0, labelsWithText(panel, "Dagannoth Prime Champion").size());
		assertEquals(0, labelsWithText(panel, "Dagannoth Prime Adept").size());
	}

	@Test
	public void aTaskPressShowsItsDescription()
	{
		final CaBossChecklistPanel panel = panel(new Recorder(), tasksView());
		pressLeft(oneLabel(panel, "DK: Prime").getParent());
		final JLabel seer = oneLabel(panel, "Death to the Seer King");
		assertEquals(1, labelsWithText(panel, "Wiki").size());
		pressLeft(seer.getParent());
		boolean described = false;
		for (JLabel label : all(panel, JLabel.class))
		{
			described |= label.getText() != null && label.getText().replace("<br>", " ").contains("whilst under attack by Dagannoth Supreme");
		}
		assertTrue(described);
		assertEquals(2, labelsWithText(panel, "Wiki").size());

		pressLeft(oneLabel(panel, "Death to the Seer King").getParent());
		for (JLabel label : all(panel, JLabel.class))
		{
			assertFalse(label.getText() != null && label.getText().replace("<br>", " ").contains("whilst under attack by Dagannoth Supreme"));
		}
		assertEquals(1, labelsWithText(panel, "Wiki").size());
	}

	/**
	 * Found in the lead's review: the name carries its full text as a hover, and a label with a hover takes the mouse
	 * itself, so a press on the name (what a player clicks) must open the task too.
	 */
	@Test
	public void aPressOnTheTaskNameOpensIt()
	{
		final CaBossChecklistPanel panel = panel(new Recorder(), tasksView());
		pressLeft(oneLabel(panel, "DK: Prime").getParent());
		pressLeft(oneLabel(panel, "Death to the Seer King"));
		assertEquals(2, labelsWithText(panel, "Wiki").size());
		pressLeft(oneLabel(panel, "Death to the Seer King"));
		assertEquals(1, labelsWithText(panel, "Wiki").size());
	}

	@Test
	public void rowNamesTakeTheGameColours()
	{
		final Map<Integer, String> labels = new HashMap<>();
		labels.put(5, "Barrows");
		labels.put(19, "DK: Prime");
		labels.put(72, "Zulrah");
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(5, new CaCompletion.Progress(3, 1));
		progress.put(19, new CaCompletion.Progress(4, 4));
		progress.put(72, new CaCompletion.Progress(8, 1));
		final CaBossChecklistPanel panel = panel(new Recorder(),
			ChecklistView.build(labels, Collections.emptyMap(), progress, Collections.emptyMap(), true, true, Collections.emptyMap()));
		final Color quiet = ViewParts.quiet("").getForeground();
		assertEquals(new Color(0xFFFF00), oneLabel(panel, "Barrows").getForeground());
		assertEquals(new Color(0x0DC10D), oneLabel(panel, "DK: Prime").getForeground());
		assertEquals(new Color(0xFF981F), oneLabel(panel, "Zulrah").getForeground());
		assertEquals(quiet, oneLabel(panel, "Done").getForeground());
		assertEquals(quiet, oneLabel(panel, "2 left").getForeground());
		assertEquals(quiet, oneLabel(panel, "7 left").getForeground());
	}

	/** The view of the dot tests: Barrows and Zulrah override the account when marked. */
	private static ChecklistView viewSeen(Map<Integer, Mark> marks, boolean auto, Map<Integer, Mark> seen)
	{
		final Map<Integer, String> labels = new HashMap<>();
		labels.put(5, "Barrows");
		labels.put(19, "DK: Prime");
		labels.put(72, "Zulrah");
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(5, new CaCompletion.Progress(6, 2));
		progress.put(19, new CaCompletion.Progress(4, 4));
		progress.put(72, new CaCompletion.Progress(8, 8));
		return ChecklistView.build(labels, Collections.emptyMap(), progress, marks, auto, false, seen);
	}

	private static Map<Integer, Mark> only(int id, Mark mark)
	{
		final Map<Integer, Mark> marks = new HashMap<>();
		marks.put(id, mark);
		return marks;
	}

	private static Map<Integer, Mark> fiveAndZulrah()
	{
		final Map<Integer, Mark> marks = only(5, Mark.DONE);
		marks.put(72, Mark.NOT_DONE);
		return marks;
	}

	/** The Manual half of the toggle: the label whose text starts "Manual". */
	private static JLabel manualHalf(CaBossChecklistPanel panel)
	{
		for (JLabel label : all(panel, JLabel.class))
		{
			if (String.valueOf(label.getText()).startsWith("Manual"))
			{
				return label;
			}
		}
		throw new AssertionError("no Manual half");
	}

	@Test
	public void theDotShowsForAnUnseenOverride() throws Exception
	{
		SwingUtilities.invokeAndWait(CaBossChecklistPanelTest::dotShowsOnTheEdt);
	}

	/** Run on the EDT, as showLoggedOut clears the search box, which refuses any other thread. */
	private static void dotShowsOnTheEdt()
	{
		final Recorder recorder = new Recorder();
		final CaBossChecklistPanel unseen = panel(recorder, viewSeen(only(5, Mark.DONE), true, Collections.emptyMap()));
		assertNotNull(manualHalf(unseen).getIcon());
		assertEquals("A new mark overrides your account", manualHalf(unseen).getToolTipText());
		final CaBossChecklistPanel seen = panel(recorder, viewSeen(only(5, Mark.DONE), true, only(5, Mark.DONE)));
		assertNull(manualHalf(seen).getIcon());
		assertNull(manualHalf(seen).getToolTipText());
		final CaBossChecklistPanel same = panel(recorder, viewSeen(only(19, Mark.DONE), true, Collections.emptyMap()));
		assertNull(manualHalf(same).getIcon());
		final CaBossChecklistPanel off = panel(recorder, viewSeen(only(5, Mark.DONE), false, Collections.emptyMap()));
		assertNull(manualHalf(off).getIcon());
		unseen.showLoggedOut();
		assertNull(manualHalf(unseen).getIcon());
		assertTrue(recorder.calls.toString(), recorder.calls.isEmpty());
	}

	@Test
	public void lookingAtManualRemembersAndClearsTheDot()
	{
		final Recorder recorder = new Recorder();
		final ChecklistView unseen = viewSeen(fiveAndZulrah(), true, Collections.emptyMap());
		final CaBossChecklistPanel panel = panel(recorder, unseen);
		panel.onActivate();
		assertTrue(recorder.calls.isEmpty());
		assertNotNull(manualHalf(panel).getIcon());
		press(manualHalf(panel));
		assertEquals(Arrays.asList("remember [5=done,72=not-done]"), recorder.calls);
		assertNull(manualHalf(panel).getIcon());
		panel.show(viewSeen(fiveAndZulrah(), true, fiveAndZulrah()));
		press(labelsWithText(panel, "Bosses").get(0));
		assertNull(manualHalf(panel).getIcon());
		assertEquals(1, recorder.calls.size());
	}

	@Test
	public void aClosedSidebarRemembersNothingUntilItOpens()
	{
		final Recorder recorder = new Recorder();
		final ChecklistView unseen = viewSeen(fiveAndZulrah(), true, Collections.emptyMap());
		final CaBossChecklistPanel panel = panel(recorder, unseen);
		press(manualHalf(panel));
		assertTrue(recorder.calls.isEmpty());
		panel.onActivate();
		assertEquals(Arrays.asList("remember [5=done,72=not-done]"), recorder.calls);
		panel.onDeactivate();
		panel.show(unseen);
		assertEquals(1, recorder.calls.size());
	}

	@Test
	public void seenMarksThatNoLongerOverrideAreForgotten()
	{
		final Recorder recorder = new Recorder();
		panel(recorder, viewSeen(only(19, Mark.DONE), true, only(19, Mark.DONE)));
		assertEquals(Arrays.asList("remember []"), recorder.calls);
	}

	@Test
	public void askingToHideAsksOnceAndHidesOnlyOnYes()
	{
		assertEquals("Hide the sidebar button...", CaBossChecklistPanel.HIDE_TEXT);
		for (boolean answer : new boolean[] {true, false})
		{
			final List<String> asked = new ArrayList<>();
			final TestSettings settings = new TestSettings(false);
			final CaBossChecklistPanel panel = new CaBossChecklistPanel(settings, new Recorder(), noWiki(),
				(title, question, yes) ->
				{
					asked.add(title + "|" + question + "|" + yes);
					return answer;
				});
			panel.askToHide();
			assertEquals(Collections.singletonList("Hide the sidebar button|" + CaBossChecklistPanel.HIDE_ASK + "|Hide"),
				asked);
			assertEquals(answer ? 1 : 0, settings.hidden);
		}
	}

	@Test
	public void theIconChoicesCarryTheirPictures()
	{
		final CaBossChecklistPanel panel = new CaBossChecklistPanel(new TestSettings(false), new Recorder(), noWiki(),
			(t, q, y) -> false);
		assertEquals(3, panel.iconItems.size());
		assertEquals("2h coin and book", panel.iconItems.get(0).getText());
		assertEquals("Book only", panel.iconItems.get(1).getText());
		assertEquals("2h coin and CA letters", panel.iconItems.get(2).getText());
		panel.iconPictures(null);
		assertNull(panel.iconItems.get(0).picture);
		assertNull(panel.iconItems.get(1).picture);
		assertEquals(16, panel.iconItems.get(2).picture.getWidth());
		assertEquals(16, panel.iconItems.get(2).picture.getHeight());
		panel.iconPictures(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB));
		for (CaBossChecklistPanel.IconChoice item : panel.iconItems)
		{
			assertEquals(16, item.picture.getWidth());
			assertEquals(16, item.picture.getHeight());
		}
	}

	private static BufferedImage painted(CaBossChecklistPanel.IconChoice item)
	{
		final Dimension size = item.getPreferredSize();
		item.setSize(size);
		final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		item.paint(g);
		g.dispose();
		return image;
	}

	@Test
	public void anIconChoiceDrawsItsPictureAtTheRight()
	{
		final CaBossChecklistPanel panel = new CaBossChecklistPanel(new TestSettings(false), new Recorder(), noWiki(),
			(t, q, y) -> false);
		final CaBossChecklistPanel.IconChoice item = panel.iconItems.get(0);
		final JRadioButtonMenuItem plain = new JRadioButtonMenuItem(item.getText());
		plain.setFont(item.getFont());
		assertEquals(plain.getPreferredSize().width + 24, item.getPreferredSize().width);

		final BufferedImage magenta = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = magenta.createGraphics();
		g.setColor(Color.MAGENTA);
		g.fillRect(0, 0, 16, 16);
		g.dispose();
		item.setPicture(magenta);
		BufferedImage drawn = painted(item);
		final int x = drawn.getWidth() - item.getInsets().right - 8;
		final int y = drawn.getHeight() / 2;
		assertEquals(Color.MAGENTA.getRGB(), drawn.getRGB(x, y));
		item.setPicture(null);
		drawn = painted(item);
		assertFalse(Color.MAGENTA.getRGB() == drawn.getRGB(x, y));
	}
}
