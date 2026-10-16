package com.codex.gearroadmap;

import com.codex.gearroadmap.core.Advisor;
import com.codex.gearroadmap.core.Fighter;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A believable mid-game account and rough Grand Exchange prices, for tests and previews.
 * The plugin itself always reads live prices from the client.
 */
public final class Fixtures
{
	private static final Object[][] PRICES = {
		{"Rune scimitar", 15_000}, {"Dragon scimitar", 60_000}, {"Dragon longsword", 60_000}, {"Dragon mace", 30_000},
		{"Dragon sword", 60_000}, {"Dragon warhammer", 24_000_000}, {"Abyssal whip", 1_300_000}, {12004, 300_000},
		{"Abyssal dagger", 1_400_000}, {"Abyssal bludgeon", 15_000_000}, {"Zamorakian hasta", 6_000_000},
		{"Saradomin sword", 180_000}, {"Dragon claws", 50_000_000}, {"Bandos godsword", 11_000_000},
		{"Elder maul", 2_500_000}, {25859, 6_000_000}, {"Ghrazi rapier", 12_000_000}, {"Inquisitor's mace", 26_000_000},
		{"Scythe of Vitur", 1_100_000_000}, {"Osmumten's fang", 25_000_000}, {"Voidwaker", 50_000_000},
		{"Noxious halberd", 20_000_000}, {"Soulreaper axe", 120_000_000}, {"Dual macuahuitl", 20_000_000},
		{"Dragon hunter lance", 30_000_000}, {"Tzhaar-ket-om", 60_000}, {"Toktz-xil-ak", 50_000},
		{"Rune full helm", 20_000}, {"Berserker helm", 40_000}, {"Helm of Neitiznot", 50_000},
		{"Serpentine helm", 3_000_000}, {24268, 12_000_000}, {"Justiciar faceguard", 20_000_000},
		{"Inquisitor's great helm", 9_000_000}, {26376, 150_000_000}, {"Obsidian helmet", 300_000},
		{"Rune platebody", 38_000}, {"Obsidian platebody", 1_200_000}, {"Bandos chestplate", 18_000_000},
		{"Inquisitor's hauberk", 14_000_000}, {26378, 230_000_000}, {"Rune platelegs", 38_000},
		{"Dragon platelegs", 160_000}, {"Obsidian platelegs", 900_000}, {"Bandos tassets", 26_000_000},
		{"Inquisitor's plateskirt", 13_000_000}, {26380, 200_000_000}, {"Rune kiteshield", 32_000},
		{22477, 35_000_000}, {"Amulet of strength", 3_000}, {"Amulet of glory", 12_000}, {"Amulet of fury", 3_000_000},
		{6585, 3_000_000}, {"Amulet of torture", 9_000_000}, {24777, 15_000_000}, {"Berserker necklace", 800_000},
		{"Obsidian cape", 50_000}, {"Combat bracelet", 15_000}, {22983, 30_000_000}, {"Rune boots", 7_000},
		{"Dragon boots", 250_000}, {"Primordial boots", 25_000_000}, {"Berserker ring", 2_500_000}, {6737, 2_500_000},
		{"Ultor ring", 160_000_000}, {"Bellator ring", 30_000_000}, {"Magic shortbow", 1_500}, {861, 1_500},
		{"Rune crossbow", 10_000}, {"Dragon crossbow", 3_000_000}, {"Dark bow", 300_000}, {"Armadyl crossbow", 33_000_000},
		{"Dragon hunter crossbow", 60_000_000}, {"Heavy ballista", 5_000_000}, {12924, 3_500_000}, {4207, 600_000},
		{"Venator bow", 30_000_000}, {"Twisted bow", 1_600_000_000}, {"Zaryte crossbow", 280_000_000},
		{"Rune arrow", 50}, {"Amethyst arrow", 300}, {"Dragon arrow", 1_200}, {"Adamant bolts", 80}, {"Runite bolts", 150},
		{"Diamond bolts (e)", 1_300}, {"Ruby bolts (e)", 1_200}, {"Dragon bolts", 1_800}, {"Ruby dragon bolts (e)", 2_600},
		{"Diamond dragon bolts (e)", 4_500}, {"Dragon javelin", 900}, {"Black d'hide body", 8_000},
		{"Black d'hide chaps", 5_000}, {"Black d'hide vambraces", 3_000}, {"Archer helm", 40_000},
		{"Armadyl helmet", 8_000_000}, {"Armadyl chestplate", 25_000_000}, {"Armadyl chainskirt", 26_000_000},
		{"Masori mask", 8_000_000}, {"Masori body", 20_000_000}, {"Masori chaps", 16_000_000}, {23956, 4_000_000},
		{"Amulet of power", 2_000}, {"Necklace of anguish", 8_000_000}, {"Zaryte vambraces", 32_000_000},
		{"Pegasian boots", 30_000_000}, {"Archers ring", 2_500_000}, {6733, 2_500_000}, {"Venator ring", 15_000_000},
		{"Twisted buckler", 18_000_000}, {"Staff of fire", 1_500}, {"Mystic fire staff", 30_000}, {"Ancient staff", 70_000},
		{"Master wand", 1_500_000}, {"Kodai wand", 70_000_000}, {"Staff of the Dead", 1_500_000}, {11791, 1_500_000},
		{12932, 8_000_000}, {"Warped sceptre", 1_000_000}, {"Trident of the Seas", 200_000}, {11908, 200_000},
		{"Sanguinesti staff", 40_000_000}, {24422, 400_000}, {24511, 60_000_000}, {"Eye of Ayak", 130_000_000},
		{"Tumeken's shadow", 1_400_000_000}, {"Mystic hat", 15_000}, {"Mystic robe top", 90_000},
		{"Mystic robe bottom", 60_000}, {"Ahrim's hood", 300_000}, {"Ahrim's robetop", 1_200_000},
		{"Ahrim's robeskirt", 1_400_000}, {"Ancestral hat", 25_000_000}, {"Ancestral robe top", 65_000_000},
		{"Ancestral robe bottom", 50_000_000}, {"Occult necklace", 900_000}, {"Arcane spirit shield", 35_000_000},
		{"Tormented bracelet", 9_000_000}, {"Mystic boots", 10_000}, {"Eternal boots", 3_500_000}, {6731, 3_000_000},
		{"Magus ring", 4_000_000},
	};

	private static final String[] MID_GAME = {
		"Abyssal whip", "Dragon defender", "Fighter torso", "Dragon platelegs", "Helm of Neitiznot", "Amulet of fury",
		"Fire cape", "Barrows gloves", "Dragon boots", "Berserker ring (i)", "Dragon scimitar", "Saradomin sword",
		"Magic shortbow (i)", "Rune crossbow", "Runite bolts", "Amethyst arrow", "Black d'hide body", "Black d'hide chaps",
		"Black d'hide vambraces", "Archer helm", "Ava's accumulator", "Toxic blowpipe", "Amulet of glory",
		"Trident of the Seas", "Mystic hat", "Mystic robe top", "Mystic robe bottom", "Mystic boots", "Occult necklace",
		"Saradomin cape", "Salve amulet (e)", "Archers ring (i)", "Seers ring (i)",
	};

	private Fixtures()
	{
	}

	public static Advisor.Prices prices(RoadmapData data)
	{
		Map<Integer, Long> byId = new HashMap<>();
		for (Object[] row : PRICES)
		{
			long price = ((Number) row[1]).longValue();
			if (row[0] instanceof Integer)
			{
				byId.put((Integer) row[0], price);
				continue;
			}
			Gear g = data.named((String) row[0]);
			if (g == null)
			{
				throw new IllegalStateException("No gear named " + row[0]);
			}
			for (int id : g.getIds())
			{
				byId.put(id, price);
			}
		}
		return id -> byId.getOrDefault(id, 0L);
	}

	public static Set<Gear> midGame(RoadmapData data)
	{
		Set<Gear> owned = new LinkedHashSet<>();
		for (String name : MID_GAME)
		{
			Gear g = data.named(name);
			if (g == null)
			{
				throw new IllegalStateException("No gear named " + name);
			}
			owned.add(g);
		}
		return owned;
	}

	public static Fighter midGameFighter()
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Attack", 85);
		levels.put("Strength", 88);
		levels.put("Defence", 80);
		levels.put("Ranged", 86);
		levels.put("Magic", 82);
		levels.put("Prayer", 74);
		levels.put("Hitpoints", 87);
		levels.put("Agility", 72);
		return new Fighter(levels, Fighter.Boost.POTIONS, true, true, false, false, false, false);
	}
}
