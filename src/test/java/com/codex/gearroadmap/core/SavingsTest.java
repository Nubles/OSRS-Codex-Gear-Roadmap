package com.codex.gearroadmap.core;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class SavingsTest
{
	private static final long DAY = 24 * 3_600_000L;
	private static final long NOW = 1_800_000_000_000L;
	private static RoadmapData data;
	private static Advisor advisor;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		advisor = new Advisor(data);
	}

	private static Savings.Goal goal(String... names)
	{
		List<Integer> ids = new ArrayList<>();
		for (String name : names)
		{
			ids.add(data.named(name).getIds()[0]);
		}
		return new Savings.Goal(String.join(" + ", names), ids);
	}

	private static Roadmap plan(Set<Gear> owned, Savings.Goal goal)
	{
		Target vorkath = data.target("Vorkath");
		return advisor.plan(owned, Fixtures.midGameFighter(), vorkath, Fixtures.prices(data), 10_000_000, goal);
	}

	@Test
	public void goalIsPricedAndWeighedFromTheLoadoutOwnedNow()
	{
		Roadmap map = plan(Fixtures.midGame(data), goal("Dragon hunter crossbow"));
		Roadmap.Option o = map.getGoal();
		assertNotNull(o);
		assertEquals(60_000_000, o.getCost());
		assertTrue("a dragon hunter crossbow beats a blowpipe at Vorkath", o.getGain() > 0);
		assertFalse(map.isGoalOwned());
		// The goal survives a change of coins, which rebuilds the road.
		assertEquals(60_000_000, map.withCoins(1).getGoal().getCost());
	}

	@Test
	public void ownedGoalIsMetAndCostsNothing()
	{
		Set<Gear> owned = Fixtures.midGame(data);
		owned.add(data.named("Dragon hunter crossbow"));
		Roadmap map = plan(owned, goal("Dragon hunter crossbow"));
		assertTrue(map.isGoalOwned());
		assertEquals(0, map.getGoal().getCost());
		assertTrue(Savings.of(map.getGoal(), true, 0, null, NOW).isAffordable());
	}

	@Test
	public void pairGoalCountsOnlyWhatIsStillToBuy()
	{
		Set<Gear> owned = Fixtures.midGame(data);
		owned.add(data.named("Masori body"));
		Roadmap map = plan(owned, goal("Masori body", "Masori chaps"));
		assertEquals(16_000_000, map.getGoal().getCost());
		assertFalse(map.isGoalOwned());
	}

	@Test
	public void unknownOrUnpricedGoalIsLeftOut()
	{
		assertNull(plan(Fixtures.midGame(data), new Savings.Goal("Mystery", Collections.singletonList(-5))).getGoal());
		assertNull(plan(Fixtures.midGame(data), null).getGoal());
		Savings.Goal unpriced = new Savings.Goal("Rune pouch", Collections.singletonList(data.named("Twisted bow").getIds()[0]));
		Target vorkath = data.target("Vorkath");
		Roadmap map = advisor.plan(Fixtures.midGame(data), Fixtures.midGameFighter(), vorkath, id -> 0L, 0, unpriced);
		assertNull("no price, no goal card, rather than a goal that reads as free", map.getGoal());
	}

	@Test
	public void goalFromAnotherStyleAddsNothingHereButStaysPriced()
	{
		// A savings goal stays put when the player aims elsewhere: Vorkath cannot be hurt by melee
		// any better with a magic staff, so the gain may be nothing, yet the price still counts.
		Target vorkath = data.target("Vorkath");
		Roadmap map = advisor.plan(Fixtures.midGame(data), Fixtures.midGameFighter(), vorkath, Fixtures.prices(data), 0,
			goal("Mystic fire staff"));
		assertEquals(30_000, map.getGoal().getCost());
		assertTrue(map.getGoal().getGain() >= 0);
	}

	@Test
	public void progressAndRemaining()
	{
		Roadmap.Option o = plan(Fixtures.midGame(data), goal("Dragon hunter crossbow")).getGoal();
		Savings.View v = Savings.of(o, false, 15_000_000, null, NOW);
		assertEquals(0.25, v.getProgress(), 1e-9);
		assertEquals(45_000_000, v.getRemaining());
		assertFalse(v.isAffordable());
		assertFalse(v.hasLedger());
		assertEquals(-1, v.getDays());
		Savings.View rich = Savings.of(o, false, 90_000_000, null, NOW);
		assertEquals(1, rich.getProgress(), 1e-9);
		assertEquals(0, rich.getRemaining());
		assertEquals(0, rich.getDays());
	}

	@Test
	public void paceIsTheLastWeeksGrowthPerDay()
	{
		List<Savings.Point> history = Arrays.asList(
			new Savings.Point(NOW - 30 * DAY, 10_000_000),
			// The first reading inside the week starts the pace; older ones are left out.
			new Savings.Point(NOW - 6 * DAY, 40_000_000),
			new Savings.Point(NOW - 3 * DAY, 45_000_000),
			new Savings.Point(NOW, 52_000_000));
		Savings.Worth worth = new Savings.Worth(52_000_000, history);
		assertEquals(Long.valueOf(2_000_000), Savings.pace(worth, NOW));
		Roadmap.Option o = plan(Fixtures.midGame(data), goal("Dragon hunter crossbow")).getGoal();
		Savings.View v = Savings.of(o, false, 14_000_000, worth, NOW);
		assertEquals(46_000_000, v.getRemaining());
		assertEquals(23, v.getDays());
		assertTrue(v.hasLedger());
		assertEquals(52_000_000, v.getWorth());
	}

	@Test
	public void paceNeedsADayOfHistoryAndRoundsDaysUp()
	{
		Savings.Worth young = new Savings.Worth(5, Arrays.asList(new Savings.Point(NOW - 3_600_000L, 1), new Savings.Point(NOW, 5)));
		assertNull(Savings.pace(young, NOW));
		assertNull(Savings.pace(new Savings.Worth(5, null), NOW));
		assertNull(Savings.pace(null, NOW));
		// Readings from the future (a clock change) are ignored rather than trusted.
		Savings.Worth odd = new Savings.Worth(5, Arrays.asList(new Savings.Point(NOW - 2 * DAY, 0), new Savings.Point(NOW + DAY, 999)));
		assertNull(Savings.pace(odd, NOW));

		Roadmap.Option o = plan(Fixtures.midGame(data), goal("Dragon hunter crossbow")).getGoal();
		Savings.Worth slow = new Savings.Worth(1, Arrays.asList(new Savings.Point(NOW - 2 * DAY, 0), new Savings.Point(NOW, 3_000_000)));
		// 1.5M a day, 59M to go: 39.3 days is 40 days, never an optimistic 39.
		assertEquals(40, Savings.of(o, false, 1_000_000, slow, NOW).getDays());
		Savings.Worth falling = new Savings.Worth(1, Arrays.asList(new Savings.Point(NOW - 2 * DAY, 9), new Savings.Point(NOW, 3)));
		assertEquals(-1, Savings.of(o, false, 1_000_000, falling, NOW).getDays());
	}

	@Test
	public void ledgerSaveFormatReads()
	{
		// The shape Codex Ledger saves: Gear Roadmap reads only the total and history, ignoring the rest.
		String json = "{\"stores\":{\"BANK\":{\"seen\":1,\"items\":{\"995\":5}}},\"prices\":{\"995\":1},"
			+ "\"history\":[{\"t\":100,\"v\":7},{\"t\":200,\"v\":9}],\"days\":[],\"total\":9,\"since\":100,\"exchange\":{}}";
		Savings.Worth worth = new Gson().fromJson(json, Savings.Worth.class);
		assertEquals(9, worth.getTotal());
		assertEquals(2, worth.getHistory().size());
		assertEquals(200, worth.getHistory().get(1).getTime());
		assertEquals(9, worth.getHistory().get(1).getValue());
	}

	@Test
	public void savedGoalRoundTrips()
	{
		Gson gson = new Gson();
		Savings.Goal g = gson.fromJson(gson.toJson(goal("Masori body", "Masori chaps")), Savings.Goal.class);
		assertTrue(g.isValid());
		assertEquals("Masori body + Masori chaps", g.getName());
		assertEquals(2, g.getIds().size());
		assertFalse(gson.fromJson("{\"name\":\"x\"}", Savings.Goal.class).isValid());
	}
}
