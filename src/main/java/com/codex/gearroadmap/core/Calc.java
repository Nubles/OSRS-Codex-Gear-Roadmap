package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.data.Target;
import java.util.Set;

/**
 * The game's combat formulas for one loadout against one monster: effective levels,
 * attack and defence rolls, max hits, and the special effects that change them, such
 * as the salve amulet, slayer helmet, void, crystal armour, the twisted bow and the
 * dragon and demon bane weapons. Every style the weapon offers is tried and the
 * fastest one is kept.
 */
public final class Calc
{
	private static final double TICK = 0.6;

	private static final String[][] STANDARD_SPELLS = {
		{"Fire Strike", "13", "8"}, {"Fire Bolt", "35", "12"}, {"Fire Blast", "59", "16"},
		{"Fire Wave", "75", "20"}, {"Fire Surge", "95", "24"},
	};
	private static final String[][] ANCIENT_SPELLS = {
		{"Ice Rush", "58", "16"}, {"Ice Burst", "70", "22"}, {"Ice Blitz", "82", "26"}, {"Ice Barrage", "94", "30"},
	};

	private final RoadmapData data;

	public Calc(RoadmapData data)
	{
		this.data = data;
	}

	/** The best result over every style the weapon offers. */
	public Result best(Loadout loadout, Fighter fighter, Target target)
	{
		return best(loadout, fighter, target, null);
	}

	/** The best result using only one kind of attack, or any when kind is null. */
	public Result best(Loadout loadout, Fighter fighter, Target target, Result.Kind kind)
	{
		return evaluate(loadout, fighter, target, kind, null, null);
	}

	/** One attack type and stance, such as "stab" and "Aggressive", as the equipment screen would set it. */
	public Result styled(Loadout loadout, Fighter fighter, Target target, String type, String stance)
	{
		return evaluate(loadout, fighter, target, null, type, stance);
	}

	private Result evaluate(Loadout loadout, Fighter fighter, Target target, Result.Kind kind, String onlyType, String onlyStance)
	{
		Gear weapon = loadout.weapon();
		String category = weapon == null ? "Unarmed" : weapon.getCategory();
		Totals totals = new Totals(loadout, data.ammoFor(weapon));
		Result best = Result.NONE;
		for (RoadmapData.Style style : data.styles(category))
		{
			String type = style.getType();
			String stance = style.getStance();
			if (stance == null || stance.startsWith("Defensive") || "Longrange".equals(stance) && !"magic".equals(type)
				|| onlyType != null && !(onlyType.equals(type) && onlyStance.equals(stance)))
			{
				continue;
			}
			Result r;
			if ("magic".equals(type))
			{
				if (kind != null && kind != Result.Kind.MAGIC)
				{
					continue;
				}
				r = "Autocast".equals(stance) ? autocast(loadout, totals, fighter, target) : powered(loadout, totals, fighter, target, stance);
			}
			else if ("ranged".equals(type))
			{
				if (kind != null && kind != Result.Kind.RANGED)
				{
					continue;
				}
				r = ranged(loadout, totals, fighter, target, stance);
			}
			else
			{
				if (kind != null && kind != Result.Kind.MELEE)
				{
					continue;
				}
				r = melee(loadout, totals, fighter, target, type, stance);
			}
			if (r.isBetterThan(best))
			{
				best = r;
			}
		}
		return best;
	}

	/** Summed bonuses, leaving out ammunition the weapon does not fire. */
	static final class Totals
	{
		final int[] offence = new int[5];
		int strength;
		int rangedStrength;
		int magicDamage;
		boolean ammoValid = true;

		Totals(Loadout loadout, Set<Integer> fires)
		{
			Gear ammo = loadout.get(Gear.AMMO);
			boolean countAmmo = fires != null && !fires.isEmpty();
			if (countAmmo)
			{
				ammoValid = ammo != null && firesAny(fires, ammo);
			}
			for (int slot = 0; slot < Gear.SLOTS; slot++)
			{
				Gear g = loadout.get(slot);
				if (g == null)
				{
					continue;
				}
				boolean projectileIgnored = slot == Gear.AMMO && !countAmmo;
				for (int t = 0; t < 5; t++)
				{
					if (!(projectileIgnored && t == Gear.RANGED))
					{
						offence[t] += g.offence(t);
					}
				}
				strength += g.strength();
				if (!projectileIgnored)
				{
					rangedStrength += g.rangedStrength();
				}
				magicDamage += g.magicDamage();
			}
		}

		private static boolean firesAny(Set<Integer> fires, Gear ammo)
		{
			for (int id : ammo.getIds())
			{
				if (fires.contains(id))
				{
					return true;
				}
			}
			return false;
		}
	}

	private Result melee(Loadout lo, Totals tot, Fighter f, Target t, String type, String stance)
	{
		int attackType = "stab".equals(type) ? Gear.STAB : "slash".equals(type) ? Gear.SLASH : Gear.CRUSH;
		Fighter.Prayer prayer = f.meleePrayer();
		int effAttack = f.attack() * prayer.accuracy / 100 + 8 + ("Accurate".equals(stance) ? 3 : "Controlled".equals(stance) ? 1 : 0);
		int effStrength = f.strength() * prayer.strength / 100 + 8 + ("Aggressive".equals(stance) ? 3 : "Controlled".equals(stance) ? 1 : 0);
		if (voidSet(lo, "void melee helm", false))
		{
			effAttack = effAttack * 11 / 10;
			effStrength = effStrength * 11 / 10;
		}
		long baseRoll = (long) effAttack * (tot.offence[attackType] + 64);
		int baseMax = (effStrength * (tot.strength + 64) + 320) / 640;
		long roll = baseRoll;
		int max = baseMax;

		boolean undead = t.is("undead");
		if (undead && lo.wearsAny("salve amulet (e)", "salve amulet(ei)"))
		{
			roll = roll * 6 / 5;
			max = max * 6 / 5;
		}
		else if (undead && lo.wearsAny("salve amulet", "salve amulet(i)"))
		{
			roll = roll * 7 / 6;
			max = max * 7 / 6;
		}
		else if (slayerBonus(lo, f, t, false))
		{
			roll = roll * 7 / 6;
			max = max * 7 / 6;
		}
		if (tzhaarWeapon(lo) && lo.wears("obsidian helmet") && lo.wears("obsidian platebody") && lo.wears("obsidian platelegs"))
		{
			roll += baseRoll / 10;
			max += baseMax / 10;
		}
		if (tzhaarWeapon(lo) && lo.wears("berserker necklace"))
		{
			max = max * 6 / 5;
		}
		if (t.is("demon"))
		{
			int bane = lo.wearsAny("arclight", "emberlight") ? 70 : lo.wearsAny("silverlight", "darklight") ? 60 : 0;
			roll += roll * bane / 100;
			max += max * bane / 100;
		}
		if (t.is("dragon") && lo.wears("dragon hunter lance"))
		{
			roll = roll * 6 / 5;
			max = max * 6 / 5;
		}
		if (attackType == Gear.CRUSH)
		{
			int inquisitor = (lo.wears("inquisitor's great helm") ? 1 : 0) + (lo.wears("inquisitor's hauberk") ? 2 : 0)
				+ (lo.wears("inquisitor's plateskirt") ? 2 : 0);
			roll = roll * (200 + inquisitor) / 200;
			max = max * (200 + inquisitor) / 200;
		}

		long defence = (long) (t.getDefence() + 9) * (t.defensive(attackType) + 64);
		Gear weapon = lo.weapon();
		boolean fang = weapon != null && weapon.key().startsWith("osmumten's fang");
		double accuracy = fang ? fangChance(roll, defence) : hitChance(roll, defence);
		double perHit = max / 2.0;
		if (fang)
		{
			// The fang never rolls in the bottom or top 15% of its range.
			int low = max * 3 / 20;
			max -= low;
			perHit = (low + max) / 2.0;
		}
		double damage = accuracy * perHit;
		if (weapon != null && weapon.key().contains("of vitur") && t.getSize() > 1)
		{
			// The scythe's second and third swings hit large monsters for half and a quarter.
			damage += accuracy * (max / 2) / 2.0;
			if (t.getSize() > 2)
			{
				damage += accuracy * (max / 4) / 2.0;
			}
		}
		int ticks = speed(weapon);
		String label = capital(type) + ", " + stance.toLowerCase();
		return new Result(Result.Kind.MELEE, damage / (ticks * TICK), max, accuracy, ticks, label, prayer.name, null);
	}

	private Result ranged(Loadout lo, Totals tot, Fighter f, Target t, String stance)
	{
		if (!tot.ammoValid)
		{
			return Result.NONE;
		}
		Gear weapon = lo.weapon();
		Fighter.Prayer prayer = f.rangedPrayer();
		int effAttack = f.ranged() * prayer.accuracy / 100 + 8 + ("Accurate".equals(stance) ? 3 : 0);
		int effStrength = f.ranged() * prayer.strength / 100 + 8 + ("Accurate".equals(stance) ? 3 : 0);
		if (voidSet(lo, "void ranger helm", false))
		{
			effAttack = effAttack * 11 / 10;
			effStrength = voidSet(lo, "void ranger helm", true) ? effStrength * 9 / 8 : effStrength * 11 / 10;
		}
		int strength = tot.rangedStrength;
		if (weapon != null && weapon.key().startsWith("toxic blowpipe"))
		{
			// Loaded with dragon darts.
			strength += 35;
		}
		long roll = (long) effAttack * (tot.offence[Gear.RANGED] + 64);
		int max = (effStrength * (strength + 64) + 320) / 640;

		if (weapon != null && (weapon.key().startsWith("crystal bow") || weapon.key().startsWith("bow of faerdhinen")))
		{
			int pieces = (lo.wears("crystal helm") ? 1 : 0) + (lo.wears("crystal legs") ? 2 : 0) + (lo.wears("crystal body") ? 3 : 0);
			roll = roll * (20 + pieces) / 20;
			max = max * (40 + pieces) / 40;
		}
		boolean undead = t.is("undead");
		if (undead && lo.wears("salve amulet(ei)"))
		{
			roll = roll * 6 / 5;
			max = max * 6 / 5;
		}
		else if (undead && lo.wears("salve amulet(i)"))
		{
			roll = roll * 7 / 6;
			max = max * 7 / 6;
		}
		else if (slayerBonus(lo, f, t, true))
		{
			roll = roll * 23 / 20;
			max = max * 23 / 20;
		}
		if (weapon != null && weapon.key().equals("twisted bow"))
		{
			int magic = Math.min(250, Math.max(t.getMagic(), t.getMagicAttack()));
			roll = twistedBow(roll, magic, true);
			max = (int) twistedBow(max, magic, false);
		}
		if (t.is("dragon") && lo.wears("dragon hunter crossbow"))
		{
			roll = roll * 13 / 10;
			max = max * 5 / 4;
		}

		String category = weapon == null ? "" : weapon.getCategory();
		int defType = "Thrown".equals(category) ? Target.D_LIGHT : "Bow".equals(category) ? Target.D_STANDARD : Target.D_HEAVY;
		long defence = (long) (t.getDefence() + 9) * (t.defensive(defType) + 64);
		double accuracy = hitChance(roll, defence);
		double damage = accuracy * max / 2.0;

		Gear ammo = lo.get(Gear.AMMO);
		if ("Crossbow".equals(category) && ammo != null)
		{
			boolean zaryte = lo.wears("zaryte crossbow");
			if (ammo.key().startsWith("diamond bolts (e)") || ammo.key().startsWith("diamond dragon bolts (e)"))
			{
				// One bolt in ten pierces armour: it always hits, for up to 15% more.
				double effect = (max * (zaryte ? 126 : 115) / 100) / 2.0;
				damage = 0.1 * effect + 0.9 * damage;
			}
			else if (ammo.key().startsWith("ruby bolts (e)") || ammo.key().startsWith("ruby dragon bolts (e)"))
			{
				// Six in a hundred take a fifth of the monster's health, up to 100.
				double effect = Math.min(zaryte ? 110 : 100, t.getHp() * (zaryte ? 22 : 20) / 100);
				damage = 0.06 * effect + 0.94 * damage;
			}
		}
		int ticks = Math.max(1, speed(weapon) - ("Rapid".equals(stance) ? 1 : 0));
		return new Result(Result.Kind.RANGED, damage / (ticks * TICK), max, accuracy, ticks, stance, prayer.name, null);
	}

	/** Staves with their own spell built in, such as tridents and Tumeken's shadow. */
	private Result powered(Loadout lo, Totals tot, Fighter f, Target t, String stance)
	{
		Gear weapon = lo.weapon();
		if (weapon == null)
		{
			return Result.NONE;
		}
		int level = f.magic();
		String key = weapon.key();
		int base;
		if (key.startsWith("trident of the seas"))
		{
			base = Math.max(1, level / 3 - 5);
		}
		else if (key.startsWith("trident of the swamp"))
		{
			base = Math.max(1, level / 3 - 2);
		}
		else if (key.contains("sanguinesti staff"))
		{
			base = Math.max(1, level / 3);
		}
		else if (key.equals("tumeken's shadow"))
		{
			base = Math.max(1, level / 3 + 1);
		}
		else if (key.equals("eye of ayak"))
		{
			base = Math.max(1, level / 3 - 6);
		}
		else if (key.equals("warped sceptre"))
		{
			base = Math.max(1, (8 * level + 96) / 37);
		}
		else if (key.equals("accursed sceptre"))
		{
			base = Math.max(1, level / 3 - 6);
		}
		else if (key.equals("thammaron's sceptre"))
		{
			base = Math.max(1, level / 3 - 8);
		}
		else
		{
			return Result.NONE;
		}
		boolean shadow = key.equals("tumeken's shadow");
		return magic(lo, tot, f, t, base, "Accurate".equals(stance) ? 2 : 0, shadow, speed(weapon), null, null,
			"Accurate".equals(stance) ? "Accurate" : "Longrange");
	}

	/** Staves that autocast a combat spell: the strongest fire spell, or ice from the Ancient Magicks. */
	private Result autocast(Loadout lo, Totals tot, Fighter f, Target t)
	{
		Result best = Result.NONE;
		int real = f.level("Magic");
		Gear weapon = lo.weapon();
		boolean harmonised = weapon != null && weapon.key().startsWith("harmonised nightmare staff");
		String[] fire = pick(STANDARD_SPELLS, real);
		if (fire != null)
		{
			best = magic(lo, tot, f, t, Integer.parseInt(fire[2]), 0, false, harmonised ? 4 : 5, fire[0], "fire", "Autocast");
		}
		String[] ice = f.isAncients() ? pick(ANCIENT_SPELLS, real) : null;
		if (ice != null)
		{
			Result r = magic(lo, tot, f, t, Integer.parseInt(ice[2]), 0, false, 5, ice[0], "water", "Autocast");
			if (r.isBetterThan(best))
			{
				best = r;
			}
		}
		return best;
	}

	private static String[] pick(String[][] spells, int level)
	{
		String[] best = null;
		for (String[] spell : spells)
		{
			if (level >= Integer.parseInt(spell[1]))
			{
				best = spell;
			}
		}
		return best;
	}

	private Result magic(Loadout lo, Totals tot, Fighter f, Target t, int base, int stanceBonus, boolean shadow, int ticks,
		String spell, String element, String style)
	{
		Fighter.Prayer prayer = f.magicPrayer();
		int effMagic = f.magic() * prayer.accuracy / 100 + 9 + stanceBonus;
		if (voidSet(lo, "void mage helm", false))
		{
			effMagic = effMagic * 29 / 20;
		}
		int bonus = tot.offence[Gear.MAGIC];
		int damageBonus = tot.magicDamage;
		if (shadow)
		{
			bonus *= 3;
			damageBonus = Math.min(1000, damageBonus * 3);
		}
		if (voidSet(lo, "void mage helm", true))
		{
			damageBonus += 50;
		}
		long baseRoll = (long) effMagic * (bonus + 64);
		long roll = baseRoll;
		boolean undead = t.is("undead");
		boolean mask = false;
		if (undead && lo.wears("salve amulet(ei)"))
		{
			roll = roll * 120 / 100;
			damageBonus += 200;
		}
		else if (undead && lo.wears("salve amulet(i)"))
		{
			roll = roll * 115 / 100;
			damageBonus += 150;
		}
		else if (slayerBonus(lo, f, t, true))
		{
			mask = true;
		}
		if (mask)
		{
			roll = roll * 23 / 20;
		}
		boolean weak = element != null && element.equals(t.weakness());
		if (weak)
		{
			roll += baseRoll * t.weaknessSeverity() / 100;
		}
		damageBonus += prayer.magicDamage;
		int max = base + base * damageBonus / 1000;
		if (mask)
		{
			max = max * 23 / 20;
		}
		if (weak)
		{
			max += base * t.weaknessSeverity() / 100;
		}
		long defence = (long) (t.getMagic() + 9) * (t.defensive(Target.D_MAGIC) + 64);
		double accuracy = hitChance(roll, defence);
		double damage = accuracy * max / 2.0;
		return new Result(Result.Kind.MAGIC, damage / (ticks * TICK), max, accuracy, ticks, style, prayer.name, spell);
	}

	private static boolean slayerBonus(Loadout lo, Fighter f, Target t, boolean imbuedOnly)
	{
		if (!f.isOnTask() || !t.isSlayer())
		{
			return false;
		}
		Gear head = lo.get(Gear.HEAD);
		if (head == null)
		{
			return false;
		}
		String key = head.key();
		boolean mask = key.contains("slayer helmet") || key.startsWith("black mask");
		return mask && (!imbuedOnly || key.contains("(i)"));
	}

	private static boolean tzhaarWeapon(Loadout lo)
	{
		Gear weapon = lo.weapon();
		if (weapon == null)
		{
			return false;
		}
		String key = weapon.key();
		return key.startsWith("toktz-xil-") || key.startsWith("tzhaar-ket-") || key.startsWith("toktz-mej-");
	}

	/** Whether a full void set with the given helm is worn; elite asks for the elite top and robe. */
	private static boolean voidSet(Loadout lo, String helm, boolean elite)
	{
		if (!lo.wears(helm) || !lo.wears("void knight gloves"))
		{
			return false;
		}
		boolean eliteSet = lo.wears("elite void top") && lo.wears("elite void robe");
		if (elite)
		{
			return eliteSet;
		}
		return eliteSet || lo.wears("void knight top") && lo.wears("void knight robe");
	}

	private static int speed(Gear weapon)
	{
		return weapon == null || weapon.getSpeed() <= 0 ? 4 : weapon.getSpeed();
	}

	static double hitChance(long attack, long defence)
	{
		if (attack > defence)
		{
			return 1 - (defence + 2.0) / (2.0 * (attack + 1));
		}
		return attack / (2.0 * (defence + 1));
	}

	/** Osmumten's fang rolls its accuracy twice and keeps the better one. */
	static double fangChance(long attack, long defence)
	{
		double a = attack;
		double d = defence;
		if (attack > defence)
		{
			return 1 - (d + 2) * (2 * d + 3) / (6 * (a + 1) * (a + 1));
		}
		return a * (4 * a + 5) / (6 * (a + 1) * (d + 1));
	}

	/** The twisted bow's scaling with the target's magic, for accuracy or damage. */
	static long twistedBow(long value, int magic, boolean accuracy)
	{
		int factor = accuracy ? 10 : 14;
		int base = accuracy ? 140 : 250;
		long t2 = (3L * magic - factor) / 100;
		long inner = 3L * magic / 10 - 10L * factor;
		long t3 = inner * inner / 100;
		long bonus = Math.max(0, Math.min(base, base + t2 - t3));
		return value * bonus / 100;
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
