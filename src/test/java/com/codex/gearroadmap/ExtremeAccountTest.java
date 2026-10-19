package com.codex.gearroadmap;

import com.codex.gearroadmap.core.Fights;
import com.codex.gearroadmap.core.Holdings;
import com.codex.gearroadmap.core.Savings;
import com.codex.gearroadmap.data.RoadmapData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The accounts at the ends, timed on the work Gear Roadmap does on the client thread: reading a
 * bank of 1,500 kinds of item (its own and Bank Steward's) and choosing between them, Codex Ledger's
 * worth with 10,000 points of history, and a full Chronicle journal; and a brand-new account with
 * nothing saved anywhere. Times are the best of several runs, printed for the record.
 */
public class ExtremeAccountTest
{
	private static final Gson GSON = new GsonBuilder().create();
	private static final long NOW = 1_790_000_000_000L;
	private static final long DAY = 86_400_000L;
	/** What a piece of client-thread work may take. */
	private static final double BUDGET_MS = 10;
	private static RoadmapData data;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(GSON);
	}

	@Test
	public void aHugeBankIsReadAndChosenQuickly()
	{
		StringBuilder own = new StringBuilder("{\"items\":[");
		StringBuilder steward = new StringBuilder("{\"version\":1,\"savedAt\":" + (NOW - DAY) + ",\"items\":{");
		for (int i = 0; i < 1_500; i++)
		{
			own.append(i == 0 ? "" : ",").append(1_000 + i * 7);
			steward.append(i == 0 ? "" : ",").append('"').append(1_000 + i * 7).append("\":").append(1 + i % 90);
		}
		own.append("],\"coins\":2147483647}");
		steward.append("},\"truncated\":false}");
		String ownJson = own.toString();
		String stewardJson = steward.toString();
		CodexGearRoadmapPlugin.Bank[] bank = new CodexGearRoadmapPlugin.Bank[1];
		Holdings.Shared[] shared = new Holdings.Shared[1];
		double readOwn = best(() -> bank[0] = CodexGearRoadmapPlugin.readBank(GSON, ownJson));
		double readShared = best(() -> shared[0] = Holdings.readSteward(GSON, stewardJson, NOW));
		double choose = best(() -> Holdings.choose(bank[0].items, bank[0].coins, null, NOW - 2 * DAY, shared[0], id -> data.gear(id) != null));
		print("1,500-item bank", readOwn, readShared, choose);
		assertEquals(1_500, bank[0].items.size());
		assertEquals(1_500, shared[0].getItems().size());
		assertTrue("own " + readOwn, readOwn < BUDGET_MS);
		assertTrue("shared " + readShared, readShared < BUDGET_MS);
		assertTrue("choose " + choose, choose < BUDGET_MS);
	}

	@Test
	public void aLedgerWithTenThousandPointsAndAFullJournal()
	{
		StringBuilder ledger = new StringBuilder("{\"stores\":{},\"total\":5000000000,\"since\":1,\"history\":[");
		for (int i = 0; i < 10_000; i++)
		{
			ledger.append(i == 0 ? "" : ",").append("{\"t\":").append(NOW - (10_000 - i) * 600_000L).append(",\"v\":").append(4_000_000_000L + i * 100_000L).append('}');
		}
		ledger.append("],\"days\":[]}");
		String ledgerJson = ledger.toString();
		Savings.Worth[] worth = new Savings.Worth[1];
		double read = best(() -> worth[0] = Savings.readWorth(GSON, ledgerJson));
		double pace = best(() -> Savings.paceOf(worth[0], NOW));
		assertEquals(10_000, worth[0].getHistory().size());
		assertNotNull(Savings.paceOf(worth[0], NOW));

		StringBuilder journal = new StringBuilder("[");
		for (int s = 0; s < 100; s++)
		{
			journal.append(s == 0 ? "" : ",").append("{\"startedAt\":").append(NOW - s * 7 * 3_600_000L).append(",\"kills\":{");
			for (int k = 0; k < 30; k++)
			{
				journal.append(k == 0 ? "" : ",").append("\"monster ").append(k).append("\":{\"name\":\"Monster ").append(k)
					.append("\",\"looted\":40,\"firstKc\":1,\"lastKc\":40,\"tasks\":{\"\":40}}");
			}
			journal.append(",\"vorkath\":{\"name\":\"Vorkath\",\"looted\":20}}}");
		}
		journal.append(']');
		String journalJson = journal.toString();
		@SuppressWarnings("unchecked")
		List<Fights.Logged>[] sessions = new List[1];
		double readJournal = best(() -> sessions[0] = Fights.read(GSON, journalJson, true));
		double tally = best(() -> Fights.tally(sessions[0], data.targets(), NOW));
		print("10,000 points, 100 sessions", read, pace, readJournal, tally);
		assertEquals(100, sessions[0].size());
		// Eight times the 1,200 points Ledger ever keeps, read only when Ledger saves (every few minutes).
		assertTrue("worth " + read, read < 3 * BUDGET_MS);
		assertTrue("pace " + pace, pace < BUDGET_MS);
		// A hundred sessions of thirty monsters each, more than Chronicle keeps by default.
		assertTrue("journal " + readJournal, readJournal < 3 * BUDGET_MS);
		assertTrue("tally " + tally, tally < BUDGET_MS);
	}

	@Test
	public void aBrandNewAccountHasNothingAndSaysNothing()
	{
		assertEquals(null, CodexGearRoadmapPlugin.readBank(GSON, null));
		assertEquals(null, Savings.readWorth(GSON, null));
		assertEquals(null, Savings.readGoal(GSON, null));
		assertTrue(Fights.read(GSON, null, true).isEmpty());
		assertEquals(null, Holdings.readSteward(GSON, null, NOW));
		assertTrue(Fights.tally(Fights.read(GSON, null, true), data.targets(), NOW).isEmpty());
		assertEquals(Holdings.NONE, Holdings.choose(null, 0, null, 0, null, id -> true));
		// A Ledger that has not seen the bank yet saves a total of what it has counted and since 0: no pace.
		assertEquals(null, Savings.paceOf(Savings.readWorth(GSON, "{\"total\":25000,\"since\":0,\"history\":[]}"), NOW));
	}

	private static double best(Runnable work)
	{
		// Warmed well first: a test machine's first runs are the compiler's, not the plugin's.
		for (int i = 0; i < 25; i++)
		{
			work.run();
		}
		// Then sampled in a few rounds a moment apart, so a shared test machine busy for a while (a
		// build beside it, a collection) shows the work's own time rather than its neighbours'.
		double best = Double.MAX_VALUE;
		for (int round = 0; round < 4; round++)
		{
			for (int i = 0; i < 7; i++)
			{
				long t = System.nanoTime();
				work.run();
				best = Math.min(best, (System.nanoTime() - t) / 1e6);
			}
			pause();
		}
		return best;
	}

	private static void pause()
	{
		try
		{
			Thread.sleep(25);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}

	private static void print(String what, double... ms)
	{
		StringBuilder line = new StringBuilder(what).append(":");
		for (double m : ms)
		{
			line.append(String.format(Locale.ENGLISH, " %.2f ms", m));
		}
		System.out.println(line);
	}
}
