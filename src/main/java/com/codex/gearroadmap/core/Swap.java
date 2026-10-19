package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * What the player is wearing against the best loadout they own for the aimed target: the pieces to
 * put on, where each one is (carried or in the bank), and the damage the change adds. Only pieces to
 * put on are listed; a slot the best loadout leaves empty is not a reason to take anything off.
 */
public final class Swap
{
	/** Below this, wearing something else is not worth a line. */
	static final double MIN_GAIN = 0.005;

	/** Where a piece to put on is kept. */
	public enum Where
	{
		INVENTORY,
		BANK
	}

	/** One piece to put on. */
	public static final class Change
	{
		private final Gear wear;
		private final Gear off;
		private final Where where;

		Change(Gear wear, Gear off, Where where)
		{
			this.wear = wear;
			this.off = off;
			this.where = where;
		}

		public Gear getWear()
		{
			return wear;
		}

		/** What it replaces, or null for an empty slot (or one holding something the roadmap does not know). */
		public Gear getOff()
		{
			return off;
		}

		public Where getWhere()
		{
			return where;
		}
	}

	private final List<Change> changes;
	private final double wornDps;
	private final double bestDps;
	private final Result.Kind wornKind;

	private Swap(List<Change> changes, double wornDps, double bestDps, Result.Kind wornKind)
	{
		this.changes = Collections.unmodifiableList(changes);
		this.wornDps = wornDps;
		this.bestDps = bestDps;
		this.wornKind = wornKind;
	}

	/**
	 * @param worn the gear worn now, by item id, as the game shows it; ids the roadmap does not know are ignored
	 * @param carried item ids in the inventory, to tell a piece at hand from one in the bank
	 * @param byId the roadmap's piece for an item id, or null
	 * @param best the best loadout owned for the target, or null when nothing can attack it
	 */
	public static Swap of(Set<Integer> worn, Set<Integer> carried, IntFunction<Gear> byId, Planner.Plan best,
		Calc calc, Fighter fighter, Target target)
	{
		Loadout now = Loadout.empty();
		for (int id : worn)
		{
			Gear g = byId.apply(id);
			if (g != null)
			{
				now = now.with(g);
			}
		}
		Result wornResult = calc.best(now, fighter, target);
		double wornDps = wornResult == null ? 0 : wornResult.getDps();
		Result.Kind wornKind = wornResult == null || wornDps <= 0 ? null : wornResult.getKind();
		if (best == null || best.getResult() == null || best.dps() <= 0)
		{
			return new Swap(new ArrayList<>(), wornDps, 0, wornKind);
		}
		List<Change> changes = new ArrayList<>();
		if (best.dps() - wornDps >= MIN_GAIN)
		{
			Loadout lo = best.getLoadout();
			for (int slot : ORDER)
			{
				Gear want = lo.get(slot);
				Gear have = now.get(slot);
				if (want == null || have != null && have.key().equals(want.key()))
				{
					continue;
				}
				changes.add(new Change(want, have, carries(carried, want) ? Where.INVENTORY : Where.BANK));
			}
		}
		return new Swap(changes, wornDps, best.dps(), wornKind);
	}

	/** Weapon first, as the change that matters most, then the body down. */
	private static final int[] ORDER = {Gear.WEAPON, Gear.SHIELD, Gear.AMMO, Gear.HEAD, Gear.BODY, Gear.LEGS, Gear.NECK, Gear.CAPE,
		Gear.HANDS, Gear.FEET, Gear.RING};

	private static boolean carries(Set<Integer> carried, Gear g)
	{
		for (int id : g.getIds())
		{
			if (carried.contains(id))
			{
				return true;
			}
		}
		return false;
	}

	/** The pieces to put on, weapon first; empty when what is worn is already the best owned. */
	public List<Change> getChanges()
	{
		return changes;
	}

	/** Damage a second in what is worn now. */
	public double getWornDps()
	{
		return wornDps;
	}

	/** Damage a second in the best loadout owned. */
	public double getBestDps()
	{
		return bestDps;
	}

	/** How what is worn now attacks, or null when it cannot hurt the target. */
	public Result.Kind getWornKind()
	{
		return wornKind;
	}

	/** The damage a second the changes add. */
	public double gain()
	{
		return Math.max(0, bestDps - wornDps);
	}

	public boolean isBest()
	{
		return changes.isEmpty() && bestDps > 0;
	}
}
