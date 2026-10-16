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

	public Report(boolean loggedIn, Roadmap roadmap, List<Roadmap.Survey> survey, String boosts, int owned, boolean bankSeen)
	{
		this.loggedIn = loggedIn;
		this.roadmap = roadmap;
		this.survey = survey == null ? Collections.emptyList() : survey;
		this.boosts = boosts;
		this.owned = owned;
		this.bankSeen = bankSeen;
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

	/** Whether the bank has been read, so the owned list is complete. */
	public boolean isBankSeen()
	{
		return bankSeen;
	}
}
