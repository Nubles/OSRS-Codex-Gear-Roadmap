package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.CatalogueEntry;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Weighs every upgrade in the catalogue against the player's best loadout for one target,
 * then walks the road ahead: the upgrade with the most damage per coin first, then the
 * next one from there, and so on. Earned upgrades and ones the player cannot wield yet
 * are listed on their own.
 */
public final class Advisor
{
	public static final int STEPS = 8;
	public static final int AMMO_STACK = 1000;
	/** Kills of each shortlisted target an upgrade is weighed over. */
	public static final int SHORTLIST_KILLS = 100;
	private static final int SIDE_LIST = 4;
	/** How many of the cheapest do-nothing-alone pieces the road tries in pairs. */
	private static final int PAIR_POOL = 10;

	/** Grand Exchange prices: 0 when an item has none. */
	public interface Prices
	{
		long price(int itemId);
	}

	private static final String[][] BUNDLES = {
		{"Void melee set", "Pest Control", "void melee helm", "void knight top", "void knight robe", "void knight gloves"},
		{"Void ranger set", "Pest Control", "void ranger helm", "void knight top", "void knight robe", "void knight gloves"},
		{"Void mage set", "Pest Control", "void mage helm", "void knight top", "void knight robe", "void knight gloves"},
		{"Elite void ranger set", "Pest Control and the Western Provinces hard diary", "void ranger helm", "elite void top", "elite void robe", "void knight gloves"},
		{"Elite void mage set", "Pest Control and the Western Provinces hard diary", "void mage helm", "elite void top", "elite void robe", "void knight gloves"},
		{"Obsidian armour", null, "obsidian helmet", "obsidian platebody", "obsidian platelegs"},
	};

	/** Earned upgrades that are pointless once a better form of the same item is owned. */
	private static final String[][] SUPERSEDED = {
		{"salve amulet", "salve amulet(i)", "salve amulet (e)", "salve amulet(ei)"},
		{"salve amulet(i)", "salve amulet (e)", "salve amulet(ei)"},
		{"salve amulet (e)", "salve amulet(ei)"},
		{"black mask", "black mask (i)", "slayer helmet", "slayer helmet (i)"},
		{"black mask (i)", "slayer helmet (i)"},
		{"slayer helmet", "slayer helmet (i)"},
		{"fire cape", "infernal cape"},
		{"ava's accumulator", "ava's assembler", "dizana's quiver", "blessed dizana's quiver"},
		{"saradomin cape", "imbued saradomin cape"},
	};

	private final RoadmapData data;
	private final Planner planner;

	public Advisor(RoadmapData data)
	{
		this.data = data;
		this.planner = new Planner(data);
	}

	public Planner planner()
	{
		return planner;
	}

	/** The roadmap for one target. */
	public Roadmap plan(Collection<Gear> ownedGear, Fighter fighter, Target target, Prices prices, long coins)
	{
		return plan(ownedGear, fighter, target, prices, coins, null);
	}

	/** The roadmap for one target, with the player's savings goal priced and weighed against it. */
	public Roadmap plan(Collection<Gear> ownedGear, Fighter fighter, Target target, Prices prices, long coins, Savings.Goal goal)
	{
		Set<Gear> owned = new LinkedHashSet<>(ownedGear);
		Planner.Best start = planner.optimise(owned, fighter, target);
		double base = start.dps();
		List<Candidate> candidates = candidates(owned);

		List<Roadmap.Option> free = new ArrayList<>();
		List<Roadmap.Option> locked = new ArrayList<>();
		List<Roadmap.Option> bought = new ArrayList<>();
		Map<Roadmap.Option, Integer> shortBy = new HashMap<>();
		for (Candidate c : candidates)
		{
			long cost = cost(c, owned, prices);
			if (cost < 0)
			{
				continue;
			}
			Planner.Plan after = planner.withAdded(start, owned, c.pieces, fighter, target);
			double gain = after.dps() - base;
			if (gain < threshold(base))
			{
				continue;
			}
			String missing = fighter.missing(c.requirements);
			Roadmap.Option option = new Roadmap.Option(c.name, c.pieces, c.icon(), cost, gain, base, after, missing,
				c.earn, cost == 0 && c.earn == null ? imbueNote(c) : c.note, c.ammo);
			if (missing != null)
			{
				locked.add(option);
				shortBy.put(option, fighter.shortfall(c.requirements));
			}
			else if (cost == 0)
			{
				free.add(option);
			}
			else
			{
				bought.add(option);
			}
		}
		Comparator<Roadmap.Option> byGain = Comparator.comparingDouble((Roadmap.Option o) -> -o.getGain());
		free.sort(byGain);
		// One reward of the same task in one slot is enough: the dragon gloves need not stand
		// beside the barrows gloves that the same quest gives later.
		Set<String> rewards = new HashSet<>();
		free.removeIf(o -> o.getEarn() != null && !rewards.add(o.getEarn() + "/" + o.getIcon().getSlot()));
		// What a few more levels unlock comes before a dream forty levels away.
		locked.sort(Comparator.comparingDouble((Roadmap.Option o) -> -o.getGain() / (1 + shortBy.get(o) / 10.0)));

		List<Roadmap.Step> path = walk(owned, fighter, target, prices, coins, start, candidates);
		Set<String> onPath = new HashSet<>();
		for (Roadmap.Step step : path)
		{
			onPath.add(step.getOption().getName());
		}
		List<Roadmap.Option> leaps = new ArrayList<>();
		bought.sort(byGain);
		for (Roadmap.Option o : bought)
		{
			if (!onPath.contains(o.getName()) && leaps.size() < SIDE_LIST)
			{
				leaps.add(o);
			}
		}
		Roadmap map = new Roadmap(target, start, path, cut(free, SIDE_LIST + 2), cut(locked, SIDE_LIST), leaps, coins);
		return goal == null ? map : map.withGoal(goal(owned, fighter, target, prices, start, goal), owns(owned, goal));
	}

	/**
	 * A savings goal as it stands against this target: what is still to buy and the damage it
	 * adds to the loadout owned now, which may be nothing here. Null when a piece is unknown or
	 * has no price at the moment.
	 */
	Roadmap.Option goal(Set<Gear> owned, Fighter fighter, Target target, Prices prices, Planner.Best start, Savings.Goal goal)
	{
		if (goal == null || !goal.isValid())
		{
			return null;
		}
		List<Gear> pieces = new ArrayList<>();
		Map<String, Integer> req = new HashMap<>();
		String note = null;
		for (int id : goal.getIds())
		{
			Gear g = data.gear(id);
			if (g == null)
			{
				return null;
			}
			if (!pieces.contains(g))
			{
				pieces.add(g);
			}
			CatalogueEntry entry = data.entry(g);
			if (entry != null)
			{
				entry.getRequirements().forEach((k, v) -> req.merge(k, v, Math::max));
				note = pieces.size() == 1 ? entry.getNote() : null;
			}
		}
		Candidate c = new Candidate(goal.getName(), pieces, req, null, note, pieces.size() == 1 && pieces.get(0).getSlot() == Gear.AMMO);
		long cost = cost(c, owned, prices);
		if (cost < 0)
		{
			return null;
		}
		Planner.Plan after = planner.withAdded(start, owned, pieces, fighter, target);
		double gain = Math.max(0, after.dps() - start.dps());
		return new Roadmap.Option(c.name, pieces, c.icon(), cost, gain, start.dps(), gain > 0 ? after : start.best(),
			fighter.missing(req), null, note, c.ammo);
	}

	/** Whether every piece of a goal is owned already, any variant of it counting. */
	boolean owns(Set<Gear> owned, Savings.Goal goal)
	{
		if (goal == null || !goal.isValid())
		{
			return false;
		}
		for (int id : goal.getIds())
		{
			Gear g = data.gear(id);
			if (g == null || !owned.contains(g))
			{
				return false;
			}
		}
		return true;
	}

	/** Greedy road: the best damage per coin, then again from the gear that leaves. */
	private List<Roadmap.Step> walk(Set<Gear> owned, Fighter fighter, Target target, Prices prices, long coins,
		Planner.Best start, List<Candidate> candidates)
	{
		Set<Gear> have = new LinkedHashSet<>(owned);
		Planner.Best current = start;
		List<Roadmap.Step> path = new ArrayList<>();
		long spent = 0;
		Set<Candidate> used = new HashSet<>();
		for (int step = 0; step < STEPS; step++)
		{
			double base = current.dps();
			Candidate pick = null;
			Planner.Plan pickPlan = null;
			long pickCost = 0;
			double pickRatio = 0;
			List<Candidate> pickParts = Collections.emptyList();
			List<Candidate> flat = new ArrayList<>();
			Map<Candidate, Long> flatCost = new HashMap<>();
			for (Candidate c : candidates)
			{
				if (used.contains(c) || have.containsAll(c.pieces) || fighter.missing(c.requirements) != null)
				{
					continue;
				}
				long cost = cost(c, have, prices);
				if (cost <= 0)
				{
					continue;
				}
				Planner.Plan after = planner.withAdded(current, have, c.pieces, fighter, target);
				double gain = after.dps() - base;
				if (gain < threshold(base))
				{
					if (c.pieces.size() == 1 && !c.ammo && morePower(c.pieces.get(0), current.best()))
					{
						flat.add(c);
						flatCost.put(c, cost);
					}
					continue;
				}
				double ratio = gain / cost;
				if (ratio > pickRatio)
				{
					pick = c;
					pickPlan = after;
					pickCost = cost;
					pickRatio = ratio;
				}
			}
			// A piece that adds strength or accuracy short of the next max hit does nothing alone,
			// yet two of them together can cross it: try the cheapest such pieces in pairs.
			flat.sort(Comparator.comparingLong(flatCost::get));
			int pool = Math.min(flat.size(), PAIR_POOL);
			for (int i = 0; i < pool; i++)
			{
				for (int j = i + 1; j < pool; j++)
				{
					Candidate a = flat.get(i);
					Candidate b = flat.get(j);
					if (a.pieces.get(0).getSlot() == b.pieces.get(0).getSlot())
					{
						continue;
					}
					long cost = flatCost.get(a) + flatCost.get(b);
					List<Gear> both = new ArrayList<>(a.pieces);
					both.addAll(b.pieces);
					Planner.Plan after = planner.withAdded(current, have, both, fighter, target);
					double gain = after.dps() - base;
					if (gain >= threshold(base) && gain / cost > pickRatio)
					{
						pick = new Candidate(a.name + " + " + b.name, both, a.requirements, null, null, false);
						pickPlan = after;
						pickCost = cost;
						pickRatio = gain / cost;
						pickParts = Arrays.asList(a, b);
					}
				}
			}
			if (pick == null)
			{
				break;
			}
			used.add(pick);
			used.addAll(pickParts);
			have.addAll(pick.pieces);
			Planner.Best next = planner.optimise(have, fighter, target);
			// The fresh search is never worse than the quick one; keep whichever found more.
			Planner.Plan plan = next.dps() >= pickPlan.dps() ? next.best() : pickPlan;
			spent += pickCost;
			Roadmap.Option option = new Roadmap.Option(pick.name, pick.pieces, pick.icon(), pickCost, plan.dps() - base, base, plan,
				null, null, pick.note, pick.ammo);
			path.add(new Roadmap.Step(option, spent, spent <= coins));
			// Build the next step on the loadout this one showed, or its gain is counted twice.
			current = next.keeping(plan);
		}
		return path;
	}

	/** Every item id the roadmap may price, so the plugin can look them up on the client thread. */
	public Set<Integer> priceIds()
	{
		Set<Integer> ids = new HashSet<>();
		for (Candidate c : candidates(Collections.emptySet()))
		{
			for (Gear piece : c.pieces)
			{
				for (int id : piece.getIds())
				{
					ids.add(id);
				}
				CatalogueEntry entry = data.entry(piece);
				if (entry != null && entry.getBuild() != null)
				{
					for (int id : entry.getBuild())
					{
						ids.add(id);
					}
				}
			}
		}
		return ids;
	}

	/**
	 * Upgrades weighed across where the player really fights: the fighting time each would have saved
	 * over the tally's days, at the targets that took the most of it. Ranked by time saved per coin,
	 * like the road; only upgrades the player can wear now and can buy are weighed.
	 */
	public List<Fights.Pick> fights(Collection<Gear> ownedGear, Fighter fighter, Fights.Tally tally, Prices prices)
	{
		if (tally == null || tally.isEmpty())
		{
			return Collections.emptyList();
		}
		Set<Gear> owned = new LinkedHashSet<>(ownedGear);
		List<Start> starts = new ArrayList<>();
		int targets = 0;
		for (Map.Entry<Target, Integer> e : tally.getKills().entrySet())
		{
			if (targets >= Fights.TARGETS)
			{
				break;
			}
			targets++;
			Target t = e.getKey();
			int all = e.getValue();
			// Each kill is weighed as it was fought: on a task with the slayer helmet's bonus, off one without.
			int helmet = helmetKills(fighter.getTask(), tally, t, all);
			addStart(starts, owned, fighter.withTask(SlayerTask.ALWAYS), t, helmet);
			addStart(starts, owned, fighter.withTask(SlayerTask.NEVER), t, all - helmet);
		}
		return weigh(owned, fighter, starts, prices, Fights.MIN_SECONDS * tally.getDays() / Fights.DAYS);
	}

	/**
	 * Upgrades weighed across a shortlist the player chose, such as the bosses left to finish: the
	 * fighting time each would save over {@link #SHORTLIST_KILLS} kills of every listed target,
	 * ranked by time saved per coin. A target the player cannot hurt yet is left out.
	 */
	public List<Fights.Pick> shortlist(Collection<Gear> ownedGear, Fighter fighter, List<Target> targets, Prices prices)
	{
		if (targets == null || targets.isEmpty())
		{
			return Collections.emptyList();
		}
		Set<Gear> owned = new LinkedHashSet<>(ownedGear);
		List<Start> starts = new ArrayList<>();
		for (Target t : targets)
		{
			addStart(starts, owned, fighter, t, SHORTLIST_KILLS);
		}
		return weigh(owned, fighter, starts, prices, 0);
	}

	/** Every upgrade the player can wear and buy now, by the fighting time it saves over the starts per coin. */
	private List<Fights.Pick> weigh(Set<Gear> owned, Fighter fighter, List<Start> starts, Prices prices, double minSeconds)
	{
		if (starts.isEmpty())
		{
			return Collections.emptyList();
		}
		List<Fights.Pick> picks = new ArrayList<>();
		Map<Fights.Pick, Double> value = new HashMap<>();
		for (Candidate c : candidates(owned))
		{
			if (c.earn != null || fighter.missing(c.requirements) != null)
			{
				continue;
			}
			long cost = cost(c, owned, prices);
			if (cost <= 0)
			{
				continue;
			}
			double total = 0;
			Map<String, Double> where = new HashMap<>();
			for (Start start : starts)
			{
				Target t = start.target;
				Planner.Plan after = planner.withAdded(start.best, owned, c.pieces, start.fighter, t);
				if (after.dps() <= start.best.dps() + 1e-9)
				{
					continue;
				}
				double saved = Fights.seconds(start.kills, t, start.best.dps()) - Fights.seconds(start.kills, t, after.dps());
				total += saved;
				where.merge(t.getName(), saved, Double::sum);
			}
			if (total <= 0 || total < minSeconds)
			{
				continue;
			}
			List<String> names = new ArrayList<>(where.keySet());
			names.sort(Comparator.comparingDouble((String n) -> -where.get(n)));
			Fights.Pick pick = new Fights.Pick(c.name, c.icon(), cost, total, names);
			picks.add(pick);
			value.put(pick, total / cost);
		}
		picks.sort(Comparator.comparingDouble((Fights.Pick p) -> -value.get(p)));
		return cut(picks, Fights.PICKS);
	}

	/**
	 * How many of a target's kills to weigh with the slayer helmet's bonus. With the setting on
	 * always or never, all or none. With it following the task, the kills Chronicle saw made on a
	 * task that covers the target, plus, for kills from before it kept the task, those a Slayer
	 * creature (but not a boss) would most likely have been: today's task says nothing of them.
	 */
	static int helmetKills(SlayerTask setting, Fights.Tally tally, Target t, int all)
	{
		if (setting.getMode() != SlayerTask.Mode.SMART)
		{
			return setting.covers(t) ? all : 0;
		}
		if (!t.isSlayer())
		{
			return 0;
		}
		int unknown = SlayerTask.of(SlayerTask.Mode.SMART, null).covers(t) ? tally.unknown(t) : 0;
		return Math.min(all, tally.onTask(t) + unknown);
	}

	private void addStart(List<Start> starts, Set<Gear> owned, Fighter fighter, Target t, int kills)
	{
		if (kills <= 0)
		{
			return;
		}
		Planner.Best best = planner.optimise(owned, fighter, t);
		if (best.dps() > 0)
		{
			starts.add(new Start(t, fighter, best, kills));
		}
	}

	/** Where a fought target's kills start from, for one slayer helmet assumption. */
	private static final class Start
	{
		final Target target;
		final Fighter fighter;
		final Planner.Best best;
		final int kills;

		Start(Target target, Fighter fighter, Planner.Best best, int kills)
		{
			this.target = target;
			this.fighter = fighter;
			this.best = best;
			this.kills = kills;
		}
	}

	/** Every target's best loadout, for the Targets tab. */
	public List<Roadmap.Survey> survey(Collection<Gear> owned, Fighter fighter)
	{
		List<Roadmap.Survey> rows = new ArrayList<>();
		for (Target t : data.targets())
		{
			Planner.Best best = planner.optimise(owned, fighter, t);
			rows.add(new Roadmap.Survey(t, best));
		}
		return rows;
	}

	/** Whether a piece has more damage than what the plan wears in its slot, for the plan's own style. */
	static boolean morePower(Gear g, Planner.Plan plan)
	{
		Result.Kind kind = plan.getResult().getKind();
		if (kind == null || g.getSlot() == Gear.WEAPON)
		{
			return false;
		}
		Gear worn = plan.getLoadout().get(g.getSlot());
		return power(g, kind) > (worn == null ? 0 : power(worn, kind));
	}

	private static int power(Gear g, Result.Kind kind)
	{
		switch (kind)
		{
			case RANGED:
				return g.rangedStrength();
			case MAGIC:
				return g.magicDamage();
			default:
				return g.strength();
		}
	}

	private static double threshold(double base)
	{
		return Math.max(0.02, base * 0.004);
	}

	private static <T> List<T> cut(List<T> list, int size)
	{
		return list.size() <= size ? list : new ArrayList<>(list.subList(0, size));
	}

	private static String imbueNote(Candidate c)
	{
		return c.note == null ? "Made from gear you own" : c.note + ", which you own";
	}

	/** Coins to buy what is not owned yet: -1 when something has no price. */
	long cost(Candidate c, Set<Gear> owned, Prices prices)
	{
		if (c.earn != null)
		{
			return 0;
		}
		long total = 0;
		for (Gear piece : c.pieces)
		{
			if (owned.contains(piece))
			{
				continue;
			}
			CatalogueEntry entry = data.entry(piece);
			int[] build = entry == null ? null : entry.getBuild();
			if (build != null)
			{
				for (int id : build)
				{
					Gear part = data.gear(id);
					if (part != null && owned.contains(part))
					{
						continue;
					}
					long p = prices.price(id);
					if (p <= 0)
					{
						return -1;
					}
					total += p;
				}
				continue;
			}
			long cheapest = -1;
			for (int id : piece.getIds())
			{
				long p = prices.price(id);
				if (p > 0 && (cheapest < 0 || p < cheapest))
				{
					cheapest = p;
				}
			}
			if (cheapest < 0)
			{
				return -1;
			}
			total += cheapest * (piece.getSlot() == Gear.AMMO ? AMMO_STACK : 1);
		}
		return total;
	}

	List<Candidate> candidates(Set<Gear> owned)
	{
		List<Candidate> list = new ArrayList<>();
		for (CatalogueEntry entry : data.catalogue())
		{
			Gear g = entry.getGear();
			if (owned.contains(g) || g.getName().startsWith("Void ") || g.getName().startsWith("Elite void")
				|| g.key().startsWith("obsidian helmet") || superseded(g, owned))
			{
				continue;
			}
			list.add(new Candidate(g.getName(), Collections.singletonList(g), entry.getRequirements(), entry.getEarn(),
				entry.getNote(), g.getSlot() == Gear.AMMO));
		}
		for (String[] bundle : BUNDLES)
		{
			List<Gear> pieces = new ArrayList<>();
			Map<String, Integer> req = new HashMap<>();
			for (int i = 2; i < bundle.length; i++)
			{
				Gear g = data.named(bundle[i]);
				if (g == null)
				{
					pieces = null;
					break;
				}
				pieces.add(g);
				CatalogueEntry entry = data.entry(g);
				if (entry != null)
				{
					entry.getRequirements().forEach((k, v) -> req.merge(k, v, Math::max));
				}
			}
			if (pieces != null && !owned.containsAll(pieces))
			{
				list.add(new Candidate(bundle[0], pieces, req, bundle[1], null, false));
			}
		}
		return list;
	}

	private boolean superseded(Gear g, Set<Gear> owned)
	{
		for (String[] row : SUPERSEDED)
		{
			if (!row[0].equals(g.key()))
			{
				continue;
			}
			for (int i = 1; i < row.length; i++)
			{
				Gear better = data.named(row[i]);
				if (better != null && owned.contains(better))
				{
					return true;
				}
			}
		}
		return false;
	}

	/** Something the roadmap can recommend: one piece, or a set that only works whole. */
	static final class Candidate
	{
		final String name;
		final List<Gear> pieces;
		final Map<String, Integer> requirements;
		final String earn;
		final String note;
		final boolean ammo;

		Candidate(String name, List<Gear> pieces, Map<String, Integer> requirements, String earn, String note, boolean ammo)
		{
			this.name = name;
			this.pieces = pieces;
			this.requirements = requirements;
			this.earn = earn;
			this.note = note;
			this.ammo = ammo;
		}

		/** The piece whose picture stands for it: a set shows its body. */
		Gear icon()
		{
			for (Gear g : pieces)
			{
				if (g.getSlot() == Gear.BODY)
				{
					return g;
				}
			}
			return pieces.get(0);
		}
	}
}
