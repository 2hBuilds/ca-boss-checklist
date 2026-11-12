package com.cabosschecklist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.function.BooleanSupplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;

/**
 * The boss card under the search box: the wiki's picture in a 64 x 64 box, the boss's long name, its progress, a box
 * that says whether the boss shows green in the Tasks menu, and under it the note row: the tag (the word {@link MarkTag#TEXT}) and some
 * words, shown only when the boss has a mark or the hint "Tick to show it green" applies, and taking no height at all
 * when hidden. Everything is centred.
 * Choice: the picture box and its 6 px gap are one holder that is hidden together; the note row holds its own 4 px gap
 * above, so hiding the row hides the gap.
 */
final class BossCard extends JPanel
{
	/** The card's padding, in px. */
	private static final int PADDING = 8;
	/** The picture box's side, in px. */
	private static final int BOX = 64;
	/** The width the name must fit, in px. */
	private static final int NAME_WIDTH = 203;
	/** The name's sizes, largest first; the last is the one that is cut with "..." when it still does not fit. */
	private static final float[] NAME_SIZES = {13f, 12f, 11f};

	private final MarkActions actions;
	private final BooleanSupplier wikiPictures;
	private final WikiPictures pictures;
	private final JPanel pictureHolder = new JPanel();
	private final JLabel picture = new JLabel("", SwingConstants.CENTER);
	private final JLabel name = ViewParts.centred("", ViewParts.bold(13f), Color.WHITE);
	private final JLabel progress = ViewParts.centred("", ViewParts.plain(12f), ColorScheme.LIGHT_GRAY_COLOR);
	private final JCheckBox green = new JCheckBox("Green in the Tasks menu");
	private final MarkTag tag = new MarkTag();
	/** The words after the tag; never squeezed in height, or BoxLayout would give a lone one half its height. */
	private final JLabel words = new JLabel()
	{
		@Override
		public Dimension getMinimumSize()
		{
			return new Dimension(0, getPreferredSize().height);
		}
	};
	private final JPanel noteRow = ViewParts.capped(null);
	private final LinkLabel wiki = new LinkLabel("Wiki", 11f, this::openWiki);
	/** The boss on the card; null before the first draw. */
	private ChecklistView.Boss shown;

	BossCard(MarkActions actions, BooleanSupplier wikiPictures, WikiPictures pictures)
	{
		this.actions = actions;
		this.wikiPictures = wikiPictures;
		this.pictures = pictures;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

		final Dimension box = new Dimension(BOX, BOX);
		picture.setMinimumSize(box);
		picture.setPreferredSize(box);
		picture.setMaximumSize(box);
		picture.setAlignmentX(Component.CENTER_ALIGNMENT);
		pictureHolder.setLayout(new BoxLayout(pictureHolder, BoxLayout.Y_AXIS));
		pictureHolder.setOpaque(false);
		pictureHolder.setAlignmentX(Component.CENTER_ALIGNMENT);
		pictureHolder.add(picture);
		pictureHolder.add(Box.createVerticalStrut(6));

		green.setFont(ViewParts.plain(12f));
		green.setForeground(Color.WHITE);
		green.setOpaque(false);
		green.setFocusable(false);
		green.setAlignmentX(Component.CENTER_ALIGNMENT);
		green.addActionListener(e ->
		{
			if (shown != null)
			{
				actions.flip(shown.id);
			}
		});

		add(pictureHolder);
		add(name);
		add(Box.createVerticalStrut(4));
		add(progress);
		add(Box.createVerticalStrut(6));
		add(green);
		buildNoteRow();
		add(noteRow);
		wiki.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(Box.createVerticalStrut(4));
		add(wiki);
	}

	/** The note row: a 4 px gap, then the tag and its words on one line, centred together, at most {@link #NAME_WIDTH} wide. */
	private void buildNoteRow()
	{
		noteRow.setLayout(new BoxLayout(noteRow, BoxLayout.Y_AXIS));
		noteRow.setOpaque(false);
		noteRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		final JPanel line = new JPanel()
		{
			@Override
			public Dimension getPreferredSize()
			{
				final Dimension size = super.getPreferredSize();
				return new Dimension(Math.min(size.width, NAME_WIDTH), size.height);
			}

			/** As wide as the column: BoxLayout would give a narrower, centre-aligned part half its width at the left. */
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
		line.setOpaque(false);
		line.setAlignmentX(Component.CENTER_ALIGNMENT);
		tag.setAlignmentY(Component.CENTER_ALIGNMENT);
		words.setAlignmentY(Component.CENTER_ALIGNMENT);
		words.setBorder(new EmptyBorder(0, 4, 0, 0));
		words.setFont(ViewParts.plain(11f));
		words.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		line.add(Box.createHorizontalGlue());
		line.add(tag);
		line.add(words);
		line.add(Box.createHorizontalGlue());
		noteRow.add(Box.createVerticalStrut(4));
		noteRow.add(line);
		noteRow.setVisible(false);
	}

	/** Opens the shown boss's wiki page at its Combat Achievements table; nothing before a boss is shown. */
	private void openWiki()
	{
		if (shown != null)
		{
			LinkBrowser.browse(BossPictures.wikiUrl(shown.id, shown.name));
		}
	}

	/** As wide as the column it sits in, never taller than its content. */
	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	/** Draws {@code boss} (of {@code view}) on the card. */
	void show(ChecklistView view, ChecklistView.Boss boss)
	{
		shown = boss;
		fitName(boss.name);
		progress.setText(boss.progressLine());
		green.setSelected(boss.green);
		final ChecklistView.CardNote note = view.cardNote(boss);
		noteRow.setVisible(note != ChecklistView.CardNote.NONE);
		tag.setVisible(note.tag);
		tag.setStrong(note.strong);
		words.setText(note.text);
		words.setVisible(!note.text.isEmpty());
		drawPicture();
		revalidate();
		repaint();
	}

	/** Reads the shown boss's picture again, after the wiki answered for some title. */
	void pictureArrived()
	{
		if (shown != null)
		{
			drawPicture();
			revalidate();
			repaint();
		}
	}

	/**
	 * The picture rule: with the wiki switch on and a wiki title for the boss, ask for the title (once) and show its
	 * picture when kept; when the wiki had none, hide the box and its gap; while the answer is on its way, show the empty
	 * box. With the switch off or no title, the box is hidden.
	 */
	private void drawPicture()
	{
		final String title = wikiPictures.getAsBoolean() ? BossPictures.titleFor(shown.id, shown.name) : null;
		if (title == null)
		{
			showBox(false, null);
			return;
		}
		pictures.fetch(title);
		final BufferedImage image = pictures.pictureOf(title);
		if (image != null)
		{
			showBox(true, new ImageIcon(image));
		}
		else
		{
			showBox(!pictures.isAnswered(title), null);
		}
	}

	private void showBox(boolean visible, ImageIcon icon)
	{
		picture.setIcon(icon);
		picture.setVisible(visible);
		pictureHolder.setVisible(visible);
	}

	/** Sets the name at the largest of 13, 12 and 11 px at which it fits, else cut with "..." at 11. */
	private void fitName(String text)
	{
		for (float size : NAME_SIZES)
		{
			final Font font = ViewParts.bold(size);
			if (name.getFontMetrics(font).stringWidth(text) <= NAME_WIDTH)
			{
				name.setFont(font);
				name.setText(text);
				return;
			}
		}
		final Font font = ViewParts.bold(NAME_SIZES[NAME_SIZES.length - 1]);
		name.setFont(font);
		String cut = text;
		while (!cut.isEmpty() && name.getFontMetrics(font).stringWidth(cut + "...") > NAME_WIDTH)
		{
			cut = cut.substring(0, cut.length() - 1);
		}
		name.setText(cut.trim() + "...");
	}
}
