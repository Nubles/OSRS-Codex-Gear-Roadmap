package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Claims;
import com.codex.gearroadmap.core.Outgrown;
import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.core.Savings;
import com.codex.gearroadmap.data.Gear;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** The Roadmap tab: the target, the road chart, the steps in order, then free, locked and big upgrades. */
final class RoadmapView
{
	private RoadmapView()
	{
	}

	static StackPanel build(Report report, ItemIcons icons, Runnable openTargets, Consumer<Roadmap.Option> preview, Runnable clearGoal)
	{
		return build(report, icons, openTargets, preview, clearGoal, null);
	}

	/**
	 * @param fight the offer to aim at the monster being fought, put under the target card; null for none
	 */
	static StackPanel build(Report report, ItemIcons icons, Runnable openTargets, Consumer<Roadmap.Option> preview, Runnable clearGoal,
		JComponent fight)
	{
		StackPanel stack = new StackPanel(5, 8);
		Roadmap map = report.getRoadmap();
		if (!report.isLoggedIn())
		{
			stack.add(Rows.empty("Your road starts at login", "Log in and Gear Roadmap reads the gear you own and your levels, "
				+ "then lays out the upgrades that speed up your kills the most for every coin."));
			return stack;
		}
		if (map == null)
		{
			stack.add(Rows.note("Reading your gear...", 190));
			return stack;
		}
		stack.add(targetCard(report, openTargets));
		if (fight != null)
		{
			stack.add(fight);
		}
		if (!report.isBankSeen())
		{
			stack.add(Rows.reason(com.codex.gearroadmap.ui.Mark.Kind.NOTE,
				"Open your bank once so the roadmap knows all the gear you own, not just what you carry."));
		}
		Savings.View savings = report.getSavings();
		if (savings != null)
		{
			stack.add(GoalCard.build(savings, map.getTarget().getName(), icons, clearGoal));
		}
		stack.add(new RoadChart(map, icons));

		Result.Kind kind = map.getNow().best().getResult().getKind();
		if (map.getPath().isEmpty())
		{
			stack.add(Rows.note("Nothing on the Grand Exchange would make you faster here. You have the best gear for this target.", 190));
		}
		else
		{
			stack.add(new SectionHeader("Next upgrades", "most DPS per coin"));
			int rank = 0;
			for (Roadmap.Step step : map.getPath())
			{
				Roadmap.Option o = step.getOption();
				rank++;
				stack.add(goal(UpgradeRow.build(o, UpgradeRow.Mode.STEP, rank, step.isAffordable(), kind, icons, map.getTarget(),
					() -> preview.accept(o)), o, savings));
				kind = o.getAfter().getResult().getKind();
			}
		}
		Result.Kind now = map.getNow().best().getResult().getKind();
		if (!map.getFree().isEmpty())
		{
			stack.add(new SectionHeader("Free upgrades", "earned, not bought"));
			for (Roadmap.Option o : map.getFree())
			{
				stack.add(UpgradeRow.build(o, UpgradeRow.Mode.FREE, 0, true, now, icons, map.getTarget(), () -> preview.accept(o)));
			}
		}
		if (!map.getLeaps().isEmpty())
		{
			stack.add(new SectionHeader("Big leaps", "most DPS"));
			for (Roadmap.Option o : map.getLeaps())
			{
				stack.add(goal(UpgradeRow.build(o, UpgradeRow.Mode.LEAP, 0, o.getCost() <= map.getCoins(), now, icons, map.getTarget(),
					() -> preview.accept(o)), o, savings));
			}
		}
		if (!map.getLocked().isEmpty())
		{
			stack.add(new SectionHeader("Unlock by training"));
			for (Roadmap.Option o : map.getLocked())
			{
				stack.add(goal(UpgradeRow.build(o, UpgradeRow.Mode.LOCKED, 0, false, now, icons, map.getTarget(),
					() -> preview.accept(o)), o, savings));
			}
		}
		outgrown(stack, report.getOutgrown(), map, icons);
		roadBehind(stack, report.getClaims(), icons);
		stack.add(Rows.note(footnote(report), 190));
		return stack;
	}

	/** The road as a picture to share: two buttons above the tab's footnote, and a line once one is used. */
	static void shareCard(StackPanel stack, String note, Runnable save, Runnable copy)
	{
		// The footnote on what the numbers assume stays the last thing on the tab.
		int at = Math.max(0, stack.getComponentCount() - 1);
		stack.add(new SectionHeader("Share your road"), at++);
		Card card = new Card(new BorderLayout(0, 6));
		card.setBorder(new EmptyBorder(8, 10, 8, 10));
		card.add(Ui.wrapped("Your loadout, the road ahead and your goal on a gilt card, ready to post.", Theme.small(), Theme.TEXT_MUTED, 176),
			BorderLayout.NORTH);
		JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
		buttons.setOpaque(false);
		buttons.add(new CodexButton("Save card", true, save));
		buttons.add(javax.swing.Box.createHorizontalStrut(6));
		buttons.add(new CodexButton("Copy", false, copy));
		card.add(buttons, BorderLayout.CENTER);
		if (note != null)
		{
			card.add(Ui.wrapped(note, Theme.small(), Theme.TEAL, 176), BorderLayout.SOUTH);
		}
		stack.add(card, at);
	}

	/** Claims listed on the tab; the save keeps a few more. */
	static final int BEHIND = 5;
	private static final java.time.format.DateTimeFormatter DAY = java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH);

	/** The road behind: the upgrades picked up from the road, newest first, with what each added and when. */
	static void roadBehind(StackPanel stack, List<Claims.Claim> claims, ItemIcons icons)
	{
		if (claims == null || claims.isEmpty())
		{
			return;
		}
		stack.add(new SectionHeader("Road behind", claims.size() + " claimed"));
		for (int i = 0; i < claims.size() && i < BEHIND; i++)
		{
			Claims.Claim c = claims.get(i);
			Card row = Rows.line(new IconBadge(c.getIcon(), icons, true), firstPiece(c.getName()), behindDetail(c),
				Ui.label(claimDay(c.getAt()), Theme.small(), Theme.TEXT_MUTED), i == 0 ? Theme.GOLD : null, 118);
			row.setToolTipText("<html>" + Ui.escape(c.getName()) + "<br>Picked up " + Ui.escape(claimDay(c.getAt()))
				+ ", adding " + Format.gain(c.getGain()) + " DPS against " + Ui.escape(c.getTarget()) + " at the time.</html>");
			stack.add(row);
		}
	}

	/** "+0.81 DPS at Vorkath", or "Pair, +0.23 DPS at Vorkath" for two pieces bought together. */
	static String behindDetail(Claims.Claim c)
	{
		return (c.getName().contains(" + ") ? "Pair, " : "") + Format.gain(c.getGain()) + " DPS at " + c.getTarget();
	}

	/** A pair bought together ("A + B") is listed by its first piece; the tooltip names both. */
	static String firstPiece(String name)
	{
		int plus = name.indexOf(" + ");
		return plus < 0 ? name : name.substring(0, plus);
	}

	private static String claimDay(long at)
	{
		return DAY.format(java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()));
	}

	/** Owned gear no best loadout wears, and what selling it puts toward the road. */
	static void outgrown(StackPanel stack, Outgrown outgrown, Roadmap map, ItemIcons icons)
	{
		if (outgrown == null || outgrown.isEmpty())
		{
			return;
		}
		stack.add(new SectionHeader("Outgrown", Format.coins(outgrown.getWorth())));
		int shown = 0;
		for (Outgrown.Piece p : outgrown.getPieces())
		{
			if (shown++ == Outgrown.SHOWN)
			{
				break;
			}
			Card row = Rows.line(new IconBadge(p.getGear().getId(), icons, true), p.getGear().getName(),
				Gear.SLOT_NAMES[p.getGear().getSlot()], Ui.label(Format.coins(p.getPrice()), Theme.small(), Theme.GOLD), null);
			row.setToolTipText("<html>" + Ui.escape(p.getGear().getName()) + " is in none of your best loadouts,<br>for any kind of attack against any target.</html>");
			stack.add(row);
		}
		int more = outgrown.getPieces().size() - Outgrown.SHOWN;
		StringBuilder text = new StringBuilder("None of your best loadouts, for any style against any target, wear ")
			.append(outgrown.getPieces().size() == 1 ? "this" : "these")
			.append(more > 0 ? " or " + more + " more" : "").append(".");
		if (!map.getPath().isEmpty())
		{
			Roadmap.Option next = map.getPath().get(0).getOption();
			long cost = next.getCost();
			long share = cost <= 0 ? 0 : outgrown.getWorth() * 100 / cost;
			if (share >= 1)
			{
				text.append(outgrown.getWorth() >= cost ? " Selling them would pay for " + next.getName() + "."
					: " Selling them would pay " + share + "% of " + next.getName() + ".");
			}
		}
		text.append(" Gear kept for its effect (dragonfire shields, slayer and tank gear), skilling tools and untradeables are never listed.");
		stack.add(Rows.note(text.toString(), 190));
	}

	/** The upgrade the player is saving for wears a gilt rim wherever it appears. */
	private static Card goal(Card row, Roadmap.Option o, Savings.View savings)
	{
		if (savings != null && savings.getOption().getName().equals(o.getName()))
		{
			row.highlighted(true);
			row.setToolTipText(row.getToolTipText().replace("</html>", "<br>Your savings goal</html>"));
		}
		return row;
	}

	/** What the numbers assume, in a sentence or three. */
	static String footnote(Report report)
	{
		StringBuilder text = new StringBuilder();
		if (report.isIronman())
		{
			text.append("As an ironman you cannot buy, so Grand Exchange prices here stand for how hard each item is to get. ");
		}
		else if (report.isSeasonal())
		{
			text.append("Prices are from the main game's Grand Exchange, not this world's. ");
		}
		else
		{
			text.append("Prices are live Grand Exchange prices. ");
		}
		text.append("Ammunition is priced per 1,000. Assumes ")
			.append(report.getBoosts() == null ? "no boosts" : report.getBoosts().toLowerCase(Locale.ROOT))
			.append(" and your best unlocked prayers.");
		Boolean slayer = report.getSlayer();
		if (report.getSavings() == null && !report.isIronman() && report.getRoadmap() != null && !report.getRoadmap().getPath().isEmpty())
		{
			text.append(" Click an upgrade and press Save for it to make it your savings goal.");
		}
		if (slayer != null)
		{
			String name = report.getRoadmap() == null ? "this target" : report.getRoadmap().getTarget().getName();
			text.append(slayer ? " Counts the slayer helmet bonus against " + name + "."
				: " No slayer helmet bonus against " + name + " without a task for it.");
		}
		return text.toString();
	}

	/**
	 * The monster the player is fighting, when it is a listed target the road is not aimed at: what
	 * their best gear does there, and a button to aim the road at it.
	 *
	 * @return the card, or null when there is nothing to offer
	 */
	static Card fightCard(Report report, String fighting, Runnable aim, Runnable hide)
	{
		Roadmap map = report.getRoadmap();
		if (fighting == null || map == null || !report.isLoggedIn() || report.getSurvey() == null || fighting.equals(map.getTarget().getName()))
		{
			return null;
		}
		String here = null;
		for (Roadmap.Survey row : report.getSurvey())
		{
			if (row.getTarget().getName().equals(fighting))
			{
				Result r = row.getBest().best().getResult();
				here = r.getKind() == null ? "Nothing you own can hurt it" : "Your best here: " + Format.dps(r.getDps()) + " DPS, " + r.getKind().toString().toLowerCase(Locale.ROOT);
				break;
			}
		}
		Card card = new Card(new BorderLayout(0, 4));
		card.setBorder(new EmptyBorder(6, 10, 7, 10));
		card.accent(Theme.TEAL);
		JLabel title = Ui.label("Fighting " + fighting, Theme.body(), Theme.TEXT);
		title.setMinimumSize(new Dimension(10, 10));
		title.setPreferredSize(new Dimension(10, title.getPreferredSize().height));
		card.add(title, BorderLayout.NORTH);
		if (here != null)
		{
			card.add(Ui.label(here, Theme.small(), Theme.TEXT_MUTED), BorderLayout.CENTER);
		}
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.setOpaque(false);
		buttons.add(new CodexButton("Aim the road here", true, aim));
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(new CodexButton("Hide", false, hide));
		card.add(buttons, BorderLayout.SOUTH);
		card.setToolTipText("<html>You are fighting " + Ui.escape(fighting) + ", but the road is aimed at " + Ui.escape(map.getTarget().getName())
			+ ".<br>The road never moves on its own: aim it here if this is the fight you want gear for.</html>");
		return card;
	}

	private static Card targetCard(Report report, Runnable openTargets)
	{
		Roadmap map = report.getRoadmap();
		Result r = map.getNow().best().getResult();
		Card card = new Card(new BorderLayout(8, 0));
		card.setBorder(new EmptyBorder(7, 10, 7, 10));
		card.accent(Theme.kind(r.getKind()));
		// The name shares its line with the button; the fight's numbers get the full width below.
		JPanel top = new JPanel(new BorderLayout(6, 0));
		top.setOpaque(false);
		JLabel name = Ui.label(map.getTarget().getName(), Theme.title(), Theme.GOLD_BRIGHT);
		name.setMinimumSize(new Dimension(10, 10));
		name.setPreferredSize(new Dimension(10, name.getPreferredSize().height));
		top.add(name, BorderLayout.CENTER);
		top.add(new CodexButton("Change", false, openTargets), BorderLayout.EAST);
		card.add(top, BorderLayout.NORTH);
		String line = r.getKind() == null ? "No way to hurt it yet"
			: r.getKind() + "  ·  max hit " + r.getMaxHit() + "  ·  " + Format.percent(r.getAccuracy()) + " acc";
		JLabel sub = Ui.label(line, Theme.small(), Theme.TEXT_MUTED);
		sub.setBorder(new EmptyBorder(3, 0, 0, 0));
		sub.setToolTipText(r.getKind() == null ? null : "Your best setup hits up to " + r.getMaxHit() + " and lands "
			+ Format.percent(r.getAccuracy()) + " of its attacks");
		JPanel lines = new JPanel(new BorderLayout());
		lines.setOpaque(false);
		lines.add(sub, BorderLayout.NORTH);
		if (report.isFollowingTask())
		{
			JLabel task = Ui.label(report.getTaskName() == null ? "On your slayer task" : "On task: " + Format.task(report.getTaskName()),
				Theme.small(), Theme.TEAL);
			task.setBorder(new EmptyBorder(2, 0, 0, 0));
			task.setToolTipText("The roadmap follows your slayer task. Pick a target the task does not cover to aim elsewhere until your next task");
			lines.add(task, BorderLayout.SOUTH);
		}
		card.add(lines, BorderLayout.SOUTH);
		return card;
	}
}
