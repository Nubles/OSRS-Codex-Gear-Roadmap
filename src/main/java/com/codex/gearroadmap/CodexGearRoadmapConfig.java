package com.codex.gearroadmap;

import com.codex.gearroadmap.core.Fighter;
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

	@ConfigSection(
		name = "Fighting",
		description = "What Gear Roadmap assumes about how you fight",
		position = 0
	)
	String fighting = "fighting";

	@ConfigSection(
		name = "Claimed upgrades",
		description = "What Gear Roadmap does when you get an upgrade on your road",
		position = 1
	)
	String claims = "claims";

	@ConfigItem(
		keyName = TARGET,
		name = "Target",
		description = "The monster the roadmap is for. Pick one from the Targets tab in the panel",
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
		keyName = "onTask",
		name = "On a slayer task",
		description = "Count the slayer helmet and black mask bonus against slayer monsters",
		position = 3,
		section = fighting
	)
	default boolean onTask()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlaque",
		name = "Show plaque",
		description = "Show a plaque in the game view when you get an upgrade from your road",
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
		description = "Add a line to the chat box with the damage a claimed upgrade adds",
		position = 3,
		section = claims
	)
	default boolean chatLine()
	{
		return true;
	}
}
