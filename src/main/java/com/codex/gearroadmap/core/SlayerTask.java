package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Whether the black mask and slayer helmet count against a target. Slayer creatures are
 * nearly always killed on task, bosses and training spots seldom are, so by default the
 * bonus counts for Slayer creatures and for anything the player's current task covers.
 */
public final class SlayerTask
{
	public enum Mode
	{
		SMART("Slayer monsters and your task"),
		ALWAYS("Always"),
		NEVER("Never");

		private final String label;

		Mode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	public static final SlayerTask ALWAYS = new SlayerTask(Mode.ALWAYS, null);
	public static final SlayerTask NEVER = new SlayerTask(Mode.NEVER, null);

	/** Tasks that also cover a boss or monster besides its own name, as the Slayer helper lists them. */
	private static final Map<String, Set<String>> COVERED_BY = new HashMap<>();

	static
	{
		cover("vorkath", "blue dragons", "zombies");
		cover("kree'arra", "aviansies");
		cover("k'ril tsutsaroth", "greater demons");
		cover("cerberus", "hellhounds");
		cover("abyssal sire", "abyssal demons");
		cover("alchemical hydra", "hydras");
		cover("kraken", "cave kraken", "cave kraken boss");
		cover("thermonuclear smoke devil", "smoke devils");
		cover("grotesque guardians", "gargoyles");
		cover("sarachnis", "spiders");
		cover("kalphite queen", "kalphites");
		cover("dagannoth rex", "dagannoth", "dagannoth kings");
		cover("dagannoth prime", "dagannoth", "dagannoth kings");
		cover("dagannoth supreme", "dagannoth", "dagannoth kings");
		cover("tztok-jad", "tzhaar");
		cover("tormented demon", "greater demons");
		cover("araxxor", "araxytes", "spiders");
		cover("amoxliatl", "lesser nagua");
		cover("skotizo", "black demons", "greater demons");
		cover("callisto", "bears");
		cover("artio", "bears");
		cover("venenatis", "spiders");
		cover("spindel", "spiders");
		cover("vet'ion", "skeletons");
		cover("scorpia", "scorpions");
		cover("demonic gorilla", "black demons", "monkeys");
		cover("greater nechryael", "nechryael");
		cover("lizardman shaman", "lizardmen");
		cover("rune dragon", "metal dragons");
		cover("adamant dragon", "metal dragons");
		cover("brutal black dragon", "black dragons");
		cover("kalphite soldier", "kalphites");
		cover("ammonite crab", "crabs");
		cover("sand crab", "crabs");
		cover("rock crab", "crabs");
	}

	private final Mode mode;
	private final String task;

	private SlayerTask(Mode mode, String task)
	{
		this.mode = mode;
		this.task = task == null || task.isEmpty() ? null : normal(task);
	}

	/** The assumption for a mode, with the player's current task name (or null when they have none). */
	public static SlayerTask of(Mode mode, String currentTask)
	{
		if (mode == Mode.ALWAYS)
		{
			return ALWAYS;
		}
		if (mode == Mode.NEVER)
		{
			return NEVER;
		}
		return new SlayerTask(Mode.SMART, currentTask);
	}

	public Mode getMode()
	{
		return mode;
	}

	/** The current task, lower case, or null. */
	public String getTask()
	{
		return task;
	}

	/** Whether the slayer helmet's bonus counts against this target. */
	public boolean covers(Target target)
	{
		if (target == null || !target.isSlayer() || mode == Mode.NEVER)
		{
			return false;
		}
		if (mode == Mode.ALWAYS || "Slayer".equals(target.getGroup()))
		{
			return true;
		}
		return task != null && taskCovers(task, target.getName());
	}

	/** Whether a task, by its name in the game, covers a monster by its name. */
	static boolean taskCovers(String task, String monster)
	{
		String t = normal(task);
		String m = normal(monster);
		if (t.equals(m))
		{
			return true;
		}
		return COVERED_BY.getOrDefault(m, Collections.emptySet()).contains(t);
	}

	/** Lower case, without "The" or a plural "s", so "The Abyssal Sire" and "Hill Giants" match their monsters. */
	static String normal(String name)
	{
		String s = name.toLowerCase(Locale.ROOT).trim();
		if (s.startsWith("the "))
		{
			s = s.substring(4);
		}
		if (s.endsWith("s") && !s.endsWith("ss"))
		{
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static void cover(String monster, String... tasks)
	{
		Set<String> set = COVERED_BY.computeIfAbsent(normal(monster), k -> new HashSet<>());
		for (String task : tasks)
		{
			set.add(normal(task));
		}
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof SlayerTask && ((SlayerTask) o).mode == mode
			&& (task == null ? ((SlayerTask) o).task == null : task.equals(((SlayerTask) o).task));
	}

	@Override
	public int hashCode()
	{
		return mode.hashCode() * 31 + (task == null ? 0 : task.hashCode());
	}

	@Override
	public String toString()
	{
		return mode + (task == null ? "" : ":" + task);
	}
}
