package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.RoadmapData;
import com.google.gson.Gson;
import java.io.IOException;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class SlayerTaskTest
{
	private static RoadmapData data;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
	}

	@Test
	public void slayerCreaturesCountWithoutATask()
	{
		SlayerTask none = SlayerTask.of(SlayerTask.Mode.SMART, null);
		assertTrue(none.covers(data.target("Abyssal demon")));
		assertFalse(none.covers(data.target("Vorkath")));
		assertFalse(none.covers(data.target("Sand Crab")));
		assertFalse("Nex is never a slayer monster", SlayerTask.ALWAYS.covers(data.target("Nex")));
	}

	@Test
	public void bossesCountOnTheirTask()
	{
		assertTrue(SlayerTask.of(SlayerTask.Mode.SMART, "Blue dragons").covers(data.target("Vorkath")));
		assertTrue(SlayerTask.of(SlayerTask.Mode.SMART, "Vorkath").covers(data.target("Vorkath")));
		assertTrue(SlayerTask.of(SlayerTask.Mode.SMART, "The Abyssal Sire").covers(data.target("Abyssal Sire")));
		assertTrue(SlayerTask.of(SlayerTask.Mode.SMART, "Abyssal demons").covers(data.target("Abyssal Sire")));
		assertTrue(SlayerTask.of(SlayerTask.Mode.SMART, "Greater demons").covers(data.target("K'ril Tsutsaroth")));
		assertTrue(SlayerTask.of(SlayerTask.Mode.SMART, "Hill Giants").covers(data.target("Hill Giant")));
		assertFalse(SlayerTask.of(SlayerTask.Mode.SMART, "Black demons").covers(data.target("Vorkath")));
	}

	@Test
	public void crabsTaskCoversTrainingCrabs()
	{
		// RuneLite's Slayer task list counts every crab species toward a Crabs task.
		assertTrue(SlayerTask.of(SlayerTask.Mode.SMART, "Crabs").covers(data.target("Sand Crab")));
		assertTrue(SlayerTask.of(SlayerTask.Mode.SMART, "Crabs").covers(data.target("Ammonite Crab")));
		assertTrue(SlayerTask.of(SlayerTask.Mode.SMART, "Crabs").covers(data.target("Rock Crab")));
	}

	@Test
	public void modesOverride()
	{
		assertTrue(SlayerTask.ALWAYS.covers(data.target("Vorkath")));
		assertFalse(SlayerTask.NEVER.covers(data.target("Abyssal demon")));
	}
}
