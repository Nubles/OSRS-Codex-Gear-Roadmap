package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Max hits checked against in-game cases recorded for the OSRS Wiki's DPS calculator:
 * the same gear, levels and prayers must give the same max hit here.
 */
public class CalcTest
{
	private static final int[] TORVA_MELEE = {26382, 21285, 19553, 26384, 22322, 26386, 22981, 13239, 11773};
	private static final int[] VOID_MELEE = {11665, 21285, 19553, 13072, 22322, 13073, 8842, 13239, 11773};
	private static final int[] ANCESTRAL = {21018, 21780, 12002, 21021, 21024, 19544, 13235, 28313};

	private static RoadmapData data;
	private static Calc calc;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		calc = new Calc(data);
	}

	private static Target target(int id)
	{
		for (Target t : data.targets())
		{
			if (t.getId() == id)
			{
				return t;
			}
		}
		throw new AssertionError("No target " + id);
	}

	private static Loadout wear(int[] base, int... swaps)
	{
		Loadout lo = Loadout.empty();
		for (int id : base)
		{
			lo = lo.with(gear(id));
		}
		for (int id : swaps)
		{
			lo = lo.with(gear(id));
		}
		return lo;
	}

	private static Gear gear(int id)
	{
		Gear g = data.gear(id);
		assertNotNull("No gear " + id, g);
		return g;
	}

	private static Fighter fighter(int attack, int strength, int magic, boolean prayers, boolean onTask)
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Attack", attack);
		levels.put("Strength", strength);
		levels.put("Defence", 99);
		levels.put("Ranged", 99);
		levels.put("Magic", magic);
		levels.put("Prayer", 99);
		return new Fighter(levels, Fighter.Boost.NONE, prayers, true, true, true, false, onTask);
	}

	private static int melee(Loadout lo, int target, String type, String stance, boolean onTask)
	{
		return calc.styled(lo, fighter(118, 118, 99, true, onTask), target(target), type, stance).getMaxHit();
	}

	private static int mage(Loadout lo, int target, boolean onTask)
	{
		return calc.styled(lo, fighter(99, 99, 112, false, onTask), target(target), "magic", "Accurate").getMaxHit();
	}

	@Test
	public void fang()
	{
		assertEquals(50, melee(wear(TORVA_MELEE, 26219), 415, "stab", "Aggressive", false));
		assertEquals(57, melee(wear(TORVA_MELEE, 26219, 12018), 8059, "stab", "Aggressive", false));
		assertEquals(56, melee(wear(TORVA_MELEE, 26219, 11865), 415, "stab", "Aggressive", true));
		assertEquals(47, melee(wear(VOID_MELEE, 26219), 415, "stab", "Aggressive", false));
		assertEquals(53, melee(wear(VOID_MELEE, 26219, 12018), 8059, "stab", "Aggressive", false));
	}

	@Test
	public void dragonHunterLance()
	{
		assertEquals(58, melee(wear(TORVA_MELEE, 22978), 8059, "stab", "Controlled", false));
	}

	@Test
	public void obsidian()
	{
		int[] obsidian = {21298, 21285, 19553, 21301, 22322, 21304, 22981, 13239, 11773};
		assertEquals(46, melee(wear(obsidian, 6523), 415, "stab", "Aggressive", false));
		assertEquals(52, melee(wear(obsidian, 6523, 12018), 8059, "stab", "Aggressive", false));
	}

	@Test
	public void tumekensShadow()
	{
		assertEquals(65, mage(wear(ANCESTRAL, 27275), 415, false));
		assertEquals(67, mage(wear(ANCESTRAL, 27275, 12018), 8059, false));
		assertEquals(70, mage(wear(ANCESTRAL, 27275, 11865), 415, true));
		int[] voidMage = {11663, 21780, 12002, 13072, 13073, 8842, 13235, 28313};
		assertEquals(51, mage(wear(voidMage, 27275), 415, false));
	}

	@Test
	public void hitChance()
	{
		assertEquals(0.5, Calc.hitChance(1000, 1000), 0.001);
		assertTrue(Calc.hitChance(20000, 10000) > 0.7);
		assertTrue(Calc.fangChance(10000, 10000) > Calc.hitChance(10000, 10000));
	}

	@Test
	public void twistedBowScalesWithMagic()
	{
		// At the 250 magic cap the bow tops out at 140% accuracy and 215% damage.
		assertEquals(140, Calc.twistedBow(100, 250, true));
		assertEquals(215, Calc.twistedBow(100, 250, false));
		assertTrue(Calc.twistedBow(100, 1, false) < 100);
	}

	@Test
	public void bestPicksAStyle()
	{
		Result r = calc.best(wear(TORVA_MELEE, 4151), Fighter.maxed(Fighter.Boost.POTIONS), target(415));
		assertEquals(Result.Kind.MELEE, r.getKind());
		assertTrue(r.getDps() > 3);
		assertEquals(4, r.getTicks());
	}
}
