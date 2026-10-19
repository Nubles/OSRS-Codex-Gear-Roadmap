package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Gear;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Owned combat gear that no longer earns its place: no best loadout, for any kind of attack against
 * any target the roadmap knows, wears it. Selling it is coins toward the road ahead. Left out are
 * pieces the player cannot wield yet (gear to grow into), ammunition, pieces with no price, pieces
 * worth too little to bother with, and gear kept for what it does rather than its damage: dragonfire
 * shields, slayer gear, the salve amulet, tank and utility pieces, skilling tools, and anything that
 * cannot be sold.
 */
public final class Outgrown
{
	/** A piece must sell for at least this much to be named. */
	static final long MIN_PRICE = 50_000;
	/** Pieces named at most. */
	public static final int SHOWN = 6;

	/** Kept for what they do rather than their damage, by the start of their lower-case names. */
	private static final String[] KEPT = {
		"anti-dragon", "dragonfire", "ancient wyvern", "mirror shield", "v's shield", "black mask", "slayer helmet", "salve amulet",
		"nose peg", "earmuffs", "facemask", "spiny helmet", "reinforced goggles", "insulated boots", "boots of stone", "boots of brimstone",
		"granite boots", "rock hammer", "ice gloves", "witchwood icon", "fishbowl", "graceful", "crystal", "dinh's", "elysian",
		"spectral", "justiciar", "guardian boots", "bulwark", "toktz-ket", "torag", "verac", "guthan", "dharok", "karil", "ahrim",
		"void", "elite void", "obsidian", "berserker necklace", "amulet of the damned", "ring of recoil", "ring of suffering",
		"ring of life", "ring of wealth", "amulet of glory", "explorer", "max cape", "fire cape", "infernal cape", "ava's", "god book", "book of",
		"holy book", "unholy book", "book of balance", "book of war", "book of law", "book of darkness", "rada's",
		"tome of", "keris", "arclight", "emberlight", "scorching bow", "silverlight", "darklight", "leaf-bladed", "broad",
		"dragon hunter", "slayer's staff", "wolfbane", "blisterwood", "ivandis", "barronite", "colossal blade",
		"ghommal", "wilderness", "craw's", "viggora's", "thammaron's", "webweaver", "ursine", "accursed", "voidwaker",
		"bone mace", "osmumten", "pharaoh", "staff of the dead", "toxic staff", "staff of light", "staff of balance",
		// Special attack weapons, carried for the spec rather than worn as the main weapon.
		"granite maul", "dragon dagger", "dragon claws", "burning claws", "armadyl godsword", "bandos godsword",
		"saradomin godsword", "zamorak godsword", "ancient godsword", "dragon warhammer", "elder maul", "crystal halberd",
		"dragon halberd", "dark bow", "zaryte crossbow", "dragon crossbow", "armadyl crossbow", "dragon mace", "dragon longsword",
		"dragon battleaxe", "dragon knife", "dragon thrownaxe", "abyssal dagger", "saradomin sword", "saradomin's blessed",
		"statius", "vesta", "morrigan", "zuriel", "magic shortbow", "heavy ballista", "light ballista", "eldritch nightmare",
		"volatile nightmare", "toxic blowpipe", "blazing blowpipe", "dragon sword", "dragon spear",
		"zamorakian", "dorgeshuun", "rune thrownaxe", "seercull", "excalibur", "barrelchest",
	};

	/** Pickaxes, woodcutting and felling axes and harpoons, by their lower-case names; battleaxes are weapons. */
	private static final Pattern TOOL = Pattern.compile("pickaxe|harpoon|(^|\\s)(felling )?axe(\\s|$)");

	/** One piece that has been outgrown. */
	public static final class Piece
	{
		private final Gear gear;
		private final long price;

		Piece(Gear gear, long price)
		{
			this.gear = gear;
			this.price = price;
		}

		public Gear getGear()
		{
			return gear;
		}

		/** What one sells for on the Grand Exchange before tax. */
		public long getPrice()
		{
			return price;
		}
	}

	private final List<Piece> pieces;
	private final long worth;

	private Outgrown(List<Piece> pieces, long worth)
	{
		this.pieces = Collections.unmodifiableList(pieces);
		this.worth = worth;
	}

	/**
	 * @param owned every piece of combat gear owned
	 * @param survey each target's best loadouts for every kind of attack
	 * @param wieldable whether the player can use a piece now
	 * @param sellable whether a piece can go on the Grand Exchange at all: RuneLite prices an untradeable piece
	 *     (a dragon defender (t), an imbued slayer helmet) by what it was made from, so a price is no proof
	 */
	public static Outgrown of(Collection<Gear> owned, List<Roadmap.Survey> survey, Predicate<Gear> wieldable,
		Predicate<Gear> sellable, Advisor.Prices prices)
	{
		Set<String> used = new HashSet<>();
		for (Roadmap.Survey row : survey)
		{
			Planner.Best best = row.getBest();
			if (best == null)
			{
				continue;
			}
			for (Result.Kind kind : Result.Kind.values())
			{
				wear(used, best.get(kind));
			}
			wear(used, best.best());
		}
		List<Piece> pieces = new ArrayList<>();
		long worth = 0;
		if (used.isEmpty())
		{
			// No survey yet, or one that found nothing to fight with: nothing can be called outgrown.
			return new Outgrown(pieces, 0);
		}
		for (Gear g : owned)
		{
			if (g.getSlot() == Gear.AMMO || used.contains(g.key()) || kept(g) || !wieldable.test(g) || !sellable.test(g))
			{
				continue;
			}
			long price = prices.price(g.getId());
			if (price < MIN_PRICE)
			{
				continue;
			}
			pieces.add(new Piece(g, price));
			worth += price;
		}
		pieces.sort((a, b) -> Long.compare(b.price, a.price));
		return new Outgrown(pieces, worth);
	}

	private static void wear(Set<String> used, Planner.Plan plan)
	{
		if (plan == null || plan.getResult() == null || plan.dps() <= 0)
		{
			return;
		}
		for (int slot = 0; slot < Gear.SLOTS; slot++)
		{
			Gear g = plan.getLoadout().get(slot);
			if (g != null)
			{
				used.add(g.key());
			}
		}
	}

	/** Gear kept for what it does rather than its damage. */
	static boolean kept(Gear g)
	{
		String name = g.key();
		if (TOOL.matcher(name).find() && !name.startsWith("soulreaper"))
		{
			// Skilling tools carry attack bonuses but are kept for the skill.
			return true;
		}
		if (name.contains("slayer helmet") || name.contains("battlestaff") || name.startsWith("mystic ") && name.contains(" staff")
			|| name.startsWith("staff of air") || name.startsWith("staff of water") || name.startsWith("staff of earth")
			|| name.startsWith("staff of fire"))
		{
			// Every colour of slayer helmet, and staves that stand in for elemental runes.
			return true;
		}
		for (String k : KEPT)
		{
			if (name.startsWith(k))
			{
				return true;
			}
		}
		// No attack or strength at all: armour worn for its defence.
		for (int t = 0; t <= Gear.RANGED; t++)
		{
			if (g.offence(t) > 0)
			{
				return false;
			}
		}
		return g.strength() <= 0 && g.rangedStrength() <= 0 && g.magicDamage() <= 0;
	}

	/** Outgrown pieces, the most valuable first. */
	public List<Piece> getPieces()
	{
		return pieces;
	}

	/** What they all sell for before tax. */
	public long getWorth()
	{
		return worth;
	}

	public boolean isEmpty()
	{
		return pieces.isEmpty();
	}
}
