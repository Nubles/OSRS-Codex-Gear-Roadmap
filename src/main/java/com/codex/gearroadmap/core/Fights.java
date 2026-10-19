package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Where the player really fights, from the sessions Codex Session Chronicle files in its journal,
 * and which upgrades would save them the most fighting time there. Chronicle's journal is read,
 * never written; without it there is nothing here and the rest of the plugin is unchanged.
 */
public final class Fights
{
	/** Days of sessions counted. */
	public static final int DAYS = 30;
	private static final int ON = 0;
	private static final int OFF = 1;
	/** Fought targets weighed at most, the ones that took the most fighting time first. */
	static final int TARGETS = 6;
	/** Upgrades offered at most. */
	static final int PICKS = 5;
	/** Fighting time an upgrade must save over a full window to be offered at all; less over a shorter one. */
	static final double MIN_SECONDS = 10 * 60;

	private Fights()
	{
	}

	/**
	 * One session in Chronicle's journal as it saves it (group "codexsessionchronicle", key "journal",
	 * a list of these; key "current" holds one): when it began and its kill lines by key.
	 */
	public static final class Logged
	{
		long startedAt;
		Map<String, Line> kills;

		Logged()
		{
		}

		Logged(long startedAt, Map<String, Line> kills)
		{
			this.startedAt = startedAt;
			this.kills = kills;
		}
	}

	/** One of Chronicle's kill lines, counted the way Chronicle counts it. */
	public static final class Line
	{
		String name;
		int looted;
		int firstKc;
		int lastKc;
		Map<String, int[]> counters;
		/** True for loot from something opened rather than killed; null in older saves. */
		Boolean event;
		/**
		 * Kills looted while a slayer task was assigned, by the task's name, and under "" those looted
		 * with no task; null in sessions Chronicle saved before it kept them.
		 */
		Map<String, Integer> tasks;

		Line()
		{
		}

		Line(String name, int looted)
		{
			this.name = name;
			this.looted = looted;
		}

		int count()
		{
			int counted = 0;
			if (counters != null)
			{
				for (int[] range : counters.values())
				{
					if (range != null && range.length == 2)
					{
						counted += range[1] - range[0] + 1;
					}
				}
			}
			else if (firstKc > 0)
			{
				counted = lastKc - firstKc + 1;
			}
			return Math.max(looted, counted);
		}

		boolean isKill()
		{
			return firstKc > 0 || counters != null && !counters.isEmpty() || !Boolean.TRUE.equals(event);
		}
	}

	/** Kills of each target over the last {@link #DAYS} days, most fighting first. */
	public static final class Tally
	{
		private final Map<Target, Integer> kills;
		private final Map<Target, int[]> split;
		private final int sessions;
		private final int days;

		Tally(Map<Target, Integer> kills, int sessions)
		{
			this(kills, Collections.emptyMap(), sessions, DAYS);
		}

		Tally(Map<Target, Integer> kills, Map<Target, int[]> split, int sessions, int days)
		{
			this.kills = kills;
			this.split = split;
			this.sessions = sessions;
			this.days = days;
		}

		/** Kills of a target made on a slayer task that covers it. */
		public int onTask(Target t)
		{
			int[] s = split.get(t);
			return s == null ? 0 : s[ON];
		}

		/** Kills of a target made with no task for it, as Chronicle saw. */
		public int offTask(Target t)
		{
			int[] s = split.get(t);
			return s == null ? 0 : s[OFF];
		}

		/** Kills of a target from before Chronicle kept the slayer task, or counted without loot. */
		public int unknown(Target t)
		{
			Integer all = kills.get(t);
			return all == null ? 0 : Math.max(0, all - onTask(t) - offTask(t));
		}

		/** Days the kills were counted over: fewer than {@link #DAYS} while the journal is younger than that. */
		public int getDays()
		{
			return days;
		}

		public Map<Target, Integer> getKills()
		{
			return kills;
		}

		/** Sessions in the window that had a kill of a target on the list. */
		public int getSessions()
		{
			return sessions;
		}

		public boolean isEmpty()
		{
			return kills.isEmpty();
		}

		/** A short key that changes when the tally does, for caching. */
		public String key()
		{
			StringBuilder b = new StringBuilder();
			kills.forEach((t, n) -> b.append(t.getName()).append('=').append(n).append(';'));
			return b.toString();
		}
	}

	/** An upgrade weighed across where the player fights. */
	public static final class Pick
	{
		private final String name;
		private final Gear icon;
		private final long cost;
		private final double seconds;
		private final List<String> where;

		Pick(String name, Gear icon, long cost, double seconds, List<String> where)
		{
			this.name = name;
			this.icon = icon;
			this.cost = cost;
			this.seconds = seconds;
			this.where = where;
		}

		public String getName()
		{
			return name;
		}

		public Gear getIcon()
		{
			return icon;
		}

		public long getCost()
		{
			return cost;
		}

		/** Fighting time it would have saved over the tally's days, in seconds. */
		public double getSeconds()
		{
			return seconds;
		}

		/** The fought targets it saves the most time at, most first. */
		public List<String> getWhere()
		{
			return where;
		}
	}

	/**
	 * Counts the kills of every target on the list in sessions begun in the last {@link #DAYS} days.
	 * Loot from something opened rather than killed is left out, as Chronicle leaves it out. The first
	 * of two sessions begun at the same moment is kept, so pass the journal before the live session.
	 */
	public static Tally tally(Collection<Logged> sessions, Collection<Target> targets, long now)
	{
		Map<String, Target> byKey = new HashMap<>();
		for (Target t : targets)
		{
			byKey.put(key(t.getName()), t);
		}
		long from = now - DAYS * 86_400_000L;
		Map<Target, Integer> kills = new HashMap<>();
		Map<Target, int[]> split = new HashMap<>();
		int counted = 0;
		long earliest = now;
		// A session Chronicle has just filed can still be its live one for a moment: count it once.
		Set<Long> seen = new HashSet<>();
		for (Logged s : sessions)
		{
			if (s == null || s.kills == null || s.startedAt < from || s.startedAt > now || !seen.add(s.startedAt))
			{
				continue;
			}
			boolean any = false;
			for (Map.Entry<String, Line> e : s.kills.entrySet())
			{
				Line line = e.getValue();
				if (line == null || !line.isKill())
				{
					continue;
				}
				// Chronicle's key is the folded name (Dusk files under "grotesque guardians"); the line's own
				// name is only the first one the game gave it.
				Target t = e.getKey() == null ? null : byKey.get(key(e.getKey()));
				if (t == null && line.name != null)
				{
					t = byKey.get(key(line.name));
				}
				int n = line.count();
				if (t != null && n > 0)
				{
					kills.merge(t, n, Integer::sum);
					any = true;
					if (line.tasks != null)
					{
						int[] s2 = split.computeIfAbsent(t, k -> new int[2]);
						int known = 0;
						for (Map.Entry<String, Integer> task : line.tasks.entrySet())
						{
							int k = task.getValue() == null ? 0 : Math.max(0, Math.min(task.getValue(), n - known));
							known += k;
							boolean covered = task.getKey() != null && !task.getKey().isEmpty()
								&& SlayerTask.taskCovers(task.getKey(), t.getName());
							s2[covered ? ON : OFF] += k;
						}
					}
				}
			}
			counted += any ? 1 : 0;
			earliest = any ? Math.min(earliest, s.startedAt) : earliest;
		}
		List<Map.Entry<Target, Integer>> list = new ArrayList<>(kills.entrySet());
		// Most fighting first: kills times hitpoints stands in for the time each took.
		list.sort((a, b) -> Long.compare((long) b.getValue() * b.getKey().getHp(), (long) a.getValue() * a.getKey().getHp()));
		Map<Target, Integer> sorted = new LinkedHashMap<>();
		for (Map.Entry<Target, Integer> e : list)
		{
			sorted.put(e.getKey(), e.getValue());
		}
		// A journal begun three days ago tells of three days, not a month.
		int days = (int) Math.max(1, Math.min(DAYS, (now - earliest + 86_399_999L) / 86_400_000L));
		return new Tally(Collections.unmodifiableMap(sorted), split, counted, days);
	}

	/** Names as Chronicle files them and targets as the list names them, folded alike. */
	static String key(String name)
	{
		String key = name.toLowerCase(Locale.ROOT).trim();
		if (key.startsWith("the "))
		{
			key = key.substring(4);
		}
		return key;
	}

	/** Seconds of fighting the kills take at this damage per second. */
	static double seconds(int kills, Target t, double dps)
	{
		return dps <= 0 ? Double.POSITIVE_INFINITY : kills * t.getHp() / dps;
	}
}
