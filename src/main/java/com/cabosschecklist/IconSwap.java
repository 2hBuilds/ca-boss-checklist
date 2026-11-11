package com.cabosschecklist;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Swaps the sidebar button for one with another picture and keeps the panel open if it was showing. RuneLite only
 * queues a button's removal and addition on the Swing thread while opening a panel runs at once, so an open asked for
 * in the same breath lands on the old tab, and RuneLite then falls back to another plugin's tab when the old one goes.
 * The open is therefore handed to {@code later}, after the add, and it opens the new button only if that button is
 * still the plugin's current one. The button type is generic so the order can be checked without a client.
 *
 * @param <B> the type of a sidebar button
 */
final class IconSwap<B>
{
	private final Consumer<B> remove;
	private final Consumer<B> add;
	private final Consumer<B> open;
	private final Supplier<B> current;
	private final Consumer<Runnable> later;

	/**
	 * @param remove takes a button off the toolbar
	 * @param add puts a button on the toolbar
	 * @param open opens a button's panel
	 * @param current the plugin's current button, or null when it has none
	 * @param later runs a task after the toolbar's queued work (Swing's invokeLater)
	 */
	IconSwap(Consumer<B> remove, Consumer<B> add, Consumer<B> open, Supplier<B> current, Consumer<Runnable> later)
	{
		this.remove = remove;
		this.add = add;
		this.open = open;
		this.current = current;
		this.later = later;
	}

	/** Removes {@code old}, adds {@code next}, and, when the panel was showing, opens {@code next} afterwards. */
	void swap(B old, B next, boolean wasShowing)
	{
		remove.accept(old);
		add.accept(next);
		if (wasShowing)
		{
			later.accept(() ->
			{
				if (current.get() == next)
				{
					open.accept(next);
				}
			});
		}
	}
}
