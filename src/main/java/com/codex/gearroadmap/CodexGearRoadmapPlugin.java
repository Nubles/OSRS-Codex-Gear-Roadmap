package com.codex.gearroadmap;

import com.codex.gearroadmap.core.Advisor;
import com.codex.gearroadmap.core.Aim;
import com.codex.gearroadmap.core.Fighter;
import com.codex.gearroadmap.core.Fighting;
import com.codex.gearroadmap.core.Fights;
import com.codex.gearroadmap.core.Holdings;
import com.codex.gearroadmap.core.Levels;
import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.core.Outgrown;
import com.codex.gearroadmap.core.Savings;
import com.codex.gearroadmap.core.Swap;
import com.codex.gearroadmap.core.SlayerTask;
import com.codex.gearroadmap.core.TaskWatch;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.codex.gearroadmap.overlay.UpgradePlaque;
import com.codex.gearroadmap.ui.Format;
import com.codex.gearroadmap.ui.GearRoadmapPanel;
import com.codex.gearroadmap.ui.IconWatch;
import com.codex.gearroadmap.ui.ItemIcons;
import com.codex.gearroadmap.ui.ShareActions;
import com.codex.gearroadmap.ui.Sigil;
import com.codex.gearroadmap.ui.Theme;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.NPC;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Filepath;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Codex Gear Roadmap",
	description = "Ranks your next gear upgrades by damage gained per coin against the monster you are aiming at",
	tags = {"gear", "upgrade", "dps", "damage", "bis", "best", "loadout", "money", "codex"}
)
public class CodexGearRoadmapPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CodexGearRoadmapPlugin.class);
	/** Codex Plunder Pilot and Pest Control Pilot read the saved bank under this key: never rename it. */
	static final String BANK_KEY = "bank";
	/** When Gear Roadmap last saw its bank change, epoch ms, so it can tell whose bank is fresher. */
	private static final String BANK_SEEN_KEY = "bankSeenAt";
	private static final String GOAL_KEY = "goal";
	/** Codex Ledger's config group and the key it saves its net worth history under, read and never written. */
	static final String LEDGER_GROUP = "codexledger";
	static final String LEDGER_KEY = "ledger";
	/** Codex Session Chronicle's journal and live session, read only, for where the player fights. */
	static final String CHRONICLE_GROUP = "codexsessionchronicle";
	static final String CHRONICLE_JOURNAL = "journal";
	static final String CHRONICLE_CURRENT = "current";
	private static final Type LOGGED_LIST = new TypeToken<List<Fights.Logged>>()
	{
	}.getType();
	private static final Skill[] SKILLS = {
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.PRAYER, Skill.HITPOINTS, Skill.AGILITY,
	};
	private static final Set<Integer> UNLOCK_VARBITS = new HashSet<>(Arrays.asList(
		VarbitID.PRAYER_RIGOUR_UNLOCKED, VarbitID.PRAYER_AUGURY_UNLOCKED, VarbitID.KR_KNIGHTWAVES_STATE,
		VarbitID.PRAYER_DEADEYE_UNLOCKED, VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED, VarbitID.KANDARIN_DIARY_HARD_COMPLETE,
		VarbitID.IRONMAN));
	private static final Set<Integer> TASK_VARPS = new HashSet<>(Arrays.asList(VarPlayerID.SLAYER_COUNT, VarPlayerID.SLAYER_TARGET));
	/** The slayer task id that stands for "a boss", whose name is then looked up from the boss id. */
	private static final int BOSS_TASK = 98;
	/** King's Ransom's knight training is done from this state on, which unlocks Piety and Chivalry. */
	private static final int KNIGHT_WAVES_DONE = 8;
	private static final int ANALYSE_GAP_TICKS = 3;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private Gson gson;

	@Inject
	private CodexGearRoadmapConfig config;

	private final UpgradePlaque plaque = new UpgradePlaque();
	private final AtomicInteger generation = new AtomicInteger();
	private final Set<String> claimed = new HashSet<>();

	private RoadmapData data;
	private Advisor advisor;
	private Set<Integer> priceIds = Collections.emptySet();
	/** Hands each item picture's load callback over only until it arrives. */
	private final IconWatch iconWatch = new IconWatch();
	private GearRoadmapPanel panel;
	private NavigationButton navButton;

	private String profile;
	/** Worn and carried gear by id, with how many of each (ammunition is counted by the stack). */
	private Map<Integer, Long> worn = Collections.emptyMap();
	private Map<Integer, Long> inventory = Collections.emptyMap();
	/** Ammunition held in Dizana's quiver, which is in no item container: its id (0 for none) and how many. */
	private int quiverId;
	private long quiverAmount;
	private Bank bank;
	/** Quantities of the banked gear as seen this login, or null while the bank is only known from a save. */
	private Map<Integer, Long> bankCounts;
	/** When the bank was last seen to change by this plugin, epoch ms; 0 when unknown. */
	private long bankSeenAt;
	/** Codex Bank Steward's shared bank for this account, or null without it. */
	private Holdings.Shared steward;
	/** The bank planned with: this plugin's own view or Bank Steward's, whichever is fresher. */
	private Holdings holdings = Holdings.NONE;
	private long inventoryCoins;
	private Boolean ancients;
	private boolean dirty;
	/** Codex Session Chronicle's filed sessions for this account, or empty without it. */
	private List<Fights.Logged> chronicle = Collections.emptyList();
	private boolean coinsDirty;
	/** What is worn changed without changing what is owned, so only the swap advice needs redoing. */
	private boolean wornDirty;
	private int ticksSinceAnalysis;
	private final Map<Skill, Integer> realLevels = new HashMap<>();
	/** Which task the plan was made for, believed only once the varps settle after a login or hop. */
	private final TaskWatch taskWatch = new TaskWatch();
	/** Set on a login, hop or reconnect, until the game is in the world again. */
	private boolean arriving;
	/** The listed monster the player is fighting, offered as a one-click aim; built with the data. */
	private Fighting fighting;
	/** The upgrade the player is saving for, per account. */
	private Savings.Goal goal;
	/** Codex Ledger's net worth for this account, or null when it is not installed. */
	private volatile Savings.Worth worth;
	/** Whether the goal was affordable at the last look; null until then, so a login never announces it. */
	private Boolean goalAffordable;
	/** Whether the goal was announced as in reach this login, so coins dipping and returning say it once. */
	private boolean goalAnnounced;
	/** The task the player turned away from by choosing another target, by task key. */
	private String taskDismissed;
	/** Which of the task's targets the player picked, and for which task. */
	private String taskPick;
	private String taskPickKey;
	/** The current slayer task's name as the game writes it, read with the fighter. */
	private String taskName;
	/** The analysis whose report was last published, so a coin update never lands on a road about to be replaced. */
	private volatile int publishedTicket;

	/** What the latest roadmap was, so a claimed upgrade can be named; written on the executor. */
	private volatile Roadmap lastRoadmap;
	private volatile Report lastReport;
	private volatile String lastName;
	private volatile SurveyCache survey;

	/** The bank as last seen, saved per account so the roadmap knows it between sessions. */
	static final class Bank
	{
		Set<Integer> items = new TreeSet<>();
		long coins;
	}

	private static final class SurveyCache
	{
		final String key;
		final List<Roadmap.Survey> rows;

		SurveyCache(String key, List<Roadmap.Survey> rows)
		{
			this.key = key;
			this.rows = rows;
		}
	}

	@Provides
	CodexGearRoadmapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexGearRoadmapConfig.class);
	}

	@Override
	protected void startUp() throws IOException
	{
		migrateConfig();
		data = RoadmapData.load(gson);
		advisor = new Advisor(data);
		fighting = new Fighting(data.targets());
		priceIds = advisor.priceIds();
		ItemIcons icons = (itemId, onLoaded) ->
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			// Only until it arrives: a loaded picture would run the callback again on every paint.
			iconWatch.watch(image, onLoaded, image::onLoaded);
			return image;
		};
		panel = new GearRoadmapPanel(icons, name -> clientThread.invoke(() -> aim(name)),
			option -> clientThread.invoke(() -> setGoal(option)));
		panel.setShare(new ShareActions()
		{
			@Override
			public void save(BufferedImage card, String fileName)
			{
				saveCard(card, fileName);
			}

			@Override
			public void copy(BufferedImage card)
			{
				copyCard(card);
			}
		});
		navButton = NavigationButton.builder()
			.tooltip("Codex Gear Roadmap")
			.icon(Sigil.icon(16))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(plaque);
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				loadBank();
				readContainer(InventoryID.WORN);
				readContainer(InventoryID.INV);
				readQuiver();
				dirty = true;
			}
		});
	}

	/** The old on-task switch becomes the slayer task setting; only an explicit "off" needs carrying over. */
	private void migrateConfig()
	{
		String old = configManager.getConfiguration(CodexGearRoadmapConfig.GROUP, CodexGearRoadmapConfig.OLD_ON_TASK);
		if (old == null)
		{
			return;
		}
		if ("false".equals(old)
			&& configManager.getConfiguration(CodexGearRoadmapConfig.GROUP, CodexGearRoadmapConfig.SLAYER_TASK) == null)
		{
			configManager.setConfiguration(CodexGearRoadmapConfig.GROUP, CodexGearRoadmapConfig.SLAYER_TASK, SlayerTask.Mode.NEVER);
		}
		configManager.unsetConfiguration(CodexGearRoadmapConfig.GROUP, CodexGearRoadmapConfig.OLD_ON_TASK);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(plaque);
		clientToolbar.removeNavigation(navButton);
		plaque.clear();
		generation.incrementAndGet();
		clientThread.invokeLater(this::forget);
		panel = null;
	}

	private void forget()
	{
		profile = null;
		bank = null;
		bankCounts = null;
		bankSeenAt = 0;
		steward = null;
		holdings = Holdings.NONE;
		worn = Collections.emptyMap();
		inventory = Collections.emptyMap();
		quiverId = 0;
		quiverAmount = 0;
		inventoryCoins = 0;
		ancients = null;
		lastRoadmap = null;
		lastReport = null;
		realLevels.clear();
		taskWatch.forget();
		if (fighting != null)
		{
			fighting.clear();
		}
		showFighting();
		survey = null;
		claimed.clear();
		goal = null;
		worth = null;
		chronicle = Collections.emptyList();
		goalAffordable = null;
		goalAnnounced = false;
		taskDismissed = null;
		taskPick = null;
		taskPickKey = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.HOPPING || state == GameState.LOGGING_IN || state == GameState.CONNECTION_LOST)
		{
			// The varps are taken afresh in the new world; the task may blink to none and back meanwhile.
			taskWatch.arrived();
			arriving = true;
		}
		if (state == GameState.LOGIN_SCREEN)
		{
			forget();
			plaque.clear();
			generation.incrementAndGet();
			publish(Report.loggedOut(), null);
		}
		else if (state == GameState.LOGGED_IN && arriving)
		{
			// Only arriving in a world needs a fresh plan: the game passes through LOGGED_IN again after
			// every loading screen (each teleport), and what changes there brings its own event.
			arriving = false;
			dirty = true;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		bank = null;
		bankCounts = null;
		bankSeenAt = 0;
		steward = null;
		holdings = Holdings.NONE;
		profile = null;
		ancients = null;
		survey = null;
		claimed.clear();
		goal = null;
		worth = null;
		chronicle = Collections.emptyList();
		goalAffordable = null;
		goalAnnounced = false;
		taskDismissed = null;
		taskPick = null;
		taskPickKey = null;
		loadBank();
		dirty = true;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (LEDGER_GROUP.equals(event.getGroup()))
		{
			// Codex Ledger saved its book: only the savings pace moves, never the plan.
			if (LEDGER_KEY.equals(event.getKey()) && profile != null && profile.equals(event.getProfile()))
			{
				readWorth();
				coinsDirty = true;
			}
			return;
		}
		if (Holdings.STEWARD_GROUP.equals(event.getGroup()))
		{
			// Bank Steward saw the bank: what is owned may have moved, though nothing was claimed here.
			if (Holdings.STEWARD_KEY.equals(event.getKey()) && profile != null && profile.equals(event.getProfile()))
			{
				clientThread.invoke(() ->
				{
					Set<Integer> before = ownedIds();
					long coinsBefore = coins();
					readSteward();
					if (!ownedIds().equals(before))
					{
						dirty = true;
					}
					else if (coins() != coinsBefore)
					{
						coinsDirty = true;
					}
				});
			}
			return;
		}
		if (CHRONICLE_GROUP.equals(event.getGroup()))
		{
			// Session Chronicle filed a session: where the player fights may have moved.
			if (CHRONICLE_JOURNAL.equals(event.getKey()) && profile != null && profile.equals(event.getProfile()))
			{
				clientThread.invoke(() ->
				{
					readChronicle();
					dirty = true;
				});
			}
			return;
		}
		if (!CodexGearRoadmapConfig.GROUP.equals(event.getGroup()) || event.getProfile() != null)
		{
			// The plugin's own per-account saves (bank, goal) change nothing that needs a new plan.
			return;
		}
		if (!config.showPlaque())
		{
			plaque.clear();
		}
		if (CodexGearRoadmapConfig.TARGET.equals(event.getKey()))
		{
			// A target typed in the settings is a choice too: one the task does not cover stops following it.
			String chosen = event.getNewValue();
			clientThread.invoke(() ->
			{
				Report report = lastReport;
				if (client.getGameState() == GameState.LOGGED_IN && report != null && !report.getTaskTargets().isEmpty()
					&& !report.getTaskTargets().contains(chosen))
				{
					taskDismissed = taskKey();
				}
				dirty = true;
			});
		}
		dirty = true;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id != InventoryID.WORN && id != InventoryID.INV && id != InventoryID.BANK)
		{
			return;
		}
		// The first sight of the bank is not a claim: everything in it was simply unknown before.
		boolean bankKnown = holdings.isKnown();
		Set<Integer> before = ownedIds();
		long coinsBefore = coins();
		Set<Integer> wornBefore = new HashSet<>(worn.keySet());
		Set<Integer> carriedBefore = new HashSet<>(inventory.keySet());
		readContainer(id);
		if (!worn.keySet().equals(wornBefore) || !inventory.keySet().equals(carriedBefore))
		{
			// Putting on or taking off something already owned only changes the swap advice.
			wornDirty = true;
		}
		Set<Integer> after = ownedIds();
		if (!after.equals(before))
		{
			dirty = true;
			if (bankKnown)
			{
				checkGoal(before, after);
				if (taskWatch.isSettled())
				{
					// Just after a login or hop the containers fill in again: gear arriving then is no claim.
					checkClaims(before, after);
				}
			}
		}
		else if (coins() != coinsBefore)
		{
			// Coins only move the "in reach" line, which does not need a new plan.
			coinsDirty = true;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		// Experience drops fire this on every hit; only a new real level changes the plan.
		for (Skill skill : SKILLS)
		{
			if (skill == event.getSkill())
			{
				Integer previous = realLevels.put(skill, event.getLevel());
				if (previous == null || previous != event.getLevel())
				{
					dirty = true;
				}
				return;
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (UNLOCK_VARBITS.contains(event.getVarbitId()))
		{
			dirty = true;
		}
		else if (event.getVarbitId() == -1 && (event.getVarpId() == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO
			|| event.getVarpId() == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT))
		{
			// Every arrow fired moves the count: only a change in what is owned re-plans.
			Set<Integer> before = ownedIds();
			readQuiver();
			if (!ownedIds().equals(before))
			{
				dirty = true;
			}
		}
		else if (event.getVarbitId() == VarbitID.SLAYER_TARGET_BOSSID
			|| event.getVarbitId() == -1 && TASK_VARPS.contains(event.getVarpId()))
		{
			// Only a new task, or the end of one, changes the plan; the count itself does not.
			taskChanged(taskWatch.seen(taskKey()));
		}
	}

	private void taskChanged(TaskWatch.Change change)
	{
		if (change == TaskWatch.Change.ENDED)
		{
			// The task is done: the next one is followed again, even if it is the same monster.
			taskDismissed = null;
			taskPick = null;
			taskPickKey = null;
		}
		if (change != TaskWatch.Change.NONE)
		{
			dirty = true;
		}
	}

	/** Notes the listed monster the player is attacking, and lets go of one fought long ago; cheap, so every tick. */
	private void watchFight()
	{
		Player me = client.getLocalPlayer();
		Actor foe = me == null ? null : me.getInteracting();
		long now = System.currentTimeMillis();
		boolean changed = foe instanceof NPC && foe.getCombatLevel() > 0 && fighting.saw(foe.getName(), now);
		changed |= fighting.expire(now);
		if (changed)
		{
			showFighting();
		}
	}

	private void showFighting()
	{
		Target t = fighting == null ? null : fighting.current();
		String name = t == null ? null : t.getName();
		GearRoadmapPanel target = panel;
		if (target != null)
		{
			SwingUtilities.invokeLater(() -> target.setFighting(name));
		}
	}

	/** The task by its id (and boss id), or "" without one; its kill count is left out on purpose. */
	private String taskKey()
	{
		boolean onTask = client.getVarpValue(VarPlayerID.SLAYER_COUNT) > 0;
		return onTask ? client.getVarpValue(VarPlayerID.SLAYER_TARGET) + ":" + client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID) : "";
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		// Switching to another RuneLite profile does not restart the plugin, so that profile's old setting is carried over here.
		migrateConfig();
		dirty = true;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (profile == null)
		{
			loadBank();
		}
		taskChanged(taskWatch.tick(taskKey()));
		watchFight();
		ticksSinceAnalysis++;
		// The plan reads the slayer task straight from the varps, so it waits for them to settle after
		// a login or hop: a plan made while they blink to none would aim away from the task, and the
		// settling tick, seeing the same task as before, would never plan again.
		if (dirty && ticksSinceAnalysis >= ANALYSE_GAP_TICKS && taskWatch.isSettled())
		{
			dirty = false;
			coinsDirty = false;
			wornDirty = false;
			ticksSinceAnalysis = 0;
			analyse();
		}
		else if ((coinsDirty || wornDirty) && !dirty && publishedTicket == generation.get())
		{
			coinsDirty = false;
			wornDirty = false;
			Report report = lastReport;
			if (report != null && report.getRoadmap() != null)
			{
				Report recoined = dress(report.withRoadmap(report.getRoadmap().withCoins(report.isIronman() ? 0 : coins())));
				lastReport = recoined;
				lastRoadmap = recoined.getRoadmap();
				publish(recoined, lastName);
			}
		}
	}

	private void readContainer(int id)
	{
		ItemContainer container = client.getItemContainer(id);
		if (container == null)
		{
			return;
		}
		Map<Integer, Long> items = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() > 0 && item.getQuantity() > 0 && data.gear(item.getId()) != null)
			{
				items.merge(item.getId(), (long) item.getQuantity(), Long::sum);
			}
		}
		if (id == InventoryID.WORN)
		{
			worn = items;
		}
		else if (id == InventoryID.INV)
		{
			inventory = items;
			inventoryCoins = coinsIn(container);
		}
		else
		{
			Bank seen = new Bank();
			seen.items = new TreeSet<>(items.keySet());
			seen.coins = coinsIn(container);
			bankCounts = items;
			bankSeenAt = System.currentTimeMillis();
			if (bank == null || !bank.items.equals(seen.items) || bank.coins != seen.coins)
			{
				bank = seen;
				saveBank();
			}
			choose();
		}
	}

	/** Coins and platinum tokens, each token worth a thousand. */
	private static long coinsIn(ItemContainer container)
	{
		return container.count(ItemID.COINS) + 1000L * container.count(ItemID.PLATINUM);
	}

	/** Everything owned, worn, carried and banked; ammunition only as a stack worth bringing. */
	private Set<Integer> ownedIds()
	{
		Map<Integer, Long> total = new HashMap<>(worn);
		inventory.forEach((id, n) -> total.merge(id, n, Long::sum));
		if (quiverId > 0 && quiverAmount > 0)
		{
			total.merge(quiverId, quiverAmount, Long::sum);
		}
		// A quantity never seen stays unknown, and an unknown stack counts whatever else is held.
		holdings.getGear().forEach((id, n) -> total.merge(id, n, (a, b) -> a == Holdings.UNKNOWN || b == Holdings.UNKNOWN ? Holdings.UNKNOWN : a + b));
		Set<Integer> ids = new HashSet<>();
		total.forEach((id, n) ->
		{
			Gear g = data.gear(id);
			if (g != null && Holdings.enough(g.getSlot() == Gear.AMMO, n))
			{
				ids.add(id);
			}
		});
		return ids;
	}

	/** The ammunition in Dizana's quiver, kept in varps wherever the quiver is. */
	private void readQuiver()
	{
		quiverId = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO);
		quiverAmount = Math.max(0, client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT));
	}

	private long coins()
	{
		return inventoryCoins + holdings.getCoins();
	}

	/** Settles which bank to plan with, this plugin's own or Bank Steward's. */
	private void choose()
	{
		holdings = Holdings.choose(bank == null ? null : bank.items, bank == null ? 0 : bank.coins, bankCounts, bankSeenAt,
			steward, id -> data.gear(id) != null);
	}

	/** Reads Codex Bank Steward's shared bank for this account; never writes it. Null when it is not installed. */
	private void readSteward()
	{
		steward = Holdings.readSteward(gson, configManager.getRSProfileConfiguration(Holdings.STEWARD_GROUP, Holdings.STEWARD_KEY));
		choose();
	}

	private void loadBank()
	{
		String key = configManager.getRSProfileKey();
		if (key == null)
		{
			return;
		}
		profile = key;
		loadGoal();
		readWorth();
		readChronicle();
		String json = configManager.getRSProfileConfiguration(CodexGearRoadmapConfig.GROUP, BANK_KEY);
		if (json != null && !json.isEmpty())
		{
			try
			{
				Bank saved = gson.fromJson(json, Bank.class);
				if (saved != null && saved.items != null)
				{
					bank = saved;
					bankCounts = null;
					bankSeenAt = seenAt();
				}
			}
			catch (JsonParseException e)
			{
				log.warn("Could not read the saved bank, waiting to see it again", e);
			}
		}
		readSteward();
	}

	/** When the saved bank was seen, or 0 for a bank saved before the time was kept. */
	private long seenAt()
	{
		String saved = configManager.getRSProfileConfiguration(CodexGearRoadmapConfig.GROUP, BANK_SEEN_KEY);
		try
		{
			return saved == null ? 0 : Long.parseLong(saved.trim());
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private void saveBank()
	{
		if (profile == null)
		{
			profile = configManager.getRSProfileKey();
		}
		if (profile != null && bank != null)
		{
			configManager.setConfiguration(CodexGearRoadmapConfig.GROUP, profile, BANK_KEY, gson.toJson(bank));
			configManager.setConfiguration(CodexGearRoadmapConfig.GROUP, profile, BANK_SEEN_KEY, Long.toString(bankSeenAt));
		}
	}

	/** Names the upgrades from the road the player just picked up. */
	private void checkClaims(Set<Integer> before, Set<Integer> after)
	{
		Roadmap map = lastRoadmap;
		if (map == null)
		{
			return;
		}
		List<Roadmap.Step> path = map.getPath();
		for (int i = 0; i < path.size(); i++)
		{
			Roadmap.Option option = path.get(i).getOption();
			if (claimed.contains(option.getName()) || !ownsAll(option.getPieces(), after) || ownsAll(option.getPieces(), before))
			{
				continue;
			}
			claimed.add(option.getName());
			announce(map, i, option);
		}
	}

	/** The player now owns every piece of their savings goal: say so, and the goal is done. */
	private void checkGoal(Set<Integer> before, Set<Integer> after)
	{
		if (goal == null)
		{
			return;
		}
		List<Gear> pieces = new ArrayList<>();
		for (int id : goal.getIds())
		{
			Gear g = data.gear(id);
			if (g == null)
			{
				return;
			}
			pieces.add(g);
		}
		if (!ownsAll(pieces, after) || ownsAll(pieces, before))
		{
			return;
		}
		// The same upgrade on the road is not claimed a second time.
		claimed.add(goal.getName());
		Gear icon = pieces.get(0);
		for (Gear g : pieces)
		{
			if (g.getSlot() == Gear.BODY)
			{
				icon = g;
			}
		}
		if (config.showPlaque())
		{
			plaque.show(new UpgradePlaque.View("GOAL MET", goal.getName(), "The upgrade you saved for is yours",
				"Pick your next goal on the Roadmap", Theme.GREEN, itemManager.getImage(icon.getId()), System.currentTimeMillis(),
				config.plaqueSeconds() * 1000L));
		}
		if (config.chatLine())
		{
			String line = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Gear Roadmap: ")
				.append(ChatColorType.NORMAL).append("savings goal met, ")
				.append(ChatColorType.HIGHLIGHT).append(goal.getName())
				.append(ChatColorType.NORMAL).append(" is yours.")
				.build();
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(line)
				.build());
		}
		setGoal(null);
	}

	private static boolean ownsAll(List<Gear> pieces, Set<Integer> ids)
	{
		for (Gear piece : pieces)
		{
			boolean has = false;
			for (int id : piece.getIds())
			{
				if (ids.contains(id))
				{
					has = true;
					break;
				}
			}
			if (!has)
			{
				return false;
			}
		}
		return true;
	}

	private void announce(Roadmap map, int index, Roadmap.Option option)
	{
		String target = map.getTarget().getName();
		String gain = Format.gain(option.getGain()) + " DPS against " + target;
		List<Roadmap.Step> path = map.getPath();
		String next = index + 1 < path.size()
			? "Next: " + path.get(index + 1).getOption().getName() + ", " + Format.coins(path.get(index + 1).getOption().getCost())
			: "That was the last stop on this road";
		if (config.showPlaque())
		{
			plaque.show(new UpgradePlaque.View("UPGRADE CLAIMED", option.getName(), gain, next, Theme.GOLD,
				itemManager.getImage(option.getIcon().getId()), System.currentTimeMillis(), config.plaqueSeconds() * 1000L));
		}
		if (config.chatLine())
		{
			String line = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Gear Roadmap: ")
				.append(ChatColorType.NORMAL).append(option.getName() + " adds ")
				.append(ChatColorType.HIGHLIGHT).append(Format.gain(option.getGain()) + " DPS")
				.append(ChatColorType.NORMAL).append(" against " + target + ".")
				.build();
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(line)
				.build());
		}
	}

	/**
	 * The player picked a target on the Targets tab. One of the slayer task's targets only moves
	 * the aim within the task; any other becomes the chosen target and stops following this task.
	 */
	private void aim(String name)
	{
		Report report = lastReport;
		String key = client.getGameState() == GameState.LOGGED_IN ? taskKey() : "";
		boolean onTaskTarget = report != null && report.getTaskTargets().contains(name);
		if (onTaskTarget && config.followTask())
		{
			taskPick = name;
			taskPickKey = key;
			taskDismissed = null;
			dirty = true;
			return;
		}
		if (!key.isEmpty() && report != null && !report.getTaskTargets().isEmpty())
		{
			taskDismissed = key;
		}
		if (name.equals(config.target()))
		{
			dirty = true;
		}
		configManager.setConfiguration(CodexGearRoadmapConfig.GROUP, CodexGearRoadmapConfig.TARGET, name);
	}

	/** Saves for an upgrade, or clears the goal when given null. */
	private void setGoal(Roadmap.Option option)
	{
		if (option == null)
		{
			goal = null;
		}
		else
		{
			List<Integer> ids = new ArrayList<>();
			for (Gear piece : option.getPieces())
			{
				ids.add(piece.getIds()[0]);
			}
			goal = new Savings.Goal(option.getName(), ids);
		}
		goalAffordable = null;
		goalAnnounced = false;
		saveGoal();
		dirty = true;
	}

	private void loadGoal()
	{
		goal = null;
		goalAffordable = null;
		goalAnnounced = false;
		String json = configManager.getRSProfileConfiguration(CodexGearRoadmapConfig.GROUP, GOAL_KEY);
		if (json == null || json.isEmpty())
		{
			return;
		}
		try
		{
			Savings.Goal saved = gson.fromJson(json, Savings.Goal.class);
			goal = saved != null && saved.isValid() ? saved : null;
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read the saved savings goal", e);
		}
	}

	private void saveGoal()
	{
		if (profile == null)
		{
			return;
		}
		if (goal == null)
		{
			configManager.unsetConfiguration(CodexGearRoadmapConfig.GROUP, profile, GOAL_KEY);
		}
		else
		{
			configManager.setConfiguration(CodexGearRoadmapConfig.GROUP, profile, GOAL_KEY, gson.toJson(goal));
		}
	}

	/** Reads Codex Ledger's saved net worth for this account; never writes it. Null when it is not installed. */
	private void readWorth()
	{
		String json = configManager.getRSProfileConfiguration(LEDGER_GROUP, LEDGER_KEY);
		if (json == null || json.isEmpty())
		{
			worth = null;
			return;
		}
		try
		{
			Savings.Worth read = gson.fromJson(json, Savings.Worth.class);
			worth = read == null || read.getTotal() <= 0 ? null : read;
		}
		catch (RuntimeException e)
		{
			// Another plugin's format may change under us; without it the goal simply has no pace.
			log.debug("Could not read Codex Ledger's net worth", e);
			worth = null;
		}
	}

	/** Reads Codex Session Chronicle's journal for this account; never writes it. Empty when it is not installed. */
	private void readChronicle()
	{
		chronicle = readLogged(CHRONICLE_JOURNAL, true);
	}

	/** One of Chronicle's saved values as sessions: the journal is a list, the live session one. */
	private List<Fights.Logged> readLogged(String key, boolean list)
	{
		String json = configManager.getRSProfileConfiguration(CHRONICLE_GROUP, key);
		if (json == null || json.isEmpty())
		{
			return Collections.emptyList();
		}
		try
		{
			if (list)
			{
				List<Fights.Logged> read = gson.fromJson(json, LOGGED_LIST);
				return read == null ? Collections.emptyList() : read;
			}
			Fights.Logged one = gson.fromJson(json, Fights.Logged.class);
			return one == null ? Collections.emptyList() : Collections.singletonList(one);
		}
		catch (RuntimeException e)
		{
			// Another plugin's format may change under us; without it there is simply no "where you fight".
			log.debug("Could not read Codex Session Chronicle's journal", e);
			return Collections.emptyList();
		}
	}

	/** Adds the savings goal's standing to a report, and says so once the player's coins reach it. */
	private Report dress(Report report)
	{
		report = report.withSwap(swap(report.getRoadmap()));
		Roadmap map = report.getRoadmap();
		if (map == null || goal == null || report.isIronman() || map.getGoal() == null)
		{
			return report.withSavings(null);
		}
		Savings.View view = Savings.of(map.getGoal(), map.isGoalOwned(), coins(), worth, System.currentTimeMillis());
		boolean affordable = view.isAffordable();
		if (Savings.inReach(goalAffordable, goalAnnounced, view))
		{
			goalAnnounced = true;
			announceGoal(view, map.getTarget().getName());
		}
		goalAffordable = affordable;
		return report.withSavings(view);
	}

	/** What is worn now against the best loadout owned for the road's target; one damage sum, so cheap enough for the client thread. */
	private Swap swap(Roadmap map)
	{
		if (map == null || map.getNow() == null)
		{
			return null;
		}
		return Swap.of(worn.keySet(), inventory.keySet(), data::gear, map.getNow().best(), advisor.planner().calc(), fighter(),
			map.getTarget());
	}

	private void announceGoal(Savings.View view, String target)
	{
		Roadmap.Option option = view.getOption();
		String gain = option.getGain() > 0 ? Format.gain(option.getGain()) + " DPS against " + target : "Saved for in Gear Roadmap";
		if (config.showPlaque())
		{
			plaque.show(new UpgradePlaque.View("GOAL IN REACH", option.getName(), gain,
				"Your coins now cover its " + Format.coins(option.getCost()), Theme.TEAL,
				itemManager.getImage(option.getIcon().getId()), System.currentTimeMillis(), config.plaqueSeconds() * 1000L));
		}
		if (config.chatLine())
		{
			String line = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Gear Roadmap: ")
				.append(ChatColorType.NORMAL).append("your coins now cover your goal, ")
				.append(ChatColorType.HIGHLIGHT).append(option.getName())
				.append(ChatColorType.NORMAL).append(" (" + Format.coins(option.getCost()) + ").")
				.build();
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(line)
				.build());
		}
	}

	private Fighter fighter()
	{
		Map<String, Integer> levels = new HashMap<>();
		for (Skill skill : SKILLS)
		{
			levels.put(skill.getName(), client.getRealSkillLevel(skill));
		}
		if (ancients == null)
		{
			ancients = Quest.DESERT_TREASURE_I.getState(client) == QuestState.FINISHED;
		}
		boolean piety = client.getVarbitValue(VarbitID.KR_KNIGHTWAVES_STATE) >= KNIGHT_WAVES_DONE;
		boolean rigour = client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) == 1;
		boolean augury = client.getVarbitValue(VarbitID.PRAYER_AUGURY_UNLOCKED) == 1;
		boolean deadeye = client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) == 1;
		boolean vigour = client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) == 1;
		boolean kandarin = client.getVarbitValue(VarbitID.KANDARIN_DIARY_HARD_COMPLETE) == 1;
		taskName = currentTask();
		SlayerTask task = SlayerTask.of(config.slayerTask(), taskName);
		return new Fighter(levels, config.boost(), config.prayers(), piety, rigour, augury, ancients, task)
			.withUnlocks(deadeye, vigour, kandarin);
	}

	/** The name of the player's current slayer task, as the slayer helper shows it, or null. */
	private String currentTask()
	{
		if (client.getVarpValue(VarPlayerID.SLAYER_COUNT) <= 0)
		{
			return null;
		}
		try
		{
			int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
			int row;
			if (taskId == BOSS_TASK)
			{
				List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
					DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
				if (rows.isEmpty())
				{
					return null;
				}
				row = (Integer) client.getDBTableField(rows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
			}
			else
			{
				List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
				if (rows.isEmpty())
				{
					return null;
				}
				row = rows.get(0);
			}
			return (String) client.getDBTableField(row, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];
		}
		catch (RuntimeException e)
		{
			log.debug("Could not read the slayer task", e);
			return null;
		}
	}

	private void analyse()
	{
		Set<Integer> ids = ownedIds();
		Set<Gear> owned = new LinkedHashSet<>();
		for (int id : ids)
		{
			Gear g = data.gear(id);
			if (g != null)
			{
				owned.add(g);
			}
		}
		Fighter fighter = fighter();
		Map<Integer, Long> prices = new HashMap<>();
		for (int id : priceIds)
		{
			long price = itemManager.getItemPrice(id);
			if (price > 0)
			{
				prices.put(id, price);
			}
		}
		// Owned pieces too, though no upgrade buys them, so outgrown gear has a price to sell at. Only
		// tradeable ones can be sold: an untradeable piece is priced by what it was made from.
		Set<Integer> sellable = new HashSet<>();
		for (Gear g : owned)
		{
			if (itemManager.getItemComposition(g.getId()).isTradeable())
			{
				sellable.add(g.getId());
			}
			if (!prices.containsKey(g.getId()))
			{
				long price = itemManager.getItemPrice(g.getId());
				if (price > 0)
				{
					prices.put(g.getId(), price);
				}
			}
		}
		String key = taskKey();
		boolean follow = config.followTask() && !key.equals(taskDismissed);
		boolean bossTask = !key.isEmpty() && client.getVarpValue(VarPlayerID.SLAYER_TARGET) == BOSS_TASK;
		Aim aimed = Aim.choose(data.targets(), config.target(), follow, taskName, bossTask,
			key.equals(taskPickKey) ? taskPick : null);
		Target target = aimed.getTarget();
		List<String> taskTargets = new ArrayList<>();
		for (Target t : aimed.getCovered())
		{
			taskTargets.add(t.getName());
		}
		boolean following = aimed.isFollowing();
		String shownTask = taskName;
		boolean ironman = client.getVarbitValue(VarbitID.IRONMAN) > 0;
		// Ironmen cannot buy, so their coins never put an upgrade in reach.
		long coins = ironman ? 0 : coins();
		boolean bankSeen = holdings.isKnown();
		boolean seasonal = client.getWorldType().contains(WorldType.SEASONAL);
		String boosts = config.boost().toString();
		String task = fighter.getTask().getTask();
		Boolean slayer = target.isSlayer() ? fighter.onTaskFor(target) : null;
		String surveyKey = new TreeSet<>(ids) + "|" + fighter.getLevels() + "|" + fighter.assumptions() + unlocks();
		Player player = client.getLocalPlayer();
		String name = player == null ? null : player.getName();
		Target aim = target;
		// Ironmen cannot buy, so they have no savings goal.
		Savings.Goal saving = ironman ? null : goal;
		List<Fights.Logged> sessions = new ArrayList<>(chronicle);
		sessions.addAll(readLogged(CHRONICLE_CURRENT, false));
		Fights.Tally tally = sessions.isEmpty() ? null : Fights.tally(sessions, data.targets(), System.currentTimeMillis());
		int ticket = generation.incrementAndGet();
		executor.execute(() ->
		{
			try
			{
				Roadmap map = advisor.plan(owned, fighter, aim, id -> prices.getOrDefault(id, 0L), coins, saving);
				SurveyCache cached = survey;
				if (cached == null || !cached.key.equals(surveyKey))
				{
					cached = new SurveyCache(surveyKey, advisor.survey(owned, fighter));
				}
				if (ticket == generation.get())
				{
					survey = cached;
					lastRoadmap = map;
					// Ironmen cannot buy, so there is nothing to weigh for them.
					List<Fights.Pick> fights = ironman ? Collections.emptyList()
						: advisor.fights(owned, fighter, tally, id -> prices.getOrDefault(id, 0L));
					Report planned = new Report(true, map, cached.rows, boosts, owned.size(), bankSeen)
						.withAccount(ironman, seasonal, task, slayer)
						.withTask(following, taskTargets, shownTask)
						.withExtras(tally, fights, Levels.ahead(advisor.planner().calc(), map, fighter))
						// Ironmen cannot sell, so nothing is outgrown for coins.
						.withOutgrown(ironman ? null : Outgrown.of(owned, cached.rows, g -> advisor.planner().wieldable(g, fighter),
							g -> sellable.contains(g.getId()), id -> prices.getOrDefault(id, 0L)));
					// The goal's standing is settled on the client thread, where the goal and coins live.
					clientThread.invokeLater(() ->
					{
						if (ticket != generation.get())
						{
							return;
						}
						Report report = dress(planned);
						lastReport = report;
						lastName = name;
						publishedTicket = ticket;
						publish(report, name);
					});
				}
			}
			catch (RuntimeException e)
			{
				log.warn("Gear Roadmap planning failed", e);
			}
		});
	}

	private String unlocks()
	{
		return client.getVarbitValue(VarbitID.KR_KNIGHTWAVES_STATE) + "/" + client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED)
			+ "/" + client.getVarbitValue(VarbitID.PRAYER_AUGURY_UNLOCKED);
	}

	private void publish(Report report, String name)
	{
		GearRoadmapPanel target = panel;
		if (target != null)
		{
			SwingUtilities.invokeLater(() -> target.update(report, name));
		}
	}

	/** Asks where to save the road card with RuneLite's own file chooser, then writes it there; runs on the Swing thread. */
	private void saveCard(BufferedImage card, String fileName)
	{
		GearRoadmapPanel view = panel;
		List<Filepath> chosen = new Filepath.Chooser()
			.setIsSave()
			.setAcceptsFiles()
			.setDialogTitle("Save road card")
			.addExtensionFilter("PNG image", "png")
			.setDefaultExtension("png")
			.setFileName(fileName)
			.showDialog(view);
		if (chosen == null || chosen.isEmpty())
		{
			return;
		}
		Filepath target = chosen.get(0);
		executor.execute(() ->
		{
			String note;
			try (OutputStream out = target.openOutputStream())
			{
				ImageIO.write(card, "png", out);
				note = "Saved as " + target.getFileName() + ".";
			}
			catch (IOException e)
			{
				log.warn("Could not save the road card", e);
				note = "The card could not be saved: " + e.getMessage();
			}
			String shown = note;
			GearRoadmapPanel p = panel;
			if (p != null)
			{
				SwingUtilities.invokeLater(() -> p.shareNote(shown));
			}
		});
	}

	private void copyCard(BufferedImage card)
	{
		String note;
		try
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransfer(ImageTransfer.opaque(card)), null);
			note = "Copied. Paste it anywhere you share pictures.";
		}
		catch (IllegalStateException e)
		{
			note = "The clipboard is busy, try again in a moment.";
		}
		GearRoadmapPanel p = panel;
		if (p != null)
		{
			p.shareNote(note);
		}
	}
}
