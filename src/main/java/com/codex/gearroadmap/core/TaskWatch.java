package com.codex.gearroadmap.core;

/**
 * Follows the slayer task by its key (task id and boss id, or "" without one) through logins and
 * world hops. Arriving in a world makes the client take its varps afresh, so for a few ticks the
 * task can read as none and then come back. Taking that blink at its word would end the task, and
 * with it the player's choice to look elsewhere or at one of its bosses; so a change is only
 * believed once the varps have had a few ticks to settle.
 */
public final class TaskWatch
{
	/** Ticks after arriving in a world before the task's varps are believed. */
	public static final int SETTLE_TICKS = 5;

	/** What a reading of the task means for the plan. */
	public enum Change
	{
		/** Nothing to do. */
		NONE,
		/** A task began, or is a different one: plan again. */
		CHANGED,
		/** The task was done or dropped: plan again, and follow the next one afresh. */
		ENDED
	}

	/** The task as last believed, or null before the first settled reading. */
	private String key;
	private int settle = SETTLE_TICKS;

	/** The player arrived in a world: a login, a world hop or the connection coming back. */
	public void arrived()
	{
		settle = SETTLE_TICKS;
	}

	/** Logged out: the next login's task is read afresh. */
	public void forget()
	{
		key = null;
		settle = SETTLE_TICKS;
	}

	/** Whether the varps have settled since arriving, so what changes now is the player's doing. */
	public boolean isSettled()
	{
		return settle == 0;
	}

	/** A task varp moved, reading the task as it now stands. */
	public Change seen(String now)
	{
		return settle > 0 ? Change.NONE : compare(now);
	}

	/** A game tick passed while logged in; the settling tick reads the task once. */
	public Change tick(String now)
	{
		if (settle == 0 || --settle > 0)
		{
			return Change.NONE;
		}
		return compare(now);
	}

	/** The task as last believed, "" without one, or null before it is known. */
	public String getKey()
	{
		return key;
	}

	private Change compare(String now)
	{
		if (now.equals(key))
		{
			return Change.NONE;
		}
		String was = key;
		key = now;
		return now.isEmpty() && was != null && !was.isEmpty() ? Change.ENDED : Change.CHANGED;
	}
}
