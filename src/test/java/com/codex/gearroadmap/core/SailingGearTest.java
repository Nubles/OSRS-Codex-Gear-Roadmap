package com.codex.gearroadmap.core;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.data.CatalogueEntry;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Sailing's combat gear: its wield requirements and what it takes to get yourself, as the OSRS
 * Wiki gives them, the darts its blowpipes are counted with, and an ironman's road waiting on
 * the Sailing level an item's source takes while a main simply buys it.
 */
public class SailingGearTest
{
	/** Name, item id, wield requirements, then the levels it takes to get one yourself. */
	private static final Object[][] WIKI = {
		{"Belle's folly", 31248, req("Attack", 65), req("Sailing", 45, "Slayer", 51, "Smithing", 70)},
		{"Hallowfell", 34027, req("Attack", 75), req("Sailing", 62, "Crafting", 60, "Mining", 53, "Runecraft", 47)},
		{"Camphor blowpipe", 31575, req("Ranged", 45), req("Sailing", 47, "Fishing", 52, "Woodcutting", 66, "Fletching", 58)},
		{"Ironwood blowpipe", 31579, req("Ranged", 55), req("Sailing", 72, "Fishing", 52, "Woodcutting", 80, "Fletching", 72)},
		{"Rosewood blowpipe", 31583, req("Ranged", 65), req("Sailing", 79, "Fishing", 52, "Woodcutting", 92, "Fletching", 84)},
		{"Aquanite hopper", 32879, req("Ranged", 60), req("Sailing", 73, "Slayer", 78, "Smithing", 60)},
	};

	/** Rough prices for the test account only; the plugin reads live Grand Exchange prices. */
	private static final Object[][] PRICES = {
		{"Belle's folly", 9_000_000L}, {"Hallowfell", 30_000_000L}, {"Camphor blowpipe", 200_000L},
		{"Ironwood blowpipe", 1_000_000L}, {"Rosewood blowpipe", 4_000_000L}, {"Aquanite hopper", 5_000_000L},
	};

	private static RoadmapData data;
	private static Calc calc;
	private static Advisor advisor;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		calc = new Calc(data);
		advisor = new Advisor(data);
	}

	private static Map<String, Integer> req(Object... pairs)
	{
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			map.put((String) pairs[i], (Integer) pairs[i + 1]);
		}
		return map;
	}

	@Test
	public void catalogueCarriesTheWikisRequirements()
	{
		for (Object[] row : WIKI)
		{
			Gear g = data.named((String) row[0]);
			assertNotNull((String) row[0], g);
			assertTrue(row[0] + " has id " + row[1], Arrays.stream(g.getIds()).anyMatch(id -> id == (Integer) row[1]));
			CatalogueEntry entry = data.entry(g);
			assertNotNull(row[0] + " is in the catalogue", entry);
			assertEquals((String) row[0], row[2], new HashMap<>(entry.getRequirements()));
			assertEquals((String) row[0], row[3], new HashMap<>(entry.getSource()));
			assertNull(row[0] + " is bought, not earned", entry.getEarn());
			assertTrue(row[0] + " says it comes from Sailing", entry.getNote().contains("Sailing"));
		}
	}

	@Test
	public void woodenBlowpipesFireTheBestDartTheyTake()
	{
		// Mithril darts +9, adamant +17, rune +26 ranged strength (OSRS Wiki).
		assertEquals(35, Calc.dartStrength("toxic blowpipe"));
		assertEquals(9, Calc.dartStrength("camphor blowpipe"));
		assertEquals(17, Calc.dartStrength("ironwood blowpipe"));
		assertEquals(26, Calc.dartStrength("rosewood blowpipe"));
		assertEquals(0, Calc.dartStrength("magic shortbow"));

		Map<String, Integer> levels = new HashMap<>();
		levels.put("Ranged", 99);
		Fighter fighter = new Fighter(levels, Fighter.Boost.NONE, false, false, false, false, false, false);
		// Blowpipe ranged strength (+2, +4, +6) plus its darts, through the wiki's max hit formula.
		int[][] cases = {{31575, 2 + 9}, {31579, 4 + 17}, {31583, 6 + 26}};
		for (int[] c : cases)
		{
			Gear pipe = data.gear(c[0]);
			Result r = calc.best(Loadout.of(pipe), fighter, data.target("Vorkath"), Result.Kind.RANGED);
			int effective = 99 + 8 + ("Accurate".equals(r.getStyle()) ? 3 : 0);
			assertEquals(pipe.getName(), (effective * (c[1] + 64) + 320) / 640, r.getMaxHit());
			// Three ticks, two on rapid.
			assertEquals(pipe.getName(), "Rapid".equals(r.getStyle()) ? 2 : 3, r.getTicks());
		}
	}

	private static Advisor.Prices prices()
	{
		Map<Integer, Long> extra = new HashMap<>();
		for (Object[] row : PRICES)
		{
			for (int id : data.named((String) row[0]).getIds())
			{
				extra.put(id, (Long) row[1]);
			}
		}
		Advisor.Prices base = Fixtures.prices(data);
		return id -> extra.containsKey(id) ? extra.get(id) : base.price(id);
	}

	private static Set<Gear> rangedAccount()
	{
		Set<Gear> owned = new LinkedHashSet<>();
		for (String name : new String[]{"Magic shortbow", "Rune arrow", "Green d'hide body", "Green d'hide chaps",
			"Green d'hide vambraces", "Coif", "Rune scimitar", "Amulet of power", "Black d'hide body", "Black d'hide chaps",
			"Black d'hide vambraces", "Amulet of glory", "Archer helm", "Amethyst arrow"})
		{
			owned.add(data.named(name));
		}
		return owned;
	}

	private static Fighter fighter(int sailing)
	{
		return fighter(70, sailing);
	}

	private static Fighter fighter(int combat, int sailing)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Hitpoints"})
		{
			levels.put(s, combat);
		}
		for (String s : new String[]{"Fishing", "Woodcutting", "Fletching", "Slayer", "Smithing", "Crafting", "Mining", "Runecraft"})
		{
			levels.put(s, 95);
		}
		levels.put("Sailing", sailing);
		return new Fighter(levels, Fighter.Boost.POTIONS, true, false, false, false, false, false);
	}

	private static boolean onRoad(Roadmap map, String name)
	{
		return map.getPath().stream().anyMatch(s -> s.getOption().getName().equals(name));
	}

	@Test
	public void aMainBuysWhatAnIronmanMustSailFor()
	{
		Roadmap main = advisor.plan(rangedAccount(), fighter(1), data.target("Abyssal demon"), prices(), 50_000_000);
		assertTrue("a main buys the rosewood blowpipe without sailing", onRoad(main, "Rosewood blowpipe"));
		Roadmap.Option pipe = main.getPath().stream().map(Roadmap.Step::getOption)
			.filter(o -> o.getName().equals("Rosewood blowpipe")).findFirst().get();
		assertEquals(79, pipe.getSailing());
		assertNull(pipe.getMissing());

		// With every combat level at 99 nothing else is locked, so the blowpipe shows what Sailing holds back.
		Roadmap iron = advisor.plan(rangedAccount(), fighter(99, 78).withSelfSourced(true), data.target("Abyssal demon"), prices(), 0);
		assertFalse("an ironman a level short cannot get it", onRoad(iron, "Rosewood blowpipe"));
		Roadmap.Option locked = iron.getLocked().stream().filter(o -> o.getName().equals("Rosewood blowpipe")).findFirst()
			.orElseThrow(() -> new AssertionError("the blowpipe waits on Sailing"));
		assertEquals("79 Sailing to get", locked.getMissing());

		assertEquals(79, locked.getSailing());
		Roadmap sailed = advisor.plan(rangedAccount(), fighter(99, 79).withSelfSourced(true), data.target("Abyssal demon"), prices(), 0);
		assertTrue("one more Sailing level opens it", sailed.getLocked().stream().noneMatch(o -> o.getName().equals("Rosewood blowpipe")));

		// An ironman with the levels gets the same road as a main.
		Roadmap ready = advisor.plan(rangedAccount(), fighter(99).withSelfSourced(true), data.target("Abyssal demon"), prices(), 50_000_000);
		assertTrue(onRoad(ready, "Rosewood blowpipe") == onRoad(advisor.plan(rangedAccount(), fighter(99),
			data.target("Abyssal demon"), prices(), 50_000_000), "Rosewood blowpipe"));
	}

	@Test
	public void wieldingComesBeforeGetting()
	{
		Fighter iron = fighter(1).withSelfSourced(true);
		// Short of both: the wield requirement is named, as for anyone.
		assertEquals("75 Attack", iron.missing(req("Attack", 75), req("Sailing", 62, "Mining", 53)));
		Fighter fighter = iron.withLevel("Attack", 80);
		// Sailing is named first among what it takes to get.
		assertEquals("62 Sailing to get", fighter.missing(req("Attack", 75), req("Mining", 99, "Sailing", 62)));
		assertEquals("99 Mining to get", fighter.withLevel("Sailing", 62).missing(req("Attack", 75), req("Mining", 99, "Sailing", 62)));
		// A main is never held to an item's source.
		assertNull(fighter(1).withLevel("Attack", 80).missing(req("Attack", 75), req("Sailing", 62)));
		// Copies keep the flag.
		assertTrue(iron.withLevel("Ranged", 90).isSelfSourced());
		assertTrue(iron.withTask(SlayerTask.ALWAYS).isSelfSourced());
		assertTrue(iron.withUnlocks(true, true, true).isSelfSourced());
		assertEquals(16, fighter.shortfall(req("Attack", 75), req("Sailing", 17)));
	}
}
