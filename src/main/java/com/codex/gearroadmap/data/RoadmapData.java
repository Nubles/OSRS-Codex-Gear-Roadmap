package com.codex.gearroadmap.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The bundled tables: every combat-relevant piece of equipment, the catalogue of
 * upgrades worth recommending, the attack styles of each weapon category and the
 * monsters a roadmap can aim at.
 */
public final class RoadmapData
{
	private static final String RESOURCE = "/com/codex/gearroadmap/gear-roadmap.json";

	/** What a weapon category offers: an attack type ("stab", "ranged", "magic"...) and a stance. */
	public static final class Style
	{
		private final String type;
		private final String stance;

		public Style(String type, String stance)
		{
			this.type = type;
			this.stance = stance;
		}

		public String getType()
		{
			return type;
		}

		public String getStance()
		{
			return stance;
		}
	}

	private static final class Raw
	{
		Map<String, List<List<String>>> styles;
		List<List<Integer>> ammo;
		List<Gear> gear;
		List<CatalogueEntry> catalogue;
		List<Target> targets;
	}

	private final Map<Integer, Gear> byId = new HashMap<>();
	private final Map<String, Gear> byName = new HashMap<>();
	private final Map<String, List<Style>> styles = new HashMap<>();
	private final Map<String, CatalogueEntry> catalogue = new LinkedHashMap<>();
	private final List<Target> targets;
	private final List<Gear> gear;
	private final List<Set<Integer>> ammo = new ArrayList<>();

	private RoadmapData(Raw raw)
	{
		gear = raw.gear == null ? Collections.emptyList() : raw.gear;
		for (Gear g : gear)
		{
			byName.put(g.key(), g);
			for (int id : g.getIds())
			{
				byId.putIfAbsent(id, g);
			}
		}
		if (raw.styles != null)
		{
			for (Map.Entry<String, List<List<String>>> e : raw.styles.entrySet())
			{
				List<Style> list = new ArrayList<>();
				for (List<String> pair : e.getValue())
				{
					if (pair.size() >= 2)
					{
						list.add(new Style(pair.get(0), pair.get(1)));
					}
				}
				styles.put(e.getKey(), list);
			}
		}
		if (raw.catalogue != null)
		{
			for (CatalogueEntry entry : raw.catalogue)
			{
				Gear g = byName.get(entry.getName().toLowerCase(Locale.ROOT));
				if (g != null)
				{
					entry.setGear(g);
					catalogue.put(g.key(), entry);
				}
			}
		}
		if (raw.ammo != null)
		{
			for (List<Integer> list : raw.ammo)
			{
				ammo.add(list == null ? Collections.emptySet() : new HashSet<>(list));
			}
		}
		targets = raw.targets == null ? Collections.emptyList() : raw.targets;
	}

	public static RoadmapData load(Gson gson) throws IOException
	{
		try (InputStream in = RoadmapData.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IOException("Missing " + RESOURCE);
			}
			return read(gson, new InputStreamReader(in, StandardCharsets.UTF_8));
		}
	}

	public static RoadmapData read(Gson gson, Reader reader) throws IOException
	{
		try
		{
			Raw raw = gson.fromJson(reader, Raw.class);
			if (raw == null)
			{
				throw new IOException("Empty gear data");
			}
			return new RoadmapData(raw);
		}
		catch (JsonParseException e)
		{
			throw new IOException(e);
		}
	}

	/** The piece an item id counts as, or null when it is not combat gear. */
	public Gear gear(int itemId)
	{
		return byId.get(itemId);
	}

	/**
	 * The ammunition ids a weapon fires: null when the weapon is not a ranged weapon that
	 * cares, empty when it fires no ammunition (a blowpipe or crystal bow).
	 */
	public Set<Integer> ammoFor(Gear weapon)
	{
		int list = weapon == null ? -1 : weapon.ammoList();
		return list < 0 || list >= ammo.size() ? null : ammo.get(list);
	}

	public Gear named(String name)
	{
		return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
	}

	public List<Gear> allGear()
	{
		return gear;
	}

	public List<Style> styles(String category)
	{
		List<Style> list = styles.get(category);
		return list == null ? styles.getOrDefault("Unarmed", Collections.emptyList()) : list;
	}

	public CatalogueEntry entry(Gear gear)
	{
		return gear == null ? null : catalogue.get(gear.key());
	}

	public List<CatalogueEntry> catalogue()
	{
		return new ArrayList<>(catalogue.values());
	}

	public List<Target> targets()
	{
		return targets;
	}

	public Target target(String name)
	{
		for (Target t : targets)
		{
			if (t.getName().equalsIgnoreCase(name))
			{
				return t;
			}
		}
		return null;
	}
}
