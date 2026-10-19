package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/** The listed monster being fought, offered as a one-click aim. */
public class FightingTest
{
	private static final long T0 = 1_790_000_000_000L;
	private static List<Target> targets;

	@BeforeClass
	public static void load() throws IOException
	{
		targets = RoadmapData.load(new Gson()).targets();
	}

	@Test
	public void matchesTheGamesOwnNamesToTheList()
	{
		Fighting f = new Fighting(targets);
		assertEquals("Abyssal demon", f.match("Abyssal demon").getName());
		assertEquals("Sand Crab", f.match("Sand crab").getName());
		assertEquals("The Leviathan", f.match("The Leviathan").getName());
		assertEquals("Grotesque Guardians", f.match("Dusk").getName());
		assertEquals("Grotesque Guardians", f.match("Dawn").getName());
		assertEquals("Vet'ion", f.match("Vet'ion").getName());
		assertEquals("Kree'arra", f.match("Kree arra".replace(' ', '\'')).getName());
		assertNull(f.match("Banker"));
		assertNull(f.match("Goblin"));
		assertNull(f.match(null));
	}

	@Test
	public void remembersAFightThroughABankTripThenLetsGo()
	{
		Fighting f = new Fighting(targets);
		assertFalse(f.saw("Goblin", T0));
		assertNull(f.current());
		assertTrue(f.saw("Vorkath", T0));
		assertFalse("the same fight again is no change", f.saw("Vorkath", T0 + 600));
		assertFalse(f.expire(T0 + 3 * 60_000));
		assertEquals("Vorkath", f.current().getName());
		assertTrue(f.expire(T0 + 600 + Fighting.FRESH_MS + 1));
		assertNull(f.current());
	}

	@Test
	public void aNewFightReplacesTheOld()
	{
		Fighting f = new Fighting(targets);
		f.saw("Vorkath", T0);
		assertTrue(f.saw("Zulrah", T0 + 60_000));
		assertEquals("Zulrah", f.current().getName());
		f.clear();
		assertNull(f.current());
	}
}
