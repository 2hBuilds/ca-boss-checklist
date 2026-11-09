package com.cabosschecklist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

/**
 * The Manual view: the player's marks in up to three groups, each row with its link words. Rebuilt on every update.
 * Choice: the caption sits 2 px above its first row and the note 8 px above the first group.
 */
final class ManualView
{
	/** The space between groups, in px. */
	private static final int GROUP_GAP = 8;
	/** The space between rows, and between a caption and its first row, in px. */
	private static final int ROW_GAP = 2;
	/** The space between the link words of a row, in px. */
	private static final int LINK_GAP = 8;

	private final MarkActions actions;
	private final ScrollColumn column = new ScrollColumn();
	private final JScrollPane pane = column.pane();

	ManualView(MarkActions actions)
	{
		this.actions = actions;
	}

	/** The scroll pane to place in the panel. */
	JScrollPane pane()
	{
		return pane;
	}

	/** Replaces the groups with one grey line. */
	void showLoggedOut()
	{
		column.removeAll();
		final JLabel line = ViewParts.centred("Log in to see your marks", ViewParts.plain(12f), ColorScheme.LIGHT_GRAY_COLOR);
		line.setBorder(new EmptyBorder(16, 0, 0, 0));
		column.add(line);
		finish();
	}

	/** Shows the view's marks, or the empty state when it has none. */
	void show(ChecklistView view)
	{
		column.removeAll();
		if (view.markCount == 0)
		{
			final JPanel empty = new JPanel();
			empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
			empty.setOpaque(false);
			empty.setBorder(new EmptyBorder(16, 0, 0, 0));
			empty.add(ViewParts.centred("No marks yet", ViewParts.bold(12f), Color.WHITE));
			empty.add(Box.createVerticalStrut(ROW_GAP));
			empty.add(ViewParts.centred("Tick or untick a boss to add a mark", ViewParts.plain(11f), ColorScheme.LIGHT_GRAY_COLOR));
			column.add(empty);
			finish();
			return;
		}
		column.add(line(ViewParts.quiet("Your marks win over your account"), null));
		addGroup(view.group(Marks.Group.DISAGREE), "Disagree with your account", true, false);
		addGroup(view.group(Marks.Group.NOT_NEEDED), "Not needed", false, true);
		addGroup(view.group(Marks.Group.YOURS), "Your marks", false, false);
		finish();
	}

	private void finish()
	{
		column.revalidate();
		column.repaint();
	}

	/** One line: {@code left} at the start, {@code right} (when not null) at the end. */
	private static JPanel line(JLabel left, JLabel right)
	{
		final JPanel line = ViewParts.capped(new BorderLayout(LINK_GAP, 0));
		line.setOpaque(false);
		line.add(left, BorderLayout.CENTER);
		if (right != null)
		{
			line.add(right, BorderLayout.EAST);
		}
		return line;
	}

	/** A group under its caption; nothing when it has no rows. */
	private void addGroup(List<ChecklistView.Boss> bosses, String caption, boolean withKeep, boolean withRemoveAll)
	{
		if (bosses.isEmpty())
		{
			return;
		}
		column.add(Box.createVerticalStrut(GROUP_GAP));
		JLabel removeAll = null;
		if (withRemoveAll)
		{
			final List<Integer> ids = new ArrayList<>();
			for (ChecklistView.Boss boss : bosses)
			{
				ids.add(boss.id);
			}
			removeAll = new LinkLabel("Remove all", 11f, () -> actions.remove(ids));
		}
		column.add(line(ViewParts.quiet(caption), removeAll));
		for (ChecklistView.Boss boss : bosses)
		{
			column.add(Box.createVerticalStrut(ROW_GAP));
			column.add(row(boss, withKeep));
		}
	}

	/** A boss's row: the label with its link words at the right, and the detail under it. */
	private JPanel row(ChecklistView.Boss boss, boolean withKeep)
	{
		final JPanel links = new JPanel();
		links.setLayout(new BoxLayout(links, BoxLayout.X_AXIS));
		links.setOpaque(false);
		if (withKeep)
		{
			links.add(new LinkLabel("Keep", 11f, () -> actions.keep(boss.id)));
			links.add(Box.createHorizontalStrut(LINK_GAP));
		}
		links.add(new LinkLabel("Remove", 11f, () -> actions.remove(Collections.singletonList(boss.id))));

		final JPanel top = line(ViewParts.label(boss.label, ViewParts.bold(12f), Color.WHITE), null);
		top.add(links, BorderLayout.EAST);

		final JPanel row = ViewParts.capped(new BorderLayout());
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 6, 4, 6));
		row.add(top, BorderLayout.NORTH);
		row.add(ViewParts.quiet(boss.detail()), BorderLayout.CENTER);
		return row;
	}
}
