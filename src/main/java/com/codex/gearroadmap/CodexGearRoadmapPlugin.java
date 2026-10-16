package com.codex.gearroadmap;

import com.codex.gearroadmap.core.Advisor;
import com.codex.gearroadmap.core.Fighter;
import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.codex.gearroadmap.overlay.UpgradePlaque;
import com.codex.gearroadmap.ui.Format;
import com.codex.gearroadmap.ui.GearRoadmapPanel;
import com.codex.gearroadmap.ui.ItemIcons;
import com.codex.gearroadmap.ui.Sigil;
import com.codex.gearroadmap.ui.Theme;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.io.IOException;
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
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
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
	private static final String BANK_KEY = "bank";
	private static final Skill[] SKILLS = {
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.PRAYER, Skill.HITPOINTS, Skill.AGILITY,
	};
	private static final Set<Integer> UNLOCK_VARBITS = new HashSet<>(Arrays.asList(
		VarbitID.PRAYER_RIGOUR_UNLOCKED, VarbitID.PRAYER_AUGURY_UNLOCKED, VarbitID.KR_KNIGHTWAVES_STATE));
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
	private GearRoadmapPanel panel;
	private NavigationButton navButton;

	private String profile;
	private Set<Integer> worn = Collections.emptySet();
	private Set<Integer> inventory = Collections.emptySet();
	private Bank bank;
	private long inventoryCoins;
	private Boolean ancients;
	private boolean dirty;
	private int ticksSinceAnalysis;

	/** What the latest roadmap was, so a claimed upgrade can be named; written on the executor. */
	private volatile Roadmap lastRoadmap;
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
		data = RoadmapData.load(gson);
		advisor = new Advisor(data);
		priceIds = advisor.priceIds();
		ItemIcons icons = (itemId, onLoaded) ->
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			image.onLoaded(onLoaded);
			return image;
		};
		panel = new GearRoadmapPanel(icons, name -> configManager.setConfiguration(CodexGearRoadmapConfig.GROUP,
			CodexGearRoadmapConfig.TARGET, name));
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
				dirty = true;
			}
		});
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
		worn = Collections.emptySet();
		inventory = Collections.emptySet();
		inventoryCoins = 0;
		ancients = null;
		lastRoadmap = null;
		survey = null;
		claimed.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			forget();
			plaque.clear();
			generation.incrementAndGet();
			publish(Report.loggedOut(), null);
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		bank = null;
		profile = null;
		ancients = null;
		survey = null;
		claimed.clear();
		loadBank();
		dirty = true;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CodexGearRoadmapConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if (!config.showPlaque())
		{
			plaque.clear();
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
		Set<Integer> before = ownedIds();
		long coinsBefore = coins();
		readContainer(id);
		Set<Integer> after = ownedIds();
		if (!after.equals(before) || coins() != coinsBefore)
		{
			dirty = true;
		}
		if (bank != null && !after.equals(before))
		{
			checkClaims(before, after);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		for (Skill skill : SKILLS)
		{
			if (skill == event.getSkill())
			{
				dirty = true;
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
		ticksSinceAnalysis++;
		if (dirty && ticksSinceAnalysis >= ANALYSE_GAP_TICKS)
		{
			dirty = false;
			ticksSinceAnalysis = 0;
			analyse();
		}
	}

	private void readContainer(int id)
	{
		ItemContainer container = client.getItemContainer(id);
		if (container == null)
		{
			return;
		}
		Set<Integer> items = new TreeSet<>();
		for (Item item : container.getItems())
		{
			if (item.getId() > 0 && item.getQuantity() > 0 && data.gear(item.getId()) != null)
			{
				items.add(item.getId());
			}
		}
		if (id == InventoryID.WORN)
		{
			worn = items;
		}
		else if (id == InventoryID.INV)
		{
			inventory = items;
			inventoryCoins = container.count(ItemID.COINS);
		}
		else
		{
			Bank seen = new Bank();
			seen.items = items;
			seen.coins = container.count(ItemID.COINS);
			bank = seen;
			saveBank();
		}
	}

	private Set<Integer> ownedIds()
	{
		Set<Integer> ids = new HashSet<>(worn);
		ids.addAll(inventory);
		if (bank != null)
		{
			ids.addAll(bank.items);
		}
		return ids;
	}

	private long coins()
	{
		return inventoryCoins + (bank == null ? 0 : bank.coins);
	}

	private void loadBank()
	{
		String key = configManager.getRSProfileKey();
		if (key == null)
		{
			return;
		}
		profile = key;
		String json = configManager.getRSProfileConfiguration(CodexGearRoadmapConfig.GROUP, BANK_KEY);
		if (json == null || json.isEmpty())
		{
			return;
		}
		try
		{
			Bank saved = gson.fromJson(json, Bank.class);
			if (saved != null && saved.items != null)
			{
				bank = saved;
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read the saved bank, waiting to see it again", e);
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
		return new Fighter(levels, config.boost(), config.prayers(), piety, rigour, augury, ancients, config.onTask());
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
		Target target = data.target(config.target());
		if (target == null)
		{
			target = data.targets().get(0);
		}
		long coins = coins();
		boolean bankSeen = bank != null;
		String boosts = config.boost().toString();
		String surveyKey = new TreeSet<>(ids) + "|" + fighter.getLevels() + "|" + config.boost() + config.prayers()
			+ config.onTask() + fighter.isAncients() + unlocks();
		Player player = client.getLocalPlayer();
		String name = player == null ? null : player.getName();
		Target aim = target;
		int ticket = generation.incrementAndGet();
		executor.execute(() ->
		{
			try
			{
				Roadmap map = advisor.plan(owned, fighter, aim, id -> prices.getOrDefault(id, 0L), coins);
				SurveyCache cached = survey;
				if (cached == null || !cached.key.equals(surveyKey))
				{
					cached = new SurveyCache(surveyKey, advisor.survey(owned, fighter));
				}
				if (ticket == generation.get())
				{
					survey = cached;
					lastRoadmap = map;
					publish(new Report(true, map, cached.rows, boosts, owned.size(), bankSeen), name);
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
}
