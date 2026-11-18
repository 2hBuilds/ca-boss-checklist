package com.cabosschecklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;

/** Lot 15: the chosen boss in a frame, the tasks in their own box, an open task's band, and the tier words' colours. */
public class FramedCardTest
{
	private static final int WIDTH = 242;
	private static final int HEIGHT = 900;
	private static final String[] TIERS = {"Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"};

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

	/** A view of Barrows, DK: Prime and Zulrah, with six tasks (one per tier) on DK: Prime. */
	private static ChecklistView view()
	{
		final Map<Integer, String> labels = new HashMap<>();
		labels.put(5, "Barrows");
		labels.put(19, "DK: Prime");
		labels.put(72, "Zulrah");
		final Map<Integer, CaCompletion.Progress> progress = new HashMap<>();
		progress.put(5, new CaCompletion.Progress(6, 2));
		progress.put(19, new CaCompletion.Progress(6, 1));
		progress.put(72, new CaCompletion.Progress(8, 8));
		final List<CaCompletion.TaskLine> lines = new ArrayList<>();
		for (int i = 0; i < TIERS.length; i++)
		{
			lines.add(new CaCompletion.TaskLine(100 + i, "Task " + TIERS[i], "Does the " + TIERS[i] + " thing once.", TIERS[i],
				"Speed", i == 0));
		}
		final Map<Integer, List<CaCompletion.TaskLine>> tasks = new HashMap<>();
		tasks.put(19, lines);
		return ChecklistView.build(labels, Collections.emptyMap(), progress, new HashMap<>(), true, CaBossChecklistConfig.YellowWhen.OFF,
			Collections.emptyMap()).withTasks(tasks);
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

	private static JLabel label(Component root, String text)
	{
		for (JLabel label : all(root, JLabel.class))
		{
			if (text.equals(label.getText()))
			{
				return label;
			}
		}
		throw new AssertionError("no label '" + text + "'");
	}

	/** Lays the view out as a shown sidebar would: every part invalid, then each container laid out, top down. */
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

	/** {@code part}'s bounds in {@code root}'s coordinates. */
	private static Rectangle bounds(Component part, Component root)
	{
		final Point at = SwingUtilities.convertPoint(part.getParent(), part.getLocation(), root);
		return new Rectangle(at.x, at.y, part.getWidth(), part.getHeight());
	}

	private static boolean is(BufferedImage image, int x, int y, Color colour)
	{
		return (image.getRGB(x, y) & 0xFFFFFF) == (colour.getRGB() & 0xFFFFFF);
	}

	/** The row panel of {@code boss} and the opened panel under it. */
	private static Component[] rowAndOpened(BossesView bosses, String boss)
	{
		final Component row = label(bosses, boss).getParent();
		final Component[] parts = row.getParent().getComponents();
		for (int i = 0; i < parts.length; i++)
		{
			if (parts[i] == row)
			{
				return new Component[]{row, parts[i + 1]};
			}
		}
		throw new AssertionError("row not in its column");
	}

	@Test
	public void theChosenBlockIsFramedAndOtherRowsAreNot() throws Exception
	{
		final BossesView bosses = shown();
		SwingUtilities.invokeAndWait(() ->
		{
			press(label(bosses, "DK: Prime").getParent());
			layOut(bosses);
			final Component[] parts = rowAndOpened(bosses, "DK: Prime");
			final Rectangle row = bounds(parts[0], bosses);
			final Rectangle opened = bounds(parts[1], bosses);
			final BufferedImage image = paint(bosses);
			final Color frame = ViewParts.FRAME;
			final int mid = row.x + row.width / 2;

			assertEquals("the row sits right on its block", row.y + row.height, opened.y);
			// The row: top, left and right lines, two px wide.
			for (int w = 0; w < 2; w++)
			{
				assertTrue("row top " + w, is(image, mid, row.y + w, frame));
				assertTrue("row left " + w, is(image, row.x + w, row.y + row.height / 2, frame));
				assertTrue("row right " + w, is(image, row.x + row.width - 1 - w, row.y + row.height / 2, frame));
			}
			assertFalse("no line under the row's top", is(image, mid, row.y + 2, frame));
			assertTrue("the row keeps its chosen grey", is(image, mid, row.y + 3, ColorScheme.MEDIUM_GRAY_COLOR));
			// The join: the sides carry on, nothing crosses.
			assertTrue("row's last line", is(image, row.x, row.y + row.height - 1, frame));
			assertTrue("block's first line", is(image, row.x, opened.y, frame));
			assertTrue("right at the join", is(image, row.x + row.width - 1, opened.y, frame));
			assertFalse("no line between the row and its card", is(image, mid, row.y + row.height - 1, frame));
			assertFalse("no line between the row and its card", is(image, mid, opened.y, frame));
			assertTrue("the card's ground", is(image, mid, opened.y + 1, ViewParts.CARD_GROUND));
			// The sides of the block and its closing line.
			for (int w = 0; w < 2; w++)
			{
				assertTrue("left side " + w, is(image, opened.x + w, opened.y + opened.height / 2, frame));
				assertTrue("right side " + w, is(image, opened.x + opened.width - 1 - w, opened.y + opened.height / 2, frame));
				assertTrue("bottom " + w, is(image, mid, opened.y + opened.height - 1 - w, frame));
				assertTrue("bottom left corner " + w, is(image, opened.x, opened.y + opened.height - 1 - w, frame));
			}
			assertFalse("nothing under the frame", is(image, mid, opened.y + opened.height, frame));
			// Other rows carry no frame at all.
			for (String other : new String[]{"Barrows", "Zulrah"})
			{
				final Rectangle r = bounds(label(bosses, other).getParent(), bosses);
				for (int x = r.x; x < r.x + r.width; x++)
				{
					for (int y = r.y; y < r.y + r.height; y++)
					{
						assertFalse(other + " at " + x + "," + y, is(image, x, y, frame));
					}
				}
				assertTrue(other + " keeps its grey", is(image, r.x, r.y + r.height / 2, ColorScheme.DARK_GRAY_COLOR));
			}
		});
	}

	@Test
	public void nothingChosenDrawsNoFrame() throws Exception
	{
		final BossesView bosses = shown();
		SwingUtilities.invokeAndWait(() ->
		{
			layOut(bosses);
			final BufferedImage image = paint(bosses);
			for (int x = 0; x < image.getWidth(); x++)
			{
				for (int y = 0; y < image.getHeight(); y++)
				{
					assertFalse("at " + x + "," + y, is(image, x, y, ViewParts.FRAME));
				}
			}
			// A press on the chosen row takes the choice (and the frame) off again.
			press(label(bosses, "Zulrah").getParent());
			press(label(bosses, "Zulrah").getParent());
			layOut(bosses);
			final BufferedImage after = paint(bosses);
			final Rectangle r = bounds(label(bosses, "Zulrah").getParent(), bosses);
			assertFalse(is(after, r.x, r.y + r.height / 2, ViewParts.FRAME));
		});
	}

	@Test
	public void theTasksSitInTheirOwnBox() throws Exception
	{
		final BossesView bosses = shown();
		SwingUtilities.invokeAndWait(() ->
		{
			press(label(bosses, "DK: Prime").getParent());
			layOut(bosses);
			final Component opened = rowAndOpened(bosses, "DK: Prime")[1];
			final TaskList list = all(bosses, TaskList.class).get(0);
			final Rectangle box = bounds(list.getParent(), opened);
			final Rectangle at = bounds(list, bosses);
			final Rectangle block = bounds(opened, bosses);
			assertEquals("4 px in from the frame's left line", block.x + 2 + 4, at.x);
			assertEquals("4 px in from the frame's right line", block.x + block.width - 2 - 4, at.x + at.width);
			assertEquals("4 px above the closing line", block.y + block.height - 2 - 4, at.y + at.height);
			assertTrue(box.width > 0);
			final BufferedImage image = paint(bosses);
			assertTrue("border", is(image, at.x, at.y + at.height / 2, ViewParts.TASKS_BORDER));
			assertTrue("border", is(image, at.x + at.width - 1, at.y + at.height / 2, ViewParts.TASKS_BORDER));
			assertTrue("border", is(image, at.x + at.width / 2, at.y, ViewParts.TASKS_BORDER));
			assertTrue("border", is(image, at.x + at.width / 2, at.y + at.height - 1, ViewParts.TASKS_BORDER));
			assertTrue("ground", is(image, at.x + at.width - 3, at.y + 4, ViewParts.TASKS_GROUND));
			assertTrue("card ground round the box", is(image, at.x - 2, at.y + at.height / 2, ViewParts.CARD_GROUND));
			// A 1 px line between tasks, none after the last.
			int lines = 0;
			for (Component part : list.getComponents())
			{
				if (ViewParts.TASK_LINE.equals(part.getBackground()) && part.getHeight() == 1)
				{
					lines++;
					final Rectangle line = bounds(part, bosses);
					assertTrue("line", is(image, line.x + line.width / 2, line.y, ViewParts.TASK_LINE));
				}
			}
			assertEquals(TIERS.length - 1, lines);
			final Component last = list.getComponent(list.getComponentCount() - 1);
			assertNotEquals(ViewParts.TASK_LINE, last.getBackground());
		});
	}

	@Test
	public void anOpenTaskHasALighterBandAcrossTheBox() throws Exception
	{
		final BossesView bosses = shown();
		SwingUtilities.invokeAndWait(() ->
		{
			press(label(bosses, "DK: Prime").getParent());
			press(label(bosses, "Task Medium"));
			press(label(bosses, "Task Elite"));
			layOut(bosses);
			final TaskList list = all(bosses, TaskList.class).get(0);
			final BufferedImage image = paint(bosses);
			final Rectangle box = bounds(list, bosses);
			for (String name : new String[]{"Task Easy", "Task Medium", "Task Hard", "Task Elite", "Task Master", "Task Grandmaster"})
			{
				final Component row = label(bosses, name).getParent().getParent();
				final Rectangle r = bounds(row, bosses);
				final boolean open = name.equals("Task Medium") || name.equals("Task Elite");
				final Color expected = open ? ViewParts.OPEN_BAND : ViewParts.TASKS_GROUND;
				assertEquals(name, box.x + 1, r.x);
				assertEquals(name, box.width - 2, r.width);
				// The row's edge pixels, inside the box's border, and its own top and bottom.
				assertTrue(name + " left", is(image, r.x, r.y + r.height / 2, expected));
				assertTrue(name + " right", is(image, r.x + r.width - 1, r.y + r.height / 2, expected));
				assertTrue(name + " top", is(image, r.x + r.width - 3, r.y, expected));
				assertTrue(name + " bottom", is(image, r.x + r.width - 3, r.y + r.height - 1, expected));
				if (open)
				{
					// The band covers the description and the Wiki link of its task.
					final Rectangle band = r;
					int inside = 0;
					for (Component part : all(row, Component.class))
					{
						if (part instanceof JLabel && ((JLabel) part).getText() != null
							&& (((JLabel) part).getText().startsWith("<html>") || "Wiki".equals(((JLabel) part).getText())))
						{
							assertTrue(name + " " + ((JLabel) part).getText(), band.contains(bounds(part, bosses)));
							inside++;
						}
					}
					assertEquals("description and Wiki link", 2, inside);
				}
			}
		});
	}

	@Test
	public void theTierWordHasItsColourAndTheRestStaysGrey() throws Exception
	{
		final BossesView bosses = shown();
		SwingUtilities.invokeAndWait(() ->
		{
			press(label(bosses, "DK: Prime").getParent());
			layOut(bosses);
			final Color[] expected = {new Color(168, 216, 168), new Color(164, 200, 232), new Color(143, 214, 207),
				new Color(176, 164, 232), new Color(232, 164, 164), new Color(232, 210, 122)};
			final List<TierLabel> details = all(bosses, TierLabel.class);
			assertEquals(TIERS.length, details.size());
			for (int i = 0; i < TIERS.length; i++)
			{
				final TierLabel detail = details.get(i);
				assertEquals(TIERS[i] + " · Speed", detail.getText());
				assertEquals(expected[i], ViewParts.tierColour(TIERS[i]));
				assertTrue(detail.getWidth() >= detail.getPreferredSize().width);
				assertTrue(detail.getHeight() >= detail.getPreferredSize().height);
				final BufferedImage image = paint(detail);
				final int word = detail.getFontMetrics(detail.getFont()).stringWidth(TIERS[i]);
				assertTrue(TIERS[i] + " word coloured", count(image, 0, word, expected[i]) >= 3);
				assertEquals(TIERS[i] + " no tier colour after the word", 0, count(image, word + 1, image.getWidth(), expected[i]));
				assertTrue(TIERS[i] + " rest grey", count(image, word + 1, image.getWidth(), ColorScheme.LIGHT_GRAY_COLOR) >= 3);
				assertEquals(TIERS[i] + " no grey in the word", 0, count(image, 0, word - 1, ColorScheme.LIGHT_GRAY_COLOR));
			}
			assertEquals(null, ViewParts.tierColour("Wrong"));
		});
	}

	/** The row's top-most white pixel (the tick's), as an offset from the row's top, within the tick slot's columns. */
	private static int tickOffset(BossesView bosses, String name)
	{
		layOut(bosses);
		final Component row = label(bosses, name).getParent().getParent();
		final Rectangle r = bounds(row, bosses);
		final Rectangle slot = bounds(((Container) row).getComponent(0), bosses);
		final BufferedImage image = paint(bosses);
		for (int y = r.y; y < r.y + r.height; y++)
		{
			for (int x = slot.x; x < slot.x + slot.width; x++)
			{
				if (is(image, x, y, Color.WHITE))
				{
					return y - r.y;
				}
			}
		}
		throw new AssertionError("no tick in the row of " + name);
	}

	@Test
	public void anOpenTasksTickStaysBesideItsName() throws Exception
	{
		final BossesView bosses = shown();
		SwingUtilities.invokeAndWait(() ->
		{
			press(label(bosses, "DK: Prime").getParent());
			final int closed = tickOffset(bosses, "Task Easy");
			press(label(bosses, "Task Easy"));
			final int open = tickOffset(bosses, "Task Easy");
			assertEquals("the tick of an open task sits where a closed one's does", closed, open);
			final Component row = label(bosses, "Task Easy").getParent().getParent();
			assertTrue("the row grew with its description", row.getHeight() > 40);
			press(label(bosses, "Task Easy"));
			assertEquals(closed, tickOffset(bosses, "Task Easy"));
		});
	}

	/** How many pixels of {@code image} between columns {@code from} and {@code to} are the colour or very near it. */
	private static int count(BufferedImage image, int from, int to, Color colour)
	{
		int found = 0;
		for (int x = Math.max(0, from); x < Math.min(image.getWidth(), to); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				final int argb = image.getRGB(x, y);
				if ((argb >>> 24) < 200)
				{
					continue;
				}
				final int d = Math.abs(((argb >> 16) & 0xFF) - colour.getRed()) + Math.abs(((argb >> 8) & 0xFF) - colour.getGreen())
					+ Math.abs((argb & 0xFF) - colour.getBlue());
				if (d <= 12)
				{
					found++;
				}
			}
		}
		return found;
	}
}
