package com.codex.gearroadmap.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The road behind: each upgrade from the road the player has picked up, newest first, with the
 * damage it added, where, and when. Saved per account so the panel can show how far the road has
 * come; only the latest few are kept.
 */
public final class Claims
{
	/** Claims kept; older ones fall off the end. */
	public static final int KEPT = 12;

	/** One upgrade picked up from the road. */
	public static final class Claim
	{
		private String name;
		private int icon;
		private double gain;
		private String target;
		private long at;

		/** For Gson. */
		Claim()
		{
		}

		public Claim(String name, int icon, double gain, String target, long at)
		{
			this.name = name;
			this.icon = icon;
			this.gain = gain;
			this.target = target;
			this.at = at;
		}

		public String getName()
		{
			return name;
		}

		public int getIcon()
		{
			return icon;
		}

		/** Damage per second it added against its target when it was picked up. */
		public double getGain()
		{
			return gain;
		}

		public String getTarget()
		{
			return target;
		}

		public long getAt()
		{
			return at;
		}

		boolean isValid()
		{
			return name != null && !name.isEmpty() && target != null && at > 0 && gain >= 0 && !Double.isNaN(gain);
		}
	}

	private List<Claim> claims = new ArrayList<>();

	/**
	 * Adds a claim at the front. The same upgrade claimed again (sold and bought back) moves up
	 * rather than being listed twice.
	 */
	public void add(Claim claim)
	{
		if (claim == null || !claim.isValid())
		{
			return;
		}
		List<Claim> list = list();
		list.removeIf(c -> c.name.equals(claim.name));
		list.add(0, claim);
		while (list.size() > KEPT)
		{
			list.remove(list.size() - 1);
		}
		claims = list;
	}

	/** Newest first; a copy. Claims read back broken from an old or hand-edited save are left out. */
	public List<Claim> recent()
	{
		List<Claim> out = new ArrayList<>();
		for (Claim c : list())
		{
			if (c != null && c.isValid())
			{
				out.add(c);
			}
		}
		out.sort((a, b) -> Long.compare(b.at, a.at));
		return Collections.unmodifiableList(out);
	}

	public boolean isEmpty()
	{
		return recent().isEmpty();
	}

	private List<Claim> list()
	{
		List<Claim> out = new ArrayList<>();
		if (claims != null)
		{
			for (Claim c : claims)
			{
				if (c != null && c.isValid())
				{
					out.add(c);
				}
			}
		}
		return out;
	}
}
