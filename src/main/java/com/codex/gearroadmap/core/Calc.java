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

	/** The staves that can autocast the Ancient Magicks, by the start of their names. */
	private static final String[] ANCIENT_STAVES = {
		"ancient staff", "ahrim's staff", "master wand", "kodai wand", "nightmare staff", "harmonised nightmare staff",
		"eldritch nightmare staff", "volatile nightmare staff", "staff of the dead", "toxic staff of the dead",
		"staff of light", "staff of balance", "zuriel's staff", "void knight mace", "blue moon spear",
	};
	/** The soulreaper axe is assumed to be kept at its full stacks. */
	static final int SOULREAPER_STACKS = 5;

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
			Result.Kind styleKind = "magic".equals(type) ? Result.Kind.MAGIC : "ranged".equals(type) ? Result.Kind.RANGED : Result.Kind.MELEE;
			if (!target.allows(styleKind.name()))
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
		/** Whether the weapon fires the ammunition worn, as bows and crossbows do. */
		boolean firesAmmo;

		Totals(Loadout loadout, Set<Integer> fires)
		{
			Gear ammo = loadout.get(Gear.AMMO);
			boolean countAmmo = fires != null && !fires.isEmpty();
			if (countAmmo)
			{
				ammoValid = ammo != null && firesAny(fires, ammo);
				firesAmmo = ammoValid;
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
		Gear weapon = lo.weapon();
		String key = weapon == null ? "" : weapon.key();
		Fighter.Prayer prayer = f.meleePrayer();
		int effAttack = f.attack() * prayer.accuracy / 100 + 8 + ("Accurate".equals(stance) ? 3 : "Controlled".equals(stance) ? 1 : 0);
		int strengthLevel = f.strength() * prayer.strength / 100;
		if (key.equals("soulreaper axe"))
		{
			// Kept at its full five stacks, each worth 6% of the strength level; it does not stack with prayers.
			strengthLevel += f.strength() * SOULREAPER_STACKS * 6 / 100;
		}
		int effStrength = strengthLevel + 8 + ("Aggressive".equals(stance) ? 3 : "Controlled".equals(stance) ? 1 : 0);
		if (voidSet(lo, "void melee helm", false))
		{
			effAttack = effAttack * 11 / 10;
			effStrength = effStrength * 11 / 10;
		}
		long baseRoll = (long) effAttack * (tot.offence[attackType] + 64);
		int baseMax = (effStrength * (tot.strength + 64) + 320) / 640;
		long roll = baseRoll;
		int max = baseMax;

		// The salve amulet and the black mask do not stack; the salve wins on undead.
		boolean undead = t.is("undead");
		boolean demon = t.is("demon");
		boolean dragon = t.is("dragon");
		boolean kalphite = t.is("kalphite");
		boolean revenant = revenantBonus(lo, t, "ursine chainmace", "viggora's chainmace");
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

		// Accuracy, in the order the game applies it.
		boolean obsidian = tzhaarWeapon(lo) && lo.wears("obsidian helmet") && lo.wears("obsidian platebody") && lo.wears("obsidian platelegs");
		if (obsidian)
		{
			roll += baseRoll / 10;
		}
		if (revenant)
		{
			roll = roll * 3 / 2;
		}
		int bane = !demon ? 0 : lo.wearsAny("arclight", "emberlight") ? 70 : lo.wearsAny("silverlight", "darklight") ? 60 : 0;
		bane = bane * t.demonbaneVulnerability() / 100;
		roll += roll * bane / 100;
		if (dragon && key.equals("dragon hunter lance"))
		{
			roll = roll * 6 / 5;
		}
		if (kalphite && key.equals("keris partisan of breaching"))
		{
			roll = roll * 133 / 100;
		}
		int inquisitor = attackType != Gear.CRUSH ? 0 : (lo.wears("inquisitor's great helm") ? 1 : 0)
			+ (lo.wears("inquisitor's hauberk") ? 2 : 0) + (lo.wears("inquisitor's plateskirt") ? 2 : 0);
		roll = roll * (200 + inquisitor) / 200;

		// Max hit, in the order the game applies it.
		if (demon && lo.wearsAny("arclight", "emberlight"))
		{
			max += max * bane / 100;
		}
		if (obsidian)
		{
			max += baseMax / 10;
		}
		if (dragon && key.equals("dragon hunter lance"))
		{
			max = max * 6 / 5;
		}
		if (kalphite && key.startsWith("keris"))
		{
			max = max * (key.equals("keris partisan of amascut") ? 115 : 133) / 100;
		}
		if (revenant)
		{
			max = max * 3 / 2;
		}
		if (demon && lo.wearsAny("silverlight", "darklight"))
		{
			max += max * bane / 100;
		}
		if (key.equals("colossal blade"))
		{
			max += Math.min(t.getSize() * 2, 10);
		}
		max = max * (200 + inquisitor) / 200;

		long defence = (long) (t.getDefence() + 9) * (t.defensive(attackType) + 64);
		boolean fang = key.startsWith("osmumten's fang");
		// The fang rolls its accuracy twice only when it stabs.
		double accuracy = fang && attackType == Gear.STAB ? fangChance(roll, defence) : hitChance(roll, defence);
		boolean berserker = tzhaarWeapon(lo) && lo.wears("berserker necklace");
		boolean shieldBreaker = lo.wearsAny("arclight", "emberlight", "silverlight", "darklight") || key.startsWith("abyssal");
		// Spears, hastas and halberds stab the Corporeal Beast in full, as does the fang.
		boolean corpbane = attackType == Gear.STAB && (fang || key.endsWith("halberd") || key.contains("hasta")
			|| key.contains("spear") && !key.equals("blue moon spear"));
		Npc npc = new Npc(t, Result.Kind.MELEE, shieldBreaker, corpbane);
		double damage;
		if (fang)
		{
			// The fang never rolls in the bottom or top 15% of its range.
			int low = max * 3 / 20;
			max -= low;
			damage = accuracy * npc.mean(low, max, 1, 1, 0);
		}
		else if (key.contains("of vitur"))
		{
			// The scythe's second and third swings hit large monsters for half and a quarter, each rolled on its own.
			damage = 0;
			for (int i = 0; i < Math.min(3, t.getSize()); i++)
			{
				damage += accuracy * npc.mean(0, max >> i, 1, 1, 0);
			}
		}
		else if (key.equals("dual macuahuitl"))
		{
			// Two half hits; the second is only thrown when the first lands.
			int first = max / 2;
			damage = accuracy * npc.mean(0, first, 1, 1, 0) + accuracy * accuracy * npc.mean(0, max - first, 1, 1, 0);
		}
		else if (berserker)
		{
			damage = accuracy * npc.mean(0, max, 6, 5, 0);
			max = max * 6 / 5;
		}
		else if (kalphite && key.startsWith("keris"))
		{
			// One hit in 51 deals triple damage.
			damage = accuracy * (npc.mean(0, max, 1, 1, 0) * 50 + npc.mean(0, max, 3, 1, 0)) / 51;
		}
		else
		{
			damage = accuracy * npc.mean(0, max, 1, 1, 0);
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
		String key = weapon == null ? "" : weapon.key();
		Fighter.Prayer prayer = f.rangedPrayer();
		int effAttack = f.ranged() * prayer.accuracy / 100 + 8 + ("Accurate".equals(stance) ? 3 : 0);
		int effStrength = f.ranged() * prayer.strength / 100 + 8 + ("Accurate".equals(stance) ? 3 : 0);
		if (voidSet(lo, "void ranger helm", false))
		{
			effAttack = effAttack * 11 / 10;
			effStrength = voidSet(lo, "void ranger helm", true) ? effStrength * 9 / 8 : effStrength * 11 / 10;
		}
		int bonus = tot.offence[Gear.RANGED];
		int strength = tot.rangedStrength;
		if (key.startsWith("toxic blowpipe"))
		{
			// Loaded with dragon darts.
			strength += 35;
		}
		if (tot.firesAmmo && lo.wearsAny("dizana's quiver", "blessed dizana's quiver", "dizana's max cape"))
		{
			// A charged quiver adds to arrows and bolts the weapon fires.
			bonus += 10;
			strength += 1;
		}
		long roll = (long) effAttack * (bonus + 64);
		int max = (effStrength * (strength + 64) + 320) / 640;

		boolean crystal = key.startsWith("crystal bow") || key.startsWith("bow of faerdhinen");
		int pieces = !crystal ? 0 : (lo.wears("crystal helm") ? 1 : 0) + (lo.wears("crystal legs") ? 2 : 0) + (lo.wears("crystal body") ? 3 : 0);
		roll = roll * (20 + pieces) / 20;
		max = max * (40 + pieces) / 40;

		boolean undead = t.is("undead");
		boolean dragonbane = t.is("dragon") && key.equals("dragon hunter crossbow");
		boolean demonbane = t.is("demon") && key.equals("scorching bow");
		boolean revenant = revenantBonus(lo, t, "webweaver bow", "craw's bow");
		boolean salve = undead && lo.wearsAny("salve amulet(ei)", "salve amulet(i)");
		boolean mask = !salve && slayerBonus(lo, f, t, true);
		if (salve)
		{
			int num = lo.wears("salve amulet(ei)") ? 6 : 7;
			roll = roll * num / (num - 1);
		}
		else if (mask)
		{
			roll = roll * 23 / 20;
		}
		if (key.equals("twisted bow"))
		{
			int magic = Math.min(250, Math.max(t.getMagic(), t.getMagicAttack()));
			roll = twistedBow(roll, magic, true);
		}
		if (revenant)
		{
			roll = roll * 3 / 2;
		}
		if (dragonbane)
		{
			roll = roll * 13 / 10;
		}
		int bane = 30 * t.demonbaneVulnerability() / 100;
		if (demonbane)
		{
			roll += roll * bane / 100;
		}

		if (salve)
		{
			int num = lo.wears("salve amulet(ei)") ? 6 : 7;
			max = max * num / (num - 1);
		}
		else if (mask)
		{
			// The imbued slayer helmet adds to the revenant, dragon and demon bonuses rather than multiplying them.
			int numerator = 23 + (revenant ? 10 : 0) + (dragonbane ? 5 : 0) + (demonbane ? 6 : 0);
			max = max * numerator / 20;
			revenant = false;
			dragonbane = false;
			demonbane = false;
		}
		if (key.equals("twisted bow"))
		{
			int magic = Math.min(250, Math.max(t.getMagic(), t.getMagicAttack()));
			max = (int) twistedBow(max, magic, false);
		}
		if (revenant)
		{
			max = max * 3 / 2;
		}
		if (dragonbane)
		{
			max = max * 5 / 4;
		}
		if (demonbane)
		{
			max += max * bane / 100;
		}

		String category = weapon == null ? "" : weapon.getCategory();
		int defType = "Thrown".equals(category) ? Target.D_LIGHT : "Bow".equals(category) ? Target.D_STANDARD : Target.D_HEAVY;
		long defence = (long) (t.getDefence() + 9) * (t.defensive(defType) + 64);
		double accuracy = hitChance(roll, defence);
		Npc npc = new Npc(t, Result.Kind.RANGED, key.equals("scorching bow"));
		double damage = accuracy * npc.mean(0, max, 1, 1, 0);
		if (key.equals("dark bow"))
		{
			// Two arrows each attack, each rolled on its own.
			damage *= 2;
		}

		Gear ammo = lo.get(Gear.AMMO);
		if ("Crossbow".equals(category) && ammo != null)
		{
			boolean zaryte = key.equals("zaryte crossbow");
			double diary = f.isKandarinHard() ? 1.1 : 1.0;
			if (ammo.key().startsWith("diamond bolts (e)") || ammo.key().startsWith("diamond dragon bolts (e)"))
			{
				// One bolt in ten pierces armour: it always hits, for up to 15% more.
				double chance = 0.1 * diary;
				double effect = npc.mean(0, max * (zaryte ? 126 : 115) / 100, 1, 1, 0);
				damage = chance * effect + (1 - chance) * damage;
			}
			else if (ammo.key().startsWith("ruby bolts (e)") || ammo.key().startsWith("ruby dragon bolts (e)"))
			{
				// Six in a hundred take a fifth of the monster's current health, up to 100, so the
				// effect is averaged over the whole kill rather than taken at full health.
				double chance = 0.06 * diary;
				double effect = rubyAverage(npc, t.getHp(), zaryte ? 22 : 20, zaryte ? 110 : 100);
				damage = chance * effect + (1 - chance) * damage;
			}
		}
		int ticks = Math.max(1, speed(weapon) - ("Rapid".equals(stance) ? 1 : 0));
		return new Result(Result.Kind.RANGED, damage / (ticks * TICK), max, accuracy, ticks, stance, prayer.name, null);
	}

	/** The ruby bolt's damage averaged over every health the monster passes through from full to dead. */
	static double rubyAverage(Npc npc, int hp, int percent, int cap)
	{
		if (hp <= 0)
		{
			return 0;
		}
		double sum = 0;
		for (int h = 1; h <= hp; h++)
		{
			sum += npc.hit(Math.max(1, Math.min(cap, h * percent / 100)));
		}
		return sum / hp;
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
		return magic(lo, tot, f, t, base, "Accurate".equals(stance) ? 2 : 0, speed(weapon), null, null, false,
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
			best = magic(lo, tot, f, t, Integer.parseInt(fire[2]), 0, harmonised ? 4 : 5, fire[0], "fire", false, "Autocast");
		}
		String[] ice = f.isAncients() && autocastsAncients(weapon) ? pick(ANCIENT_SPELLS, real) : null;
		if (ice != null)
		{
			// Ice spells have no element, so no weakness applies to them.
			Result r = magic(lo, tot, f, t, Integer.parseInt(ice[2]), 0, 5, ice[0], null, true, "Autocast");
			if (r.isBetterThan(best))
			{
				best = r;
			}
		}
		return best;
	}

	/** Only some staves can autocast the Ancient Magicks. */
	static boolean autocastsAncients(Gear weapon)
	{
		if (weapon == null)
		{
			return false;
		}
		String key = weapon.key();
		for (String staff : ANCIENT_STAVES)
		{
			if (key.startsWith(staff))
			{
				return true;
			}
		}
		return key.contains("ancient sceptre");
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

	private Result magic(Loadout lo, Totals tot, Fighter f, Target t, int base, int stanceBonus, int ticks,
		String spell, String element, boolean ancient, String style)
	{
		Gear weapon = lo.weapon();
		String key = weapon == null ? "" : weapon.key();
		Fighter.Prayer prayer = f.magicPrayer();
		int effMagic = f.magic() * prayer.accuracy / 100 + 9 + stanceBonus;
		if (voidSet(lo, "void mage helm", false))
		{
			effMagic = effMagic * 29 / 20;
		}
		int bonus = tot.offence[Gear.MAGIC];
		int damageBonus = tot.magicDamage;
		if (key.equals("tumeken's shadow"))
		{
			bonus *= 3;
			damageBonus = Math.min(1000, damageBonus * 3);
		}
		if (ancient)
		{
			// Each piece of virtus adds 3% to Ancient Magicks.
			damageBonus += 30 * ((lo.wears("virtus mask") ? 1 : 0) + (lo.wears("virtus robe top") ? 1 : 0)
				+ (lo.wears("virtus robe bottom") ? 1 : 0));
		}
		if (voidSet(lo, "void mage helm", true))
		{
			damageBonus += 50;
		}
		long baseRoll = (long) effMagic * (bonus + 64);
		long roll = baseRoll;
		boolean undead = t.is("undead");
		boolean mask = false;
		boolean revenant = revenantBonus(lo, t, "accursed sceptre", "thammaron's sceptre");
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
		if (revenant)
		{
			roll = roll * 3 / 2;
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
		if (revenant)
		{
			max = max * 3 / 2;
		}
		if (weak)
		{
			max += base * t.weaknessSeverity() / 100;
		}
		if ("fire".equals(element) && lo.wears("tome of fire"))
		{
			// A charged tome of fire adds a tenth to fire spells.
			max = max * 11 / 10;
		}
		long defence = (long) (t.getMagic() + 9) * (t.defensive(Target.D_MAGIC) + 64);
		double accuracy = hitChance(roll, defence);
		Npc npc = new Npc(t, Result.Kind.MAGIC, false);
		double perHit = npc.mean(0, max, 1, 1, 0);
		if (key.contains("sanguinesti staff"))
		{
			// One landed hit in five deals 8 more.
			perHit = 0.8 * perHit + 0.2 * npc.mean(0, max, 1, 1, 8);
		}
		double damage = accuracy * perHit;
		return new Result(Result.Kind.MAGIC, damage / (ticks * TICK), max, accuracy, ticks, style, prayer.name, spell);
	}

	/** Whether a revenant weapon, kept charged, gets its half again against a Wilderness monster. */
	private static boolean revenantBonus(Loadout lo, Target t, String... weapons)
	{
		Gear weapon = lo.weapon();
		if (weapon == null || !t.isWilderness())
		{
			return false;
		}
		for (String w : weapons)
		{
			if (weapon.key().equals(w))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * What one landed hit is worth against a target: the game raises an accurate 0 to 1, then the
	 * target's own rules apply, such as Zulrah's cap on big hits, the Kraken shrugging off ranged
	 * attacks, the Tormented demon's shield and flat armour.
	 */
	static final class Npc
	{
		private final boolean zulrah;
		private final int halve;
		private final int divisor;
		private final boolean shielded;
		private final int flat;

		Npc(Target t, Result.Kind kind, boolean bane)
		{
			this(t, kind, bane, kind == Result.Kind.MAGIC);
		}

		/** Corpbane weapons (stabbing spears and halberds, the fang, and magic) hurt the Corporeal Beast in full. */
		Npc(Target t, Result.Kind kind, boolean bane, boolean corpbane)
		{
			String name = t.getName();
			zulrah = "Zulrah".equals(name);
			halve = "Corporeal Beast".equals(name) && !corpbane ? 2 : 1;
			divisor = kind == Result.Kind.RANGED && "Kraken".equals(name) ? 7 : 1;
			shielded = "Tormented Demon".equals(name) && !bane;
			flat = kind == Result.Kind.MAGIC ? 0 : t.getFlatArmour();
		}

		/** The mean of a landed hit rolled from lo to hi, each scaled by num/den and given add. */
		double mean(int lo, int hi, int num, int den, int add)
		{
			if (hi <= 0 && add <= 0)
			{
				return 0;
			}
			int top = Math.max(lo, hi);
			double sum = 0;
			for (int h = lo; h <= top; h++)
			{
				// The Corporeal Beast halves hits after the 0 is raised to 1.
				sum += hit(Math.max(1, h * num / den + add) / halve);
			}
			return sum / (top - lo + 1);
		}

		/** One landed hitsplat after the target's rules. */
		double hit(int h)
		{
			if (zulrah && h > 50)
			{
				// Zulrah turns any hit over 50 into one from 45 to 50.
				double sum = 0;
				for (int i = 45; i <= 50; i++)
				{
					sum += rest(i);
				}
				return sum / 6;
			}
			return rest(h);
		}

		private double rest(int h)
		{
			int d = h >= 1 ? Math.max(1, h / divisor) : 0;
			if (shielded)
			{
				// A shielded Tormented demon takes a fifth less from anything but demonbane and abyssal weapons.
				d = Math.max(d >= 1 ? 1 : d, d * 4 / 5);
			}
			return Math.max(0, d - flat);
		}
	}

	private static boolean slayerBonus(Loadout lo, Fighter f, Target t, boolean imbuedOnly)
	{
		if (!f.onTaskFor(t))
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
