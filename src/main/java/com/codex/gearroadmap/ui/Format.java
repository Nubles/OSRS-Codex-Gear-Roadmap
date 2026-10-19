package com.codex.gearroadmap.ui;

import java.util.Locale;

/** Coins, damage and times in the short forms the panel uses. */
public final class Format
{
	private Format()
	{
	}

	/**
	 * "850", "60.0k", "1.4M", "12.0M", "1.6B"; "Free" for nothing at all. Every figure past a thousand
	 * carries one decimal, zero or not, so prices side by side read alike ("9.9M" beside "12.0M",
	 * never "12M").
	 */
	public static String coins(long gp)
	{
		if (gp <= 0)
		{
			return "Free";
		}
		if (gp < 1_000)
		{
			return String.valueOf(gp);
		}
		// A figure that rounds up to a thousand of one unit is written in the next ("1.0M", not "1000.0k").
		if (gp < 999_950)
		{
			return fixed(gp / 1_000.0) + "k";
		}
		if (gp < 999_950_000)
		{
			return fixed(gp / 1_000_000.0) + "M";
		}
		return fixed(gp / 1_000_000_000.0) + "B";
	}

	private static String fixed(double value)
	{
		return String.format(Locale.ENGLISH, "%.1f", value);
	}

	/** Damage per second to two places. */
	public static String dps(double dps)
	{
		return String.format(Locale.ENGLISH, "%.2f", Math.max(0, dps));
	}

	/** "+0.42", for a gain. */
	public static String gain(double dps)
	{
		return "+" + dps(dps);
	}

	/** "+14%", with "<1%" for the smallest gains. */
	public static String share(double share)
	{
		if (share <= 0)
		{
			return "+0%";
		}
		if (share < 0.01)
		{
			return "+<1%";
		}
		return "+" + Math.round(share * 100) + "%";
	}

	/** Whole percent. */
	public static String percent(double share)
	{
		return Math.round(Math.max(0, Math.min(1, share)) * 100) + "%";
	}

	/** "48s", "1m 32s", "12m"; "-" when it cannot be done. */
	public static String time(double seconds)
	{
		if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds <= 0)
		{
			return "-";
		}
		long s = Math.round(seconds);
		if (s < 60)
		{
			return s + "s";
		}
		long m = s / 60;
		long r = s % 60;
		if (m >= 10 || r == 0)
		{
			return m + "m";
		}
		return m + "m " + r + "s";
	}

	/** "1h 18m", "34m": a longer stretch of time, to the minute. */
	public static String span(double seconds)
	{
		long m = Math.round(Math.max(0, seconds) / 60);
		return m >= 60 ? m / 60 + "h " + m % 60 + "m" : m + "m";
	}

	/** "+0.8%", "+12%": a share to one place below ten percent. */
	public static String fine(double share)
	{
		double pct = Math.max(0, share) * 100;
		return pct < 10 ? "+" + String.format(java.util.Locale.ENGLISH, "%.1f", pct) + "%" : "+" + Math.round(pct) + "%";
	}

	/** A slayer task's name with a capital first letter, as the game lists it: "Abyssal demons". */
	public static String task(String task)
	{
		if (task == null || task.isEmpty())
		{
			return "";
		}
		return Character.toUpperCase(task.charAt(0)) + task.substring(1);
	}

	/** What each point of DPS costs on this upgrade: "85k per DPS". */
	public static String value(long cost, double gain)
	{
		if (cost <= 0 || gain <= 0)
		{
			return "";
		}
		return coins(Math.round(cost / gain)) + " per DPS";
	}
}
