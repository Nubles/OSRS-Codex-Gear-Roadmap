package com.codex.gearroadmap.core;

import com.codex.gearroadmap.data.Target;
import java.util.Collections;
import java.util.List;

/**
 * Which target the roadmap is for. Normally the one the player chose; while they have a
 * slayer task for a listed monster, and they let the roadmap follow it, the task's monster
 * instead, so the road answers "what should I buy for this task?". Bosses a broad task also
 * covers (Vorkath on blue dragons) are offered, not aimed at, until the player picks one.
 */
public final class Aim
{
	private final Target target;
	private final boolean following;
	private final List<Target> covered;

	private Aim(Target target, boolean following, List<Target> covered)
	{
		this.target = target;
		this.following = following;
		this.covered = covered;
	}

	/**
	 * @param targets every listed target, in the catalogue's order
	 * @param chosen the target the player chose, by name; the first target when it is unknown
	 * @param follow whether to follow the task: the setting is on and the player has not turned
	 *               away from this task by choosing a target it does not cover
	 * @param task the current slayer task's name, or null
	 * @param bossTask whether the task is a boss task, given by a boss slayer master
	 * @param pick which of the task's targets the player picked, or null for the task's own
	 */
	public static Aim choose(List<Target> targets, String chosen, boolean follow, String task, boolean bossTask, String pick)
	{
		Target base = null;
		for (Target t : targets)
		{
			if (t.getName().equals(chosen))
			{
				base = t;
				break;
			}
		}
		if (base == null && !targets.isEmpty())
		{
			base = targets.get(0);
		}
		List<Target> covered = SlayerTask.targetsFor(task, targets);
		if (!follow || covered.isEmpty())
		{
			return new Aim(base, false, covered);
		}
		for (Target t : covered)
		{
			if (t.getName().equals(pick))
			{
				return new Aim(t, true, covered);
			}
		}
		// The chosen target already on the task stays: someone aimed at Cerberus on a hellhound task meant it.
		if (covered.contains(base))
		{
			return new Aim(base, true, covered);
		}
		// A task only moves the aim to a boss when it is that boss's own task: a Bears task is
		// usually done on bears, not Callisto, so it waits for the player to pick the boss.
		Target first = covered.get(0);
		if (bossTask || !"Boss".equals(first.getGroup()) || SlayerTask.normal(first.getName()).equals(SlayerTask.normal(task)))
		{
			return new Aim(first, true, covered);
		}
		return new Aim(base, false, covered);
	}

	public Target getTarget()
	{
		return target;
	}

	/** Whether the target is the task's rather than the one the player chose. */
	public boolean isFollowing()
	{
		return following;
	}

	/** The listed targets the task covers, best first. */
	public List<Target> getCovered()
	{
		return Collections.unmodifiableList(covered);
	}
}
