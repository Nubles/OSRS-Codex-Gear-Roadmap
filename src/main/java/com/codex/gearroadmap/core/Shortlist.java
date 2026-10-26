package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Targets the player shortlisted to plan for together, such as the bosses left to finish on the
 * collection log or the ones a slayer grind will pass through. Upgrades are then weighed across all
 * of them at once. Saved per account as a list of target names, in the order they were added.
 */
public final class Shortlist
{
	/** Targets kept on the shortlist at most; adding one more drops the oldest. */
	public static final int KEPT = 12;

	private final List<String> names;

	private Shortlist(List<String> names)
	{
		this.names = names;
	}

	public static Shortlist empty()
	{
		return new Shortlist(Collections.emptyList());
	}

	/** Reads a saved shortlist; anything unreadable is left out, and a broken save reads as empty. */
	public static Shortlist read(Gson gson, String saved)
	{
		if (saved == null || saved.isEmpty())
		{
			return empty();
		}
		List<String> names = new ArrayList<>();
		try
		{
			JsonElement root = gson.fromJson(saved, JsonElement.class);
			if (root == null || !root.isJsonArray())
			{
				return empty();
			}
			JsonArray array = root.getAsJsonArray();
			for (JsonElement e : array)
			{
				if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString())
				{
					String name = e.getAsString().trim();
					if (!name.isEmpty() && name.length() <= 80 && !names.contains(name))
					{
						names.add(name);
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			return empty();
		}
		while (names.size() > KEPT)
		{
			names.remove(0);
		}
		return new Shortlist(Collections.unmodifiableList(names));
	}

	public String write(Gson gson)
	{
		return gson.toJson(names);
	}

	public boolean contains(String name)
	{
		return names.contains(name);
	}

	public boolean isEmpty()
	{
		return names.isEmpty();
	}

	public List<String> getNames()
	{
		return names;
	}

	/** A copy with the target added, or taken off when it is already listed. */
	public Shortlist toggle(String name)
	{
		List<String> next = new ArrayList<>(names);
		if (!next.remove(name))
		{
			next.add(name);
			while (next.size() > KEPT)
			{
				next.remove(0);
			}
		}
		return new Shortlist(Collections.unmodifiableList(next));
	}

	/** The listed targets that are still on the list of targets, in shortlist order. */
	public List<Target> targets(Collection<Target> all)
	{
		List<Target> found = new ArrayList<>();
		for (String name : names)
		{
			for (Target t : all)
			{
				if (t.getName().equals(name))
				{
					found.add(t);
					break;
				}
			}
		}
		return found;
	}
}
