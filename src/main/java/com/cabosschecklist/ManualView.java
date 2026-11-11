package com.cabosschecklist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
 * The Manual view: the player's marks in one or two groups, each row holding the boss with a link that removes the mark
 * and a line saying which colour the mark shows, instead of which, and how far the account is. Rebuilt on every update.
 * Choice: the caption sits 2 px above its first row; the note's labels sit side by side, the spaces in their texts.
 */
final class ManualView
{
	/** The space between groups, in px. */
	private static final int GROUP_GAP = 8;
	/** The space between rows, and between a caption and its first row, in px. */
	private static final int ROW_GAP = 2;
	/** The space between a caption and its link, in px. */
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
			empty.add(ViewParts.centred("Tick or untick a boss to add a mark", ViewParts.plain(11f),
				ColorScheme.LIGHT_GRAY_COLOR));
			column.add(empty);
			finish();
			return;
		}
		if (view.autoDetect)
		{
			addGroup(view.group(Marks.Group.DIFFERENT), "Your marks override your account", false);
			addGroup(view.group(Marks.Group.SAME), "Same as your account", true);
		}
		else
		{
			addGroup(view.marked(), "Your marks", false);
		}
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
	private void addGroup(List<ChecklistView.Boss> bosses, String caption, boolean withRemoveAll)
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
		column.add(line(ViewParts.label(caption + " (" + bosses.size() + ")", ViewParts.bold(11f), Color.WHITE),
			removeAll));
		for (ChecklistView.Boss boss : bosses)
		{
			column.add(Box.createVerticalStrut(ROW_GAP));
			column.add(row(boss));
		}
	}

	/** A boss's row: its name and the remove sign, then a line saying what the mark does and the account's progress. */
	private JPanel row(ChecklistView.Boss boss)
	{
		final JLabel name = ViewParts.label(boss.label, ViewParts.bold(12f), Color.WHITE);
		name.setToolTipText(boss.label);
		final LinkLabel remove = new LinkLabel("\u00d7", 12f, () -> actions.remove(Collections.singletonList(boss.id)));
		remove.setToolTipText("Remove this mark");

		final JPanel note = new JPanel(new GridBagLayout());
		note.setOpaque(false);
		final List<JLabel> parts = new ArrayList<>();
		parts.add(ViewParts.label(boss.shownWord(), ViewParts.plain(11f), ViewParts.colourOf(boss.shade)));
		parts.add(ViewParts.quiet(boss.overrides() ? " instead of " : " either way"));
		if (boss.overrides())
		{
			parts.add(ViewParts.label(boss.unmarkedWord(), ViewParts.plain(11f),
				ViewParts.colourOf(boss.unmarkedShade)));
		}
		final JLabel progress = ViewParts.quiet(" \u00b7 " + boss.accountText());
		progress.setToolTipText(boss.accountLine());
		parts.add(progress);
		final GridBagConstraints at = new GridBagConstraints();
		at.anchor = GridBagConstraints.WEST;
		at.gridx = 0;
		at.gridy = 0;
		for (JLabel part : parts)
		{
			note.add(part, at);
			at.gridx++;
		}
		at.weightx = 1;
		note.add(Box.createHorizontalGlue(), at);

		final JPanel top = line(name, remove);
		final JPanel lines = new JPanel(new BorderLayout());
		lines.setOpaque(false);
		lines.add(top, BorderLayout.NORTH);
		lines.add(note, BorderLayout.CENTER);

		final JPanel row = ViewParts.capped(new BorderLayout());
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 6, 4, 6));
		row.add(lines, BorderLayout.CENTER);
		return row;
	}
}
