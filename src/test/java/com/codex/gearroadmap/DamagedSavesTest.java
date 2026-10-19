package com.codex.gearroadmap;

import com.codex.gearroadmap.core.Claims;
import com.codex.gearroadmap.core.Fights;
import com.codex.gearroadmap.core.Holdings;
import com.codex.gearroadmap.core.Saved;
import com.codex.gearroadmap.core.Savings;
import com.codex.gearroadmap.data.RoadmapData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Every value Gear Roadmap reads back from the profile, its own (bank, road behind, savings goal)
 * and other Codex plugins' (Codex Ledger's worth, Session Chronicle's journal and live session, Bank
 * Steward's bank), must survive whatever is found there: empty, cut short, the wrong type, numbers
 * too big or below zero, a later version's fields, or garbage. None of it may throw, and a broken
 * entry must cost only itself, never the good ones beside it.
 */
public class DamagedSavesTest
{
	private static final Gson GSON = new GsonBuilder().create();
	private static final JsonParser PARSER = new JsonParser();
	private static final long NOW = 1_790_000_000_000L;
	private static final long DAY = 86_400_000L;
	/** Values of every wrong kind, put in place of a good one. */
	private static final String[] BAD = {
		"null", "\"\"", "\"garbage\"", "true", "0", "-1", "-9223372036854775808", "9223372036854775807",
		"1e30", "1.5", "[]", "[null]", "[\"x\"]", "{}", "{\"x\":1}", "[{}]",
	};
	/** Whole saves that are not what they should be at all. */
	private static final String[] WHOLE = {
		null, "", " ", "null", "{", "}", "[", "[]", "[{}]", "42", "\"text\"", "true", "garbage{{", "\u0000",
	};
	private static RoadmapData data;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(GSON);
	}

	@Test
	public void theSavedBank()
	{
		String good = "{\"items\":[4151,9050,13072],\"coins\":1234567}";
		for (String save : cut(good))
		{
			bank(save);
		}
		for (String key : new String[]{"items", "coins"})
		{
			for (String bad : BAD)
			{
				JsonObject o = PARSER.parse(good).getAsJsonObject();
				o.add(key, PARSER.parse(bad));
				CodexGearRoadmapPlugin.Bank read = bank(o.toString());
				if (key.equals("coins"))
				{
					// Coins gone wrong cost the coins, never the gear.
					assertNotNull(bad, read);
					assertEquals(bad, 3, read.items.size());
				}
				if (read != null)
				{
					assertTrue(bad, read.coins >= 0);
				}
			}
		}
		// One id that is not one costs only itself.
		CodexGearRoadmapPlugin.Bank read = bank("{\"items\":[4151,null,\"x\",-5,0,1e30,9050],\"coins\":-40}");
		assertNotNull(read);
		assertEquals(2, read.items.size());
		assertEquals(0, read.coins);
		assertNotNull(bank("{\"items\":[4151],\"coins\":1,\"version\":3,\"tabs\":[[1]]}"));
	}

	@Test
	public void theRoadBehind()
	{
		Claims good = new Claims();
		good.add(new Claims.Claim("Abyssal whip", 4151, 1.2, "Vorkath", NOW - 3 * DAY));
		good.add(new Claims.Claim("Fire cape", 6570, 0.4, "Vorkath", NOW - 2 * DAY));
		good.add(new Claims.Claim("Ghrazi rapier", 22324, 0.8, "Zulrah", NOW - DAY));
		String whole = GSON.toJson(good);
		for (String save : cut(whole))
		{
			claims(save);
		}
		for (String bad : BAD)
		{
			JsonObject o = PARSER.parse(whole).getAsJsonObject();
			o.getAsJsonArray("claims").set(1, PARSER.parse(bad));
			assertEquals(bad, 2, claims(o.toString()).recent().size());
			for (String field : new String[]{"name", "icon", "gain", "target", "at"})
			{
				o = PARSER.parse(whole).getAsJsonObject();
				o.getAsJsonArray("claims").get(0).getAsJsonObject().add(field, PARSER.parse(bad));
				assertTrue(field + " = " + bad, claims(o.toString()).recent().size() >= 2);
			}
		}
		// A claim dated past any clock, or adding more damage than any upgrade can, is damage: it is left
		// out rather than heading the road behind for ever. A later version's fields are ignored.
		JsonObject o = PARSER.parse(whole).getAsJsonObject();
		o.addProperty("version", 4);
		o.getAsJsonArray("claims").get(0).getAsJsonObject().addProperty("at", Long.MAX_VALUE);
		o.getAsJsonArray("claims").get(0).getAsJsonObject().addProperty("gain", 1e300);
		Claims read = claims(o.toString());
		assertEquals(2, read.recent().size());
		assertEquals("Fire cape", read.recent().get(0).getName());
	}

	@Test
	public void theSavingsGoal()
	{
		String good = "{\"name\":\"Ghrazi rapier\",\"ids\":[22324]}";
		for (String save : cut(good))
		{
			goal(save);
		}
		for (String key : new String[]{"name", "ids"})
		{
			for (String bad : BAD)
			{
				JsonObject o = PARSER.parse(good).getAsJsonObject();
				o.add(key, PARSER.parse(bad));
				goal(o.toString());
			}
		}
		// A broken piece beside a good one costs only itself.
		Savings.Goal read = goal("{\"name\":\"Pair\",\"ids\":[22324,null,\"x\",-3,4151],\"version\":2}");
		assertNotNull(read);
		assertEquals(java.util.Arrays.asList(22324, 4151), read.getIds());
		assertNull(goal("{\"name\":\"Nothing\",\"ids\":[null,-1]}"));
	}

	@Test
	public void codexLedgersWorth()
	{
		JsonObject good = new JsonObject();
		good.addProperty("total", 900_000_000L);
		good.addProperty("since", NOW - 30 * DAY);
		JsonArray history = new JsonArray();
		for (int d = 6; d >= 0; d--)
		{
			JsonObject p = new JsonObject();
			p.addProperty("t", NOW - d * DAY);
			p.addProperty("v", 900_000_000L - d * 10_000_000L);
			history.add(p);
		}
		good.add("history", history);
		// Ledger's other fields, damaged, must not matter: Gear Roadmap reads only these two.
		good.add("days", PARSER.parse("[{\"date\":\"garbage\",\"items\":7}]"));
		String whole = good.toString();
		Savings.Worth ok = worth(whole);
		assertEquals(Long.valueOf(10_000_000L), pace(ok));
		for (String save : cut(whole))
		{
			worth(save);
		}
		for (String key : new String[]{"total", "history"})
		{
			for (String bad : BAD)
			{
				JsonObject o = good.deepCopy();
				o.add(key, PARSER.parse(bad));
				Savings.Worth read = worth(o.toString());
				if (read != null)
				{
					assertTrue(read.getTotal() > 0);
				}
			}
		}
		for (String bad : BAD)
		{
			JsonObject o = good.deepCopy();
			o.getAsJsonArray("history").set(3, PARSER.parse(bad));
			// One broken point leaves the pace to the others.
			assertEquals(bad, Long.valueOf(10_000_000L), pace(worth(o.toString())));
		}
		// Worth no account has, from any point, is no pace at all rather than a wild one.
		for (long v : new long[]{Long.MAX_VALUE, Long.MIN_VALUE, -5})
		{
			JsonObject o = good.deepCopy();
			o.getAsJsonArray("history").get(6).getAsJsonObject().addProperty("v", v);
			Long pace = pace(worth(o.toString()));
			assertTrue(v + " gave " + pace, pace == null || Math.abs(pace) < 1_000_000_000L);
		}
		JsonObject o = good.deepCopy();
		o.addProperty("total", Long.MAX_VALUE);
		Savings.Worth huge = worth(o.toString());
		assertTrue(huge == null || huge.getTotal() <= 10_000_000_000_000L);
	}

	@Test
	public void sessionChroniclesJournalAndLiveSession()
	{
		JsonArray journal = new JsonArray();
		for (int i = 0; i < 3; i++)
		{
			journal.add(PARSER.parse("{\"startedAt\":" + (NOW - (i + 1) * DAY) + ",\"playedMs\":3600000,\"kills\":{"
				+ "\"vorkath\":{\"name\":\"Vorkath\",\"looted\":20,\"firstKc\":400,\"lastKc\":419}}}"));
		}
		String whole = journal.toString();
		assertEquals(60, vorkaths(Fights.read(GSON, whole, true)));
		for (String save : cut(whole))
		{
			tally(Fights.read(GSON, save, true));
			tally(Fights.read(GSON, save, false));
		}
		for (String bad : BAD)
		{
			JsonArray j = journal.deepCopy();
			j.set(1, PARSER.parse(bad));
			// One broken session leaves the other two.
			assertEquals(bad, 40, vorkaths(Fights.read(GSON, j.toString(), true)));
			for (String field : new String[]{"startedAt", "kills"})
			{
				j = journal.deepCopy();
				j.get(1).getAsJsonObject().add(field, PARSER.parse(bad));
				assertTrue(field + " = " + bad, vorkaths(Fights.read(GSON, j.toString(), true)) >= 40);
			}
			for (String field : new String[]{"looted", "firstKc", "lastKc", "counters", "tasks", "event", "name"})
			{
				j = journal.deepCopy();
				j.get(1).getAsJsonObject().getAsJsonObject("kills").getAsJsonObject("vorkath").add(field, PARSER.parse(bad));
				int n = vorkaths(Fights.read(GSON, j.toString(), true));
				assertTrue(field + " = " + bad + " gave " + n, n >= 40 && n <= 40 + Fights.MAX_KILLS);
			}
			JsonObject live = journal.get(0).getAsJsonObject().deepCopy();
			live.add("kills", PARSER.parse(bad));
			tally(Fights.read(GSON, live.toString(), false));
		}
		// Kill counts no session could have are not counted as a lifetime of fighting, nor wrap below zero.
		String[] huge = {
			"{\"name\":\"Vorkath\",\"looted\":2147483647}",
			"{\"name\":\"Vorkath\",\"firstKc\":-2147483648,\"lastKc\":2147483647}",
			"{\"name\":\"Vorkath\",\"counters\":{\"vorkath\":[-2147483648,2147483647],\"vork\":[5,1]}}",
			"{\"name\":\"Vorkath\",\"counters\":{\"vorkath\":[1,2,3]},\"tasks\":{\"Vorkath\":-5,\"\":2147483647}}",
		};
		for (String line : huge)
		{
			JsonArray j = journal.deepCopy();
			j.get(1).getAsJsonObject().getAsJsonObject("kills").add("vorkath", PARSER.parse(line));
			j.get(2).getAsJsonObject().getAsJsonObject("kills").add("vorkath", PARSER.parse(line));
			int n = vorkaths(Fights.read(GSON, j.toString(), true));
			assertTrue(line + " gave " + n, n >= 20 && n <= 20 + 2 * Fights.MAX_KILLS);
		}
	}

	@Test
	public void whateverIsLeftOutIsSetAsideBeforeItIsWrittenOver()
	{
		Claims claims = new Claims();
		claims.add(new Claims.Claim("Abyssal whip", 4151, 0.42, "Vorkath", NOW - DAY));
		claims.add(new Claims.Claim("Dragon defender", 12954, 0.21, "Vorkath", NOW));
		String whole = GSON.toJson(claims);
		String goal = GSON.toJson(new Savings.Goal("Twisted bow", Collections.singletonList(20997)));
		// What this version writes reads whole: nothing to set aside.
		assertTrue(!Saved.lost(GSON, whole, Claims.read(GSON, whole)));
		assertTrue(!Saved.lost(GSON, goal, Savings.readGoal(GSON, goal)));
		assertTrue(!Saved.lost(GSON, null, Claims.read(GSON, null)));
		// A later version's field, or a broken claim, would be gone at the next save: set aside first.
		JsonObject later = PARSER.parse(whole).getAsJsonObject();
		later.getAsJsonArray("claims").get(0).getAsJsonObject().addProperty("style", "ranged");
		assertTrue(Saved.lost(GSON, later.toString(), Claims.read(GSON, later.toString())));
		JsonObject broken = PARSER.parse(whole).getAsJsonObject();
		broken.getAsJsonArray("claims").set(1, PARSER.parse("\"garbage\""));
		assertEquals(1, Claims.read(GSON, broken.toString()).recent().size());
		assertTrue(Saved.lost(GSON, broken.toString(), Claims.read(GSON, broken.toString())));
		JsonObject newer = PARSER.parse(goal).getAsJsonObject();
		newer.addProperty("deadline", NOW);
		assertTrue(Saved.lost(GSON, newer.toString(), Savings.readGoal(GSON, newer.toString())));
		assertTrue(Saved.lost(GSON, "{\"name\":", Savings.readGoal(GSON, "{\"name\":")));
		// One copy is kept: the first, then only a save at least as full, never a stub over it.
		assertTrue(Saved.setAside(null, whole));
		assertTrue(!Saved.setAside(whole, whole));
		assertTrue(!Saved.setAside(whole, "{\"claims\":["));
		assertTrue(Saved.setAside("{\"claims\":[", whole));
	}

	@Test
	public void aLongGrindKeepsEveryKill()
	{
		// Chronicle runs one session across logins a few minutes apart, so a day's grind of one monster
		// can file tens of thousands of kills on one line: real, and once counted as only 20,000.
		JsonArray journal = new JsonArray();
		journal.add(PARSER.parse("{\"startedAt\":" + (NOW - DAY) + ",\"playedMs\":64800000,\"kills\":{"
			+ "\"vorkath\":{\"name\":\"Vorkath\",\"looted\":25000}}}"));
		journal.add(PARSER.parse("{\"startedAt\":" + (NOW - 2 * DAY) + ",\"playedMs\":3600000,\"kills\":{"
			+ "\"vorkath\":{\"name\":\"Vorkath\",\"looted\":10,\"firstKc\":50000,\"lastKc\":50009}}}"));
		assertEquals(25_010, vorkaths(Fights.read(GSON, journal.toString(), true)));
		// However many sessions a damaged journal holds, the sum never wraps below zero.
		JsonArray many = new JsonArray();
		for (int i = 0; i < 3_000; i++)
		{
			many.add(PARSER.parse("{\"startedAt\":" + (NOW - DAY + i) + ",\"kills\":{\"vorkath\":{\"name\":\"Vorkath\",\"looted\":2147483647}}}"));
		}
		assertTrue(vorkaths(Fights.read(GSON, many.toString(), true)) > 0);
	}

	@Test
	public void bankStewardsBank()
	{
		String good = "{\"version\":1,\"savedAt\":" + (NOW - DAY) + ",\"items\":{\"4151\":1,\"995\":5000000},\"truncated\":false}";
		for (String save : cut(good))
		{
			steward(save);
		}
		for (String key : new String[]{"items", "savedAt", "truncated", "version"})
		{
			for (String bad : BAD)
			{
				JsonObject o = PARSER.parse(good).getAsJsonObject();
				o.add(key, PARSER.parse(bad));
				steward(o.toString());
			}
		}
		// A copy dated in the future must not count as fresher than the bank seen just now.
		Holdings.Shared future = Holdings.readSteward(GSON,
			"{\"version\":1,\"savedAt\":9223372036854775807,\"items\":{\"4151\":1}}", NOW);
		assertNotNull(future);
		assertTrue(future.getSavedAt() <= NOW);
		Holdings.Shared huge = Holdings.readSteward(GSON, "{\"items\":{\"4151\":9223372036854775807,\"995\":1e30}}", NOW);
		assertNotNull(huge);
		assertTrue(huge.getItems().get(4151) <= Integer.MAX_VALUE);
	}

	// ---- reading each key as the plugin does, then using what was read ----

	private static CodexGearRoadmapPlugin.Bank bank(String save)
	{
		try
		{
			CodexGearRoadmapPlugin.Bank read = CodexGearRoadmapPlugin.readBank(GSON, save);
			Holdings.choose(read == null ? null : read.items, read == null ? 0 : read.coins, null, 0, null, id -> data.gear(id) != null);
			return read;
		}
		catch (RuntimeException e)
		{
			throw new AssertionError("bank " + save + " threw " + e, e);
		}
	}

	private static Claims claims(String save)
	{
		try
		{
			Claims read = Claims.read(GSON, save);
			assertNotNull(read);
			for (Claims.Claim c : read.recent())
			{
				assertTrue(c.getName().length() > 0 && c.getTarget() != null && c.getGain() >= 0);
			}
			// Claimed again and saved: what was read survives the round trip with the new claim on top.
			Claims again = Claims.read(GSON, GSON.toJson(read));
			again.add(new Claims.Claim("Dragon boots", 11840, 0.1, "Vorkath", NOW));
			assertEquals(Math.min(Claims.KEPT, read.recent().size() + 1), Claims.read(GSON, GSON.toJson(again)).recent().size());
			return read;
		}
		catch (RuntimeException e)
		{
			throw new AssertionError("claims " + save + " threw " + e, e);
		}
	}

	private static Savings.Goal goal(String save)
	{
		try
		{
			Savings.Goal read = Savings.readGoal(GSON, save);
			if (read != null)
			{
				for (int id : read.getIds())
				{
					data.gear(id);
				}
				read.getName().length();
			}
			return read;
		}
		catch (RuntimeException e)
		{
			throw new AssertionError("goal " + save + " threw " + e, e);
		}
	}

	private static Savings.Worth worth(String save)
	{
		try
		{
			Savings.Worth read = Savings.readWorth(GSON, save);
			pace(read);
			return read;
		}
		catch (RuntimeException e)
		{
			throw new AssertionError("worth " + save + " threw " + e, e);
		}
	}

	private static Long pace(Savings.Worth worth)
	{
		return Savings.paceOf(worth, NOW);
	}

	private static Holdings.Shared steward(String save)
	{
		try
		{
			Holdings.Shared read = Holdings.readSteward(GSON, save, NOW);
			Holdings.choose(null, 0, null, 0, read, id -> data.gear(id) != null);
			return read;
		}
		catch (RuntimeException e)
		{
			throw new AssertionError("steward " + save + " threw " + e, e);
		}
	}

	private static Fights.Tally tally(List<Fights.Logged> sessions)
	{
		try
		{
			return Fights.tally(sessions, data.targets(), NOW);
		}
		catch (RuntimeException e)
		{
			throw new AssertionError("tally threw " + e, e);
		}
	}

	private static int vorkaths(List<Fights.Logged> sessions)
	{
		Fights.Tally t = tally(sessions);
		for (Map.Entry<com.codex.gearroadmap.data.Target, Integer> e : t.getKills().entrySet())
		{
			if (e.getKey().getName().equals("Vorkath"))
			{
				return e.getValue();
			}
		}
		return 0;
	}

	/** A good save, then every whole-save failure and the good one cut short at a few places. */
	private static List<String> cut(String good)
	{
		List<String> out = new ArrayList<>();
		Collections.addAll(out, WHOLE);
		for (int at : new int[]{1, good.length() / 3, good.length() / 2, good.length() - 1})
		{
			out.add(good.substring(0, at));
		}
		return out;
	}
}
