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

	private static Loadout named(String... names)
	{
		Loadout lo = Loadout.empty();
		for (String n : names)
		{
			Gear g = data.named(n);
			assertNotNull("No gear named " + n, g);
			lo = lo.with(g);
		}
		return lo;
	}

	private static Target mob(String name)
	{
		Target t = data.target(name);
		assertNotNull(name, t);
		return t;
	}

	@Test
	public void plainSlayerHelmetAndBlackMaskCountForMelee()
	{
		// These have no attack bonuses, and were once missing from the gear table.
		Fighter onTask = fighter(99, 99, 99, true, true);
		Target demon = mob("Abyssal demon");
		double bare = calc.best(named("Abyssal whip"), onTask, demon, Result.Kind.MELEE).getDps();
		assertTrue(calc.best(named("Abyssal whip", "Slayer helmet"), onTask, demon, Result.Kind.MELEE).getDps() > bare * 1.15);
		assertTrue(calc.best(named("Abyssal whip", "Black mask"), onTask, demon, Result.Kind.MELEE).getDps() > bare * 1.15);
		assertNotNull(data.gear(8901));
	}

	@Test
	public void imbuedSalveCountsForRanged()
	{
		Fighter f = fighter(99, 99, 99, true, false);
		Target vorkath = mob("Vorkath");
		double plain = calc.best(named("Armadyl crossbow", "Diamond dragon bolts (e)"), f, vorkath, Result.Kind.RANGED).getDps();
		double salve = calc.best(named("Armadyl crossbow", "Diamond dragon bolts (e)", "Salve amulet(i)"), f, vorkath,
			Result.Kind.RANGED).getDps();
		assertTrue(salve > plain * 1.1);
	}

	@Test
	public void iceSpellsHaveNoElementAndNeedTheRightStaff()
	{
		Fighter f = Fighter.maxed(Fighter.Boost.NONE);
		Target zulrah = mob("Zulrah");
		Result fire = calc.best(named("Staff of fire"), f, mob("General Graardor"), Result.Kind.MAGIC);
		assertEquals("A staff of fire cannot autocast ancients", "Fire Surge", fire.getSpell());
		Result ancient = calc.best(named("Ancient staff"), f, mob("General Graardor"), Result.Kind.MAGIC);
		assertEquals("Ice Barrage", ancient.getSpell());
		// Zulrah is weak to fire, but ice spells have no element, so barrage gains nothing there.
		Result barrage = calc.styled(named("Kodai wand"), f, zulrah, "magic", "Autocast");
		assertEquals("Fire Surge", barrage.getSpell());
	}

	@Test
	public void dualMacuahuitlHitsTwiceForHalf()
	{
		Fighter f = fighter(99, 99, 99, true, false);
		Result r = calc.best(named("Dual macuahuitl"), f, mob("General Graardor"), Result.Kind.MELEE);
		// The second half only lands after the first, so it is worth less than one full hit.
		double oneHit = r.getAccuracy() * r.getMaxHit() / 2.0 / (r.getTicks() * 0.6);
		assertTrue(r.getDps() < oneHit);
	}

	@Test
	public void dragonHunterCrossbowAddsToTheImbuedHelmet()
	{
		Fighter f = fighter(99, 99, 99, false, true);
		Result r = calc.styled(named("Dragon hunter crossbow", "Dragon bolts", "Slayer helmet (i)"), f, mob("Vorkath"), "ranged", "Rapid");
		// 99 Ranged, +122 ranged strength from the bolts: 27 base, then 28/20 rather than 23/20 times 5/4.
		Result bare = calc.styled(named("Dragon hunter crossbow", "Dragon bolts"), f, mob("General Graardor"), "ranged", "Rapid");
		assertEquals(bare.getMaxHit() * 28 / 20, r.getMaxHit());
	}

	@Test
	public void targetsHaveTheirOwnRules()
	{
		Fighter f = Fighter.maxed(Fighter.Boost.POTIONS);
		Calc.Npc zulrah = new Calc.Npc(mob("Zulrah"), Result.Kind.RANGED, false);
		assertEquals(47.5, zulrah.hit(80), 1e-9);
		Calc.Npc kraken = new Calc.Npc(mob("Kraken"), Result.Kind.RANGED, false);
		assertEquals(5, kraken.hit(35), 1e-9);
		assertEquals(1, kraken.hit(3), 1e-9);
		assertEquals(0, calc.best(named("Abyssal whip"), f, mob("Kraken"), Result.Kind.MELEE).getDps(), 0);
		double corp = calc.best(named("Abyssal whip"), f, mob("Corporeal Beast"), Result.Kind.MELEE).getDps();
		double spear = calc.best(named("Zamorakian hasta"), f, mob("Corporeal Beast"), Result.Kind.MELEE).getDps();
		assertTrue("hastas stab the Corporeal Beast in full", spear > corp * 1.5);
	}

	@Test
	public void revenantWeaponsOnlyShineInTheWilderness()
	{
		Fighter f = fighter(99, 99, 99, true, false);
		double wild = calc.best(named("Webweaver bow"), f, mob("Callisto"), Result.Kind.RANGED).getDps();
		Result r = calc.best(named("Webweaver bow"), f, mob("Callisto"), Result.Kind.RANGED);
		assertTrue(r.getMaxHit() > calc.best(named("Webweaver bow"), f, mob("General Graardor"), Result.Kind.RANGED).getMaxHit());
		assertTrue(wild > 0);
	}

	@Test
	public void rubyBoltsAreAveragedOverTheKill()
	{
		Calc.Npc plain = new Calc.Npc(mob("General Graardor"), Result.Kind.RANGED, false);
		// 255 hitpoints: a fifth of the health left, so far less than the 51 a full-health proc does.
		double average = Calc.rubyAverage(plain, 255, 20, 100);
		assertTrue(average > 24 && average < 27);
		assertEquals(100, Calc.rubyAverage(plain, 100000, 20, 100), 0.3);
	}
}
