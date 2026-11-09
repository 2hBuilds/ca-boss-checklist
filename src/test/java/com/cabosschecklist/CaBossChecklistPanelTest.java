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
import java.awt.event.MouseEvent;
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
		public void keep(int id)
		{
			calls.add("keep " + id);
		}

		@Override
		public void remove(List<Integer> ids)
		{
			calls.add("remove " + ids);
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
		return ChecklistView.build(labels, Collections.emptyMap(), progress, marks, true);
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
		public void resetToDefault()
		{
		}
	}

	private static CaBossChecklistPanel panel(Recorder recorder, ChecklistView view)
	{
		final CaBossChecklistPanel panel = new CaBossChecklistPanel(new TestSettings(false), recorder, noWiki());
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

	@Test
	public void manualLinksCallActions()
	{
		final Recorder recorder = new Recorder();
		final Map<Integer, Mark> marks = new HashMap<>();
		marks.put(5, Mark.DONE);
		marks.put(19, Mark.DONE);
		marks.put(72, Mark.NOT_DONE_KEPT);
		final CaBossChecklistPanel panel = panel(recorder, view(marks));

		final List<JLabel> keep = labelsWithText(panel, "Keep");
		assertEquals(1, keep.size());
		press(keep.get(0));
		final List<JLabel> remove = labelsWithText(panel, "Remove");
		assertEquals(3, remove.size());
		press(remove.get(0));
		final List<JLabel> removeAll = labelsWithText(panel, "Remove all");
		assertEquals(1, removeAll.size());
		press(removeAll.get(0));
		assertEquals(Arrays.asList("keep 5", "remove [5]", "remove [19]"), recorder.calls);
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
		return ChecklistView.build(labels, names, progress, new HashMap<>(), true);
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
		self[0] = new CaBossChecklistPanel(new TestSettings(true), new Recorder(), pictures);
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
		assertEquals(0, labelsWithText(panel, "Wiki").size());
		pressLeft(seer.getParent());
		boolean described = false;
		for (JLabel label : all(panel, JLabel.class))
		{
			described |= label.getText() != null && label.getText().replace("<br>", " ").contains("whilst under attack by Dagannoth Supreme");
		}
		assertTrue(described);
		assertEquals(1, labelsWithText(panel, "Wiki").size());

		pressLeft(oneLabel(panel, "Death to the Seer King").getParent());
		for (JLabel label : all(panel, JLabel.class))
		{
			assertFalse(label.getText() != null && label.getText().replace("<br>", " ").contains("whilst under attack by Dagannoth Supreme"));
		}
		assertEquals(0, labelsWithText(panel, "Wiki").size());
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
		assertEquals(1, labelsWithText(panel, "Wiki").size());
		pressLeft(oneLabel(panel, "Death to the Seer King"));
		assertEquals(0, labelsWithText(panel, "Wiki").size());
	}
}
