package com.codex.gearroadmap.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * The bank Gear Roadmap plans with. Gear Roadmap keeps its own view of the bank (the gear it knows
 * and the coins, with no quantities once saved), and Codex Bank Steward, when installed, publishes
 * the whole bank with every quantity under {@code codexbanksteward/bankV1}:
 * {@code {"version":1,"savedAt":1760000000000,"items":{"995":1234567,"26950":1},"truncated":false}}.
 * Bank Steward's is read, never written, and is used when it is at least as fresh as Gear Roadmap's
 * own view, or tells the same story with quantities added; without it nothing changes.
 */
public final class Holdings
{
	public static final String STEWARD_GROUP = "codexbanksteward";
	public static final String STEWARD_KEY = "bankV1";
	/** Ammunition held below this is a few strays, not a stack worth bringing: it is not counted as owned. */
	public static final long AMMO_TRIP = 100;
	/** A quantity that was never seen; the item counts as owned whatever it is. */
	public static final long UNKNOWN = -1;

	static final int COINS = 995;
	static final int PLATINUM = 13204;
	/** Readers take no more kinds of item than Bank Steward ever writes. */
	private static final int MAX_ITEMS = 2000;

	/** Where the bank in use came from. */
	public enum Source
	{
		/** Nothing yet: the bank has not been seen by either plugin. */
		NONE,
		/** Gear Roadmap's own view. */
		OWN,
		/** Codex Bank Steward's shared bank. */
		BANK_STEWARD
	}

	/** Bank Steward's shared bank, as read. */
	public static final class Shared
	{
		private final Map<Integer, Long> items;
		private final long savedAt;

		Shared(Map<Integer, Long> items, long savedAt)
		{
			this.items = Collections.unmodifiableMap(items);
			this.savedAt = savedAt;
		}

		public Map<Integer, Long> getItems()
		{
			return items;
		}

		public long getSavedAt()
		{
			return savedAt;
		}
	}

	private final Source source;
	private final Map<Integer, Long> gear;
	private final long coins;

	private Holdings(Source source, Map<Integer, Long> gear, long coins)
	{
		this.source = source;
		this.gear = Collections.unmodifiableMap(gear);
		this.coins = coins;
	}

	public static final Holdings NONE = new Holdings(Source.NONE, Collections.emptyMap(), 0);

	public Source getSource()
	{
		return source;
	}

	/** Banked gear by item id, with its quantity or {@link #UNKNOWN}. */
	public Map<Integer, Long> getGear()
	{
		return gear;
	}

	/** Banked coins, platinum tokens counted at a thousand. */
	public long getCoins()
	{
		return coins;
	}

	public boolean isKnown()
	{
		return source != Source.NONE;
	}

	/**
	 * Reads Bank Steward's shared bank; null when there is none or it cannot be read. Anything
	 * odd in it (a bad id, a negative count) is skipped rather than trusted.
	 */
	public static Shared readSteward(Gson gson, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}
		try
		{
			JsonElement root = gson.fromJson(json, JsonElement.class);
			if (root == null || !root.isJsonObject())
			{
				return null;
			}
			JsonObject o = root.getAsJsonObject();
			JsonElement list = o.get("items");
			if (list == null || !list.isJsonObject())
			{
				return null;
			}
			Map<Integer, Long> items = new HashMap<>();
			for (Map.Entry<String, JsonElement> e : list.getAsJsonObject().entrySet())
			{
				Integer id = parseInt(e.getKey());
				Long qty = number(e.getValue());
				if (id != null && id > 0 && qty != null && qty > 0 && items.size() < MAX_ITEMS)
				{
					items.put(id, qty);
				}
			}
			Long savedAt = number(o.get("savedAt"));
			return new Shared(items, savedAt == null ? 0 : savedAt);
		}
		catch (JsonParseException | IllegalStateException | UnsupportedOperationException e)
		{
			return null;
		}
	}

	/**
	 * The bank to plan with.
	 *
	 * @param own Gear Roadmap's own banked gear ids, or null when it has never seen the bank
	 * @param ownCoins the coins in Gear Roadmap's own view
	 * @param ownCounts quantities Gear Roadmap saw itself this login, or null when its view was read back from a save
	 * @param ownSeenAt when Gear Roadmap last saw its bank change, epoch ms, or 0 when it does not know
	 * @param steward Bank Steward's shared bank, or null
	 * @param isGear whether Gear Roadmap knows an item id as gear
	 */
	public static Holdings choose(Set<Integer> own, long ownCoins, Map<Integer, Long> ownCounts, long ownSeenAt,
		Shared steward, IntPredicate isGear)
	{
		Holdings mine = own == null ? null : mine(own, ownCoins, ownCounts);
		Holdings shared = steward == null ? null : shared(steward, isGear);
		if (shared == null)
		{
			return mine == null ? NONE : mine;
		}
		if (mine == null)
		{
			return shared;
		}
		// Seen this login, Gear Roadmap's own view is as fresh as a bank can be and has its counts.
		if (ownCounts != null && ownSeenAt >= steward.getSavedAt())
		{
			return mine;
		}
		// The same gear in both: Bank Steward's adds the quantities, whichever saw it last.
		if (mine.gear.keySet().equals(shared.gear.keySet()))
		{
			return shared;
		}
		return steward.getSavedAt() >= ownSeenAt ? shared : mine;
	}

	private static Holdings mine(Set<Integer> own, long coins, Map<Integer, Long> counts)
	{
		Map<Integer, Long> gear = new HashMap<>();
		for (int id : own)
		{
			Long n = counts == null ? null : counts.get(id);
			gear.put(id, n == null || n <= 0 ? UNKNOWN : n);
		}
		return new Holdings(Source.OWN, gear, coins);
	}

	private static Holdings shared(Shared steward, IntPredicate isGear)
	{
		Map<Integer, Long> gear = new HashMap<>();
		long coins = 0;
		for (Map.Entry<Integer, Long> e : steward.getItems().entrySet())
		{
			int id = e.getKey();
			if (id == COINS)
			{
				coins += e.getValue();
			}
			else if (id == PLATINUM)
			{
				coins += 1000L * e.getValue();
			}
			else if (isGear.test(id))
			{
				gear.put(id, e.getValue());
			}
		}
		return new Holdings(Source.BANK_STEWARD, gear, coins);
	}

	/**
	 * Whether a stack counts as owned: anything at all, except ammunition, which needs at least
	 * {@link #AMMO_TRIP} across the bank, the inventory and the quiver. An unknown quantity counts.
	 */
	public static boolean enough(boolean ammo, long quantity)
	{
		return quantity == UNKNOWN || (ammo ? quantity >= AMMO_TRIP : quantity > 0);
	}

	private static Integer parseInt(String s)
	{
		try
		{
			return Integer.valueOf(s.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private static Long number(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return null;
		}
		try
		{
			double d = e.getAsDouble();
			if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.floor(d))
			{
				return null;
			}
			return e.getAsLong();
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}
}
