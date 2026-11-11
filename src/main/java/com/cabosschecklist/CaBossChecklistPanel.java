package com.cabosschecklist;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * The sidebar panel: the status card (a bold line and a grey line, centred) whose top row ends with the support pair,
 * the Bosses | Manual toggle under it, the two views, and the settings menu the settings icon opens (STANDARD A4 and A5).
 */
public class CaBossChecklistPanel extends PluginPanel
{
	/** The plugin's GitHub page, what the menu header's fourth mark opens. */
	static final String GITHUB_URL = "https://github.com/2hBuilds/ca-boss-checklist";

	/** The menu's rows. */
	static final String RESET_TEXT = "Reset to default";
	static final String OK_TEXT = "OK";

	/** The card's padding, in px. */
	private static final int PADDING = 8;
	/** The space between the card, the toggle and the views, in px. */
	private static final int GAP = 6;
	private static final String BOSSES_CARD = "bosses";
	private static final String MANUAL_CARD = "manual";
	/** The card's lines while logged out. */
	static final String LOGGED_OUT_HEADLINE = "Log in to start";
	static final String LOGGED_OUT_HELPER = "Finished bosses show green";
	/** The menu's switches. */
	private static final String AUTO_DETECT_TEXT = "Green when all tasks done";
	private static final String ALMOST_DONE_TEXT = "Yellow when almost done";
	private static final String WIKI_PICTURES_TEXT = "Show wiki pictures";
	/** The icon submenu's title; the current choice follows it. */
	private static final String ICON_TEXT = "Sidebar icon";
	/** The settings icon's size, in px. */
	private static final int ICON_SIZE = 12;
	/** A press on the settings icon this soon after the menu closed is the press that closed it, not a new open. */
	private static final long REOPEN_GUARD_NANOS = 250_000_000L;

	/** The menu's row that hides the sidebar button, and the question window it opens. */
	static final String HIDE_TEXT = "Hide the sidebar button...";
	static final String HIDE_TITLE = "Hide the sidebar button";
	static final String HIDE_YES = "Hide";
	static final String HIDE_ASK = "This hides the plugin's sidebar button in RuneLite.\n"
		+ "Colours stay automatic, from your account only.\n"
		+ "Your marks are kept, but not used while it is hidden.\n"
		+ "To show it again: RuneLite's settings, 2h CA Boss Checklist.";

	/** Puts the question before hiding the sidebar button to the player; true when they said yes. */
	interface HidePrompt
	{
		boolean ask(String title, String question, String yes);
	}

	/** The tooltip of the dot on the Manual half. */
	static final String DOT_TIP = "A new mark overrides your account";

	private final MenuSettings settings;
	private final HidePrompt prompt;
	private final MarkActions actions;
	private final BossesView bosses;
	private final ManualView manual;
	private final HalfToggle toggle;
	private final CardLayout cards = new CardLayout();
	private final JPanel views = new JPanel(cards);
	private final JLabel headlineLabel = line(LOGGED_OUT_HEADLINE, FontManager.getDefaultBoldFont().deriveFont(14f), Color.WHITE);
	private final JLabel helperLabel = line(LOGGED_OUT_HELPER, FontManager.getDefaultFont().deriveFont(12f), ColorScheme.LIGHT_GRAY_COLOR);
	private JCheckBoxMenuItem autoDetectItem;
	private JCheckBoxMenuItem almostDoneItem;
	private JCheckBoxMenuItem wikiPicturesItem;
	private JMenu iconMenu;
	final List<IconChoice> iconItems = new ArrayList<>();
	private boolean syncing;
	private final SupportLinks support;
	private final JPopupMenu menu;
	private long closedAtNanos = System.nanoTime() - 2 * REOPEN_GUARD_NANOS;
	/** The view on show; null while logged out. */
	private ChecklistView current;
	/** Whether RuneLite has the sidebar panel open: between {@code onActivate} and {@code onDeactivate}. */
	private boolean active;
	/** The view in front: 0 Bosses, 1 Manual. */
	private int front;

	/**
	 * @param settings what the settings menu reads and writes
	 * @param actions what the boxes and links do to the marks
	 * @param pictures the wiki's pictures, which the boss card reads
	 * @param prompt asks the player the question that comes before hiding the sidebar button
	 */
	CaBossChecklistPanel(MenuSettings settings, MarkActions actions, WikiPictures pictures, HidePrompt prompt)
	{
		super(false);
		this.settings = settings;
		this.prompt = prompt;
		this.actions = actions;
		this.support = new SupportLinks("2h CA Boss Checklist", Version.CURRENT, GITHUB_URL, LinkBrowser::browse, this::closeMenu);
		this.menu = buildMenu();

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

		bosses = new BossesView(actions, settings::wikiPictures, pictures);
		manual = new ManualView(actions);
		toggle = new HalfToggle("Bosses", "Manual", this::showView);
		views.setOpaque(false);
		views.add(bosses, BOSSES_CARD);
		views.add(manual.pane(), MANUAL_CARD);

		// The header's right edge lines up with the views' bars.
		final int bar = bosses.pane().getVerticalScrollBar().getPreferredSize().width;
		final JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(0, 0, 0, bar));
		header.add(messageCard());
		header.add(Box.createVerticalStrut(GAP));
		header.add(toggle);
		header.add(Box.createVerticalStrut(GAP));
		add(header, BorderLayout.NORTH);
		add(views, BorderLayout.CENTER);
	}

	/** Shows the log-in message and the logged-out views; called on the EDT. */
	void showLoggedOut()
	{
		headlineLabel.setText(LOGGED_OUT_HEADLINE);
		helperLabel.setText(LOGGED_OUT_HELPER);
		toggle.setWord(1, "Manual");
		current = null;
		toggle.setDot(1, null);
		bosses.showLoggedOut();
		manual.showLoggedOut();
	}

	/** Shows the card, the toggle's word and both views for {@code view}; called on the EDT. */
	void show(ChecklistView view)
	{
		headlineLabel.setText(view.headline());
		helperLabel.setText(view.helper());
		toggle.setWord(1, view.manualWord());
		bosses.show(view);
		manual.show(view);
		current = view;
		look();
	}

	/** RuneLite opened the sidebar panel: a Manual tab in front now counts as looked at. */
	@Override
	public void onActivate()
	{
		active = true;
		look();
	}

	/** RuneLite closed the sidebar panel. */
	@Override
	public void onDeactivate()
	{
		active = false;
	}

	/**
	 * Keeps the seen marks and the dot in step with what the player is looking at: looking means the panel is open with
	 * the Manual view in front. The dot shows on the Manual half while it is not in front and an overriding mark is
	 * new.
	 */
	private void look()
	{
		if (current == null)
		{
			toggle.setDot(1, null);
			return;
		}
		final Map<Integer, Mark> next = current.seenAfter(active && front == 1);
		if (!next.equals(current.seen))
		{
			actions.remember(next);
		}
		toggle.setDot(1, front != 1 && current.unseen() ? DOT_TIP : null);
	}

	/** Passes a finished wiki answer on to the Bosses view; called on the EDT. */
	void pictureArrived()
	{
		bosses.pictureArrived();
	}

	/** Lights half {@code index} of the toggle and shows its view. */
	private void showView(int index)
	{
		toggle.setLit(index);
		cards.show(views, index == 1 ? MANUAL_CARD : BOSSES_CARD);
		front = index == 1 ? 1 : 0;
		look();
	}

	/** The message card: the top row with the support pair at its right end, then the two centred lines. */
	private JPanel messageCard()
	{
		final JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

		final JPanel topRow = new JPanel(new BorderLayout());
		topRow.setOpaque(false);
		topRow.add(support.supportPair(settingsIcon()), BorderLayout.EAST);
		card.add(topRow, BorderLayout.NORTH);

		final JPanel lines = new JPanel();
		lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
		lines.setOpaque(false);
		lines.setBorder(new EmptyBorder(PADDING, 0, PADDING, 0));
		lines.add(headlineLabel);
		lines.add(Box.createVerticalStrut(4));
		final JPanel helperRow = new JPanel();
		helperRow.setLayout(new BoxLayout(helperRow, BoxLayout.X_AXIS));
		helperRow.setOpaque(false);
		helperRow.add(Box.createHorizontalGlue());
		helperRow.add(helperLabel);
		helperRow.add(Box.createHorizontalGlue());
		lines.add(helperRow);
		card.add(lines, BorderLayout.CENTER);
		return card;
	}

	/** One centred line of the card. */
	private static JLabel line(String text, Font font, Color colour)
	{
		final JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setFont(font);
		label.setForeground(colour);
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		return label;
	}

	/** The settings icon: grey at rest, orange under the mouse; a press opens the menu under it. */
	private JLabel settingsIcon()
	{
		final JLabel[] self = new JLabel[1];
		self[0] = SupportLinks.namedIconButton(GearGlyph.icon(ICON_SIZE, ColorScheme.LIGHT_GRAY_COLOR),
			GearGlyph.icon(ICON_SIZE, ColorScheme.BRAND_ORANGE), SupportLinks.SETTINGS_TIP, () -> openMenu(self[0]));
		self[0].setBorder(SupportLinks.iconBorder());
		return self[0];
	}

	/** Opens the menu under {@code anchor}; the press that closed it a moment ago opens nothing, so the icon toggles. */
	private void openMenu(JLabel anchor)
	{
		if (!anchor.isShowing() || System.nanoTime() - closedAtNanos < REOPEN_GUARD_NANOS)
		{
			return;
		}
		menu.show(anchor, 0, anchor.getHeight());
	}

	/** Takes the menu down: OK, a link press, or the plugin swapping the sidebar button. */
	void closeMenu()
	{
		menu.setVisible(false);
	}

	/** Asks the player, and turns "Show the sidebar button" off only on a yes. */
	void askToHide()
	{
		if (prompt.ask(HIDE_TITLE, HIDE_ASK, HIDE_YES))
		{
			settings.hideSidebar();
		}
	}

	/**
	 * The menu row "Hide the sidebar button...": a plain item in the switches' font and colour. Choice: unlike the
	 * switches it keeps Swing's own release, so a click closes the menu before the window opens.
	 */
	private JMenuItem hideItem()
	{
		final JMenuItem item = new JMenuItem(HIDE_TEXT);
		item.setFont(menuFont());
		item.setForeground(Color.WHITE);
		item.addActionListener(e -> askToHide());
		return item;
	}

	/** The settings menu: header, rule, three switches, rule, icon submenu, the hide row, rule, then the OK row. */
	private JPopupMenu buildMenu()
	{
		final JPopupMenu popup = new JPopupMenu();
		popup.setBorder(new EmptyBorder(5, 5, 5, 5));
		popup.add(support.header());
		popup.addSeparator();
		autoDetectItem = check(AUTO_DETECT_TEXT, settings::setAutoDetect);
		popup.add(autoDetectItem);
		almostDoneItem = check(ALMOST_DONE_TEXT, settings::setAlmostDone);
		popup.add(almostDoneItem);
		wikiPicturesItem = check(WIKI_PICTURES_TEXT, settings::setWikiPictures);
		popup.add(wikiPicturesItem);
		popup.addSeparator();
		iconMenu = group(ICON_TEXT);
		for (IconChoice item : radios(CaBossChecklistConfig.SidebarIcon.values(), settings::setSidebarIcon))
		{
			iconMenu.add(item);
		}
		popup.add(iconMenu);
		popup.add(hideItem());
		popup.addSeparator();
		popup.add(okRow());
		popup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				syncControls();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
				closedAtNanos = System.nanoTime();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
				closedAtNanos = System.nanoTime();
			}
		});
		return popup;
	}

	/**
	 * A switch: a check in the menu's 12 px type, white, writing its state when clicked. Choice: the release is taken
	 * over so a click flips it and the menu stays open (A5).
	 */
	private JCheckBoxMenuItem check(String text, Consumer<Boolean> write)
	{
		final JCheckBoxMenuItem check = new JCheckBoxMenuItem(text)
		{
			@Override
			protected void processMouseEvent(MouseEvent e)
			{
				if (!takeRelease(this, e))
				{
					super.processMouseEvent(e);
				}
			}
		};
		check.setFont(FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE));
		check.setForeground(Color.WHITE);
		check.addActionListener(e ->
		{
			if (!syncing)
			{
				write.accept(check.isSelected());
			}
		});
		return check;
	}

	/**
	 * Shows the stored state of the switches and the icon choice without writing it back: when the menu opens, and after
	 * "Reset to default". The submenu's title carries the current choice.
	 */
	private void syncControls()
	{
		syncing = true;
		autoDetectItem.setSelected(settings.autoDetect());
		almostDoneItem.setSelected(settings.almostDone());
		wikiPicturesItem.setSelected(settings.wikiPictures());
		final CaBossChecklistConfig.SidebarIcon icon = settings.sidebarIcon();
		iconMenu.setText(ICON_TEXT + ": " + icon);
		iconItems.get(icon.ordinal()).setSelected(true);
		syncing = false;
	}

	private static Font menuFont()
	{
		return FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE);
	}

	/** A submenu in the menu's face that opens its popup beside the menu. */
	private static JMenu group(String name)
	{
		final JMenu group = new JMenu(name);
		group.setFont(menuFont());
		group.setForeground(Color.WHITE);
		return group;
	}

	/**
	 * One radio item per value, in one button group, in the menu's face, each taking the left release over so a pick keeps
	 * the menu and the submenu open. A pick writes the setting. Choice: the items are kept in {@code iconItems}, in the
	 * order of {@code values}, for {@link #syncControls} to select the current one; they are icon choices, so they
	 * carry a picture.
	 */
	private <T> List<IconChoice> radios(T[] values, Consumer<T> write)
	{
		final ButtonGroup group = new ButtonGroup();
		for (final T value : values)
		{
			final IconChoice item = new IconChoice(String.valueOf(value));
			item.setFont(menuFont());
			item.setForeground(Color.WHITE);
			item.addActionListener(e ->
			{
				if (!syncing)
				{
					write.accept(value);
				}
			});
			group.add(item);
			iconItems.add(item);
		}
		return iconItems;
	}

	/**
	 * Gives each icon choice the picture the sidebar button would have for it: the drawn coin and letters always, the
	 * two book choices once {@code book} is there (null: no picture yet).
	 */
	void iconPictures(BufferedImage book)
	{
		for (CaBossChecklistConfig.SidebarIcon value : CaBossChecklistConfig.SidebarIcon.values())
		{
			final boolean drawable = value == CaBossChecklistConfig.SidebarIcon.COIN_AND_LETTERS || book != null;
			iconItems.get(value.ordinal()).setPicture(drawable ? NavIcon.icon(value, book) : null);
		}
	}

	/**
	 * A radio item of the "Sidebar icon" submenu with its picture at the right end of the row. Choice: the row is always
	 * {@code 8 + NavIcon.SIZE} wider, picture or not, so the menu never changes width.
	 */
	static final class IconChoice extends JRadioButtonMenuItem
	{
		/** The picture at the row's right end; null for none. */
		BufferedImage picture;

		IconChoice(String text)
		{
			super(text);
		}

		void setPicture(BufferedImage picture)
		{
			this.picture = picture;
			repaint();
		}

		@Override
		public Dimension getPreferredSize()
		{
			final Dimension size = super.getPreferredSize();
			return new Dimension(size.width + 8 + NavIcon.SIZE, size.height);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (picture != null)
			{
				g.drawImage(picture, getWidth() - getInsets().right - NavIcon.SIZE,
					(getHeight() - NavIcon.SIZE) / 2, null);
			}
		}

		@Override
		protected void processMouseEvent(MouseEvent e)
		{
			if (!takeRelease(this, e))
			{
				super.processMouseEvent(e);
			}
		}
	}

	/** Takes a left release inside {@code item} over: clicks it and keeps Swing from closing the menu. */
	private static boolean takeRelease(JMenuItem item, MouseEvent e)
	{
		if (e.getID() != MouseEvent.MOUSE_RELEASED || !SwingUtilities.isLeftMouseButton(e) || !item.contains(e.getPoint()))
		{
			return false;
		}
		item.doClick(0);
		// The click leaves the item un-armed; arming it again keeps its hover highlight while the mouse stays on it.
		item.getModel().setArmed(true);
		e.consume();
		return true;
	}

	/** The menu's last row: "Reset to default" at its left, "OK" at its right. */
	private JPanel okRow()
	{
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 6, 2, 6));
		final JButton reset = button(RESET_TEXT);
		reset.addActionListener(e ->
		{
			settings.resetToDefault();
			syncControls();
		});
		final JButton ok = button(OK_TEXT);
		ok.addActionListener(e -> closeMenu());
		row.add(reset);
		row.add(Box.createHorizontalGlue());
		row.add(ok);
		return row;
	}

	/** A small button in the menu's 12 px type, no wider than its word. */
	private static JButton button(String text)
	{
		final JButton button = new JButton(text);
		button.setFont(FontManager.getDefaultFont().deriveFont(12f));
		button.setFocusable(false);
		button.setMaximumSize(button.getPreferredSize());
		return button;
	}
}
