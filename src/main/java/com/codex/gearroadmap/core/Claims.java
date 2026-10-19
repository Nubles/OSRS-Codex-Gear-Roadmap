package com.codex.gearroadmap.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
	static final double MAX_GAIN = 100;
	/** Far enough ahead for any clock, short of a date no claim could have. */
	static final long MAX_AT = 4_102_444_800_000L;

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
			// No upgrade adds a hundred damage a second; a figure past it, or a time to come, is a damaged save.
			return name != null && !name.isEmpty() && target != null && at > 0 && at < MAX_AT && gain >= 0 && gain < MAX_GAIN;
		}
	}

	private List<Claim> claims = new ArrayList<>();

	/**
	 * The saved road behind, or none when it cannot be read. A claim that cannot be read is left out
	 * on its own, so the others are kept (and saved again with the next claim).
	 */
	public static Claims read(Gson gson, String json)
	{
		JsonObject o = Saved.object(gson, json);
		Claims read = new Claims();
		for (Claim c : Saved.list(gson, o == null ? null : o.get("claims"), Claim.class))
		{
			if (c.isValid())
			{
				read.claims.add(c);
			}
		}
		read.claims.sort((a, b) -> Long.compare(b.at, a.at));
		while (read.claims.size() > KEPT)
		{
			read.claims.remove(read.claims.size() - 1);
		}
		return read;
	}

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
