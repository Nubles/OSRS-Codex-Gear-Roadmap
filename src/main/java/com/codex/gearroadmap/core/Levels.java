package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Which levels are worth training for the fight the roadmap is aimed at: for each skill that
 * powers the best loadout, the nearest level that raises the damage it does, how much, and what
 * that is worth in coins at the price the road's first step charges for damage.
 */
public final class Levels
{
	/** Levels ahead looked at for each skill. */
	static final int AHEAD = 10;
	/** A gain smaller than this share of the damage is not worth naming. */
	static final double MIN_SHARE = 0.005;

	private Levels()
	{
	}

	/** One skill's next level that pays. */
	public static final class Step
	{
		private final String skill;
		private final int from;
		private final int to;
		private final double gain;
		private final double share;
		private final int maxHit;
		private final boolean newMaxHit;
		private final long worth;
		private final String prayer;

		Step(String skill, int from, int to, double gain, double share, int maxHit, boolean newMaxHit, long worth)
		{
			this(skill, from, to, gain, share, maxHit, newMaxHit, worth, null);
		}

		Step(String skill, int from, int to, double gain, double share, int maxHit, boolean newMaxHit, long worth, String prayer)
		{
			this.prayer = prayer;
			this.skill = skill;
			this.from = from;
			this.to = to;
			this.gain = gain;
			this.share = share;
			this.maxHit = maxHit;
			this.newMaxHit = newMaxHit;
			this.worth = worth;
		}

		public String getSkill()
		{
			return skill;
		}

		public int getFrom()
		{
			return from;
		}

		/** The level that pays. */
		public int getTo()
		{
			return to;
		}

		/** Damage per second it adds. */
		public double getGain()
		{
			return gain;
		}

		/** The gain as a share of the damage now. */
		public double getShare()
		{
			return share;
		}

		public int getMaxHit()
		{
			return maxHit;
		}

		/** Whether it is the max hit going up, rather than accuracy. */
		public boolean isNewMaxHit()
		{
			return newMaxHit;
		}

		/** The stronger prayer the level unlocks, such as Piety at 70 Defence, or null. */
		public String getPrayer()
		{
			return prayer;
		}

		/** What the same damage would cost in gear at the road's average price for it, or 0 with no road. */
		public long getWorth()
		{
			return worth;
		}
	}

	/**
	 * The skills that power an attack of this kind, and Prayer and Defence, whose levels unlock the
	 * stronger prayers (Piety and Rigour at 70 Defence, Augury at 77 Prayer).
	 */
	static String[] skills(Result.Kind kind)
	{
		switch (kind)
		{
			case RANGED:
				return new String[]{"Ranged", "Prayer", "Defence"};
			case MAGIC:
				return new String[]{"Magic", "Prayer", "Defence"};
			default:
				return new String[]{"Strength", "Attack", "Prayer", "Defence"};
		}
	}

	/**
	 * For each skill behind the best loadout's attack, the nearest level within {@link #AHEAD} that
	 * adds at least {@link #MIN_SHARE} of the damage, worn in the same gear. Best value first.
	 */
	public static List<Step> ahead(Calc calc, Roadmap map, Fighter fighter)
	{
		Planner.Plan now = map.getNow().best();
		Result r = now.getResult();
		if (r.getKind() == null || r.getDps() <= 0)
		{
			return Collections.emptyList();
		}
		Target target = map.getTarget();
		double perDps = 0;
		if (!map.getPath().isEmpty())
		{
			// The road's average price for damage, from here to its end.
			Roadmap.Step last = map.getPath().get(map.getPath().size() - 1);
			double gained = last.getOption().getAfter().dps() - now.dps();
			perDps = gained > 0 ? last.getSpent() / gained : 0;
		}
		List<Step> steps = new ArrayList<>();
		for (String skill : skills(r.getKind()))
		{
			int from = fighter.level(skill);
			for (int level = from + 1; level <= Math.min(99, from + AHEAD); level++)
			{
				Result next = calc.best(now.getLoadout(), fighter.withLevel(skill, level), target, r.getKind());
				double gain = next.getDps() - r.getDps();
				if (gain >= r.getDps() * MIN_SHARE)
				{
					String prayer = next.getPrayer() != null && !next.getPrayer().equals(r.getPrayer()) ? next.getPrayer() : null;
					steps.add(new Step(skill, from, level, gain, gain / r.getDps(), next.getMaxHit(), next.getMaxHit() > r.getMaxHit(),
						Math.round(gain * perDps), prayer));
					break;
				}
			}
		}
		// The most damage for the fewest levels first.
		steps.sort((a, b) -> Double.compare(b.getGain() / (b.getTo() - b.getFrom()), a.getGain() / (a.getTo() - a.getFrom())));
		return steps;
	}
}
