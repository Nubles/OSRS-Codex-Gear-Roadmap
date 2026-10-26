package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Target;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The listed target the player is fighting right now, if any, so the roadmap can offer to aim at
 * it in one click. It never moves the aim by itself: someone fighting Vorkath may well be planning
 * for Zulrah. A fight is remembered for a few minutes after the last blow, through a bank trip.
 */
public final class Fighting
{
	/** How long a fight is offered after the player last attacked it. */
	public static final long FRESH_MS = 5 * 60_000L;
	/** Monsters the game names apart from the target that stands for them. */
	private static final String[][] ALIASES = {
		{"dusk", "grotesque guardians"},
		{"dawn", "dawn (grotesque guardians)"},
	};

	private final Map<String, Target> byKey = new HashMap<>();
	private Target target;
	private long lastAt;

	public Fighting(Collection<Target> targets)
	{
		for (Target t : targets)
		{
			byKey.put(Fights.key(t.getName()), t);
		}
		for (String[] alias : ALIASES)
		{
			Target t = byKey.get(alias[1]);
			if (t != null)
			{
				byKey.put(alias[0], t);
			}
		}
	}

	/** The listed target a monster of this name stands for, or null. */
	public Target match(String npcName)
	{
		return npcName == null ? null : byKey.get(Fights.key(npcName.replace(' ', ' ')));
	}

	/**
	 * The player is attacking a monster of this name.
	 *
	 * @return true when the target being fought changed
	 */
	public boolean saw(String npcName, long now)
	{
		Target t = match(npcName);
		if (t == null)
		{
			return false;
		}
		lastAt = now;
		if (t == target)
		{
			return false;
		}
		target = t;
		return true;
	}

	/**
	 * Lets go of a fight not seen for a while.
	 *
	 * @return true when it was let go
	 */
	public boolean expire(long now)
	{
		if (target != null && now - lastAt > FRESH_MS)
		{
			target = null;
			return true;
		}
		return false;
	}

	/** The listed target being fought, or null. */
	public Target current()
	{
		return target;
	}

	public void clear()
	{
		target = null;
		lastAt = 0;
	}
}
