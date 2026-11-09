package com.cabosschecklist;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;

/**
 * One boss's task list, under its card: a row per task with a tick slot, the task's name, and its tier and type. A left
 * press on a row opens it to show the game's description and a "Wiki" link, and a second press closes it. The rows are
 * rebuilt on each {@link #show}; which tasks are open is kept by task id while the same boss stays shown.
 * Choice: the description wraps to a fixed 170 px, which is the row's width in the 242 px sidebar less its padding, the
 * tick slot and the scroll bar; the full name is the name's hover whether or not it is cut.
 */
final class TaskList extends JPanel
{
	/** The OSRS Wiki's search, to which the task's URL-encoded name is added. */
	static final String SEARCH_URL = "https://oldschool.runescape.wiki/w/Special:Search?search=";

	/** The space between rows, in px. */
	private static final int ROW_GAP = 2;
	/** The tick slot's width, in px. */
	private static final int SLOT_WIDTH = 14;
	/** The tick's side, in px. */
	private static final int TICK_SIZE = 10;
	/**
	 * The width a description's lines are kept within, in px: the row's text column is about 189 px at RuneLite's sidebar
	 * width. The lines are broken here, measured in the label's own font, because Swing's HTML reads a CSS width in px
	 * about a third wider than the space it lays out in, and a line that long was cut at the row's edge.
	 */
	static final int DESCRIPTION_WIDTH = 176;

	/** The tick slot: a white tick drawn in code when the task is done, empty when not. */
	private static final class Tick extends JComponent
	{
		private final boolean done;

		Tick(boolean done)
		{
			this.done = done;
			final Dimension size = new Dimension(SLOT_WIDTH, TICK_SIZE);
			setPreferredSize(size);
			setMinimumSize(size);
			setMaximumSize(size);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			if (!done)
			{
				return;
			}
			final Graphics2D g = (Graphics2D) graphics.create();
			try
			{
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(Color.WHITE);
				g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				final double x = (getWidth() - TICK_SIZE) / 2.0;
				final double y = (getHeight() - TICK_SIZE) / 2.0;
				final Path2D.Double tick = new Path2D.Double();
				tick.moveTo(x + 1, y + TICK_SIZE * 0.55);
				tick.lineTo(x + TICK_SIZE * 0.4, y + TICK_SIZE - 1.5);
				tick.lineTo(x + TICK_SIZE - 1, y + 1.5);
				g.draw(tick);
			}
			finally
			{
				g.dispose();
			}
		}
	}

	private final Set<Integer> open = new HashSet<>();
	/** The boss whose rows are shown; null before the first {@link #show}. */
	private Integer bossId;
	private List<CaCompletion.TaskLine> lines = Collections.emptyList();

	TaskList()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
	}

	/** As wide as the column it sits in, never taller than its content. */
	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	/** Shows {@code taskLines} of boss {@code id}; another boss than before closes every task. */
	void show(int id, List<CaCompletion.TaskLine> taskLines)
	{
		if (bossId == null || bossId != id)
		{
			open.clear();
			bossId = id;
		}
		lines = taskLines;
		rebuild();
	}

	private void rebuild()
	{
		removeAll();
		if (lines.isEmpty())
		{
			final JLabel none = ViewParts.centred("No tasks yet", ViewParts.plain(12f), ColorScheme.LIGHT_GRAY_COLOR);
			add(none);
		}
		for (int i = 0; i < lines.size(); i++)
		{
			if (i > 0)
			{
				add(Box.createVerticalStrut(ROW_GAP));
			}
			add(row(lines.get(i)));
		}
		revalidate();
		repaint();
	}

	/** One task's row. */
	private JPanel row(CaCompletion.TaskLine line)
	{
		final JPanel panel = ViewParts.capped(new BorderLayout(4, 0));
		panel.setOpaque(false);
		panel.setBorder(new EmptyBorder(4, 6, 4, 6));
		final Tick tick = new Tick(line.done);
		final JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		final JLabel name = ViewParts.label(line.name, ViewParts.bold(12f),
			line.done ? ColorScheme.LIGHT_GRAY_COLOR : Color.WHITE);
		name.setToolTipText(line.name);
		name.setAlignmentX(Component.LEFT_ALIGNMENT);
		final JLabel detail = ViewParts.quiet(line.detail());
		detail.setAlignmentX(Component.LEFT_ALIGNMENT);
		text.add(name);
		text.add(detail);
		if (open.contains(line.taskId))
		{
			final JLabel description = ViewParts.label("", ViewParts.plain(11f), ColorScheme.LIGHT_GRAY_COLOR);
			final StringBuilder html = new StringBuilder("<html>");
			for (String part : lines(line.description, description.getFontMetrics(description.getFont())))
			{
				html.append(html.length() > "<html>".length() ? "<br>" : "").append(escaped(part));
			}
			description.setText(html.append("</html>").toString());
			description.setAlignmentX(Component.LEFT_ALIGNMENT);
			final LinkLabel wiki = new LinkLabel("Wiki", 11f,
				() -> LinkBrowser.browse(SEARCH_URL + URLEncoder.encode(line.name, StandardCharsets.UTF_8)));
			wiki.setAlignmentX(Component.LEFT_ALIGNMENT);
			text.add(Box.createVerticalStrut(2));
			text.add(description);
			text.add(Box.createVerticalStrut(2));
			text.add(wiki);
		}
		panel.add(tick, BorderLayout.WEST);
		panel.add(text, BorderLayout.CENTER);
		final MouseAdapter toggle = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (HalfToggle.isPress(e))
				{
					flip(line.taskId);
				}
			}
		};
		// The name is in the list too: its hover makes it take the mouse itself, so a press on it would not reach the row.
		for (Container part : new Container[]{panel, tick, text, name})
		{
			part.addMouseListener(toggle);
		}
		return panel;
	}

	/** Opens a closed task and closes an open one. */
	private void flip(int taskId)
	{
		if (!open.remove(taskId))
		{
			open.add(taskId);
		}
		rebuild();
	}

	/**
	 * The text broken into lines no wider than {@link #DESCRIPTION_WIDTH} in {@code metrics}, at the spaces between
	 * words; a word wider than that stands alone on its line. Runs of spaces count as one.
	 */
	static List<String> lines(String text, FontMetrics metrics)
	{
		final List<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : text.trim().split(" +"))
		{
			if (word.isEmpty())
			{
				continue;
			}
			final String tried = current.length() == 0 ? word : current + " " + word;
			if (current.length() > 0 && metrics.stringWidth(tried) > DESCRIPTION_WIDTH)
			{
				result.add(current.toString());
				current = new StringBuilder(word);
			}
			else
			{
				current = new StringBuilder(tried);
			}
		}
		if (current.length() > 0)
		{
			result.add(current.toString());
		}
		return result;
	}

	/** The text with the four characters HTML reads as markup written out. */
	private static String escaped(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
