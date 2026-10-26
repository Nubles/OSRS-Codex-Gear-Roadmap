package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class ShortlistTest
{
	private static final Gson GSON = new Gson();
	private static RoadmapData data;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(GSON);
	}

	@Test
	public void togglesAndKeepsItsOrderThroughASave()
	{
		Shortlist list = Shortlist.empty().toggle("Vorkath").toggle("Zulrah").toggle("Cerberus").toggle("Zulrah");
		assertEquals(Arrays.asList("Vorkath", "Cerberus"), list.getNames());
		assertEquals(list.getNames(), Shortlist.read(GSON, list.write(GSON)).getNames());
	}

	@Test
	public void dropsTheOldestPastTheLimit()
	{
		Shortlist list = Shortlist.empty();
		for (int i = 0; i <= Shortlist.KEPT; i++)
		{
			list = list.toggle("Target " + i);
		}
		assertEquals(Shortlist.KEPT, list.getNames().size());
		assertFalse(list.contains("Target 0"));
	}

	@Test
	public void aDamagedSaveReadsAsWhatCanBeRead()
	{
		for (String broken : new String[]{null, "", "garbage", "{}", "42", "[", "{\"a\":1}"})
		{
			assertTrue(broken, Shortlist.read(GSON, broken).isEmpty());
		}
		assertEquals(Collections.singletonList("Vorkath"), Shortlist.read(GSON, "[\"Vorkath\", 3, null, {}, \"Vorkath\", \" \"]").getNames());
	}

	@Test
	public void forgetsTargetsNoLongerListed()
	{
		List<Target> found = Shortlist.read(GSON, "[\"Vorkath\",\"Gone boss\",\"Zulrah\"]").targets(data.targets());
		assertEquals(2, found.size());
		assertEquals("Vorkath", found.get(0).getName());
	}

	@Test
	public void weighsUpgradesAcrossTheShortlist()
	{
		Advisor advisor = new Advisor(data);
		java.util.Map<String, Integer> levels = new java.util.HashMap<>();
		for (String skill : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Hitpoints"})
		{
			levels.put(skill, 80);
		}
		Fighter f = new Fighter(levels, Fighter.Boost.POTIONS, true, true, true, true, true, true);
		List<Target> targets = Shortlist.empty().toggle("General Graardor").toggle("Vorkath").targets(data.targets());
		List<Fights.Pick> picks = advisor.shortlist(Collections.singleton(data.gear(4151)), f, targets, id -> 1_000_000L);
		assertFalse(picks.isEmpty());
		for (Fights.Pick p : picks)
		{
			assertTrue(p.getSeconds() > 0);
			assertFalse(p.getWhere().isEmpty());
		}
		assertTrue(advisor.shortlist(Collections.emptySet(), f, Collections.emptyList(), id -> 1L).isEmpty());
	}
}
