package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.RoadmapData;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Which bank Gear Roadmap plans with. {@code bank-steward-bankV1.json} is a byte-for-byte copy of
 * the fixture Codex Bank Steward's own BankSnapshotTest proves its writer produces.
 */
public class HoldingsTest
{
	private static final Gson GSON = new Gson();
	private static final int WHIP = 4151;
	private static final int VOID_TOP = 8839;
	private static final int DRAGON_ARROW = 11212;
	private static RoadmapData data;

	@BeforeClass
	public static void load() throws Exception
	{
		data = RoadmapData.load(GSON);
	}

	private static String fixture(String name) throws Exception
	{
		try (InputStream in = HoldingsTest.class.getResourceAsStream(name))
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			for (int n; (n = in.read(buf)) > 0; )
			{
				out.write(buf, 0, n);
			}
			return new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
		}
	}

	private static Holdings.Shared steward(long savedAt, Object... idQty)
	{
		Map<Integer, Long> items = new HashMap<>();
		for (int i = 0; i < idQty.length; i += 2)
		{
			items.put((Integer) idQty[i], ((Number) idQty[i + 1]).longValue());
		}
		return new Holdings.Shared(items, savedAt);
	}

	private static Set<Integer> ids(Integer... ids)
	{
		return new TreeSet<>(Arrays.asList(ids));
	}

	@Test
	public void readsBankStewardsRealSnapshot() throws Exception
	{
		// CodexBankStewardConfig.GROUP and BankSnapshot.KEY.
		assertEquals("codexbanksteward", Holdings.STEWARD_GROUP);
		assertEquals("bankV1", Holdings.STEWARD_KEY);
		Holdings.Shared shared = Holdings.readSteward(GSON, fixture("bank-steward-bankV1.json"));
		assertEquals(1_760_000_000_000L, shared.getSavedAt());
		assertEquals(Long.valueOf(240), shared.getItems().get(385));
		assertEquals(Long.valueOf(1_234_567), shared.getItems().get(995));

		Holdings h = Holdings.choose(null, 0, null, 0, shared, id -> data.gear(id) != null);
		assertSame(Holdings.Source.BANK_STEWARD, h.getSource());
		assertTrue(h.isKnown());
		// Only gear is kept, with its count; the sharks, potions and the sceptre are not gear.
		assertEquals(Collections.singletonMap(VOID_TOP, 1L), h.getGear());
		assertEquals(1_234_567, h.getCoins());
	}

	@Test
	public void anUnreadableOrMissingSnapshotIsNoBank()
	{
		assertNull(Holdings.readSteward(GSON, null));
		assertNull(Holdings.readSteward(GSON, ""));
		assertNull(Holdings.readSteward(GSON, "not json {"));
		assertNull(Holdings.readSteward(GSON, "{\"items\":[4151]}"));
		assertNull(Holdings.readSteward(GSON, "[1,2]"));
		// Bad entries are skipped, the rest kept.
		Holdings.Shared odd = Holdings.readSteward(GSON, "{\"items\":{\"4151\":1,\"x\":2,\"995\":-5,\"11212\":1.5,\"8839\":\"a\"}}");
		assertEquals(Collections.singletonMap(WHIP, 1L), odd.getItems());
		assertEquals(0, odd.getSavedAt());
		assertSame(Holdings.NONE, Holdings.choose(null, 0, null, 0, null, id -> true));
		assertFalse(Holdings.NONE.isKnown());
	}

	@Test
	public void platinumTokensCountAsCoins()
	{
		Holdings h = Holdings.choose(null, 0, null, 0, steward(5, 995, 500, 13204, 3), id -> false);
		assertEquals(3_500, h.getCoins());
	}

	@Test
	public void withoutBankStewardTheOwnViewIsUsed()
	{
		Holdings h = Holdings.choose(ids(WHIP), 1_000, null, 0, null, id -> true);
		assertSame(Holdings.Source.OWN, h.getSource());
		assertEquals(Collections.singletonMap(WHIP, Holdings.UNKNOWN), h.getGear());
		assertEquals(1_000, h.getCoins());
	}

	@Test
	public void theFresherBankWins()
	{
		// Gear Roadmap last saw the bank at 100 and Bank Steward at 200, after the whip was sold.
		Holdings h = Holdings.choose(ids(WHIP, VOID_TOP), 1_000, null, 100, steward(200, VOID_TOP, 1, 995, 9_000), id -> id != 995);
		assertSame(Holdings.Source.BANK_STEWARD, h.getSource());
		assertEquals(Collections.singletonMap(VOID_TOP, 1L), h.getGear());
		assertEquals(9_000, h.getCoins());

		// The other way round, Gear Roadmap's newer view stands.
		h = Holdings.choose(ids(WHIP, VOID_TOP), 1_000, null, 300, steward(200, VOID_TOP, 1), id -> true);
		assertSame(Holdings.Source.OWN, h.getSource());
		assertEquals(1_000, h.getCoins());

		// A bank saved before Gear Roadmap kept the time defers to Bank Steward.
		h = Holdings.choose(ids(WHIP), 1_000, null, 0, steward(200, VOID_TOP, 1), id -> true);
		assertSame(Holdings.Source.BANK_STEWARD, h.getSource());
	}

	@Test
	public void theSameGearTakesBankStewardsCountsWhoeverSawItLast()
	{
		Holdings h = Holdings.choose(ids(DRAGON_ARROW), 0, null, 300, steward(200, DRAGON_ARROW, 37), id -> true);
		assertSame(Holdings.Source.BANK_STEWARD, h.getSource());
		assertEquals(Long.valueOf(37), h.getGear().get(DRAGON_ARROW));
	}

	@Test
	public void aBankSeenThisLoginKeepsItsOwnCounts()
	{
		Map<Integer, Long> counts = Collections.singletonMap(DRAGON_ARROW, 2_000L);
		Holdings h = Holdings.choose(ids(DRAGON_ARROW), 0, counts, 300, steward(200, DRAGON_ARROW, 37), id -> true);
		assertSame(Holdings.Source.OWN, h.getSource());
		assertEquals(Long.valueOf(2_000), h.getGear().get(DRAGON_ARROW));
	}

	@Test
	public void ammunitionNeedsAStackWorthBringing()
	{
		assertFalse(Holdings.enough(true, 37));
		assertTrue(Holdings.enough(true, Holdings.AMMO_TRIP));
		assertTrue("a count never seen still counts", Holdings.enough(true, Holdings.UNKNOWN));
		assertTrue(Holdings.enough(false, 1));
		assertFalse(Holdings.enough(false, 0));
	}
}
