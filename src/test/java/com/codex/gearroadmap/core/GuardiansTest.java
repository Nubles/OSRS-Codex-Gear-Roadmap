package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.CatalogueEntry;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The Grotesque Guardians are two fights in one: Dusk takes only melee, while Dawn can also be
 * shot. A player reported that the roadmap never offered the granite hammer, which does about as
 * well as far dearer weapons there, and never looked at ranged for Dawn.
 */
public class GuardiansTest
{
	private static RoadmapData data;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
	}

	@Test
	public void theGraniteHammerIsOnTheShoppingList()
	{
		boolean listed = false;
		for (CatalogueEntry e : data.catalogue())
		{
			listed |= "Granite hammer".equals(e.getName());
		}
		assertTrue(listed);
	}

	@Test
	public void dawnCanBeShotButDuskCannot()
	{
		Target dusk = data.target("Grotesque Guardians");
		Target dawn = data.target("Dawn (Grotesque Guardians)");
		assertTrue(dusk.allows("MELEE"));
		assertFalse(dusk.allows("RANGED"));
		assertTrue(dawn.allows("MELEE"));
		assertTrue(dawn.allows("RANGED"));
		assertFalse(dawn.allows("MAGIC"));
	}

	@Test
	public void aBowDoesDamageToDawn()
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String skill : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer"})
		{
			levels.put(skill, 90);
		}
		Fighter f = new Fighter(levels, Fighter.Boost.POTIONS, true, true, true, true, true, true);
		Gear bow = data.gear(27610);
		Loadout venator = Loadout.empty().with(bow).with(data.gear(11212));
		Calc calc = new Calc(data);
		assertTrue(calc.best(venator, f, data.target("Dawn (Grotesque Guardians)")).getDps() > 0);
		assertFalse(calc.best(venator, f, data.target("Grotesque Guardians")).getDps() > 0);
	}
}
