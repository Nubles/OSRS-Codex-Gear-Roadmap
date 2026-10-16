package com.codex.gearroadmap;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CodexGearRoadmapPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CodexGearRoadmapPlugin.class);
		RuneLite.main(args);
	}
}
