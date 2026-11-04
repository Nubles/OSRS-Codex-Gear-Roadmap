package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The bosses still to finish on the collection log, for the shortlist. The log's pages come from
 * the game cache; which slots are filled comes from Codex Log Insight's saved progress, so this
 * needs that plugin to have read the log at least once.
 */
public final class LogPages
{
	/** Where Codex Log Insight keeps the slots it has seen filled, on the RuneScape profile. */
	public static final String INSIGHT_GROUP = "codexloginsight";
	public static final String INSIGHT_KEY = "progress";

	/** Log pages whose name differs from the target's. */
	private static final Map<String, List<String>> ALIASES = new HashMap<>();

	static
	{
		ALIASES.put("the fight caves", Collections.singletonList("TzTok-Jad"));
		ALIASES.put("fortis colosseum", Collections.singletonList("Sol Heredit"));
		ALIASES.put("dagannoth kings", java.util.Arrays.asList("Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme"));
		ALIASES.put("grotesque guardians", java.util.Arrays.asList("Grotesque Guardians", "Dawn (Grotesque Guardians)"));
		ALIASES.put("callisto and artio", java.util.Arrays.asList("Callisto", "Artio"));
		ALIASES.put("venenatis and spindel", java.util.Arrays.asList("Venenatis", "Spindel"));
		ALIASES.put("vet'ion and calvar'ion", Collections.singletonList("Vet'ion"));
		ALIASES.put("tormented demons", Collections.singletonList("Tormented Demon"));
	}

	private LogPages()
	{
	}

	/** Slots Log Insight has seen filled, or null when it has never saved any. */
	public static Set<Integer> readOwned(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return null;
		}
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonElement owned = root == null ? null : root.get("owned");
			if (owned == null || !owned.isJsonArray())
			{
				return null;
			}
			Set<Integer> out = new HashSet<>();
			JsonArray arr = owned.getAsJsonArray();
			for (JsonElement e : arr)
			{
				if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber())
				{
					out.add(e.getAsInt());
				}
			}
			return out;
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	/**
	 * Targets whose log page still has empty slots, the nearest to done first. Pages are by name, each
	 * with its slots; a page with no matching target is left out.
	 */
	public static List<String> unfinished(Map<String, List<Integer>> pages, Set<Integer> owned, Collection<Target> targets)
	{
		if (pages == null || owned == null)
		{
			return Collections.emptyList();
		}
		Map<String, Integer> left = new LinkedHashMap<>();
		for (Map.Entry<String, List<Integer>> page : pages.entrySet())
		{
			int missing = 0;
			for (int id : page.getValue())
			{
				if (!owned.contains(id))
				{
					missing++;
				}
			}
			if (missing == 0)
			{
				continue;
			}
			for (String name : match(page.getKey(), targets))
			{
				left.merge(name, missing, Math::min);
			}
		}
		List<String> names = new ArrayList<>(left.keySet());
		names.sort(Comparator.comparingInt(left::get));
		return names;
	}

	/** The targets a log page stands for. */
	static List<String> match(String page, Collection<Target> targets)
	{
		String p = norm(page);
		List<String> alias = ALIASES.get(page.toLowerCase(Locale.ROOT).trim());
		List<String> out = new ArrayList<>();
		for (Target t : targets)
		{
			if (alias != null ? alias.contains(t.getName()) : norm(t.getName()).equals(p))
			{
				out.add(t.getName());
			}
		}
		return out;
	}

	private static String norm(String s)
	{
		String n = s.toLowerCase(Locale.ROOT).trim();
		if (n.startsWith("the "))
		{
			n = n.substring(4);
		}
		return n.replaceAll("[^a-z]", "");
	}
}
