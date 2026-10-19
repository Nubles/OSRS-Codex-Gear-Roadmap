package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.core.Advisor;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.core.Savings;
import com.codex.gearroadmap.data.RoadmapData;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class GoalCardTest
{
	private static final long DAY = 24 * 3_600_000L;
	private static final long NOW = 1_800_000_000_000L;
	private static Roadmap.Option dhcb;

	@BeforeClass
	public static void load() throws IOException
	{
		RoadmapData data = RoadmapData.load(new Gson());
		Savings.Goal goal = new Savings.Goal("Dragon hunter crossbow", Collections.singletonList(data.named("Dragon hunter crossbow").getIds()[0]));
		dhcb = new Advisor(data).plan(Fixtures.midGame(data), Fixtures.midGameFighter(), data.target("Vorkath"), Fixtures.prices(data),
			0, goal).getGoal();
	}

	private static Savings.Worth growing(long perDay, long total)
	{
		return new Savings.Worth(total, Arrays.asList(new Savings.Point(NOW - 4 * DAY, total - 4 * perDay), new Savings.Point(NOW, total)));
	}

	@Test
	public void paceLineSaysWhenInPlainWords()
	{
		assertEquals("Your coins cover it. Buy it whenever you like.",
			GoalCard.paceLine(Savings.of(dhcb, false, 60_000_000, null, NOW)));
		assertEquals("You own it now. Clear this to pick your next goal.", GoalCard.paceLine(Savings.of(dhcb, true, 0, null, NOW)));
		assertEquals("50M to go. With Codex Ledger, this also says when you will get there.",
			GoalCard.paceLine(Savings.of(dhcb, false, 10_000_000, null, NOW)));
		assertEquals("50M to go. About 17 days at this week's pace (net worth +3M a day).",
			GoalCard.paceLine(Savings.of(dhcb, false, 10_000_000, growing(3_000_000, 20_000_000), NOW)));
		assertEquals("50M to go. About a day at this week's pace (net worth +60M a day)."
				+ " Your net worth of 200M would cover it if you sold other things.",
			GoalCard.paceLine(Savings.of(dhcb, false, 10_000_000, growing(60_000_000, 200_000_000), NOW)));
		assertTrue(GoalCard.paceLine(Savings.of(dhcb, false, 10_000_000, growing(-1_000, 90_000_000), NOW))
			.endsWith("no pace to go by. Your net worth of 90M would cover it if you sold other things."));
		assertEquals("50M to go. Codex Ledger needs a day of history to tell when.",
			GoalCard.paceLine(Savings.of(dhcb, false, 10_000_000, new Savings.Worth(12_000_000, null), NOW)));
	}

	@Test
	public void gainLineNamesTheTarget()
	{
		String[] line = GoalCard.gainLine(dhcb, "Vorkath");
		assertTrue(line[0], line[0].startsWith("+") && line[0].contains("DPS against Vorkath"));
	}
}
