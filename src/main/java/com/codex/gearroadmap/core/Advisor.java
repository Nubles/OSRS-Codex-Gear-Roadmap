package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.CatalogueEntry;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
	private static final int SIDE_LIST = 4;

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
		Set<Gear> owned = new LinkedHashSet<>(ownedGear);
		Planner.Best start = planner.optimise(owned, fighter, target);
		double base = start.dps();
		List<Candidate> candidates = candidates(owned);

		List<Roadmap.Option> free = new ArrayList<>();
		List<Roadmap.Option> locked = new ArrayList<>();
		List<Roadmap.Option> bought = new ArrayList<>();
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
		locked.sort(byGain);

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
		return new Roadmap(target, start, path, cut(free, SIDE_LIST + 2), cut(locked, SIDE_LIST), leaps, coins);
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
			if (pick == null)
			{
				break;
			}
			used.add(pick);
			have.addAll(pick.pieces);
			Planner.Best next = planner.optimise(have, fighter, target);
			// The fresh search is never worse than the quick one; keep whichever found more.
			Planner.Plan plan = next.dps() >= pickPlan.dps() ? next.best() : pickPlan;
			spent += pickCost;
			Roadmap.Option option = new Roadmap.Option(pick.name, pick.pieces, pick.icon(), pickCost, plan.dps() - base, base, plan,
				null, null, pick.note, pick.ammo);
			path.add(new Roadmap.Step(option, spent, spent <= coins));
			current = next;
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

	/** Every target's best loadout, for the Targets tab. */
	public List<Roadmap.Survey> survey(Collection<Gear> owned, Fighter fighter)
	{
		List<Roadmap.Survey> rows = new ArrayList<>();
		for (Target t : data.targets())
		{
			Planner.Best best = planner.optimise(owned, fighter, t);
			rows.add(new Roadmap.Survey(t, best.best()));
		}
		return rows;
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
				|| g.key().startsWith("obsidian helmet"))
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
