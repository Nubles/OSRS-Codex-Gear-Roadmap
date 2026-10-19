package com.codex.gearroadmap.core;

import java.util.ArrayList;
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
	private Boolean slayer;
	private Savings.View savings;
	private boolean following;
	private String taskName;
	private List<String> taskTargets = Collections.emptyList();
	private Fights.Tally tally;
	private List<Fights.Pick> fights = Collections.emptyList();
	private List<Levels.Step> levels = Collections.emptyList();

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
		return withAccount(ironman, seasonal, task, null);
	}

	/** As above, with whether the slayer helmet counts against the aimed target, or null when it is no slayer monster. */
	public Report withAccount(boolean ironman, boolean seasonal, String task, Boolean slayer)
	{
		Report r = copy(roadmap);
		r.ironman = ironman;
		r.seasonal = seasonal;
		r.task = task;
		r.slayer = slayer;
		return r;
	}

	/** A copy with another roadmap, such as the same road with the player's coins updated. */
	public Report withRoadmap(Roadmap roadmap)
	{
		return copy(roadmap);
	}

	/**
	 * A copy that knows which targets the slayer task covers, and whether the roadmap is aimed
	 * at one of them because it follows the task rather than the chosen target.
	 */
	public Report withTask(boolean following, List<String> taskTargets, String taskName)
	{
		Report r = copy(roadmap);
		r.following = following;
		r.taskName = taskName;
		r.taskTargets = taskTargets == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(taskTargets));
		return r;
	}

	/**
	 * A copy that knows where the player fights (from Codex Session Chronicle), the upgrades that save
	 * the most time there, and the levels worth training for the aimed target.
	 */
	public Report withExtras(Fights.Tally tally, List<Fights.Pick> fights, List<Levels.Step> levels)
	{
		Report r = copy(roadmap);
		r.tally = tally;
		r.fights = fights == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(fights));
		r.levels = levels == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(levels));
		return r;
	}

	/** Kills by target over the last month from Codex Session Chronicle, or null when it is not installed. */
	public Fights.Tally getTally()
	{
		return tally;
	}

	/** Upgrades weighed across where the player fights, best value first. */
	public List<Fights.Pick> getFights()
	{
		return fights;
	}

	/** The next level worth training in each skill behind the best loadout, best value first. */
	public List<Levels.Step> getLevels()
	{
		return levels;
	}

	/** A copy with the savings goal's standing, or none. */
	public Report withSavings(Savings.View savings)
	{
		Report r = copy(roadmap);
		r.savings = savings;
		return r;
	}

	private Report copy(Roadmap roadmap)
	{
		Report r = new Report(loggedIn, roadmap, survey, boosts, owned, bankSeen);
		r.ironman = ironman;
		r.seasonal = seasonal;
		r.task = task;
		r.slayer = slayer;
		r.savings = savings;
		r.following = following;
		r.taskName = taskName;
		r.taskTargets = taskTargets;
		r.tally = tally;
		r.fights = fights;
		r.levels = levels;
		return r;
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

	/** Whether the slayer helmet counts against the aimed target; null when it is not a slayer monster. */
	public Boolean getSlayer()
	{
		return slayer;
	}

	/** The current slayer task's name, lower case, or null. */
	public String getTask()
	{
		return task;
	}

	/** The savings goal's standing, or null without a goal. */
	public Savings.View getSavings()
	{
		return savings;
	}

	/** Whether the roadmap is aimed at the slayer task's monster rather than the chosen target. */
	public boolean isFollowingTask()
	{
		return following;
	}

	/** The current slayer task's name as the game writes it, or null. */
	public String getTaskName()
	{
		return taskName;
	}

	/** The targets the current slayer task covers, best first; empty without a task or when none are listed. */
	public List<String> getTaskTargets()
	{
		return taskTargets;
	}

	/** Whether the bank has been read, so the owned list is complete. */
	public boolean isBankSeen()
	{
		return bankSeen;
	}
}
