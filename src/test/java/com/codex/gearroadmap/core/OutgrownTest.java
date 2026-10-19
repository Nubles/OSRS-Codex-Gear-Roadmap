package com.codex.gearroadmap.core;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class OutgrownTest
{
	private static RoadmapData data;
	private static Advisor advisor;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		advisor = new Advisor(data);
	}

	private static Set<String> names(Outgrown o)
	{
		return o.getPieces().stream().map(p -> p.getGear().getName()).collect(Collectors.toSet());
	}

	@Test
	public void gearNoBestLoadoutWearsIsOutgrown()
	{
		Fighter fighter = Fixtures.midGameFighter();
		Set<Gear> owned = new LinkedHashSet<>(Fixtures.midGame(data));
		// A rune scimitar beside a whip and a dragon scimitar helps against nothing.
		owned.add(data.named("Rune scimitar"));
		List<Roadmap.Survey> survey = advisor.survey(owned, fighter);
		Outgrown o = Outgrown.of(owned, survey, g -> advisor.planner().wieldable(g, fighter), id -> 60_000L);
		System.out.println("  outgrown: " + names(o));
		assertTrue(names(o).contains("Rune scimitar"));
		// The whip is worn somewhere, so it is not.
		assertFalse(names(o).contains("Abyssal whip"));
		// Kept for its effect, never listed even where nothing wears it.
		assertFalse(names(o).contains("Salve amulet (e)"));
		assertEquals(o.getPieces().size() * 60_000L, o.getWorth());
	}

	@Test
	public void gearToGrowIntoCheapGearAndNoSurveyAreNeverOutgrown()
	{
		Fighter fighter = Fixtures.midGameFighter();
		Set<Gear> owned = new LinkedHashSet<>(Fixtures.midGame(data));
		owned.add(data.named("Rune scimitar"));
		List<Roadmap.Survey> survey = advisor.survey(owned, fighter);
		assertTrue("too cheap to bother with", Outgrown.of(owned, survey, g -> true, id -> 20_000L).isEmpty());
		assertTrue("not yet wieldable", Outgrown.of(owned, survey, g -> false, id -> 60_000L).isEmpty());
		assertTrue("nothing surveyed", Outgrown.of(owned, Collections.emptyList(), g -> true, id -> 60_000L).isEmpty());
	}

	@Test
	public void dragonfireTankAndSpecialAttackGearAreKept()
	{
		assertTrue(Outgrown.kept(data.named("Dragonfire shield")));
		assertTrue(Outgrown.kept(data.named("Justiciar faceguard")));
		assertTrue(Outgrown.kept(data.named("Granite maul")));
		assertTrue(Outgrown.kept(data.named("Dragon claws")));
		assertFalse(Outgrown.kept(data.named("Rune scimitar")));
	}
}
