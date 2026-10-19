package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Target;

/** What one loadout does to one target with its best attack style. */
public final class Result
{
	public enum Kind
	{
		MELEE("Melee"),
		RANGED("Ranged"),
		MAGIC("Magic");

		private final String label;

		Kind(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	static final Result NONE = new Result(null, 0, 0, 0, 0, null, null, null);

	private final Kind kind;
	private final double dps;
	private final int maxHit;
	private final double accuracy;
	private final int ticks;
	private final String style;
	private final String prayer;
	private final String spell;

	Result(Kind kind, double dps, int maxHit, double accuracy, int ticks, String style, String prayer, String spell)
	{
		this.kind = kind;
		this.dps = dps;
		this.maxHit = maxHit;
		this.accuracy = accuracy;
		this.ticks = ticks;
		this.style = style;
		this.prayer = prayer;
		this.spell = spell;
	}

	public Kind getKind()
	{
		return kind;
	}

	/** Expected damage per second. */
	public double getDps()
	{
		return dps;
	}

	public int getMaxHit()
	{
		return maxHit;
	}

	/** Chance each attack hits. */
	public double getAccuracy()
	{
		return accuracy;
	}

	/** Game ticks between attacks. */
	public int getTicks()
	{
		return ticks;
	}

	/** The attack style chosen, such as "Slash, aggressive" or "Rapid". */
	public String getStyle()
	{
		return style;
	}

	public String getPrayer()
	{
		return prayer;
	}

	public String getSpell()
	{
		return spell;
	}

	/** Seconds to kill the target at this rate, ignoring overkill and downtime. */
	public double secondsToKill(Target target)
	{
		return dps <= 0 ? Double.POSITIVE_INFINITY : target.getHp() / dps;
	}

	/** The same attack landing only this share of the fight, as when a phase keeps the player off the target. */
	Result timesUptime(int percent)
	{
		if (percent >= 100 || kind == null)
		{
			return this;
		}
		return new Result(kind, dps * percent / 100.0, maxHit, accuracy, ticks, style, prayer, spell);
	}

	public boolean isBetterThan(Result other)
	{
		return other == null || dps > other.dps + 1e-9;
	}
}
