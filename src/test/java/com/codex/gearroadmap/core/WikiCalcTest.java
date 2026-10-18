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
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Damage per second for varied loadouts, checked against the OSRS Wiki's DPS calculator
 * (weirdgloop/osrs-dps-calc) given the same gear, levels, potions, prayers and style.
 * Each row: target id, worn item ids by slot, attack type, stance, potions, whether
 * Rigour and Augury (else Deadeye and Mystic Vigour) are unlocked, the Kandarin hard
 * diary, on task, then Attack, Strength, Defence, Ranged, Magic and Prayer, and the
 * calculator's damage per second.
 */
public class WikiCalcTest
{
	private static final Object[][] CASES = {
		{7243, new int[]{11865, 28955, 4081, 9144, 11785, 21021, 12954, 23979, 2491, 13237, 6737}, "ranged", "Rapid", Fighter.Boost.POTIONS, false, true, true, new int[]{80, 78, 78, 76, 77, 72}, 4.771267},
		{14009, new int[]{11864, 22109, 0, 11212, 0, 23975, 0, 21024, 26235, 4131, 11770}, "crush", "Aggressive", Fighter.Boost.BEST, false, true, false, new int[]{84, 82, 85, 84, 85, 84}, 1.239983},
		{8713, new int[]{24419, 28951, 12017, 0, 1401, 0, 12954, 13073, 26235, 13237, 6733}, "crush", "Aggressive", Fighter.Boost.BEST, true, true, false, new int[]{88, 95, 96, 94, 93, 93}, 3.326283},
		{2054, new int[]{26241, 28955, 10588, 9143, 0, 8839, 6889, 4093, 2491, 0, 0}, "crush", "Accurate", Fighter.Boost.POTIONS, true, false, false, new int[]{99, 97, 99, 99, 98, 99}, 0.409946},
		{7278, new int[]{4708, 6570, 0, 9243, 25731, 4712, 6889, 11834, 0, 0, 11773}, "magic", "Accurate", Fighter.Boost.POTIONS, false, false, false, new int[]{99, 99, 99, 99, 99, 99}, 8.010005},
		{6615, new int[]{27226, 21295, 19553, 21944, 24417, 0, 0, 4087, 7461, 0, 0}, "crush", "Aggressive", Fighter.Boost.BEST, false, false, true, new int[]{90, 97, 90, 95, 89, 89}, 1.895987},
		{2215, new int[]{23971, 10499, 6585, 892, 25731, 0, 20714, 8840, 8842, 11840, 28307}, "magic", "Accurate", Fighter.Boost.BEST, true, false, false, new int[]{97, 92, 90, 97, 95, 92}, 1.785575},
		{6610, new int[]{11665, 6570, 4081, 9243, 26374, 26384, 0, 23979, 8842, 13235, 6737}, "ranged", "Rapid", Fighter.Boost.POTIONS, false, false, true, new int[]{95, 98, 95, 99, 96, 97}, 1.753491},
		{1047, new int[]{11784, 28955, 12018, 9242, 21006, 1127, 12954, 4087, 26235, 4131, 0}, "magic", "Autocast", Fighter.Boost.BEST, false, false, true, new int[]{73, 73, 76, 77, 77, 77}, 2.665621},
		{14779, new int[]{10828, 28902, 12002, 0, 4710, 8839, 20714, 0, 19544, 13235, 0}, "magic", "Autocast", Fighter.Boost.NONE, true, true, true, new int[]{99, 99, 99, 98, 99, 99}, 5.884121},
		{494, new int[]{0, 21295, 12002, 9243, 11785, 11828, 12825, 13073, 0, 13237, 6733}, "ranged", "Rapid", Fighter.Boost.POTIONS, true, true, false, new int[]{99, 99, 99, 99, 99, 99}, 0.817094},
		{319, new int[]{4089, 2412, 12017, 0, 6914, 0, 12825, 8840, 0, 4131, 0}, "magic", "Autocast", Fighter.Boost.NONE, true, false, true, new int[]{93, 89, 96, 97, 96, 96}, 0.564091},
		{2042, new int[]{26382, 6568, 0, 21946, 21012, 2503, 0, 27232, 26235, 11840, 11770}, "ranged", "Rapid", Fighter.Boost.POTIONS, true, false, false, new int[]{77, 83, 77, 83, 84, 83}, 2.895092},
		{6609, new int[]{1163, 28902, 12017, 892, 12904, 8839, 1201, 24421, 2491, 0, 0}, "magic", "Autocast", Fighter.Boost.NONE, true, false, true, new int[]{89, 96, 92, 91, 96, 98}, 2.876575},
		{2042, new int[]{11865, 28955, 4081, 9144, 12904, 21021, 1201, 0, 7462, 4097, 28310}, "magic", "Autocast", Fighter.Boost.NONE, false, true, true, new int[]{97, 98, 99, 99, 91, 91}, 5.435268},
		{319, new int[]{12931, 28902, 11128, 892, 11791, 26384, 0, 26386, 7462, 13239, 0}, "stab", "Accurate", Fighter.Boost.POTIONS, false, false, false, new int[]{88, 92, 88, 89, 87, 88}, 1.183234},
		{2042, new int[]{0, 28902, 1704, 11212, 11791, 4712, 0, 0, 22981, 13239, 11770}, "magic", "Autocast", Fighter.Boost.BEST, false, false, true, new int[]{99, 94, 99, 99, 94, 95}, 4.802749},
		{11992, new int[]{21298, 28902, 4081, 21905, 21006, 21021, 21000, 21024, 0, 13239, 11773}, "magic", "Autocast", Fighter.Boost.BEST, true, true, true, new int[]{98, 99, 99, 99, 99, 99}, 5.230577},
		{2515, new int[]{21298, 21791, 4081, 21905, 21902, 0, 1201, 4093, 22981, 13235, 28316}, "ranged", "Rapid", Fighter.Boost.BEST, true, true, false, new int[]{98, 99, 96, 98, 99, 97}, 6.103537},
		{6609, new int[]{21018, 22109, 1725, 21944, 27665, 1127, 20714, 0, 7462, 0, 11773}, "magic", "Accurate", Fighter.Boost.POTIONS, false, false, false, new int[]{92, 91, 91, 86, 85, 86}, 4.872110},
		{6610, new int[]{0, 21791, 0, 21326, 27610, 0, 0, 4714, 0, 13235, 0}, "ranged", "Rapid", Fighter.Boost.NONE, true, false, false, new int[]{77, 79, 77, 75, 76, 80}, 0.490498},
		{11992, new int[]{26241, 0, 0, 9144, 25731, 26243, 1201, 26245, 19544, 4097, 28316}, "magic", "Accurate", Fighter.Boost.BEST, false, true, false, new int[]{80, 79, 79, 84, 76, 79}, 6.252306},
		{319, new int[]{11663, 6570, 11128, 11212, 23995, 10551, 0, 21024, 7462, 13239, 0}, "stab", "Controlled", Fighter.Boost.POTIONS, false, true, false, new int[]{76, 80, 78, 79, 80, 79}, 1.002245},
		{2042, new int[]{26382, 22109, 11128, 9143, 12899, 21301, 12954, 23979, 7461, 13239, 11771}, "magic", "Accurate", Fighter.Boost.BEST, false, true, false, new int[]{76, 84, 82, 79, 79, 80}, 3.751281},
		{319, new int[]{21018, 10499, 12018, 892, 1401, 21301, 6889, 4714, 7462, 11840, 0}, "magic", "Autocast", Fighter.Boost.POTIONS, true, false, false, new int[]{82, 85, 88, 84, 85, 87}, 0.362237},
		{494, new int[]{11664, 0, 0, 21326, 29591, 27229, 0, 1079, 7462, 13237, 6737}, "ranged", "Rapid", Fighter.Boost.BEST, false, true, false, new int[]{99, 97, 99, 99, 97, 99}, 0.927504},
		{494, new int[]{0, 22109, 12018, 21326, 22555, 26243, 20714, 0, 7461, 4097, 28313}, "magic", "Accurate", Fighter.Boost.BEST, false, false, false, new int[]{96, 99, 93, 97, 99, 95}, 6.168322},
		{494, new int[]{0, 0, 12017, 9144, 31113, 21021, 6889, 26386, 11126, 4097, 11773}, "magic", "Accurate", Fighter.Boost.NONE, true, true, false, new int[]{98, 99, 99, 97, 97, 99}, 7.341766},
		{494, new int[]{3751, 21295, 4081, 0, 12904, 23975, 1201, 0, 0, 11840, 11771}, "magic", "Autocast", Fighter.Boost.NONE, false, false, true, new int[]{99, 99, 99, 99, 99, 99}, 5.032257},
		{319, new int[]{11865, 10499, 1704, 9143, 21012, 0, 20714, 4714, 26235, 13239, 28316}, "ranged", "Rapid", Fighter.Boost.NONE, false, false, true, new int[]{80, 84, 77, 78, 83, 78}, 0.370887},
		{2042, new int[]{3749, 28955, 10588, 9243, 27275, 21021, 0, 11830, 2491, 0, 11771}, "magic", "Accurate", Fighter.Boost.BEST, true, false, false, new int[]{88, 88, 85, 85, 87, 86}, 5.643529},
		{319, new int[]{22326, 22109, 19547, 21946, 11785, 0, 6889, 0, 0, 0, 28313}, "ranged", "Rapid", Fighter.Boost.POTIONS, false, false, true, new int[]{87, 92, 95, 89, 95, 91}, 0.973543},
	};

	private static RoadmapData data;
	private static Calc calc;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		calc = new Calc(data);
	}

	@Test
	public void matchesTheWikiCalculator()
	{
		for (Object[] row : CASES)
		{
			Target target = null;
			for (Target t : data.targets())
			{
				if (t.getId() == (Integer) row[0])
				{
					target = t;
				}
			}
			assertNotNull("target " + row[0], target);
			Loadout lo = Loadout.empty();
			for (int id : (int[]) row[1])
			{
				if (id > 0)
				{
					Gear g = data.gear(id);
					assertNotNull("item " + id, g);
					lo = lo.with(g);
				}
			}
			int[] lv = (int[]) row[8];
			Map<String, Integer> levels = new HashMap<>();
			levels.put("Attack", lv[0]);
			levels.put("Strength", lv[1]);
			levels.put("Defence", lv[2]);
			levels.put("Ranged", lv[3]);
			levels.put("Magic", lv[4]);
			levels.put("Prayer", lv[5]);
			boolean unlocks = (Boolean) row[5];
			Fighter f = new Fighter(levels, (Fighter.Boost) row[4], true, true, unlocks, unlocks, true, (Boolean) row[7])
				.withUnlocks(!unlocks, !unlocks, (Boolean) row[6]);
			Result r = calc.styled(lo, f, target, (String) row[2], (String) row[3]);
			double expected = (Double) row[9];
			assertEquals(target.getName() + " " + lo + " " + row[2] + " " + row[3], expected, r.getDps(), expected * 0.005);
		}
	}
}
