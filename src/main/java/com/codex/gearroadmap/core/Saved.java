package com.codex.gearroadmap.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reading back what was saved in the profile, this plugin's own or another Codex plugin's, when any
 * of it may be missing, cut short, of the wrong type or written by a later version. Nothing here
 * throws: what cannot be read is left out, and only that.
 */
public final class Saved
{
	/** More than any account is worth or any stack holds; a figure past it is damage. */
	public static final long MAX_WORTH = 10_000_000_000_000L;

	private Saved()
	{
	}

	/** The saved text as a JSON object, or null when it is blank, not JSON or not an object. */
	public static JsonObject object(Gson gson, String json)
	{
		JsonElement root = tree(gson, json);
		return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
	}

	/** The saved text as JSON, or null when it is blank or not JSON. */
	static JsonElement tree(Gson gson, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}
		try
		{
			return gson.fromJson(json, JsonElement.class);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	/** One saved object as a class, or null when it cannot be read as one. */
	public static <T> T as(Gson gson, JsonElement e, Class<T> type)
	{
		if (e == null || !e.isJsonObject())
		{
			return null;
		}
		try
		{
			return gson.fromJson(e, type);
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	/** A saved list of objects read one by one, so a broken entry costs only itself; empty without one. */
	public static <T> List<T> list(Gson gson, JsonElement e, Class<T> type)
	{
		if (e == null || !e.isJsonArray())
		{
			return Collections.emptyList();
		}
		List<T> out = new ArrayList<>();
		for (JsonElement entry : e.getAsJsonArray())
		{
			T read = as(gson, entry, type);
			if (read != null)
			{
				out.add(read);
			}
		}
		return out;
	}

	/** The saved text as a list of objects read one by one; empty when it is not a list. */
	public static <T> List<T> list(Gson gson, String json, Class<T> type)
	{
		return list(gson, tree(gson, json), type);
	}

	/** The entries of a saved list that read as this class on their own, or null when it is not a list. */
	static JsonArray readable(Gson gson, JsonElement e, Class<?> type)
	{
		if (e == null || !e.isJsonArray())
		{
			return null;
		}
		JsonArray kept = new JsonArray();
		for (JsonElement entry : e.getAsJsonArray())
		{
			if (as(gson, entry, type) != null)
			{
				kept.add(entry);
			}
		}
		return kept;
	}

	/** The longest damaged save set aside; one past it is only logged, so the profile never doubles in size for it. */
	public static final int MAX_ASIDE = 1_000_000;

	/**
	 * True when {@code json} was saved and {@code read}, what was read from it, written out again lacks
	 * part of it: a broken entry left out, or a later version's field passed over in silence. Such a
	 * save would be lost at the next write, so it is set aside first. Only the shape is compared, not
	 * the figures, which reading may bring back to sense.
	 */
	public static boolean lost(Gson gson, String json, Object read)
	{
		JsonElement saved = tree(gson, json);
		if (json == null || json.trim().isEmpty())
		{
			return false;
		}
		JsonElement reread;
		try
		{
			reread = read == null ? null : gson.toJsonTree(read);
		}
		catch (RuntimeException | StackOverflowError e)
		{
			reread = null;
		}
		return saved == null || !covers(saved, reread);
	}

	/** True when every field and list entry in {@code saved} is still there in {@code reread}; a field saved as null was nothing to lose. */
	static boolean covers(JsonElement saved, JsonElement reread)
	{
		if (saved == null || saved.isJsonNull())
		{
			return true;
		}
		if (reread == null || reread.isJsonNull())
		{
			return false;
		}
		if (saved.isJsonObject())
		{
			if (!reread.isJsonObject())
			{
				return false;
			}
			JsonObject now = reread.getAsJsonObject();
			for (java.util.Map.Entry<String, JsonElement> field : saved.getAsJsonObject().entrySet())
			{
				if (!covers(field.getValue(), now.get(field.getKey())))
				{
					return false;
				}
			}
			return true;
		}
		if (saved.isJsonArray())
		{
			if (!reread.isJsonArray() || reread.getAsJsonArray().size() < saved.getAsJsonArray().size())
			{
				return false;
			}
			JsonArray then = saved.getAsJsonArray();
			JsonArray now = reread.getAsJsonArray();
			for (int i = 0; i < then.size(); i++)
			{
				if (!covers(then.get(i), now.get(i)))
				{
					return false;
				}
			}
			return true;
		}
		return true;
	}

	/**
	 * Whether a save that could not be read whole should be set aside in place of the copy already
	 * there, which is kept to just one: yes when there is none, and otherwise only for a save at least
	 * as long, so a later stub never replaces a fuller copy.
	 */
	public static boolean setAside(String kept, String damaged)
	{
		if (damaged == null || damaged.trim().isEmpty() || damaged.length() > MAX_ASIDE)
		{
			return false;
		}
		return kept == null || kept.isEmpty() || !kept.equals(damaged) && damaged.length() >= kept.length();
	}

	/** A whole number, or null for anything else (text, a fraction, too big to be a long). */
	public static Long whole(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return null;
		}
		try
		{
			return new java.math.BigDecimal(e.getAsString()).longValueExact();
		}
		catch (ArithmeticException | NumberFormatException ex)
		{
			return null;
		}
	}
}
