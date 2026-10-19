package com.codex.gearroadmap.core;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class SwapTest
{
	private static RoadmapData data;
	private static Advisor advisor;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		advisor = new Advisor(data);
	}

	private static Set<Integer> ids(Loadout lo)
	{
		Set<Integer> ids = new HashSet<>();
		for (int i = 0; i < Gear.SLOTS; i++)
		{
			if (lo.get(i) != null)
			{
				ids.add(lo.get(i).getId());
			}
		}
		return ids;
	}

	@Test
	public void wearingTheBestOwnedSaysSo()
	{
		Fighter fighter = Fixtures.midGameFighter();
		Target target = data.target("General Graardor");
		Planner.Best best = advisor.planner().optimise(Fixtures.midGame(data), fighter, target);
		Swap swap = Swap.of(ids(best.best().getLoadout()), Collections.emptySet(), data::gear, best.best(), advisor.planner().calc(),
			fighter, target);
		assertTrue(swap.isBest());
		assertEquals(best.dps(), swap.getWornDps(), 1e-9);
	}

	@Test
	public void namesThePiecesToPutOnAndWhereTheyAre()
	{
		Fighter fighter = Fixtures.midGameFighter();
		Target target = data.target("General Graardor");
		Planner.Best best = advisor.planner().optimise(Fixtures.midGame(data), fighter, target);
		Loadout lo = best.best().getLoadout();
		Gear weapon = lo.weapon();
		Gear body = lo.get(Gear.BODY);
		// Wearing the best bar the weapon and body; the body is carried, the weapon is in the bank.
		Set<Integer> worn = ids(lo.without(Gear.WEAPON).without(Gear.BODY));
		Set<Integer> carried = new HashSet<>();
		carried.add(body.getIds()[body.getIds().length - 1]);
		Swap swap = Swap.of(worn, carried, data::gear, best.best(), advisor.planner().calc(), fighter, target);
		assertFalse(swap.isBest());
		assertEquals(2, swap.getChanges().size());
		Swap.Change first = swap.getChanges().get(0);
		assertEquals("the weapon comes first", weapon.key(), first.getWear().key());
		assertEquals(Swap.Where.BANK, first.getWhere());
		assertNull(first.getOff());
		assertEquals(body.key(), swap.getChanges().get(1).getWear().key());
		assertEquals(Swap.Where.INVENTORY, swap.getChanges().get(1).getWhere());
		assertTrue(swap.gain() > 0);
	}

	@Test
	public void piecesItDoesNotKnowAreIgnoredAndAnotherStyleIsNamed()
	{
		Fighter fighter = Fixtures.midGameFighter();
		Target target = data.target("General Graardor");
		Planner.Best best = advisor.planner().optimise(Fixtures.midGame(data), fighter, target);
		Gear trident = data.named("Trident of the Seas");
		Set<Integer> worn = new HashSet<>();
		worn.add(trident.getId());
		// A ring of wealth, say: not combat gear, so not in the roadmap's data.
		worn.add(2572);
		Swap swap = Swap.of(worn, Collections.emptySet(), data::gear, best.best(), advisor.planner().calc(), fighter, target);
		assertEquals(trident.key(), swap.getChanges().get(0).getOff().key());
		assertTrue(swap.getWornDps() < swap.getBestDps());
		assertTrue(swap.getChanges().stream().noneMatch(c -> c.getOff() != null && c.getOff().getId() == 2572));
	}
}
