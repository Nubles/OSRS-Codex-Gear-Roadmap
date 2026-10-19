package com.codex.gearroadmap.data;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A monster the player can aim their gear at, with the stats that defend against each attack. */
public final class Target
{
	public static final int D_STAB = 0;
	public static final int D_SLASH = 1;
	public static final int D_CRUSH = 2;
	public static final int D_MAGIC = 3;
	public static final int D_LIGHT = 4;
	public static final int D_STANDARD = 5;
	public static final int D_HEAVY = 6;

	private int id;
	private String name;
	private String group;
	private int hp;
	private int def;
	private int mag;
	private int mb;
	private int[] d;
	private int size;
	private List<String> attr;
	private boolean slayer;
	private List<Object> weak;
	private List<String> only;
	private boolean wild;
	private int fa;
	private Map<String, Integer> up;

	public Target()
	{
	}

	public Target(String name, String group, int hp, int def, int magic, int[] defensive, int size, List<String> attributes, boolean slayer)
	{
		this.name = name;
		this.group = group;
		this.hp = hp;
		this.def = def;
		this.mag = magic;
		this.d = defensive;
		this.size = size;
		this.attr = attributes;
		this.slayer = slayer;
	}

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	/** Boss, Slayer or Training. */
	public String getGroup()
	{
		return group;
	}

	public int getHp()
	{
		return hp;
	}

	public int getDefence()
	{
		return def;
	}

	public int getMagic()
	{
		return mag;
	}

	public int getMagicAttack()
	{
		return mb;
	}

	public int defensive(int type)
	{
		return d == null ? 0 : d[type];
	}

	public int getSize()
	{
		return Math.max(1, size);
	}

	public boolean is(String attribute)
	{
		return attr != null && attr.contains(attribute);
	}

	public List<String> getAttributes()
	{
		return attr == null ? Collections.emptyList() : attr;
	}

	public boolean isSlayer()
	{
		return slayer;
	}

	/** Whether it is fought in the Wilderness, where revenant weapons are stronger. */
	public boolean isWilderness()
	{
		return wild;
	}

	/** Armour taken off every melee and ranged hit that lands, or added when below zero. */
	public int getFlatArmour()
	{
		return fa;
	}

	/** Percent of a demonbane weapon's bonus this demon takes: Duke Sucellus resists, Yama is weak to it. */
	public int demonbaneVulnerability()
	{
		if ("Duke Sucellus".equals(name))
		{
			return 70;
		}
		return "Yama".equals(name) ? 120 : 100;
	}

	/** Bosses, Slayer creatures or training spots. */
	public boolean isBoss()
	{
		return "Boss".equals(group);
	}

	/** The element this monster takes extra magic damage from, or null. */
	public String weakness()
	{
		return weak == null || weak.size() < 2 || weak.get(0) == null ? null : weak.get(0).toString().toLowerCase(Locale.ROOT);
	}

	/** Percent of extra accuracy and damage the weak element gets. */
	public int weaknessSeverity()
	{
		if (weak == null || weak.size() < 2 || !(weak.get(1) instanceof Number))
		{
			return 0;
		}
		return ((Number) weak.get(1)).intValue();
	}

	@Override
	public String toString()
	{
		return name;
	}

	/**
	 * Percent of the fight this kind of attack can keep hitting, against what the damage formulas assume.
	 * Vorkath's acid phase and fire bombs keep a melee player walking and off him, while ranged and magic
	 * attack through them, so his melee damage counts at 80%. Every other target is 100.
	 */
	public int uptime(String kind)
	{
		Integer pct = up == null ? null : up.get(kind.toLowerCase(Locale.ROOT));
		return pct == null ? 100 : Math.max(1, Math.min(100, pct));
	}

	/** Whether the target can be hurt with this kind of attack: melee, ranged or magic. */
	public boolean allows(String kind)
	{
		return only == null || only.contains(kind.toLowerCase(Locale.ROOT));
	}
}
