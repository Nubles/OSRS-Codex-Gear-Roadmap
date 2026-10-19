package com.codex.gearroadmap;

import com.codex.gearroadmap.core.Fighter;
import com.codex.gearroadmap.core.SlayerTask;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CodexGearRoadmapConfig.GROUP)
public interface CodexGearRoadmapConfig extends Config
{
	String GROUP = "codexgearroadmap";
	String TARGET = "target";
	String SLAYER_TASK = "slayerTask";
	/** The old on-task switch, read once to carry a "never" choice over to the slayer task setting. */
	String OLD_ON_TASK = "onTask";
	String FOLLOW_TASK = "followTask";
	String NAME_ON_CARD = "nameOnCard";
	String WORTH_ON_CARD = "worthOnCard";

	@ConfigSection(
		name = "Fighting",
		description = "What Gear Roadmap assumes about how you fight",
		position = 0
	)
	String fighting = "fighting";

	@ConfigSection(
		name = "Claimed upgrades",
		description = "What Gear Roadmap does when you get an upgrade on your road, or your coins reach your savings goal",
		position = 1
	)
	String claims = "claims";

	@ConfigSection(
		name = "Cards",
		description = "What the road card you share shows",
		position = 2
	)
	String cards = "cards";

	@ConfigItem(
		keyName = TARGET,
		name = "Target",
		description = "The monster the roadmap is for; pick one on the panel's Targets tab",
		position = 0,
		section = fighting
	)
	default String target()
	{
		return "Vorkath";
	}

	@ConfigItem(
		keyName = "boost",
		name = "Potions",
		description = "Which boosting potions to assume you drink",
		position = 1,
		section = fighting
	)
	default Fighter.Boost boost()
	{
		return Fighter.Boost.POTIONS;
	}

	@ConfigItem(
		keyName = "prayers",
		name = "Offensive prayers",
		description = "Assume you pray the best offensive prayer you have unlocked",
		position = 2,
		section = fighting
	)
	default boolean prayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = SLAYER_TASK,
		name = "Slayer helmet bonus",
		description = "When to count the black mask and slayer helmet bonus: by default against Slayer monsters,"
			+ " and against a boss only while your task covers it",
		position = 3,
		section = fighting
	)
	default SlayerTask.Mode slayerTask()
	{
		return SlayerTask.Mode.SMART;
	}

	@ConfigItem(
		keyName = FOLLOW_TASK,
		name = "Follow slayer task",
		description = "While your slayer task covers a listed monster, aim the roadmap at it instead of your target"
			+ " (picking another target pauses this until your next task)",
		position = 4,
		section = fighting
	)
	default boolean followTask()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlaque",
		name = "Upgrade plaque",
		description = "Show a plaque in the game view when you get an upgrade from your road, or can afford your savings goal",
		position = 1,
		section = claims
	)
	default boolean showPlaque()
	{
		return true;
	}

	@Range(min = 3, max = 30)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "plaqueSeconds",
		name = "Plaque time",
		description = "How long the plaque stays up",
		position = 2,
		section = claims
	)
	default int plaqueSeconds()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "chatLine",
		name = "Upgrade in chat",
		description = "Add a line to the chat box with the damage a claimed upgrade adds, and when your savings goal is in reach",
		position = 3,
		section = claims
	)
	default boolean chatLine()
	{
		return true;
	}

	@ConfigItem(
		keyName = NAME_ON_CARD,
		name = "Name on cards",
		description = "Write your character's name on the road card; turn off to share it without it",
		position = 0,
		section = cards
	)
	default boolean nameOnCard()
	{
		return true;
	}

	@ConfigItem(
		keyName = WORTH_ON_CARD,
		name = "Worth on cards",
		description = "Show your coins, and how far they go toward your savings goal, on the road card; off, it can be shared without them",
		position = 1,
		section = cards
	)
	default boolean worthOnCard()
	{
		return false;
	}
}
