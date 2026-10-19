package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Everything the panel shows for one target: the loadouts owned now and the upgrades ahead. */
public final class Roadmap
{
	/** One upgrade weighed against the loadout it would join. */
	public static final class Option
	{
		private final String name;
		private final List<Gear> pieces;
		private final Gear icon;
		private final long cost;
		private final double gain;
		private final double before;
		private final Planner.Plan after;
		private final String missing;
		private final String earn;
		private final String note;
		private final boolean ammo;

		Option(String name, List<Gear> pieces, Gear icon, long cost, double gain, double before, Planner.Plan after,
			String missing, String earn, String note, boolean ammo)
		{
			this.name = name;
			this.pieces = pieces;
			this.icon = icon;
			this.cost = cost;
			this.gain = gain;
			this.before = before;
			this.after = after;
			this.missing = missing;
			this.earn = earn;
			this.note = note;
			this.ammo = ammo;
		}

		public String getName()
		{
			return name;
		}

		public List<Gear> getPieces()
		{
			return pieces;
		}

		public Gear getIcon()
		{
			return icon;
		}

		/** Coins to buy it; 0 when it is earned or made from gear already owned. */
		public long getCost()
		{
			return cost;
		}

		/** Damage per second it adds. */
		public double getGain()
		{
			return gain;
		}

		/** The gain as a share of the damage before it. */
		public double getGainShare()
		{
			return before <= 0 ? 0 : gain / before;
		}

		public Planner.Plan getAfter()
		{
			return after;
		}

		/** The level still missing to wield it, such as "80 Ranged", or null. */
		public String getMissing()
		{
			return missing;
		}

		public String getEarn()
		{
			return earn;
		}

		public String getNote()
		{
			return note;
		}

		/** Ammunition is priced per thousand. */
		public boolean isAmmo()
		{
			return ammo;
		}
	}

	/** One step of the road, with what the road has cost up to and including it. */
	public static final class Step
	{
		private final Option option;
		private final long spent;
		private final boolean affordable;

		Step(Option option, long spent, boolean affordable)
		{
			this.option = option;
			this.spent = spent;
			this.affordable = affordable;
		}

		public Option getOption()
		{
			return option;
		}

		public long getSpent()
		{
			return spent;
		}

		/** Whether the player's coins already cover the road up to here. */
		public boolean isAffordable()
		{
			return affordable;
		}
	}

	/** One target in the Targets list: the best the player can do there now. */
	public static final class Survey
	{
		private final Target target;
		private final Planner.Plan plan;
		private final Planner.Best best;

		Survey(Target target, Planner.Best best)
		{
			this.target = target;
			this.plan = best.best();
			this.best = best;
		}

		/** The best plan for each kind of attack, not only the best of them. */
		public Planner.Best getBest()
		{
			return best;
		}

		public Target getTarget()
		{
			return target;
		}

		public Planner.Plan getPlan()
		{
			return plan;
		}
	}

	private final Target target;
	private final Planner.Best now;
	private final List<Step> path;
	private final List<Option> free;
	private final List<Option> locked;
	private final List<Option> leaps;
	private final long coins;
	private Option goal;
	private boolean goalOwned;

	Roadmap(Target target, Planner.Best now, List<Step> path, List<Option> free, List<Option> locked, List<Option> leaps, long coins)
	{
		this.target = target;
		this.now = now;
		this.path = path;
		this.free = free;
		this.locked = locked;
		this.leaps = leaps;
		this.coins = coins;
	}

	public Target getTarget()
	{
		return target;
	}

	/** The same road, with what the player's coins now cover. */
	public Roadmap withCoins(long coins)
	{
		List<Step> steps = new ArrayList<>();
		for (Step s : path)
		{
			steps.add(new Step(s.option, s.spent, s.spent <= coins));
		}
		List<Option> bought = new ArrayList<>(leaps);
		return new Roadmap(target, now, steps, free, locked, bought, coins).withGoal(goal, goalOwned);
	}

	/** The same road with the player's savings goal, as weighed against this target. */
	Roadmap withGoal(Option goal, boolean owned)
	{
		this.goal = goal;
		this.goalOwned = owned;
		return this;
	}

	/** The savings goal priced against this target, or null without one (or while it has no price). */
	public Option getGoal()
	{
		return goal;
	}

	/** Whether every piece of the savings goal is owned already. */
	public boolean isGoalOwned()
	{
		return goalOwned;
	}

	public Planner.Best getNow()
	{
		return now;
	}

	public List<Step> getPath()
	{
		return Collections.unmodifiableList(path);
	}

	public List<Option> getFree()
	{
		return free;
	}

	public List<Option> getLocked()
	{
		return locked;
	}

	public List<Option> getLeaps()
	{
		return leaps;
	}

	public long getCoins()
	{
		return coins;
	}

	/** Damage per second at the end of the road. */
	public double finalDps()
	{
		return path.isEmpty() ? now.dps() : path.get(path.size() - 1).getOption().getAfter().dps();
	}
}
