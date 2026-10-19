package com.codex.gearroadmap.core;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class FightsTest
{
	private static final long NOW = 1_790_000_000_000L;
	private static final long DAY = 86_400_000L;
	private static RoadmapData data;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
	}

	@Test
	public void chronicleJournalIsTalliedByTarget()
	{
		// Session Chronicle's saved journal, as it writes it: a list of sessions with kill lines by key.
		String journal = "[{\"startedAt\":" + (NOW - DAY) + ",\"playedMs\":3600000,\"kills\":{"
			+ "\"vorkath\":{\"name\":\"Vorkath\",\"looted\":20,\"firstKc\":400,\"lastKc\":421},"
			+ "\"bird nest\":{\"name\":\"Bird nest\",\"looted\":9,\"event\":true},"
			+ "\"chambers of xeric\":{\"name\":\"Chambers of Xeric\",\"looted\":2,\"event\":true,\"counters\":{\"chambers of xeric\":[50,51]}}},"
			+ "\"skills\":{},\"loot\":{},\"moments\":[]},"
			+ "{\"startedAt\":" + (NOW - 3 * DAY) + ",\"kills\":{\"abyssal demon\":{\"name\":\"Abyssal demon\",\"looted\":100,\"event\":false}}},"
			+ "{\"startedAt\":" + (NOW - 40 * DAY) + ",\"kills\":{\"vorkath\":{\"name\":\"Vorkath\",\"looted\":99}}},"
			+ "{\"startedAt\":" + (NOW - 2 * DAY) + ",\"kills\":{\"the leviathan\":{\"name\":\"The Leviathan\",\"looted\":3}}}]";
		List<Fights.Logged> sessions = new Gson().fromJson(journal, new TypeToken<List<Fights.Logged>>()
		{
		}.getType());
		Fights.Tally tally = Fights.tally(sessions, data.targets(), NOW);
		Map<String, Integer> byName = new HashMap<>();
		tally.getKills().forEach((t, n) -> byName.put(t.getName(), n));
		assertEquals("the game's count beats the looted count, older sessions are left out", 22, (int) byName.get("Vorkath"));
		assertEquals(100, (int) byName.get("Abyssal demon"));
		assertEquals("Chronicle files The Leviathan without its article", 3, (int) byName.get("The Leviathan"));
		assertEquals(3, byName.size());
		assertEquals(3, tally.getSessions());
		// Most fighting first: 22 Vorkaths (750 hp) outweigh 100 abyssal demons (150 hp).
		assertEquals("Vorkath", tally.getKills().keySet().iterator().next().getName());
	}

	@Test
	public void killsAreMatchedByChroniclesKeyNotTheNameTheLootGaveThem()
	{
		// Session Chronicle's journal exactly as its own code saves it (pinned by Chronicle's SavedShapeTest):
		// the Grotesque Guardians drop their loot as Dusk, so the line is named "Dusk" under the key
		// "grotesque guardians". Matching by the name alone lost every Guardians kill.
		String journal = "[{\"startedAt\":" + (NOW - DAY) + ",\"lastSeenAt\":" + (NOW - DAY) + ",\"endedAt\":" + (NOW - DAY)
			+ ",\"playedMs\":0,\"player\":\"Zezima\",\"skills\":{},\"loot\":{},\"kills\":{"
			+ "\"bird nest\":{\"name\":\"Bird nest\",\"looted\":3,\"firstKc\":0,\"lastKc\":0,\"event\":true},"
			+ "\"grotesque guardians\":{\"name\":\"Dusk\",\"looted\":2,\"firstKc\":41,\"lastKc\":42,"
			+ "\"counters\":{\"grotesque guardians\":[41,42]},\"event\":false},"
			+ "\"abyssal demon\":{\"name\":\"Abyssal demon\",\"looted\":1,\"firstKc\":0,\"lastKc\":0,\"event\":false}},"
			+ "\"moments\":[],\"deaths\":0,\"otherLoot\":0,\"otherDrops\":0,\"tripCount\":0,\"tripMs\":0,\"bestTripMs\":0}]";
		List<Fights.Logged> sessions = new Gson().fromJson(journal, new TypeToken<List<Fights.Logged>>()
		{
		}.getType());
		Fights.Tally tally = Fights.tally(sessions, data.targets(), NOW);
		assertEquals(2, (int) tally.getKills().get(data.target("Grotesque Guardians")));
		assertEquals(1, (int) tally.getKills().get(data.target("Abyssal demon")));
		assertEquals(2, tally.getKills().size());
	}

	@Test
	public void aSessionInBothJournalAndCurrentCountsOnce()
	{
		// Chronicle files a session into the journal a moment before it rewrites "current", so for
		// that moment the same session can be read from both keys.
		Map<String, Fights.Line> kills = new HashMap<>();
		kills.put("vorkath", new Fights.Line("Vorkath", 10));
		Fights.Logged filed = new Fights.Logged(NOW - DAY, kills);
		Fights.Logged live = new Fights.Logged(NOW - DAY, new HashMap<>(kills));
		Fights.Tally tally = Fights.tally(Arrays.asList(filed, live), data.targets(), NOW);
		assertEquals(10, (int) tally.getKills().get(data.target("Vorkath")));
		assertEquals(1, tally.getSessions());
	}

	@Test
	public void aShortJournalSaysHowManyDaysItCovers()
	{
		// Chronicle installed three days ago: the time saved is over those three days, not a month.
		Map<String, Fights.Line> kills = new HashMap<>();
		kills.put("vorkath", new Fights.Line("Vorkath", 10));
		Fights.Tally tally = Fights.tally(Arrays.asList(new Fights.Logged(NOW - 3 * DAY + 3_600_000L, kills),
			new Fights.Logged(NOW - DAY, kills)), data.targets(), NOW);
		assertEquals(3, tally.getDays());
		Fights.Tally old = Fights.tally(Collections.singletonList(new Fights.Logged(NOW - 29 * DAY - 1, kills)), data.targets(), NOW);
		assertEquals(Fights.DAYS, old.getDays());
	}

	@Test
	public void upgradesAreWeighedAcrossWhereThePlayerFights()
	{
		Map<Target, Integer> kills = new java.util.LinkedHashMap<>();
		kills.put(data.target("Vorkath"), 120);
		kills.put(data.target("Abyssal demon"), 900);
		Fights.Tally tally = new Fights.Tally(kills, 10);
		Advisor advisor = new Advisor(data);
		long t0 = System.nanoTime();
		List<Fights.Pick> picks = advisor.fights(Fixtures.midGame(data), Fixtures.midGameFighter(), tally, Fixtures.prices(data));
		long ms = (System.nanoTime() - t0) / 1_000_000;
		System.out.println("Fights weighed in " + ms + " ms");
		assertFalse(picks.isEmpty());
		assertTrue(picks.size() <= Fights.PICKS);
		double lastValue = Double.MAX_VALUE;
		for (Fights.Pick p : picks)
		{
			System.out.printf("  %-28s %,12d  %5.1f min  %s%n", p.getName(), p.getCost(), p.getSeconds() / 60, p.getWhere());
			assertTrue(p.getSeconds() >= Fights.MIN_SECONDS);
			assertTrue(p.getCost() > 0);
			assertFalse(p.getWhere().isEmpty());
			double value = p.getSeconds() / p.getCost();
			assertTrue(value <= lastValue + 1e-12);
			lastValue = value;
		}
		assertTrue(advisor.fights(Fixtures.midGame(data), Fixtures.midGameFighter(),
			new Fights.Tally(Collections.emptyMap(), 0), Fixtures.prices(data)).isEmpty());
	}
}
