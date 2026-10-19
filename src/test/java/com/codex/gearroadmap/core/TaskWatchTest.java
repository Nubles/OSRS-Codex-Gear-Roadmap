package com.codex.gearroadmap.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The slayer task through logins and world hops, where the client takes its varps afresh. */
public class TaskWatchTest
{
	private static void settle(TaskWatch w, String key)
	{
		for (int i = 0; i < TaskWatch.SETTLE_TICKS - 1; i++)
		{
			assertEquals(TaskWatch.Change.NONE, w.tick(key));
		}
	}

	@Test
	public void aHopsBlinkNeitherEndsTheTaskNorReplans()
	{
		TaskWatch w = new TaskWatch();
		settle(w, "42:0");
		assertEquals(TaskWatch.Change.CHANGED, w.tick("42:0"));
		assertTrue(w.isSettled());

		// The hop: the varps read none while the new world's are sent, then the task again.
		w.arrived();
		assertFalse(w.isSettled());
		assertEquals(TaskWatch.Change.NONE, w.seen(""));
		assertEquals(TaskWatch.Change.NONE, w.tick(""));
		assertEquals(TaskWatch.Change.NONE, w.seen("42:0"));
		settle(w, "42:0");
		assertEquals("same task after the hop: nothing to do", TaskWatch.Change.NONE, w.tick("42:0"));
		assertEquals("42:0", w.getKey());
	}

	@Test
	public void theTaskEndingOnceSettledIsBelieved()
	{
		TaskWatch w = new TaskWatch();
		settle(w, "42:0");
		w.tick("42:0");
		assertEquals(TaskWatch.Change.NONE, w.seen("42:0"));
		assertEquals(TaskWatch.Change.ENDED, w.seen(""));
		assertEquals(TaskWatch.Change.CHANGED, w.seen("98:3"));
	}

	@Test
	public void aTaskChangedWhileAwayIsSeenAtTheSettlingTick()
	{
		TaskWatch w = new TaskWatch();
		settle(w, "42:0");
		w.tick("42:0");
		w.arrived();
		settle(w, "7:0");
		assertEquals(TaskWatch.Change.CHANGED, w.tick("7:0"));
		w.arrived();
		settle(w, "");
		assertEquals("finished on another device: the next task is followed afresh", TaskWatch.Change.ENDED, w.tick(""));
	}

	@Test
	public void afterLoggingOutTheFirstReadingIsNoEnd()
	{
		TaskWatch w = new TaskWatch();
		settle(w, "42:0");
		w.tick("42:0");
		w.forget();
		settle(w, "");
		assertEquals(TaskWatch.Change.CHANGED, w.tick(""));
		assertEquals(TaskWatch.Change.NONE, w.seen(""));
	}
	/**
	 * The plan reads the task straight from the varps, so it is only made once settled: a plan made
	 * during the blink would aim away from the task, and nothing after it would plan again.
	 */
	@Test
	public void aPlanMadeDuringTheBlinkWouldNeverBeRedone()
	{
		TaskWatch w = new TaskWatch();
		settle(w, "42:0");
		w.tick("42:0");
		w.arrived();
		assertFalse("not settled on the first tick after a hop, when the varps may still read none", w.isSettled());
		assertEquals(TaskWatch.Change.NONE, w.tick(""));
		assertFalse(w.isSettled());
		for (int i = 0; i < TaskWatch.SETTLE_TICKS - 2; i++)
		{
			assertEquals(TaskWatch.Change.NONE, w.tick("42:0"));
		}
		assertEquals("the task came back as it was: no new plan is asked for", TaskWatch.Change.NONE, w.tick("42:0"));
		assertTrue(w.isSettled());
	}
}
