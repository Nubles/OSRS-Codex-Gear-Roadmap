package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The player as the formulas need them: real levels, which potions and prayers to
 * assume, which offensive prayers they have unlocked and the spellbook they can use.
 */
public final class Fighter
{
	public enum Boost
	{
		NONE("No potions"),
		POTIONS("Combat potions"),
		BEST("Best boosts");

		private final String label;

		Boost(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final Map<String, Integer> levels;
	private final Boost boost;
	private final boolean prayers;
	private final boolean piety;
	private final boolean rigour;
	private final boolean augury;
	private final boolean ancients;
	private final SlayerTask task;
	private boolean deadeye;
	private boolean vigour;
	private boolean kandarinHard;
	private boolean selfSourced;

	public Fighter(Map<String, Integer> levels, Boost boost, boolean prayers, boolean piety, boolean rigour, boolean augury,
		boolean ancients, boolean onTask)
	{
		this(levels, boost, prayers, piety, rigour, augury, ancients, onTask ? SlayerTask.ALWAYS : SlayerTask.NEVER);
	}

	public Fighter(Map<String, Integer> levels, Boost boost, boolean prayers, boolean piety, boolean rigour, boolean augury,
		boolean ancients, SlayerTask task)
	{
		this.levels = levels == null ? Collections.emptyMap() : new HashMap<>(levels);
		this.boost = boost == null ? Boost.NONE : boost;
		this.prayers = prayers;
		this.piety = piety;
		this.rigour = rigour;
		this.augury = augury;
		this.ancients = ancients;
		this.task = task == null ? SlayerTask.NEVER : task;
	}

	/**
	 * The same fighter with the prayer scrolls read from the game (Deadeye and Mystic Vigour)
	 * and whether the Kandarin hard diary is done, which makes enchanted bolts trigger more often.
	 */
	public Fighter withUnlocks(boolean deadeye, boolean vigour, boolean kandarinHard)
	{
		Fighter f = new Fighter(levels, boost, prayers, piety, rigour, augury, ancients, task);
		f.deadeye = deadeye;
		f.vigour = vigour;
		f.kandarinHard = kandarinHard;
		f.selfSourced = selfSourced;
		return f;
	}

	/**
	 * The same fighter getting their gear themselves, as an ironman does: an item is then within
	 * reach only once they have the levels its source takes, such as the Sailing to reach its island.
	 */
	public Fighter withSelfSourced(boolean self)
	{
		Fighter f = withUnlocks(deadeye, vigour, kandarinHard);
		f.selfSourced = self;
		return f;
	}

	/** Whether gear has to be got rather than bought. */
	public boolean isSelfSourced()
	{
		return selfSourced;
	}

	/** The same fighter with another slayer helmet assumption, to weigh kills made on and off a task apart. */
	public Fighter withTask(SlayerTask other)
	{
		return new Fighter(levels, boost, prayers, piety, rigour, augury, ancients, other).withUnlocks(deadeye, vigour, kandarinHard)
			.withSelfSourced(selfSourced);
	}

	/** The same fighter with one skill at another level, to weigh what training it is worth. */
	public Fighter withLevel(String skill, int level)
	{
		Map<String, Integer> raised = new HashMap<>(levels);
		raised.put(skill, level);
		return new Fighter(raised, boost, prayers, piety, rigour, augury, ancients, task).withUnlocks(deadeye, vigour, kandarinHard)
			.withSelfSourced(selfSourced);
	}

	/** A maxed account with every unlock, used for previews and tests. */
	public static Fighter maxed(Boost boost)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String skill : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Hitpoints", "Agility", "Slayer", "Sailing",
			"Smithing", "Crafting", "Mining", "Runecraft", "Fishing", "Woodcutting", "Fletching"})
		{
			levels.put(skill, 99);
		}
		return new Fighter(levels, boost, true, true, true, true, true, false).withUnlocks(true, true, true);
	}

	public int level(String skill)
	{
		Integer level = levels.get(skill);
		return level == null ? 1 : level;
	}

	public Map<String, Integer> getLevels()
	{
		return Collections.unmodifiableMap(levels);
	}

	public Boost getBoost()
	{
		return boost;
	}

	public boolean isAncients()
	{
		return ancients;
	}

	public SlayerTask getTask()
	{
		return task;
	}

	/** Whether the black mask and slayer helmet count against this target. */
	public boolean onTaskFor(Target target)
	{
		return task.covers(target);
	}

	public boolean isKandarinHard()
	{
		return kandarinHard;
	}

	/** Everything that changes the numbers besides levels and gear, for caching. */
	public String assumptions()
	{
		return boost + "/" + prayers + piety + rigour + augury + deadeye + vigour + "/" + ancients + kandarinHard + "/" + task;
	}

	/** Whether the player meets every level an item asks for. */
	public boolean meets(Map<String, Integer> requirements)
	{
		for (Map.Entry<String, Integer> r : requirements.entrySet())
		{
			if (level(r.getKey()) < r.getValue())
			{
				return false;
			}
		}
		return true;
	}

	/** The first requirement not met, as "75 Ranged", or null. */
	public String missing(Map<String, Integer> requirements)
	{
		for (Map.Entry<String, Integer> r : requirements.entrySet())
		{
			if (level(r.getKey()) < r.getValue())
			{
				return r.getValue() + " " + r.getKey();
			}
		}
		return null;
	}

	/**
	 * The first thing standing between the fighter and an item: a wield requirement as "75 Ranged",
	 * or, for a fighter who gets their own gear, a level its source takes as "47 Sailing to get",
	 * Sailing named first. Null when nothing does.
	 */
	public String missing(Map<String, Integer> requirements, Map<String, Integer> source)
	{
		String wield = missing(requirements);
		if (wield != null || !selfSourced || source == null || source.isEmpty())
		{
			return wield;
		}
		Integer sailing = source.get("Sailing");
		if (sailing != null && level("Sailing") < sailing)
		{
			return sailing + " Sailing to get";
		}
		String get = missing(source);
		return get == null ? null : get + " to get";
	}

	/** How many levels in all the fighter is short of an item's wield and, if they get their own gear, source levels. */
	public int shortfall(Map<String, Integer> requirements, Map<String, Integer> source)
	{
		Map<String, Integer> all = new HashMap<>(requirements);
		if (selfSourced && source != null)
		{
			source.forEach((k, v) -> all.merge(k, v, Math::max));
		}
		return shortfall(all);
	}

	/** How many levels in all the fighter is short of these requirements. */
	public int shortfall(Map<String, Integer> requirements)
	{
		int total = 0;
		for (Map.Entry<String, Integer> r : requirements.entrySet())
		{
			total += Math.max(0, r.getValue() - level(r.getKey()));
		}
		return total;
	}

	int attack()
	{
		return melee(level("Attack"));
	}

	int strength()
	{
		return melee(level("Strength"));
	}

	private int melee(int level)
	{
		// Super combat potion: 5 plus 15%.
		return boost == Boost.NONE ? level : level + 5 + level * 15 / 100;
	}

	int ranged()
	{
		int level = level("Ranged");
		// Ranging potion: 4 plus 10%.
		return boost == Boost.NONE ? level : level + 4 + level / 10;
	}

	int magic()
	{
		int level = level("Magic");
		switch (boost)
		{
			case POTIONS:
				// Magic potion.
				return level + 4;
			case BEST:
				// Saturated heart.
				return level + 4 + level / 10;
			default:
				return level;
		}
	}

	/** The best melee prayer as accuracy and strength percentages, with its name. */
	Prayer meleePrayer()
	{
		int prayer = level("Prayer");
		int defence = level("Defence");
		if (!prayers)
		{
			return Prayer.NONE;
		}
		if (piety && prayer >= 70 && defence >= 70)
		{
			return new Prayer("Piety", 120, 123, 0);
		}
		if (piety && prayer >= 60 && defence >= 65)
		{
			return new Prayer("Chivalry", 115, 118, 0);
		}
		int accuracy = prayer >= 34 ? 115 : prayer >= 16 ? 110 : prayer >= 7 ? 105 : 100;
		int damage = prayer >= 31 ? 115 : prayer >= 13 ? 110 : prayer >= 4 ? 105 : 100;
		if (prayer >= 34)
		{
			return new Prayer("Incredible Reflexes and Ultimate Strength", accuracy, damage, 0);
		}
		return accuracy == 100 && damage == 100 ? Prayer.NONE : new Prayer("Offensive prayers", accuracy, damage, 0);
	}

	Prayer rangedPrayer()
	{
		int prayer = level("Prayer");
		if (!prayers)
		{
			return Prayer.NONE;
		}
		if (rigour && prayer >= 74 && level("Defence") >= 70)
		{
			return new Prayer("Rigour", 120, 123, 0);
		}
		if (deadeye && prayer >= 62)
		{
			return new Prayer("Deadeye", 118, 118, 0);
		}
		if (prayer >= 44)
		{
			return new Prayer("Eagle Eye", 115, 115, 0);
		}
		if (prayer >= 26)
		{
			return new Prayer("Hawk Eye", 110, 110, 0);
		}
		return prayer >= 8 ? new Prayer("Sharp Eye", 105, 105, 0) : Prayer.NONE;
	}

	Prayer magicPrayer()
	{
		int prayer = level("Prayer");
		if (!prayers)
		{
			return Prayer.NONE;
		}
		if (augury && prayer >= 77 && level("Defence") >= 70)
		{
			return new Prayer("Augury", 125, 100, 40);
		}
		if (vigour && prayer >= 63)
		{
			return new Prayer("Mystic Vigour", 118, 100, 30);
		}
		if (prayer >= 45)
		{
			return new Prayer("Mystic Might", 115, 100, 20);
		}
		if (prayer >= 27)
		{
			return new Prayer("Mystic Lore", 110, 100, 10);
		}
		return prayer >= 9 ? new Prayer("Mystic Will", 105, 100, 0) : Prayer.NONE;
	}

	/** An offensive prayer: accuracy and strength as percentages, and magic damage in tenths of a percent. */
	static final class Prayer
	{
		static final Prayer NONE = new Prayer(null, 100, 100, 0);

		final String name;
		final int accuracy;
		final int strength;
		final int magicDamage;

		Prayer(String name, int accuracy, int strength, int magicDamage)
		{
			this.name = name;
			this.accuracy = accuracy;
			this.strength = strength;
			this.magicDamage = magicDamage;
		}
	}
}
