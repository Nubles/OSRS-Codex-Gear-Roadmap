package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Gear;
import java.util.Arrays;

/** Eleven equipment slots. Changes return a new loadout, so loadouts can be shared freely. */
public final class Loadout
{
	private static final Loadout EMPTY = new Loadout(new Gear[Gear.SLOTS]);

	private final Gear[] slots;

	private Loadout(Gear[] slots)
	{
		this.slots = slots;
	}

	public static Loadout empty()
	{
		return EMPTY;
	}

	public static Loadout of(Gear... gear)
	{
		Loadout loadout = EMPTY;
		for (Gear g : gear)
		{
			if (g != null)
			{
				loadout = loadout.with(g);
			}
		}
		return loadout;
	}

	public Gear get(int slot)
	{
		return slots[slot];
	}

	public Gear weapon()
	{
		return slots[Gear.WEAPON];
	}

	/** Wears a piece in its own slot; a two-handed weapon takes the shield off, and a shield takes a two-hander off. */
	public Loadout with(Gear gear)
	{
		Gear[] next = slots.clone();
		next[gear.getSlot()] = gear;
		if (gear.getSlot() == Gear.WEAPON && gear.isTwoHanded())
		{
			next[Gear.SHIELD] = null;
		}
		if (gear.getSlot() == Gear.SHIELD && next[Gear.WEAPON] != null && next[Gear.WEAPON].isTwoHanded())
		{
			next[Gear.WEAPON] = null;
		}
		return new Loadout(next);
	}

	public Loadout without(int slot)
	{
		Gear[] next = slots.clone();
		next[slot] = null;
		return new Loadout(next);
	}

	public boolean wears(String key)
	{
		for (Gear g : slots)
		{
			if (g != null && g.key().equals(key))
			{
				return true;
			}
		}
		return false;
	}

	public boolean wearsAny(String... keys)
	{
		for (String key : keys)
		{
			if (wears(key))
			{
				return true;
			}
		}
		return false;
	}

	public boolean contains(Gear gear)
	{
		return gear != null && slots[gear.getSlot()] == gear;
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Loadout && Arrays.equals(slots, ((Loadout) o).slots);
	}

	@Override
	public int hashCode()
	{
		return Arrays.hashCode(slots);
	}

	@Override
	public String toString()
	{
		return Arrays.toString(slots);
	}
}
