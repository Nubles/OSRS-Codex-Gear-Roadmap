package com.codex.gearroadmap.core;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.data.RoadmapData;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class LevelsTest
{
	private static RoadmapData data;
	private static Advisor advisor;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		advisor = new Advisor(data);
	}

	@Test
	public void theNearestLevelThatAddsDamageIsNamedWithItsWorth()
	{
		Fighter fighter = Fixtures.midGameFighter();
		Roadmap map = advisor.plan(Fixtures.midGame(data), fighter, data.target("General Graardor"), Fixtures.prices(data), 0);
		List<Levels.Step> steps = Levels.ahead(advisor.planner().calc(), map, fighter);
		assertFalse(steps.isEmpty());
		Roadmap.Step last = map.getPath().get(map.getPath().size() - 1);
		double perDps = last.getSpent() / (last.getOption().getAfter().dps() - map.getNow().dps());
		for (Levels.Step s : steps)
		{
			System.out.printf("  %s %d -> %d  +%.3f (%.1f%%) max %d %s worth %,d%n", s.getSkill(), s.getFrom(), s.getTo(), s.getGain(),
				s.getShare() * 100, s.getMaxHit(), s.isNewMaxHit(), s.getWorth());
			assertTrue(s.getTo() > s.getFrom() && s.getTo() <= s.getFrom() + Levels.AHEAD);
			assertTrue(s.getShare() >= Levels.MIN_SHARE);
			assertEquals(s.getGain() * perDps, s.getWorth(), 1);
			// Nothing short of that level adds as much.
			Fighter shy = fighter.withLevel(s.getSkill(), s.getTo() - 1);
			double short1 = advisor.planner().calc().best(map.getNow().best().getLoadout(), shy, map.getTarget(),
				map.getNow().best().getResult().getKind()).getDps();
			assertTrue(short1 - map.getNow().dps() < map.getNow().dps() * Levels.MIN_SHARE);
		}
	}

	@Test
	public void maxedSkillsHaveNothingAhead()
	{
		Fighter maxed = Fighter.maxed(Fighter.Boost.POTIONS);
		Roadmap map = advisor.plan(Fixtures.midGame(data), maxed, data.target("General Graardor"), Fixtures.prices(data), 0);
		assertTrue(Levels.ahead(advisor.planner().calc(), map, maxed).isEmpty());
	}
}
