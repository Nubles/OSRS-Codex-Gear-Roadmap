package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Special attacks in the kill time. A spec weapon rides in the pack: each kill opens with as many
 * specs as the energy allows, then the main weapon finishes. Damage specs land one big hit early;
 * drain specs (the warhammer, the Bandos godsword, Arclight) lower the target's defence so every
 * hit after them lands more often. Energy comes back at 10% every 30 seconds, and a short gap
 * between kills is counted, so a long fight gets more specs than a quick one.
 */
public final class Specs
{
	/** Seconds between kills counted towards energy: walking, looting, the next spawn. */
	static final double GAP_SECONDS = 15;
	/** Energy back each second: 10% every 30 seconds. */
	static final double REGEN = 10.0 / 30.0;
	private static final double TICK = 0.6;

	enum Drain
	{
		NONE,
		/** Defence lowered by a share of its level on a landed hit. */
		PERCENT,
		/** Defence lowered by the damage dealt. */
		DAMAGE
	}

	/** One special attack: its weapon, cost and what it does against a normal hit. */
	static final class Spec
	{
		final String key;
		final int cost;
		final Result.Kind kind;
		final double accuracy;
		final double damage;
		final int hits;
		final Drain drain;
		final int drainPercent;
		final int bonus;

		Spec(String key, int cost, Result.Kind kind, double accuracy, double damage, int hits, Drain drain, int drainPercent, int bonus)
		{
			this.key = key;
			this.cost = cost;
			this.kind = kind;
			this.accuracy = accuracy;
			this.damage = damage;
			this.hits = hits;
			this.drain = drain;
			this.drainPercent = drainPercent;
			this.bonus = bonus;
		}
	}

	static final List<Spec> SPECS = Collections.unmodifiableList(java.util.Arrays.asList(
		new Spec("dragon dagger", 25, Result.Kind.MELEE, 1.15, 1.15, 2, Drain.NONE, 0, 0),
		new Spec("abyssal dagger", 25, Result.Kind.MELEE, 1.25, 0.85, 2, Drain.NONE, 0, 0),
		new Spec("dragon longsword", 25, Result.Kind.MELEE, 1.0, 1.25, 1, Drain.NONE, 0, 0),
		new Spec("dragon mace", 25, Result.Kind.MELEE, 1.25, 1.5, 1, Drain.NONE, 0, 0),
		new Spec("dragon claws", 50, Result.Kind.MELEE, 1.0, 1.0, 4, Drain.NONE, 0, 0),
		new Spec("armadyl godsword", 50, Result.Kind.MELEE, 2.0, 1.375, 1, Drain.NONE, 0, 0),
		new Spec("saradomin godsword", 50, Result.Kind.MELEE, 2.0, 1.1, 1, Drain.NONE, 0, 0),
		new Spec("zamorak godsword", 50, Result.Kind.MELEE, 2.0, 1.1, 1, Drain.NONE, 0, 0),
		new Spec("ancient godsword", 50, Result.Kind.MELEE, 2.0, 1.1, 1, Drain.NONE, 0, 25),
		new Spec("bandos godsword", 50, Result.Kind.MELEE, 2.0, 1.21, 1, Drain.DAMAGE, 0, 0),
		new Spec("dragon warhammer", 50, Result.Kind.MELEE, 1.0, 1.5, 1, Drain.PERCENT, 30, 0),
		new Spec("arclight", 50, Result.Kind.MELEE, 1.0, 1.0, 1, Drain.PERCENT, 5, 0),
		new Spec("voidwaker", 50, Result.Kind.MELEE, 1.0, 1.0, 1, Drain.NONE, 0, 0),
		new Spec("toxic blowpipe", 50, Result.Kind.RANGED, 2.0, 1.5, 1, Drain.NONE, 0, 0),
		new Spec("dark bow", 55, Result.Kind.RANGED, 1.0, 1.3, 2, Drain.NONE, 0, 0),
		new Spec("dragon crossbow", 60, Result.Kind.RANGED, 1.0, 1.2, 1, Drain.NONE, 0, 0)));

	/** A kill with a spec weapon in the pack, against the same kill without. */
	public static final class Plan
	{
		private final Gear weapon;
		private final int specs;
		private final double seconds;
		private final double without;
		private final long cost;

		Plan(Gear weapon, int specs, double seconds, double without, long cost)
		{
			this.weapon = weapon;
			this.specs = specs;
			this.seconds = seconds;
			this.without = without;
			this.cost = cost;
		}

		public Gear getWeapon()
		{
			return weapon;
		}

		/** Specs used each kill, opening the fight. */
		public int getSpecs()
		{
			return specs;
		}

		/** Seconds a kill takes with the specs. */
		public double getSeconds()
		{
			return seconds;
		}

		/** Seconds a kill takes with the main weapon alone. */
		public double getWithout()
		{
			return without;
		}

		public double getSaved()
		{
			return without - seconds;
		}

		/** Coins still to spend on the weapon; 0 when owned. */
		public long getCost()
		{
			return cost;
		}
	}

	/** The best spec weapon owned for a fight, and the best ones to buy. */
	public static final class View
	{
		private final Target target;
		private final Plan owned;
		private final List<Plan> buy;

		View(Target target, Plan owned, List<Plan> buy)
		{
			this.target = target;
			this.owned = owned;
			this.buy = buy;
		}

		public Target getTarget()
		{
			return target;
		}

		/** The owned spec weapon that saves the most, or null when none helps. */
		public Plan getOwned()
		{
			return owned;
		}

		/** Spec weapons to buy that would save more than the owned one, best value first. */
		public List<Plan> getBuy()
		{
			return buy;
		}
	}

	private final RoadmapData data;
	private final Planner planner;

	public Specs(RoadmapData data, Planner planner)
	{
		this.data = data;
		this.planner = planner;
	}

	/** Spec weapons for the fight with this main loadout: the best owned one and up to three to buy. */
	public View view(Collection<Gear> owned, Fighter fighter, Target target, Loadout main, Advisor.Prices prices, boolean canBuy)
	{
		Plan bestOwned = null;
		List<Plan> buy = new ArrayList<>();
		for (Spec spec : SPECS)
		{
			Gear weapon = pick(spec, owned);
			boolean have = weapon != null;
			if (!have)
			{
				if (!canBuy)
				{
					continue;
				}
				weapon = data.named(capital(spec.key));
			}
			if (weapon == null || !planner.wieldable(weapon, fighter))
			{
				continue;
			}
			long cost = have ? 0 : prices == null ? 0 : prices.price(weapon.getId());
			if (!have && cost <= 0)
			{
				continue;
			}
			Plan p = plan(spec, weapon, main, fighter, target, cost);
			if (p == null || p.getSaved() < 0.5)
			{
				continue;
			}
			if (have)
			{
				if (bestOwned == null || p.getSaved() > bestOwned.getSaved())
				{
					bestOwned = p;
				}
			}
			else
			{
				buy.add(p);
			}
		}
		double floor = bestOwned == null ? 0 : bestOwned.getSaved();
		buy.removeIf(p -> p.getSaved() <= floor + 0.5);
		buy.sort(Comparator.comparingDouble((Plan p) -> -(p.getSaved() - floor) / Math.max(1, p.getCost())));
		return new View(target, bestOwned, buy.size() > 3 ? new ArrayList<>(buy.subList(0, 3)) : buy);
	}

	/** The owned item that fires this spec, any variant counting. */
	static Gear pick(Spec spec, Collection<Gear> owned)
	{
		for (Gear g : owned)
		{
			if (g.getSlot() == Gear.WEAPON && g.key().startsWith(spec.key))
			{
				return g;
			}
		}
		return null;
	}

	/** One spec weapon's kill against the main loadout alone, or null when it can't attack the target. */
	Plan plan(Spec spec, Gear weapon, Loadout main, Fighter fighter, Target target, long cost)
	{
		Calc calc = planner.calc();
		Result base = calc.best(main, fighter, target);
		if (base.getKind() == null || base.getDps() <= 0 || !target.allows(spec.kind.name()))
		{
			return null;
		}
		double without = target.getHp() / base.getDps();
		Loadout specLoadout = main.with(weapon);
		if (weapon.isTwoHanded())
		{
			specLoadout = specLoadout.without(Gear.SHIELD);
		}
		Result specBase = calc.best(specLoadout, fighter, target, spec.kind);
		if (specBase.getKind() == null)
		{
			return null;
		}

		// Energy at the start of a kill comes from the last kill and the gap after it; settle that loop.
		double seconds = without;
		int best = 0;
		double bestSeconds = without;
		for (int round = 0; round < 4; round++)
		{
			double energy = Math.min(100, REGEN * (seconds + GAP_SECONDS));
			int most = (int) Math.floor(energy / spec.cost + 1e-9);
			best = 0;
			bestSeconds = without;
			for (int n = 1; n <= most; n++)
			{
				double s = kill(spec, n, main, specLoadout, fighter, target, base, specBase);
				if (s < bestSeconds - 1e-9)
				{
					best = n;
					bestSeconds = s;
				}
			}
			seconds = bestSeconds;
		}
		if (best == 0)
		{
			return null;
		}
		return new Plan(weapon, best, bestSeconds, without, cost);
	}

	/** Seconds for a kill that opens with n specs. */
	private double kill(Spec spec, int n, Loadout main, Loadout specLoadout, Fighter fighter, Target target, Result base, Result specBase)
	{
		Calc calc = planner.calc();
		Target now = target;
		Result specNow = specBase;
		double hp = target.getHp();
		double time = 0;
		double drainedDps = base.getDps();
		for (int i = 0; i < n && hp > 0; i++)
		{
			double chance = Calc.hitChance((long) (specNow.getAttackRoll() * spec.accuracy), specNow.getDefenceRoll());
			double dealt = damage(spec, specNow, chance);
			hp -= dealt;
			time += specNow.getTicks() * TICK;
			if (spec.drain != Drain.NONE)
			{
				int level = now.getDefence();
				double lowered = spec.drain == Drain.PERCENT ? level * spec.drainPercent / 100.0 : dealt / Math.max(chance, 1e-9);
				// Drained on a landed hit only: the expected level weighs the miss.
				int after = (int) Math.round(level - chance * lowered);
				if (after < level)
				{
					now = now.withDefence(after);
					specNow = calc.best(specLoadout, fighter, now, spec.kind);
					Result mainNow = calc.best(main, fighter, now, base.getKind());
					drainedDps = mainNow.getDps() > 0 ? mainNow.getDps() : drainedDps;
				}
			}
		}
		if (hp > 0)
		{
			time += hp / drainedDps;
		}
		return time;
	}

	/** The mean damage of one spec at the given accuracy. */
	static double damage(Spec spec, Result r, double chance)
	{
		double max = r.getMaxHit();
		if (spec.key.equals("dragon claws"))
		{
			// The first of four rolls to land sets the damage: about 1.5, 1.25, 1 and 0.75 max hits.
			double miss = 1 - chance;
			return max * chance * (1.5 + miss * 1.25 + miss * miss * 1.0 + miss * miss * miss * 0.75);
		}
		if (spec.key.equals("voidwaker"))
		{
			// Always hits, for 50% to 150% of the max hit.
			return max;
		}
		double each = chance * Math.floor(max * spec.damage) / 2.0;
		return each * spec.hits + chance * spec.bonus;
	}

	private static String capital(String key)
	{
		return key.isEmpty() ? key : key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
	}
}
