package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** The Roadmap tab: the target, the road chart, the steps in order, then free, locked and big upgrades. */
final class RoadmapView
{
	private RoadmapView()
	{
	}

	static StackPanel build(Report report, ItemIcons icons, Runnable openTargets, Consumer<Roadmap.Option> preview)
	{
		StackPanel stack = new StackPanel(5, 8);
		Roadmap map = report.getRoadmap();
		if (!report.isLoggedIn())
		{
			stack.add(Rows.note("Log in to see the gear upgrades that will speed up your kills the most for every coin.", 190));
			return stack;
		}
		if (map == null)
		{
			stack.add(Rows.note("Reading your gear...", 190));
			return stack;
		}
		stack.add(targetCard(report, openTargets));
		if (!report.isBankSeen())
		{
			stack.add(Rows.reason(com.codex.gearroadmap.ui.Mark.Kind.NOTE,
				"Open your bank once so the roadmap knows all the gear you own, not just what you carry."));
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
				stack.add(UpgradeRow.build(o, UpgradeRow.Mode.STEP, rank, step.isAffordable(), kind, icons, () -> preview.accept(o)));
				kind = o.getAfter().getResult().getKind();
			}
		}
		Result.Kind now = map.getNow().best().getResult().getKind();
		if (!map.getFree().isEmpty())
		{
			stack.add(new SectionHeader("Free upgrades", "earned, not bought"));
			for (Roadmap.Option o : map.getFree())
			{
				stack.add(UpgradeRow.build(o, UpgradeRow.Mode.FREE, 0, true, now, icons, () -> preview.accept(o)));
			}
		}
		if (!map.getLeaps().isEmpty())
		{
			stack.add(new SectionHeader("Big leaps", "most DPS"));
			for (Roadmap.Option o : map.getLeaps())
			{
				stack.add(UpgradeRow.build(o, UpgradeRow.Mode.LEAP, 0, o.getCost() <= map.getCoins(), now, icons, () -> preview.accept(o)));
			}
		}
		if (!map.getLocked().isEmpty())
		{
			stack.add(new SectionHeader("Unlock by training"));
			for (Roadmap.Option o : map.getLocked())
			{
				stack.add(UpgradeRow.build(o, UpgradeRow.Mode.LOCKED, 0, false, now, icons, () -> preview.accept(o)));
			}
		}
		stack.add(Rows.note(footnote(report), 190));
		return stack;
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
			.append(report.getBoosts() == null ? "no boosts" : report.getBoosts().toLowerCase())
			.append(" and your best unlocked prayers.");
		Boolean slayer = report.getSlayer();
		if (slayer != null)
		{
			String name = report.getRoadmap() == null ? "this target" : report.getRoadmap().getTarget().getName();
			text.append(slayer ? " Counts the slayer helmet bonus against " + name + "."
				: " No slayer helmet bonus against " + name + " without a task for it.");
		}
		return text.toString();
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
		card.add(sub, BorderLayout.SOUTH);
		return card;
	}
}
