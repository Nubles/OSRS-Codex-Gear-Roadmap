package com.codex.gearroadmap.core;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class AdvisorTest
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
	public void everyCatalogueItemResolves()
	{
		assertEquals(200, data.catalogue().size());
		assertTrue(data.targets().size() > 60);
	}

	@Test
	public void roadmapClimbs()
	{
		Set<Gear> owned = Fixtures.midGame(data);
		Fighter fighter = Fixtures.midGameFighter();
		Target vorkath = data.target("Vorkath");
		Roadmap map = advisor.plan(owned, fighter, vorkath, Fixtures.prices(data), 42_000_000);
		System.out.println("Now " + map.getNow().best().getResult().getKind() + " " + map.getNow().dps() + " "
			+ map.getNow().best().getLoadout());
		double previous = map.getNow().dps();
		long spent = 0;
		for (Roadmap.Step step : map.getPath())
		{
			Roadmap.Option o = step.getOption();
			System.out.printf("  %-28s %,13d  +%.3f  -> %.3f  %s%n", o.getName(), o.getCost(), o.getGain(), o.getAfter().dps(),
				o.getAfter().getResult().getKind());
			assertTrue(o.getAfter().dps() > previous);
			assertTrue(step.getSpent() > spent);
			previous = o.getAfter().dps();
			spent = step.getSpent();
		}
		for (Roadmap.Option o : map.getFree())
		{
			System.out.printf("  free %-23s +%.3f %s%n", o.getName(), o.getGain(), o.getEarn());
			assertEquals(0, o.getCost());
		}
		for (Roadmap.Option o : map.getLocked())
		{
			System.out.printf("  locked %-21s +%.3f %s%n", o.getName(), o.getGain(), o.getMissing());
			assertFalse(o.getMissing().isEmpty());
		}
		for (Roadmap.Option o : map.getLeaps())
		{
			System.out.printf("  leap %-23s %,13d +%.3f%n", o.getName(), o.getCost(), o.getGain());
		}
		assertFalse(map.getPath().isEmpty());
	}

	@Test
	public void piecesThatOnlyHelpTogetherAreBoughtAsAPair()
	{
		// With a rapier at Vorkath neither a +3 strength helm nor +1 strength legs moves the max hit
		// alone, but together they do, and that pair is far better value than a 25M fang.
		Set<Gear> owned = Fixtures.midGame(data);
		owned.add(data.named("Ghrazi rapier"));
		Roadmap map = advisor.plan(owned, Fixtures.midGameFighter(), data.target("Vorkath"), Fixtures.prices(data), 0);
		Roadmap.Option first = map.getPath().get(0).getOption();
		assertEquals(2, first.getPieces().size());
		assertTrue(first.getName(), first.getName().contains(" + "));
		assertTrue(first.getGain() > 0.1);
	}

	@Test
	public void aNewAccountGetsAStarterRoad()
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String skill : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Agility"})
		{
			levels.put(skill, 30);
		}
		levels.put("Hitpoints", 30);
		Fighter fresh = new Fighter(levels, Fighter.Boost.NONE, true, true, false, false, false, false);
		Roadmap map = advisor.plan(new HashSet<>(), fresh, data.target("Hill Giant"), Fixtures.prices(data), 0);
		assertFalse(map.getPath().isEmpty());
		assertTrue(map.getPath().get(0).getOption().getCost() < 10_000);
		boolean dragonGloves = false;
		boolean barrowsGloves = false;
		for (Roadmap.Option o : map.getFree())
		{
			dragonGloves |= o.getName().equals("Dragon gloves");
			barrowsGloves |= o.getName().equals("Barrows gloves");
		}
		assertTrue(barrowsGloves);
		assertFalse("one Recipe for Disaster reward is enough", dragonGloves);
		Map<String, Integer> fang = Collections.singletonMap("Attack", 82);
		assertEquals(52, fresh.shortfall(fang));
	}

	@Test
	public void newCoinsMoveOnlyTheReach()
	{
		Roadmap map = advisor.plan(Fixtures.midGame(data), Fixtures.midGameFighter(), data.target("Vorkath"),
			Fixtures.prices(data), 0);
		assertFalse(map.getPath().get(0).isAffordable());
		long second = map.getPath().get(1).getSpent();
		Roadmap richer = map.withCoins(second);
		assertEquals(map.getPath().size(), richer.getPath().size());
		assertTrue(richer.getPath().get(0).isAffordable());
		assertTrue(richer.getPath().get(1).isAffordable());
		assertFalse(richer.getPath().get(2).isAffordable());
		assertEquals(map.getLeaps(), richer.getLeaps());
		assertEquals(second, richer.getCoins());
	}

	@Test
	public void ownedUpgradesAreNotSuggested()
	{
		Set<Gear> owned = Fixtures.midGame(data);
		Roadmap map = advisor.plan(owned, Fixtures.midGameFighter(), data.target("General Graardor"), Fixtures.prices(data), 0);
		for (Roadmap.Step step : map.getPath())
		{
			for (Gear g : step.getOption().getPieces())
			{
				assertFalse(g.getName(), owned.contains(g));
			}
			assertFalse(step.isAffordable());
		}
		assertNull(map.getPath().isEmpty() ? "empty" : null);
	}

	@Test
	public void vorkathRoadDoesNotSwapToMeleeForACheapSword()
	{
		// Vorkath's acid phase and fire bombs keep a melee player walking while a ranger keeps shooting.
		// Before melee counted at 80% there, a 60k dragon sword with the salve (e) out-damaged the
		// blowpipe on paper, and the whole road turned into melee gear.
		Target vorkath = data.target("Vorkath");
		assertEquals(80, vorkath.uptime("Melee"));
		assertEquals(100, vorkath.uptime("Ranged"));
		assertEquals(100, data.target("General Graardor").uptime("Melee"));
		Roadmap map = advisor.plan(Fixtures.midGame(data), Fixtures.midGameFighter(), vorkath, Fixtures.prices(data), 0);
		double previous = map.getNow().dps();
		for (Roadmap.Step step : map.getPath())
		{
			Roadmap.Option o = step.getOption();
			assertFalse(o.getName().contains("Dragon sword"));
			// Each step builds on the loadout the step before showed, so no gain is counted twice.
			assertEquals(o.getName(), previous + o.getGain(), o.getAfter().dps(), 1e-9);
			assertTrue(o.getName(), o.getGain() > 0);
			previous = o.getAfter().dps();
		}

		// The melee figure itself is the formula's damage times the uptime.
		Set<Gear> owned = Fixtures.midGame(data);
		owned.add(data.named("Dragon sword"));
		Planner planner = new Planner(data);
		double atVorkath = planner.optimise(owned, Fixtures.midGameFighter(), vorkath).get(Result.Kind.MELEE).dps();
		Target twin = new Target("Twin", "Boss", vorkath.getHp(), vorkath.getDefence(), vorkath.getMagic(),
			new int[]{26, 108, 108, 240, 26, 26, 26}, 7, vorkath.getAttributes(), true);
		double full = planner.optimise(owned, Fixtures.midGameFighter(), twin).get(Result.Kind.MELEE).dps();
		assertEquals(full * 0.8, atVorkath, 1e-9);
	}
}
