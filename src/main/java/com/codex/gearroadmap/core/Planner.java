package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.CatalogueEntry;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the best loadout a player can put together from what they own, for each kind of
 * attack, by trying their strongest weapons and then improving one slot at a time until
 * no single swap helps. The same search answers "what if I also owned this?" for every
 * upgrade the roadmap weighs.
 */
public final class Planner
{
	/** Slots in the order they are improved; the weapon is chosen first and held. */
	private static final int[] ORDER = {Gear.AMMO, Gear.BODY, Gear.LEGS, Gear.HEAD, Gear.SHIELD, Gear.NECK, Gear.CAPE,
		Gear.HANDS, Gear.FEET, Gear.RING};
	private static final int WEAPONS_TRIED = 5;
	private static final String[][] SETS = {
		{"void melee helm", "void knight top", "void knight robe", "void knight gloves"},
		{"void ranger helm", "void knight top", "void knight robe", "void knight gloves"},
		{"void mage helm", "void knight top", "void knight robe", "void knight gloves"},
		{"void ranger helm", "elite void top", "elite void robe", "void knight gloves"},
		{"void mage helm", "elite void top", "elite void robe", "void knight gloves"},
		{"obsidian helmet", "obsidian platebody", "obsidian platelegs"},
	};

	/** One loadout and what it does. */
	public static final class Plan
	{
		private final Loadout loadout;
		private final Result result;

		Plan(Loadout loadout, Result result)
		{
			this.loadout = loadout;
			this.result = result;
		}

		public Loadout getLoadout()
		{
			return loadout;
		}

		public Result getResult()
		{
			return result;
		}

		public double dps()
		{
			return result.getDps();
		}
	}

	/** The best plan for each kind of attack, and the best of them. */
	public static final class Best
	{
		private final Map<Result.Kind, Plan> plans;
		private final Plan best;

		Best(Map<Result.Kind, Plan> plans)
		{
			this.plans = plans;
			Plan top = null;
			for (Plan p : plans.values())
			{
				if (top == null || p.dps() > top.dps() + 1e-9)
				{
					top = p;
				}
			}
			this.best = top == null ? new Plan(Loadout.empty(), Result.NONE) : top;
		}

		public Plan get(Result.Kind kind)
		{
			return plans.get(kind);
		}

		public Plan best()
		{
			return best;
		}

		public double dps()
		{
			return best.dps();
		}
	}

	private final RoadmapData data;
	private final Calc calc;

	public Planner(RoadmapData data)
	{
		this.data = data;
		this.calc = new Calc(data);
	}

	public Calc calc()
	{
		return calc;
	}

	/** The best loadouts from the gear owned. */
	public Best optimise(Collection<Gear> owned, Fighter fighter, Target target)
	{
		Map<Result.Kind, Plan> plans = new EnumMap<>(Result.Kind.class);
		for (Result.Kind kind : Result.Kind.values())
		{
			plans.put(kind, target.allows(kind.name()) ? optimise(owned, fighter, target, kind) : new Plan(Loadout.empty(), Result.NONE));
		}
		return new Best(plans);
	}

	private Plan optimise(Collection<Gear> owned, Fighter fighter, Target target, Result.Kind kind)
	{
		List<List<Gear>> bySlot = bySlot(owned, fighter, kind);
		List<Plan> weapons = new ArrayList<>();
		for (Gear weapon : bySlot.get(Gear.WEAPON))
		{
			Loadout start = withAmmo(Loadout.of(weapon), bySlot, fighter, target, kind);
			weapons.add(new Plan(start, calc.best(start, fighter, target, kind)));
		}
		if (kind == Result.Kind.MELEE)
		{
			weapons.add(new Plan(Loadout.empty(), calc.best(Loadout.empty(), fighter, target, kind)));
		}
		weapons.sort(Comparator.comparingDouble((Plan p) -> -p.dps()));

		Plan best = new Plan(Loadout.empty(), Result.NONE);
		int tried = 0;
		for (Plan start : weapons)
		{
			if (start.dps() <= 0 && kind != Result.Kind.MELEE || tried++ >= WEAPONS_TRIED)
			{
				break;
			}
			Plan p = ascend(start.getLoadout(), bySlot, fighter, target, kind, -1, 3);
			if (p.dps() > best.dps() + 1e-9)
			{
				best = p;
			}
			for (String[] set : SETS)
			{
				Loadout seeded = seed(start.getLoadout(), set, bySlot);
				if (seeded != null)
				{
					Plan s = ascend(seeded, bySlot, fighter, target, kind, -1, 3);
					if (s.dps() > best.dps() + 1e-9)
					{
						best = s;
					}
				}
			}
		}
		return best;
	}

	/**
	 * The best overall plan if the player also owned these pieces, starting from the plans
	 * they already have. Faster than a fresh search, and the same answer in practice.
	 */
	public Plan withAdded(Best current, Collection<Gear> owned, List<Gear> added, Fighter fighter, Target target)
	{
		Set<Gear> all = new LinkedHashSet<>(owned);
		all.addAll(added);
		Plan best = current.best();
		for (Result.Kind kind : Result.Kind.values())
		{
			if (!fits(added, kind) || !target.allows(kind.name()))
			{
				continue;
			}
			List<List<Gear>> bySlot = bySlot(all, fighter, kind);
			Plan from = current.get(kind);
			Loadout start = from == null || from.dps() <= 0 ? Loadout.empty() : from.getLoadout();
			int locked = -1;
			for (Gear g : added)
			{
				if (g.getSlot() == Gear.SHIELD && start.weapon() != null && start.weapon().isTwoHanded())
				{
					start = oneHanded(start, g, bySlot, fighter, target, kind);
				}
				start = start.with(g);
				locked = added.size() == 1 ? g.getSlot() : -1;
			}
			if (start.weapon() == null && kind != Result.Kind.MELEE)
			{
				continue;
			}
			if (start.weapon() != null && containsWeapon(added))
			{
				start = withAmmo(start, bySlot, fighter, target, kind);
			}
			Plan p = ascend(start, bySlot, fighter, target, kind, locked, 2);
			if (p.dps() > best.dps() + 1e-9)
			{
				best = p;
			}
		}
		return best;
	}

	/** Swaps a two-handed weapon for the best one-handed one, so a shield can be tried. */
	private Loadout oneHanded(Loadout lo, Gear shield, List<List<Gear>> bySlot, Fighter fighter, Target target, Result.Kind kind)
	{
		Loadout best = lo.without(Gear.WEAPON);
		Result bestResult = calc.best(best.with(shield), fighter, target, kind);
		for (Gear weapon : bySlot.get(Gear.WEAPON))
		{
			if (weapon.isTwoHanded())
			{
				continue;
			}
			Loadout next = withAmmo(lo.with(weapon), bySlot, fighter, target, kind);
			Result r = calc.best(next.with(shield), fighter, target, kind);
			if (r.isBetterThan(bestResult))
			{
				best = next;
				bestResult = r;
			}
		}
		return best;
	}

	private static boolean containsWeapon(List<Gear> gear)
	{
		for (Gear g : gear)
		{
			if (g.getSlot() == Gear.WEAPON)
			{
				return true;
			}
		}
		return false;
	}

	/** Whether any of the pieces can matter to this kind of attack. */
	private boolean fits(List<Gear> added, Result.Kind kind)
	{
		for (Gear g : added)
		{
			if (relevant(g, kind))
			{
				return true;
			}
		}
		return false;
	}

	private Plan ascend(Loadout loadout, List<List<Gear>> bySlot, Fighter fighter, Target target, Result.Kind kind,
		int lockedSlot, int passes)
	{
		Loadout lo = loadout;
		Result current = calc.best(lo, fighter, target, kind);
		for (int pass = 0; pass < passes; pass++)
		{
			boolean changed = false;
			for (int slot : ORDER)
			{
				if (slot == lockedSlot || slot == Gear.SHIELD && lo.weapon() != null && lo.weapon().isTwoHanded())
				{
					continue;
				}
				Loadout bestLo = lo;
				Result bestResult = current;
				if (lo.get(slot) != null)
				{
					Loadout bare = lo.without(slot);
					Result r = calc.best(bare, fighter, target, kind);
					if (r.isBetterThan(bestResult))
					{
						bestLo = bare;
						bestResult = r;
					}
				}
				for (Gear g : bySlot.get(slot))
				{
					if (lo.get(slot) == g)
					{
						continue;
					}
					Loadout next = lo.with(g);
					Result r = calc.best(next, fighter, target, kind);
					if (r.isBetterThan(bestResult))
					{
						bestLo = next;
						bestResult = r;
					}
				}
				if (bestLo != lo)
				{
					lo = bestLo;
					current = bestResult;
					changed = true;
				}
			}
			if (!changed)
			{
				break;
			}
		}
		return new Plan(lo, current);
	}

	/** Loads the best ammunition the player owns for a weapon that needs it. */
	private Loadout withAmmo(Loadout lo, List<List<Gear>> bySlot, Fighter fighter, Target target, Result.Kind kind)
	{
		Set<Integer> fires = data.ammoFor(lo.weapon());
		if (fires == null || fires.isEmpty())
		{
			return lo;
		}
		Loadout best = lo;
		Result bestResult = calc.best(lo, fighter, target, kind);
		for (Gear ammo : bySlot.get(Gear.AMMO))
		{
			Loadout next = lo.with(ammo);
			Result r = calc.best(next, fighter, target, kind);
			if (r.isBetterThan(bestResult))
			{
				best = next;
				bestResult = r;
			}
		}
		return best;
	}

	private Loadout seed(Loadout start, String[] set, List<List<Gear>> bySlot)
	{
		Loadout lo = start;
		for (String key : set)
		{
			Gear piece = null;
			for (List<Gear> slot : bySlot)
			{
				for (Gear g : slot)
				{
					if (g.key().equals(key))
					{
						piece = g;
					}
				}
			}
			if (piece == null)
			{
				return null;
			}
			lo = lo.with(piece);
		}
		return lo;
	}

	/** The owned pieces worth trying in each slot for one kind of attack, keeping only what the player can wield. */
	private List<List<Gear>> bySlot(Collection<Gear> owned, Fighter fighter, Result.Kind kind)
	{
		List<List<Gear>> slots = new ArrayList<>();
		for (int i = 0; i < Gear.SLOTS; i++)
		{
			slots.add(new ArrayList<>());
		}
		Set<Gear> seen = new HashSet<>();
		for (Gear g : owned)
		{
			if (g == null || !seen.add(g) || !wieldable(g, fighter))
			{
				continue;
			}
			if (g.getSlot() == Gear.WEAPON ? usesKind(g, kind) : relevant(g, kind))
			{
				slots.get(g.getSlot()).add(g);
			}
		}
		return slots;
	}

	public boolean wieldable(Gear gear, Fighter fighter)
	{
		CatalogueEntry entry = data.entry(gear);
		return entry == null || fighter.meets(entry.getRequirements());
	}

	/** Whether a weapon offers an attack of this kind. */
	public boolean usesKind(Gear weapon, Result.Kind kind)
	{
		for (RoadmapData.Style style : data.styles(weapon.getCategory()))
		{
			String type = style.getType();
			Result.Kind k = "magic".equals(type) ? Result.Kind.MAGIC : "ranged".equals(type) ? Result.Kind.RANGED : Result.Kind.MELEE;
			if (k == kind)
			{
				return true;
			}
		}
		return false;
	}

	/** Whether a piece can raise damage for this kind of attack. */
	boolean relevant(Gear g, Result.Kind kind)
	{
		if (g.getSlot() == Gear.WEAPON)
		{
			return usesKind(g, kind);
		}
		String key = g.key();
		if (key.startsWith("salve amulet") || key.contains("slayer helmet") || key.startsWith("black mask")
			|| key.startsWith("void ") || key.startsWith("elite void") || key.startsWith("obsidian") || key.startsWith("crystal "))
		{
			return true;
		}
		switch (kind)
		{
			case MELEE:
				return g.offence(Gear.STAB) > 0 || g.offence(Gear.SLASH) > 0 || g.offence(Gear.CRUSH) > 0 || g.strength() > 0;
			case RANGED:
				return g.offence(Gear.RANGED) > 0 || g.rangedStrength() > 0;
			default:
				return g.offence(Gear.MAGIC) > 0 || g.magicDamage() > 0;
		}
	}
}
