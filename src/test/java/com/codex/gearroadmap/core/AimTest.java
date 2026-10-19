package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class AimTest
{
	private static List<Target> targets;

	@BeforeClass
	public static void load() throws IOException
	{
		targets = RoadmapData.load(new Gson()).targets();
	}

	private static String names(Aim aim)
	{
		StringBuilder b = new StringBuilder();
		for (Target t : aim.getCovered())
		{
			b.append(b.length() == 0 ? "" : ", ").append(t.getName());
		}
		return b.toString();
	}

	@Test
	public void followsTheTaskToItsOwnMonsterFirst()
	{
		Aim aim = Aim.choose(targets, "Vorkath", true, "Abyssal demons", false, null);
		assertEquals("Abyssal demon", aim.getTarget().getName());
		assertTrue(aim.isFollowing());
		assertEquals("Abyssal demon, Abyssal Sire", names(aim));

		Aim smoke = Aim.choose(targets, "Vorkath", true, "Smoke devils", false, null);
		assertEquals("Smoke devil", smoke.getTarget().getName());
	}

	@Test
	public void staysOnTheChosenTargetWithoutATaskOrWhenNotFollowing()
	{
		assertEquals("Zulrah", Aim.choose(targets, "Zulrah", true, null, false, null).getTarget().getName());
		Aim off = Aim.choose(targets, "Zulrah", false, "Abyssal demons", false, null);
		assertEquals("Zulrah", off.getTarget().getName());
		assertFalse(off.isFollowing());
		// The task's targets are still offered on the Targets tab.
		assertEquals(2, off.getCovered().size());
		// A task with nothing listed for it leaves the aim alone.
		Aim bandits = Aim.choose(targets, "Zulrah", true, "Bandits", false, null);
		assertEquals("Zulrah", bandits.getTarget().getName());
		assertFalse(bandits.isFollowing());
	}

	@Test
	public void aChosenTargetOnTheTaskIsKept()
	{
		Aim aim = Aim.choose(targets, "Cerberus", true, "Hellhounds", false, null);
		assertEquals("Cerberus", aim.getTarget().getName());
		assertTrue(aim.isFollowing());
	}

	@Test
	public void aPickWithinTheTaskWins()
	{
		Aim aim = Aim.choose(targets, "Vorkath", true, "Abyssal demons", false, "Abyssal Sire");
		assertEquals("Abyssal Sire", aim.getTarget().getName());
		// A stale pick from another task is ignored.
		assertEquals("Abyssal demon", Aim.choose(targets, "Vorkath", true, "Abyssal demons", false, "Cerberus").getTarget().getName());
	}

	@Test
	public void bossesOfABroadTaskAreOfferedNotAimedAt()
	{
		Aim bears = Aim.choose(targets, "Zulrah", true, "Bears", false, null);
		assertEquals("Zulrah", bears.getTarget().getName());
		assertFalse(bears.isFollowing());
		assertEquals("Callisto, Artio", names(bears));
		Aim blue = Aim.choose(targets, "Zulrah", true, "Blue dragons", false, null);
		assertEquals("Zulrah", blue.getTarget().getName());
		// A boss's own task does aim at it, and so does one from a boss slayer master.
		assertEquals("Vorkath", Aim.choose(targets, "Zulrah", true, "Vorkath", true, null).getTarget().getName());
		assertEquals("Abyssal Sire", Aim.choose(targets, "Zulrah", true, "The Abyssal Sire", false, null).getTarget().getName());
		assertEquals("Dagannoth Rex", Aim.choose(targets, "Zulrah", true, "Dagannoth Kings", true, null).getTarget().getName());
	}

	@Test
	public void unknownChosenTargetFallsBackToTheFirst()
	{
		assertEquals(targets.get(0), Aim.choose(targets, "Nobody", true, null, false, null).getTarget());
	}
}
