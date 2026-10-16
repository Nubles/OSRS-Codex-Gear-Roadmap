package com.codex.gearroadmap.data;

import java.util.Locale;

/**
 * One piece of equipment as the combat formulas see it: its slot, attack bonuses,
 * strength bonuses and, for weapons, its category and attack speed. The several ids
 * of one item (charged and uncharged, ornament kits, locked copies) share one Gear.
 */
public final class Gear
{
	public static final int HEAD = 0;
	public static final int CAPE = 1;
	public static final int NECK = 2;
	public static final int AMMO = 3;
	public static final int WEAPON = 4;
	public static final int BODY = 5;
	public static final int SHIELD = 6;
	public static final int LEGS = 7;
	public static final int HANDS = 8;
	public static final int FEET = 9;
	public static final int RING = 10;
	public static final int SLOTS = 11;

	public static final int STAB = 0;
	public static final int SLASH = 1;
	public static final int CRUSH = 2;
	public static final int MAGIC = 3;
	public static final int RANGED = 4;

	public static final String[] SLOT_NAMES = {"Head", "Cape", "Neck", "Ammo", "Weapon", "Body", "Shield", "Legs", "Hands", "Feet", "Ring"};

	// Field names follow the bundled JSON.
	private int id;
	private int[] ids;
	private String name;
	private int slot;
	private int[] o;
	private int[] p;
	private String cat;
	private int spd;
	private boolean two;
	private Integer am;

	private transient String key;

	public Gear()
	{
	}

	public Gear(int id, String name, int slot, int[] offence, int[] power, String category, int speed, boolean twoHanded)
	{
		this.id = id;
		this.name = name;
		this.slot = slot;
		this.o = offence;
		this.p = power;
		this.cat = category;
		this.spd = speed;
		this.two = twoHanded;
	}

	public int getId()
	{
		return id;
	}

	/** Every item id that counts as this piece. */
	public int[] getIds()
	{
		return ids == null || ids.length == 0 ? new int[]{id} : ids;
	}

	public String getName()
	{
		return name;
	}

	/** The lower-case name the special effects are keyed on. */
	public String key()
	{
		if (key == null)
		{
			key = name.toLowerCase(Locale.ROOT);
		}
		return key;
	}

	public int getSlot()
	{
		return slot;
	}

	/** Stab, slash, crush, magic and ranged attack bonuses. */
	public int offence(int type)
	{
		return o == null ? 0 : o[type];
	}

	public int strength()
	{
		return p == null ? 0 : p[0];
	}

	public int rangedStrength()
	{
		return p == null ? 0 : p[1];
	}

	/** Magic damage in tenths of a percent, as the equipment screen counts it times ten. */
	public int magicDamage()
	{
		return p == null ? 0 : p[2];
	}

	public String getCategory()
	{
		return cat == null ? "" : cat;
	}

	public int getSpeed()
	{
		return spd;
	}

	/** Index of the ammunition list this weapon fires, or -1 when it is not on that table. */
	public int ammoList()
	{
		return am == null ? -1 : am;
	}

	public boolean isTwoHanded()
	{
		return two;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
