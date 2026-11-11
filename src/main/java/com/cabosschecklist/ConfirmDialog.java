package com.cabosschecklist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import net.runelite.client.ui.ColorScheme;

/**
 * A small question window over the client, with a yes button and <i>Cancel</i>, in RuneLite's dark colours. The yes
 * button is the default button and takes the focus, so Enter answers yes; Escape, <i>Cancel</i> and the window's close
 * box all answer no. It is modal to the client window that owns it. Modelled on the bank plugin's question window.
 *
 * <p>The panel of controls ({@link #content}) is built without a window, so a test can press its buttons on a machine
 * with no screen; the {@link JDialog} that hosts it is made only by {@link #window}.
 */
final class ConfirmDialog
{
	static final String CANCEL_TEXT = "Cancel";

	private static final int PAD_TOP = 14;
	private static final int PAD_SIDE = 18;
	private static final int PAD_BOTTOM = 12;
	/** The tile holding the doubled picture, in px, and the picture's scale. */
	private static final int TILE = 42;
	private static final int SCALE = 2;
	/** The gap between the tile and the lines, and under the first line, between the middle lines, above the last. */
	private static final int TILE_GAP = 14;
	private static final int AFTER_FIRST = 6;
	private static final int BETWEEN = 3;
	private static final int BEFORE_LAST = 8;
	private static final int BUTTON_GAP = 6;
	private static final int BUTTONS_TOP = 14;
	private static final Color MIDDLE_GRAY = new Color(0xD6D6D6);
	/** The key Escape is bound under in {@link #content}'s action map. */
	static final String CANCEL_KEY = "cancel";

	/** Everything the window shows, built without a window. */
	final JPanel content = new JPanel(new BorderLayout());
	/** One label per line of the question, in order. */
	final List<JLabel> lines;
	/** The label holding the doubled picture; null when there is no picture. */
	final JLabel pictureLabel;
	final JButton yesButton;
	final JButton cancelButton;
	/** True once the yes button was pressed; false until then, whatever else closes the window. EDT. */
	boolean answeredYes;
	/** The hosting dialog; null until {@link #window}, and again once closed. EDT. */
	private JDialog dialog;

	/**
	 * Builds the controls: the {@code picture} (the sidebar button's icon, or null for none) drawn at twice its size in
	 * a tile at the left, then one label per line of {@code question}, then the buttons. Choice: the tile is a
	 * 42 x 42 panel with a 1 px border at the top of the row, so it is level with the first line; with two lines
	 * the second is a middle line, and a single line is the bold first line.
	 */
	ConfirmDialog(BufferedImage picture, String question, String yes)
	{
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		content.setBorder(new EmptyBorder(PAD_TOP, PAD_SIDE, PAD_BOTTOM, PAD_SIDE));

		final String[] parts = question.split("\n");
		final List<JLabel> made = new ArrayList<>();
		final JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		for (int i = 0; i < parts.length; i++)
		{
			final boolean first = i == 0;
			final boolean last = i == parts.length - 1 && parts.length >= 3;
			final JLabel label = new JLabel(parts[i]);
			label.setFont(first ? ViewParts.bold(12f) : ViewParts.plain(12f));
			label.setForeground(first ? Color.WHITE : last ? ColorScheme.LIGHT_GRAY_COLOR : MIDDLE_GRAY);
			label.setAlignmentX(Component.LEFT_ALIGNMENT);
			if (i > 0)
			{
				text.add(Box.createVerticalStrut(i == 1 ? AFTER_FIRST : last ? BEFORE_LAST : BETWEEN));
			}
			text.add(label);
			made.add(label);
		}
		lines = Collections.unmodifiableList(made);

		final JPanel row = new JPanel(new BorderLayout(TILE_GAP, 0));
		row.setOpaque(false);
		if (picture == null)
		{
			pictureLabel = null;
		}
		else
		{
			pictureLabel = new JLabel(new ImageIcon(doubled(picture)));
			final JPanel tile = new JPanel(new GridBagLayout());
			tile.setBackground(ColorScheme.DARK_GRAY_COLOR);
			tile.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
			tile.setPreferredSize(new Dimension(TILE, TILE));
			tile.add(pictureLabel);
			final JPanel top = new JPanel(new BorderLayout());
			top.setOpaque(false);
			top.add(tile, BorderLayout.NORTH);
			row.add(top, BorderLayout.WEST);
		}
		row.add(text, BorderLayout.CENTER);
		content.add(row, BorderLayout.CENTER);

		yesButton = smallButton(yes, true);
		cancelButton = smallButton(CANCEL_TEXT, false);
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, BUTTON_GAP, 0));
		buttons.setOpaque(false);
		buttons.setBorder(new EmptyBorder(BUTTONS_TOP, 0, 0, 0));
		buttons.add(yesButton);
		buttons.add(cancelButton);
		content.add(buttons, BorderLayout.SOUTH);

		// Escape is Cancel wherever the focus is in the window.
		content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL_KEY);
		content.getActionMap().put(CANCEL_KEY, new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				answer(false);
			}
		});
	}

	/** A new image of {@code picture} at twice its size, drawn with nearest-neighbour scaling so it stays sharp. */
	private static BufferedImage doubled(BufferedImage picture)
	{
		final BufferedImage out = new BufferedImage(picture.getWidth() * SCALE, picture.getHeight() * SCALE,
			BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = out.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.drawImage(picture, 0, 0, out.getWidth(), out.getHeight(), null);
		}
		finally
		{
			g.dispose();
		}
		return out;
	}

	private JButton smallButton(String text, boolean yes)
	{
		final JButton button = new JButton(text);
		button.setFont(ViewParts.plain(12f));
		button.setFocusPainted(false);
		button.setMargin(new Insets(1, 5, 1, 5));
		button.addActionListener(e -> answer(yes));
		return button;
	}

	/**
	 * EDT. Puts the question to the player and answers what they chose: true for the yes button only. Blocks - the
	 * dialog is modal - until it is closed. Answers false at once on a machine with no display.
	 *
	 * @param owner the RuneLite frame (the panel's window ancestor), or null
	 * @param picture the picture drawn at twice its size at the left, or null for none
	 * @param title the window's title
	 * @param yes the word on the button that answers yes
	 */
	static boolean ask(Window owner, BufferedImage picture, String title, String question, String yes)
	{
		if (GraphicsEnvironment.isHeadless())
		{
			return false;
		}
		final ConfirmDialog window = window(owner, picture, title, question, yes);
		window.dialog.setVisible(true);
		return window.answeredYes;
	}

	/**
	 * The window built and sized but not yet shown: a {@link JDialog} owned by {@code owner}, modal to its owner's
	 * windows, closing by disposing, with the yes button as its default button and the first focus, not resizable,
	 * centred on the owner. Needs a display.
	 */
	static ConfirmDialog window(Window owner, BufferedImage picture, String title, String question, String yes)
	{
		final ConfirmDialog window = new ConfirmDialog(picture, question, yes);
		final JDialog dialog = new JDialog(owner, title);
		dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setContentPane(window.content);
		dialog.getRootPane().setDefaultButton(window.yesButton);
		dialog.setResizable(false);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy()
		{
			@Override
			public Component getInitialComponent(Window w)
			{
				return window.yesButton;
			}
		});
		window.dialog = dialog;
		return window;
	}

	/** A button's press, or Escape: remembers the answer and closes the window; harmless with no window. */
	private void answer(boolean yes)
	{
		answeredYes = yes;
		final JDialog host = dialog;
		dialog = null;
		if (host != null)
		{
			host.dispose();
		}
	}
}
