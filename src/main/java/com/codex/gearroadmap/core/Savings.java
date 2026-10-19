package com.codex.gearroadmap.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A savings goal: one upgrade the player is putting coins aside for, how far their coins
 * already go, and, when Codex Ledger keeps their net worth, how long the rest should take
 * at the pace their wealth has grown over the last week.
 */
public final class Savings
{
	/** How far back the pace looks. */
	public static final long WINDOW_MS = 7L * 24 * 3_600_000L;
	/** A pace from less history than this would mostly be noise. */
	public static final long MIN_SPAN_MS = 24 * 3_600_000L;
	private static final long DAY_MS = 24 * 3_600_000L;
	/** Past this many days an estimate says nothing useful. */
	private static final int MAX_DAYS = 999;

	/** The saved goal: the upgrade's name and one item id per piece, so it can be found again on any road. */
	public static final class Goal
	{
		private String name;
		private List<Integer> ids;

		public Goal()
		{
		}

		public Goal(String name, List<Integer> ids)
		{
			this.name = name;
			this.ids = new ArrayList<>(ids);
		}

		public String getName()
		{
			return name == null ? "" : name;
		}

		public List<Integer> getIds()
		{
			return ids == null ? Collections.emptyList() : Collections.unmodifiableList(ids);
		}

		/** A goal read back from the profile is only usable with a name and at least one piece. */
		public boolean isValid()
		{
			return name != null && !name.isEmpty() && ids != null && !ids.isEmpty();
		}
	}

	/** Net worth over time, as Codex Ledger saves it; only the fields Gear Roadmap reads. */
	public static final class Worth
	{
		private long total;
		private List<Point> history;

		public Worth()
		{
		}

		public Worth(long total, List<Point> history)
		{
			this.total = total;
			this.history = history;
		}

		public long getTotal()
		{
			return total;
		}

		public List<Point> getHistory()
		{
			return history == null ? Collections.emptyList() : history;
		}
	}

	/** One reading of net worth: when, and how much. Named as Codex Ledger names them. */
	public static final class Point
	{
		private long t;
		private long v;

		public Point()
		{
		}

		public Point(long time, long value)
		{
			this.t = time;
			this.v = value;
		}

		public long getTime()
		{
			return t;
		}

		public long getValue()
		{
			return v;
		}
	}

	/** What the panel shows for a goal. */
	public static final class View
	{
		private final Roadmap.Option option;
		private final boolean owned;
		private final long coins;
		private final long worth;
		private final boolean ledger;
		private final Long perDay;

		View(Roadmap.Option option, boolean owned, long coins, long worth, boolean ledger, Long perDay)
		{
			this.option = option;
			this.owned = owned;
			this.coins = coins;
			this.worth = worth;
			this.ledger = ledger;
			this.perDay = perDay;
		}

		/** The upgrade as it stands against the aimed target now; its gain may be nothing here. */
		public Roadmap.Option getOption()
		{
			return option;
		}

		/** The player already owns every piece: the goal is met. */
		public boolean isOwned()
		{
			return owned;
		}

		public long getCost()
		{
			return option.getCost();
		}

		public long getCoins()
		{
			return coins;
		}

		public long getRemaining()
		{
			return Math.max(0, getCost() - coins);
		}

		/** Share of the price the player's coins already cover, 0 to 1. */
		public double getProgress()
		{
			if (owned || getCost() <= 0)
			{
				return 1;
			}
			return Math.max(0, Math.min(1, coins / (double) getCost()));
		}

		public boolean isAffordable()
		{
			return owned || coins >= getCost();
		}

		/** Whether Codex Ledger's net worth was readable at all. */
		public boolean hasLedger()
		{
			return ledger;
		}

		/** Net worth as Codex Ledger last saved it, 0 without it. */
		public long getWorth()
		{
			return worth;
		}

		/** Net worth gained per day over the last week, or null when there is too little history to say. */
		public Long getPerDay()
		{
			return perDay;
		}

		/** Whole days until the rest is saved at that pace, or -1 when it cannot be told (no pace, or not growing). */
		public int getDays()
		{
			if (isAffordable())
			{
				return 0;
			}
			if (perDay == null || perDay <= 0)
			{
				return -1;
			}
			long days = (getRemaining() + perDay - 1) / perDay;
			return days > MAX_DAYS ? -1 : (int) days;
		}
	}

	private Savings()
	{
	}

	/**
	 * The goal's standing.
	 *
	 * @param option the goal priced and weighed against the aimed target
	 * @param owned whether every piece is already owned
	 * @param coins coins and platinum the player holds
	 * @param worth Codex Ledger's saved net worth, or null when it is not installed or has not started
	 * @param now the time now, to read the last week of history
	 */
	public static View of(Roadmap.Option option, boolean owned, long coins, Worth worth, long now)
	{
		if (option == null)
		{
			return null;
		}
		return new View(option, owned, Math.max(0, coins), worth == null ? 0 : worth.getTotal(), worth != null, pace(worth, now));
	}

	/**
	 * Net worth gained per day across the last week: from the first reading inside the window
	 * to the latest, so one lucky drop counts for what it was rather than for every day after.
	 * Null with less than a day of history to go on.
	 */
	static Long pace(Worth worth, long now)
	{
		if (worth == null)
		{
			return null;
		}
		List<Point> points = worth.getHistory();
		Point first = null;
		Point last = null;
		for (Point p : points)
		{
			if (p == null || p.getTime() > now || now - p.getTime() > WINDOW_MS)
			{
				continue;
			}
			if (first == null || p.getTime() < first.getTime())
			{
				first = p;
			}
			if (last == null || p.getTime() > last.getTime())
			{
				last = p;
			}
		}
		if (first == null || last.getTime() - first.getTime() < MIN_SPAN_MS)
		{
			return null;
		}
		double days = (last.getTime() - first.getTime()) / (double) DAY_MS;
		return Math.round((last.getValue() - first.getValue()) / days);
	}
}
