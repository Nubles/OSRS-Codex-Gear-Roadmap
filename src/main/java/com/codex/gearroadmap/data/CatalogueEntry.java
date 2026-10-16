package com.codex.gearroadmap.data;

import java.util.Collections;
import java.util.Map;

/**
 * An item the roadmap may recommend: what it takes to wield, and how it is had,
 * either bought (the item itself or the tradeable parts it is made from) or earned.
 */
public final class CatalogueEntry
{
	private String name;
	private Map<String, Integer> req;
	private int[] build;
	private String earn;
	private String note;

	private transient Gear gear;

	public CatalogueEntry()
	{
	}

	public CatalogueEntry(Gear gear, Map<String, Integer> requirements, int[] build, String earn, String note)
	{
		this.name = gear.getName();
		this.gear = gear;
		this.req = requirements;
		this.build = build;
		this.earn = earn;
		this.note = note;
	}

	public String getName()
	{
		return name;
	}

	public Gear getGear()
	{
		return gear;
	}

	void setGear(Gear gear)
	{
		this.gear = gear;
	}

	/** Skill name to the level needed to wield it. */
	public Map<String, Integer> getRequirements()
	{
		return req == null ? Collections.emptyMap() : req;
	}

	/** Tradeable parts bought to make it, or null when the item itself is bought. */
	public int[] getBuild()
	{
		return build;
	}

	/** Where it is earned, for items that cannot be bought. */
	public String getEarn()
	{
		return earn;
	}

	public boolean isEarned()
	{
		return earn != null;
	}

	public String getNote()
	{
		return note;
	}
}
