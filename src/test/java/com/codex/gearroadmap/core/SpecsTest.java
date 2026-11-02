package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/** Special attacks in the kill time, and the bosses left on the collection log. */
public class SpecsTest
{
	private static final int[] TORVA_MELEE = {26382, 21285, 19553, 26384, 22322, 26386, 22981, 13239, 11773};
	private static RoadmapData data;
	private static Planner planner;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		planner = new Planner(data);
	}

	private static Fighter fighter()
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Hitpoints", "Slayer"})
		{
			levels.put(s, 99);
		}
		return new Fighter(levels, Fighter.Boost.NONE, true, true, true, true, false, false);
	}

	private static Loadout torva()
	{
		Loadout lo = Loadout.empty();
		for (int id : TORVA_MELEE)
		{
			lo = lo.with(data.gear(id));
		}
		return lo;
	}

	private static Gear named(String name)
	{
		Gear g = data.named(name);
		assertNotNull(name, g);
		return g;
	}

	@Test
	public void warhammerShortensATankyBoss()
	{
		Target graardor = data.target("General Graardor");
		Specs specs = new Specs(data, planner);
		Specs.Spec dwh = Specs.SPECS.stream().filter(s -> s.key.equals("dragon warhammer")).findFirst().get();
		Specs.Plan p = specs.plan(dwh, named("Dragon warhammer"), torva(), fighter(), graardor, 0);
		assertNotNull(p);
		assertTrue(p.getSpecs() >= 1);
		assertTrue("saved " + p.getSaved(), p.getSaved() > 1);
		assertTrue(p.getSeconds() < p.getWithout());
	}

	@Test
	public void ownedSpecIsPickedAndBuysRankAfterIt()
	{
		List<Gear> owned = new ArrayList<>();
		for (int id : TORVA_MELEE)
		{
			owned.add(data.gear(id));
		}
		owned.add(named("Dragon dagger"));
		Target target = data.target("Vorkath");
		Map<Integer, Long> prices = new HashMap<>();
		for (Specs.Spec s : Specs.SPECS)
		{
			Gear g = data.named(s.key);
			if (g != null)
			{
				prices.put(g.getId(), 10_000_000L);
			}
		}
		Specs.View view = new Specs(data, planner).view(owned, fighter(), target, torva(), id -> prices.getOrDefault(id, 0L), true);
		assertNotNull(view.getOwned());
		assertEquals("Dragon dagger", view.getOwned().getWeapon().getName());
		for (Specs.Plan p : view.getBuy())
		{
			assertTrue(p.getSaved() > view.getOwned().getSaved());
			assertTrue(p.getCost() > 0);
		}
		Specs.View iron = new Specs(data, planner).view(owned, fighter(), target, torva(), id -> 0L, false);
		assertTrue(iron.getBuy().isEmpty());
	}

	@Test
	public void noSpecWhenTheTargetCantBeMeleed()
	{
		Target zulrah = data.target("Zulrah");
		Specs.Spec ags = Specs.SPECS.stream().filter(s -> s.key.equals("armadyl godsword")).findFirst().get();
		assertNull(new Specs(data, planner).plan(ags, named("Armadyl godsword"), torva(), fighter(), zulrah, 0));
	}

	@Test
	public void logPagesBecomeTargets()
	{
		Map<String, List<Integer>> pages = new LinkedHashMap<>();
		pages.put("Vorkath", Arrays.asList(1, 2, 3));
		pages.put("Dagannoth Kings", Arrays.asList(4, 5, 6, 7));
		pages.put("Zulrah", Arrays.asList(8));
		pages.put("Wintertodt", Arrays.asList(9));
		Set<Integer> owned = new HashSet<>(Arrays.asList(1, 2, 4, 8));
		List<String> left = LogPages.unfinished(pages, owned, data.targets());
		assertEquals(Arrays.asList("Vorkath", "Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme"), left);
		assertEquals(owned, LogPages.readOwned(new Gson(), "{\"owned\":[1,2,4,8],\"counters\":{}}"));
		assertNull(LogPages.readOwned(new Gson(), "not json"));
	}

	@Test
	public void shortlistTakesTheLogWithoutDroppingPicks()
	{
		Shortlist s = Shortlist.empty().toggle("Zulrah").addAll(Arrays.asList("Vorkath", "Zulrah", "Cerberus"));
		assertEquals(Arrays.asList("Zulrah", "Vorkath", "Cerberus"), s.getNames());
	}
}
