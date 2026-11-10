package com.cabosschecklist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;

/**
 * The Bosses view: a search box, and one row per boss that the search leaves, a box that shows whether the boss is green,
 * its label and its status. The rows are built once per list of ids and then updated in place, so the scroll position and
 * the mouse state survive an update. Typing in the search box filters the list and chooses the first boss left; a press
 * on a row chooses that boss, and a press on the chosen row takes the choice off. The chosen boss's card and task list
 * form one panel that the list holds right after the boss's row, 2 px below it.
 * Choice: the opened panel is in the list only while a boss is chosen, so a closed one holds no component, and it moves
 * to its place on each draw; a chosen row keeps its chosen ground under the mouse.
 */
final class BossesView extends JPanel
{
	/** The space between rows, in px. */
	private static final int ROW_GAP = 2;
	/** The space between the search box and the list, and between the card and the task list, in px. */
	private static final int GAP = 6;
	/** The search box's height, in px. */
	private static final int SEARCH_HEIGHT = 30;

	/** The game's green for a finished boss's name. */
	private static final Color NAME_GREEN = new Color(0x0DC10D);
	/** The game's yellow for a boss with one or two tasks left. */
	private static final Color NAME_YELLOW = new Color(0xFFFF00);
	/** The game's own orange in its Monster list. */
	private static final Color NAME_ORANGE = new Color(0xFF981F);

	/** One boss's row: the box, the label and the status word. */
	private static final class Row
	{
		final JPanel panel = ViewParts.capped(new BorderLayout(4, 0));
		final JCheckBox box = new JCheckBox();
		final JLabel name = ViewParts.label("", ViewParts.bold(12f), Color.WHITE);
		final JLabel status = ViewParts.quiet("");
		private boolean chosen;
		private boolean hovered;

		Row(int id, MarkActions actions, IntConsumer pressed)
		{
			panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
			panel.setBorder(new EmptyBorder(4, 4, 4, 6));
			box.setOpaque(false);
			box.setFocusable(false);
			box.addActionListener(e -> actions.flip(id));
			panel.add(box, BorderLayout.WEST);
			panel.add(name, BorderLayout.CENTER);
			panel.add(status, BorderLayout.EAST);
			panel.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (HalfToggle.isPress(e))
					{
						pressed.accept(id);
					}
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					hovered = true;
					paintGround();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					hovered = false;
					paintGround();
				}
			});
		}

		void update(ChecklistView.Boss boss, boolean isChosen)
		{
			box.setSelected(boss.green);
			name.setText(boss.label);
			name.setForeground(boss.shade == CaCompletion.Shade.GREEN ? NAME_GREEN
				: boss.shade == CaCompletion.Shade.YELLOW ? NAME_YELLOW : NAME_ORANGE);
			status.setText(boss.status());
			chosen = isChosen;
			paintGround();
		}

		/** The chosen row is medium grey, a row under the mouse is the hover grey, any other the dark grey. */
		private void paintGround()
		{
			panel.setBackground(chosen ? ColorScheme.MEDIUM_GRAY_COLOR
				: hovered ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARK_GRAY_COLOR);
		}
	}

	private final MarkActions actions;
	private final ScrollColumn column = new ScrollColumn();
	private final JScrollPane pane = column.pane();
	private final List<Row> rows = new ArrayList<>();
	private final IconTextField search = new IconTextField();
	private final BossCard card;
	private final TaskList taskList = new TaskList();
	/** The chosen boss's card and task list on the dark grey ground, inside a holder that adds the 2 px above it. */
	private final JPanel opened = ViewParts.capped(new BorderLayout());
	private final JPanel top = new JPanel();
	private final Component searchGap = Box.createVerticalStrut(GAP);
	/** The ids the rows were built for, in order; null while the column holds something else. */
	private List<Integer> shownIds;
	/** The last view; null while logged out. */
	private ChecklistView view;
	/** The search text, as typed. */
	private String query = "";
	/** The id of the boss on the card; null when the card is hidden. */
	private Integer chosen;
	/** Set while this view writes the search box itself, so its own writes are not taken as typing. */
	private boolean writing;

	BossesView(MarkActions actions, BooleanSupplier wikiPictures, WikiPictures pictures)
	{
		super(new BorderLayout());
		this.actions = actions;
		this.card = new BossCard(actions, wikiPictures, pictures);
		setOpaque(false);

		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(0, SEARCH_HEIGHT));
		search.setBackground(ColorScheme.DARK_GRAY_COLOR);
		search.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		search.setAlignmentX(Component.LEFT_ALIGNMENT);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				typed();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				typed();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				typed();
			}
		});
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		taskList.setAlignmentX(Component.LEFT_ALIGNMENT);
		final JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.add(card);
		body.add(Box.createVerticalStrut(GAP));
		body.add(taskList);
		opened.setOpaque(false);
		opened.setBorder(new EmptyBorder(ROW_GAP, 0, 0, 0));
		opened.add(body, BorderLayout.CENTER);

		// The search box ends where the panel's header does, so every right edge lines up.
		final int bar = pane.getVerticalScrollBar().getPreferredSize().width;
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setOpaque(false);
		top.setBorder(new EmptyBorder(0, 0, 0, bar));
		top.add(search);
		top.add(searchGap);
		add(top, BorderLayout.NORTH);
		add(pane, BorderLayout.CENTER);
		top.setVisible(false);
	}

	/** The scroll pane inside this view (its bar's width is the header's right inset). */
	JScrollPane pane()
	{
		return pane;
	}

	/** Hides the search box, clears the text and the choice, and replaces the rows with one grey line. */
	void showLoggedOut()
	{
		view = null;
		chosen = null;
		query = "";
		writing = true;
		search.setText("");
		writing = false;
		top.setVisible(false);
		shownIds = null;
		rows.clear();
		column.removeAll();
		showLine("Log in to see your bosses");
	}

	/** Shows the view's bosses that the search leaves; the rows are kept while the ids (in order) are the same. */
	void show(ChecklistView next)
	{
		view = next;
		top.setVisible(true);
		draw();
	}

	/** Passes a finished wiki answer on to the card. */
	void pictureArrived()
	{
		card.pictureArrived();
	}

	/** The search text changed: a blank one shows every boss and chooses none, else the first boss left is chosen. */
	private void typed()
	{
		if (writing)
		{
			return;
		}
		query = search.getText();
		chosen = null;
		if (view != null && !query.trim().isEmpty())
		{
			final List<ChecklistView.Boss> left = filtered();
			chosen = left.isEmpty() ? null : left.get(0).id;
		}
		if (view != null)
		{
			draw();
		}
	}

	/** A press on a row: the pressed boss is chosen, or not any more when it already was. */
	private void pressed(int id)
	{
		chosen = chosen != null && chosen == id ? null : Integer.valueOf(id);
		draw();
	}

	/** The view's bosses that match the search text, in row order. */
	private List<ChecklistView.Boss> filtered()
	{
		final List<ChecklistView.Boss> left = new ArrayList<>();
		for (ChecklistView.Boss boss : view.bosses)
		{
			if (boss.matches(query))
			{
				left.add(boss);
			}
		}
		return left;
	}

	/** Draws the rows, their grounds and the opened panel from the last view, the text and the choice. */
	private void draw()
	{
		final List<ChecklistView.Boss> left = filtered();
		ChecklistView.Boss onCard = null;
		int chosenAt = -1;
		for (int i = 0; i < left.size(); i++)
		{
			if (chosen != null && left.get(i).id == chosen)
			{
				onCard = left.get(i);
				chosenAt = i;
			}
		}
		if (onCard == null)
		{
			chosen = null;
		}
		else
		{
			card.show(view, onCard);
			taskList.show(onCard.id, view.tasksOf(onCard.id));
		}
		if (opened.getParent() == column)
		{
			column.remove(opened);
		}

		if (left.isEmpty())
		{
			shownIds = null;
			rows.clear();
			column.removeAll();
			showLine("No boss matches");
			return;
		}
		final List<Integer> ids = new ArrayList<>();
		for (ChecklistView.Boss boss : left)
		{
			ids.add(boss.id);
		}
		if (!ids.equals(shownIds))
		{
			column.removeAll();
			rows.clear();
			for (int i = 0; i < ids.size(); i++)
			{
				if (i > 0)
				{
					column.add(Box.createVerticalStrut(ROW_GAP));
				}
				final Row row = new Row(ids.get(i), actions, this::pressed);
				rows.add(row);
				column.add(row.panel);
			}
			shownIds = ids;
			column.revalidate();
		}
		for (int i = 0; i < rows.size(); i++)
		{
			final ChecklistView.Boss boss = left.get(i);
			rows.get(i).update(boss, chosen != null && boss.id == chosen);
		}
		if (chosenAt >= 0)
		{
			// Row i sits at component 2 * i of the column (a gap comes before every row but the first).
			column.add(opened, 2 * chosenAt + 1);
		}
		column.revalidate();
		column.repaint();
	}

	/** Replaces the column's content with one grey line. */
	private void showLine(String text)
	{
		final JLabel line = ViewParts.centred(text, ViewParts.plain(12f), ColorScheme.LIGHT_GRAY_COLOR);
		line.setBorder(new EmptyBorder(16, 0, 0, 0));
		column.add(line);
		column.revalidate();
		column.repaint();
	}
}
