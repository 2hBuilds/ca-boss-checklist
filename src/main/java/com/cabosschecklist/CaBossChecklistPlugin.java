package com.cabosschecklist;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.StructComposition;
import net.runelite.api.WorldType;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.AsyncBufferedImage;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2h CA Boss Checklist: colours a Monster or Tier name green in the game's Tasks menu when the account has finished it
 * or the player marked it, and shows the bosses and the player's per-account marks in the sidebar.
 */
@PluginDescriptor(
	name = "2h CA Boss Checklist",
	internalName = "ca-boss-checklist",
	description = "A checklist of every Combat Achievements boss and its tasks. Finished bosses, and any you mark, show green in the game's Tasks menu. (v" + Version.CURRENT + ")",
	tags = {"combat achievements", "ca", "boss", "checklist", "green", "tasks"}
)
public class CaBossChecklistPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CaBossChecklistPlugin.class);

	/** The sidebar button's place. */
	private static final int NAV_PRIORITY = 7;

	/** The game's Easy to Grandmaster task lists (enums; RuneLite 1.13.6 has no gameval constants for them). */
	private static final int[] TASK_ENUMS = {3981, 3982, 3983, 3984, 3985, 3986};
	/** The Monster dropdown's own list (enum). */
	private static final int MONSTER_ENUM = 3970;
	/** The Monster list's long names (enum; RuneLite 1.13.6 has no gameval constant for it). */
	private static final int MONSTER_NAME_ENUM = 3971;
	/** A task struct's task id (param). */
	private static final int PARAM_TASK_ID = 1306;
	/** A task struct's monster id in the Monster list (param). */
	private static final int PARAM_MONSTER_ID = 1312;
	/** A task struct's largest group ironman group size that skips it (param). */
	private static final int PARAM_GIM_MAX_GROUP_SIZE = 1425;
	/** A task struct's flag for being skipped on a Deadman world (param). */
	private static final int PARAM_DEADMAN_SKIPPED = 2641;
	/** A task struct's name (param, string). */
	private static final int PARAM_TASK_NAME = 1308;
	/** A task struct's description (param, string). */
	private static final int PARAM_TASK_DESCRIPTION = 1309;
	/** A task struct's tier, 1 Easy to 6 Grandmaster (param, int). */
	private static final int PARAM_TASK_TIER = 1310;
	/** A task struct's type, 1 Stamina to 6 Speed (param, int). */
	private static final int PARAM_TASK_TYPE = 1311;
	/** The tier words, keys 1 to 6 (enum). */
	private static final int TIER_WORD_ENUM = 3967;
	/** The type words, keys 1 to 6 (enum). */
	private static final int TYPE_WORD_ENUM = 3969;
	/** The game's own green. */
	private static final int COMPLETE_COLOR = 0x0DC10D;
	/** The yellow of a name with one or two tasks left. */
	private static final int ALMOST_DONE_COLOR = 0xFFFF00;

	/** The 21 varps holding the completed-task bits, in order. */
	private static final int[] COMPLETED_VARPS = {
		VarPlayerID.CA_TASK_COMPLETED_0, VarPlayerID.CA_TASK_COMPLETED_1, VarPlayerID.CA_TASK_COMPLETED_2,
		VarPlayerID.CA_TASK_COMPLETED_3, VarPlayerID.CA_TASK_COMPLETED_4, VarPlayerID.CA_TASK_COMPLETED_5,
		VarPlayerID.CA_TASK_COMPLETED_6, VarPlayerID.CA_TASK_COMPLETED_7, VarPlayerID.CA_TASK_COMPLETED_8,
		VarPlayerID.CA_TASK_COMPLETED_9, VarPlayerID.CA_TASK_COMPLETED_10, VarPlayerID.CA_TASK_COMPLETED_11,
		VarPlayerID.CA_TASK_COMPLETED_12, VarPlayerID.CA_TASK_COMPLETED_13, VarPlayerID.CA_TASK_COMPLETED_14,
		VarPlayerID.CA_TASK_COMPLETED_15, VarPlayerID.CA_TASK_COMPLETED_16, VarPlayerID.CA_TASK_COMPLETED_17,
		VarPlayerID.CA_TASK_COMPLETED_18, VarPlayerID.CA_TASK_COMPLETED_19, VarPlayerID.CA_TASK_COMPLETED_20
	};
	/** The tier-status varbits for tiers 1 to 6; a value of 2 is complete (the Overview's own green test). */
	private static final int[] TIER_STATUS_VARBITS = {
		VarbitID.CA_TIER_STATUS_EASY, VarbitID.CA_TIER_STATUS_MEDIUM, VarbitID.CA_TIER_STATUS_HARD,
		VarbitID.CA_TIER_STATUS_ELITE, VarbitID.CA_TIER_STATUS_MASTER, VarbitID.CA_TIER_STATUS_GRANDMASTER
	};

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Client client;

	@Inject
	private CaBossChecklistConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	@Inject
	private ItemManager itemManager;

	private NavigationButton navButton;
	private CaBossChecklistPanel panel;
	private WikiPictures pictures;
	/** The game's Combat Achievements book once RuneLite has drawn it (EDT); null before. */
	private BufferedImage book;
	/** The picture the button shows now; COIN_AND_LETTERS while the drawn icon shows (EDT). */
	private CaBossChecklistConfig.SidebarIcon shown;
	private List<CaCompletion.Task> tasks = new ArrayList<>();
	private volatile Map<Integer, String> labels = Collections.emptyMap();
	private volatile Map<Integer, String> names = Collections.emptyMap();
	/** Null while logged out or before the first read. */
	private volatile Map<Integer, CaCompletion.Progress> progress;
	private volatile Map<Integer, Mark> marks = Collections.emptyMap();
	/** Each boss's task rows; empty while logged out or before the first read. */
	private volatile Map<Integer, List<CaCompletion.TaskLine>> taskLines = Collections.emptyMap();
	/** The task words, read once with the tasks (game thread). */
	private Map<Integer, CaCompletion.TaskText> texts = new HashMap<>();
	private Map<Integer, String> tierWords = new HashMap<>();
	private Map<Integer, String> typeWords = new HashMap<>();
	private Set<Integer> completeMonsters = Collections.emptySet();
	private Set<Integer> completeTiers = Collections.emptySet();
	private volatile boolean recompute;

	@Provides
	CaBossChecklistConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CaBossChecklistConfig.class);
	}

	@Override
	protected void startUp()
	{
		pictures = new WikiPictures(okHttpClient, gson, this::pictureArrived);
		panel = new CaBossChecklistPanel(new Settings(), new Actions(), pictures);
		navButton = navigationButton(NavIcon.create());
		shown = CaBossChecklistConfig.SidebarIcon.COIN_AND_LETTERS;
		clientToolbar.addNavigation(navButton);
		final AsyncBufferedImage drawn = itemManager.getImage(ItemID.CA_BOOK);
		drawn.onLoaded(() -> SwingUtilities.invokeLater(() ->
		{
			book = drawn;
			applyIcon();
		}));
		reloadMarks();
		recompute = true;
		pushView();
		log.info("2h CA Boss Checklist {} started on RuneLite {}", Version.CURRENT, RuneLiteProperties.getVersion());
	}

	/** The sidebar button, with {@code icon}. */
	private NavigationButton navigationButton(BufferedImage icon)
	{
		return NavigationButton.builder()
			.tooltip("2h CA Boss Checklist")
			.icon(icon)
			.priority(NAV_PRIORITY)
			.panel(panel)
			.build();
	}

	/**
	 * Shows the picture the setting asks for (EDT): the drawn icon while RuneLite has not drawn the book, else the chosen
	 * one. A button's icon cannot change, so a different picture swaps the button; the settings menu is closed first, and
	 * the panel is opened again if it was showing. Nothing is swapped when the picture would not change, and nothing
	 * happens after {@code shutDown}, which has dropped the button.
	 */
	private void applyIcon()
	{
		if (navButton == null || panel == null)
		{
			return;
		}
		final CaBossChecklistConfig.SidebarIcon wanted =
			book == null ? CaBossChecklistConfig.SidebarIcon.COIN_AND_LETTERS : config.sidebarIcon();
		if (wanted == shown)
		{
			return;
		}
		final boolean wasShowing = panel.isShowing();
		panel.closeMenu();
		clientToolbar.removeNavigation(navButton);
		navButton = navigationButton(NavIcon.icon(config.sidebarIcon(), book));
		clientToolbar.addNavigation(navButton);
		if (wasShowing)
		{
			clientToolbar.openPanel(navButton);
		}
		shown = wanted;
	}

	/** The settings menu's view of the config: reads through {@code config}, writes through {@code configManager}. */
	private final class Settings implements MenuSettings
	{
		@Override
		public boolean autoDetect()
		{
			return config.autoDetect();
		}

		@Override
		public void setAutoDetect(boolean on)
		{
			configManager.setConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.AUTO_DETECT_KEY, on);
		}

		@Override
		public boolean almostDone()
		{
			return config.almostDone();
		}

		@Override
		public void setAlmostDone(boolean on)
		{
			configManager.setConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.ALMOST_DONE_KEY, on);
		}

		@Override
		public boolean wikiPictures()
		{
			return config.wikiPictures();
		}

		@Override
		public void setWikiPictures(boolean on)
		{
			configManager.setConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.WIKI_PICTURES_KEY, on);
		}

		@Override
		public CaBossChecklistConfig.SidebarIcon sidebarIcon()
		{
			return config.sidebarIcon();
		}

		@Override
		public void setSidebarIcon(CaBossChecklistConfig.SidebarIcon icon)
		{
			configManager.setConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.SIDEBAR_ICON_KEY, icon);
		}

		@Override
		public void resetToDefault()
		{
			configManager.unsetConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.AUTO_DETECT_KEY);
			configManager.unsetConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.ALMOST_DONE_KEY);
			configManager.unsetConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.WIKI_PICTURES_KEY);
			configManager.unsetConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.SIDEBAR_ICON_KEY);
		}
	}

	@Override
	protected void shutDown()
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		panel = null;
		pictures = null;
		book = null;
		shown = null;
		tasks = new ArrayList<>();
		completeMonsters = Collections.emptySet();
		completeTiers = Collections.emptySet();
		labels = Collections.emptyMap();
		names = Collections.emptyMap();
		progress = null;
		taskLines = Collections.emptyMap();
		marks = Collections.emptyMap();
	}

	/** Reads this account's marks from the RuneScape profile. */
	private void reloadMarks()
	{
		marks = Marks.parse(configManager.getRSProfileConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.MARKS_KEY));
	}

	/** Asks the EDT to draw the newest state. */
	private void pushView()
	{
		SwingUtilities.invokeLater(this::refreshPanel);
	}

	/** A wiki answer came in on any thread: asks the EDT to let the card read its picture again. */
	private void pictureArrived()
	{
		final CaBossChecklistPanel target = panel;
		if (target != null)
		{
			SwingUtilities.invokeLater(target::pictureArrived);
		}
	}

	/** Draws the state as it is now, so the last post always shows the newest state (EDT). */
	private void refreshPanel()
	{
		final CaBossChecklistPanel target = panel;
		if (target == null)
		{
			return;
		}
		final Map<Integer, CaCompletion.Progress> current = progress;
		if (current == null || configManager.getRSProfileKey() == null)
		{
			target.showLoggedOut();
		}
		else
		{
			target.show(ChecklistView.build(labels, names, current, marks, config.autoDetect()).withTasks(taskLines));
		}
	}

	/** The sidebar's actions on the marks (EDT); each does nothing while logged out. */
	private final class Actions implements MarkActions
	{
		@Override
		public void flip(int id)
		{
			final Map<Integer, CaCompletion.Progress> current = progress;
			if (current == null || configManager.getRSProfileKey() == null)
			{
				return;
			}
			final boolean auto = config.autoDetect();
			final Set<Integer> complete = CaCompletion.completeIds(current);
			final Mark next = Marks.flip(Marks.isGreen(id, auto, complete, marks), complete.contains(id), auto);
			final Map<Integer, Mark> changed = new HashMap<>(marks);
			if (next == null)
			{
				changed.remove(id);
			}
			else
			{
				changed.put(id, next);
			}
			write(changed);
		}

		@Override
		public void keep(int id)
		{
			final Mark mark = marks.get(id);
			if (progress == null || configManager.getRSProfileKey() == null || mark == null)
			{
				return;
			}
			final Map<Integer, Mark> changed = new HashMap<>(marks);
			changed.put(id, mark.asKept());
			write(changed);
		}

		@Override
		public void remove(List<Integer> ids)
		{
			if (progress == null || configManager.getRSProfileKey() == null)
			{
				return;
			}
			final Map<Integer, Mark> changed = new HashMap<>(marks);
			changed.keySet().removeAll(ids);
			write(changed);
		}

		/** Stores the marks; the ConfigChanged that follows reloads and redraws. */
		private void write(Map<Integer, Mark> next)
		{
			final String value = Marks.format(next);
			if (value.isEmpty())
			{
				configManager.unsetRSProfileConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.MARKS_KEY);
			}
			else
			{
				configManager.setRSProfileConfiguration(CaBossChecklistConfig.GROUP, CaBossChecklistConfig.MARKS_KEY, value);
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		final GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			recompute = true;
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			completeMonsters = Collections.emptySet();
			completeTiers = Collections.emptySet();
			if (state == GameState.LOGIN_SCREEN)
			{
				progress = null;
				taskLines = Collections.emptyMap();
				pushView();
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (contains(COMPLETED_VARPS, event.getVarpId()) || contains(TIER_STATUS_VARBITS, event.getVarbitId())
			|| event.getVarbitId() == VarbitID.GIM_GROUPSIZE)
		{
			recompute = true;
		}
	}

	private static boolean contains(int[] ids, int id)
	{
		for (int candidate : ids)
		{
			if (candidate == id)
			{
				return true;
			}
		}
		return false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CaBossChecklistConfig.GROUP.equals(event.getGroup()))
		{
			reloadMarks();
			recompute = true;
			pushView();
			if (CaBossChecklistConfig.SIDEBAR_ICON_KEY.equals(event.getKey()))
			{
				SwingUtilities.invokeLater(this::applyIcon);
			}
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		reloadMarks();
		pushView();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!recompute || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (tasks.isEmpty())
		{
			loadTasks();
		}
		final int[] varps = new int[COMPLETED_VARPS.length];
		for (int i = 0; i < varps.length; i++)
		{
			varps[i] = client.getVarpValue(COMPLETED_VARPS[i]);
		}
		final boolean deadman = client.getWorldType().contains(WorldType.DEADMAN);
		final int groupSize = client.getVarbitValue(VarbitID.GIM_GROUPSIZE);
		final Map<Integer, CaCompletion.Progress> read =
			Collections.unmodifiableMap(CaCompletion.progress(tasks, varps, deadman, groupSize));
		completeMonsters = CaCompletion.completeIds(read);
		progress = read;
		taskLines = Collections.unmodifiableMap(
			CaCompletion.taskLines(tasks, texts, tierWords, typeWords, varps, deadman, groupSize));
		final Set<Integer> tiers = new HashSet<>();
		for (int i = 0; i < TIER_STATUS_VARBITS.length; i++)
		{
			if (client.getVarbitValue(TIER_STATUS_VARBITS[i]) == 2)
			{
				tiers.add(i + 1);
			}
		}
		completeTiers = tiers;
		pushView();
		recompute = false;
	}

	/** Reads every task's four params from the game's task lists. */
	private void loadTasks()
	{
		final List<CaCompletion.Task> loaded = new ArrayList<>();
		final Map<Integer, CaCompletion.TaskText> loadedTexts = new HashMap<>();
		for (int enumId : TASK_ENUMS)
		{
			for (int structId : client.getEnum(enumId).getIntVals())
			{
				final StructComposition struct = client.getStructComposition(structId);
				final int taskId = struct.getIntValue(PARAM_TASK_ID);
				loaded.add(new CaCompletion.Task(
					taskId,
					struct.getIntValue(PARAM_MONSTER_ID),
					struct.getIntValue(PARAM_GIM_MAX_GROUP_SIZE),
					struct.getIntValue(PARAM_DEADMAN_SKIPPED)));
				loadedTexts.put(taskId, new CaCompletion.TaskText(
					struct.getStringValue(PARAM_TASK_NAME),
					struct.getStringValue(PARAM_TASK_DESCRIPTION),
					struct.getIntValue(PARAM_TASK_TIER),
					struct.getIntValue(PARAM_TASK_TYPE)));
			}
		}
		texts = loadedTexts;
		tierWords = words(TIER_WORD_ENUM);
		typeWords = words(TYPE_WORD_ENUM);
		tasks = loaded;
		final Map<Integer, String> loadedLabels = new LinkedHashMap<>();
		final EnumComposition monsters = client.getEnum(MONSTER_ENUM);
		for (int key : monsters.getKeys())
		{
			if (key > 0)
			{
				loadedLabels.put(key, monsters.getStringValue(key));
			}
		}
		final Map<Integer, String> loadedNames = new LinkedHashMap<>();
		final EnumComposition longNames = client.getEnum(MONSTER_NAME_ENUM);
		for (int key : longNames.getKeys())
		{
			if (key > 0)
			{
				loadedNames.put(key, longNames.getStringValue(key));
			}
		}
		names = Collections.unmodifiableMap(loadedNames);
		labels = Collections.unmodifiableMap(loadedLabels);
	}

	/** The words of an enum's keys 1 to 6. */
	private Map<Integer, String> words(int enumId)
	{
		final Map<Integer, String> loaded = new HashMap<>();
		final EnumComposition composition = client.getEnum(enumId);
		for (int key = 1; key <= 6; key++)
		{
			loaded.put(key, composition.getStringValue(key));
		}
		return loaded;
	}

	/** The game's hover script resets a row's colour while the mouse is over the list, so the colour is set every frame. */
	@Subscribe
	public void onClientTick(ClientTick event)
	{
		final boolean autoDetect = config.autoDetect();
		final boolean almostDone = config.almostDone();
		final Map<Integer, Mark> markNow = marks;
		final Map<Integer, CaCompletion.Progress> progressNow = progress;
		final Widget monsters = client.getWidget(InterfaceID.CaTasks.DROPDOWN_MONSTER);
		if (monsters != null && !monsters.isHidden())
		{
			final Widget[] rows = monsters.getDynamicChildren();
			if (rows != null)
			{
				for (Widget row : rows)
				{
					final int id = CaCompletion.idForRow(row.getIndex());
					if (Marks.isGreen(id, autoDetect, completeMonsters, markNow))
					{
						row.setTextColor(COMPLETE_COLOR);
					}
					else if (almostDone && progressNow != null && CaCompletion.isAlmostDone(progressNow.get(id)))
					{
						row.setTextColor(ALMOST_DONE_COLOR);
					}
				}
			}
		}
		final Widget tiers = client.getWidget(InterfaceID.CaTasks.DROPDOWN_TIER);
		if (tiers != null && !tiers.isHidden())
		{
			final Widget[] rows = tiers.getDynamicChildren();
			if (rows != null)
			{
				for (Widget row : rows)
				{
					final int id = CaCompletion.idForRow(row.getIndex());
					if (id >= 1 && id <= 6 && completeTiers.contains(id))
					{
						row.setTextColor(COMPLETE_COLOR);
					}
				}
			}
		}
	}
}
