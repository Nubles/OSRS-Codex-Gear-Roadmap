package com.codex.gearroadmap.core;

import java.util.Collections;
import java.util.List;

/** One snapshot for the panel: the roadmap for the focused target and the survey of every target. */
public final class Report
{
	private final boolean loggedIn;
	private final Roadmap roadmap;
	private final List<Roadmap.Survey> survey;
	private final String boosts;
	private final int owned;
	private final boolean bankSeen;
	private boolean ironman;
	private boolean seasonal;
	private String task;

	public Report(boolean loggedIn, Roadmap roadmap, List<Roadmap.Survey> survey, String boosts, int owned, boolean bankSeen)
	{
		this.loggedIn = loggedIn;
		this.roadmap = roadmap;
		this.survey = survey == null ? Collections.emptyList() : survey;
		this.boosts = boosts;
		this.owned = owned;
		this.bankSeen = bankSeen;
	}

	/** A copy that knows the account: ironmen cannot buy, and seasonal worlds have their own economy. */
	public Report withAccount(boolean ironman, boolean seasonal, String task)
	{
		Report r = new Report(loggedIn, roadmap, survey, boosts, owned, bankSeen);
		r.ironman = ironman;
		r.seasonal = seasonal;
		r.task = task;
		return r;
	}

	/** A copy with another roadmap, such as the same road with the player's coins updated. */
	public Report withRoadmap(Roadmap roadmap)
	{
		return new Report(loggedIn, roadmap, survey, boosts, owned, bankSeen).withAccount(ironman, seasonal, task);
	}

	public static Report loggedOut()
	{
		return new Report(false, null, null, null, 0, false);
	}

	public boolean isLoggedIn()
	{
		return loggedIn;
	}

	/** Null until the first plan is ready. */
	public Roadmap getRoadmap()
	{
		return roadmap;
	}

	public List<Roadmap.Survey> getSurvey()
	{
		return survey;
	}

	/** The potions assumed, as the config names them. */
	public String getBoosts()
	{
		return boosts;
	}

	/** How many pieces of combat gear the player owns. */
	public int getOwned()
	{
		return owned;
	}

	/** Ironmen cannot buy from the Grand Exchange, so prices stand for how hard an item is to get. */
	public boolean isIronman()
	{
		return ironman;
	}

	public boolean isSeasonal()
	{
		return seasonal;
	}

	/** The current slayer task's name, lower case, or null. */
	public String getTask()
	{
		return task;
	}

	/** Whether the bank has been read, so the owned list is complete. */
	public boolean isBankSeen()
	{
		return bankSeen;
	}
}
