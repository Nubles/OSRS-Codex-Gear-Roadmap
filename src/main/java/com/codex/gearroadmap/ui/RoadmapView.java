package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import java.awt.BorderLayout;
import java.util.function.Consumer;
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
		stack.add(Rows.note("Prices are live Grand Exchange prices. Ammunition is priced per 1,000. Assumes "
			+ (report.getBoosts() == null ? "no boosts" : report.getBoosts().toLowerCase()) + " and your best unlocked prayers.", 190));
		return stack;
	}

	private static Card targetCard(Report report, Runnable openTargets)
	{
		Roadmap map = report.getRoadmap();
		Result r = map.getNow().best().getResult();
		Card card = new Card(new BorderLayout(8, 0));
		card.setBorder(new EmptyBorder(7, 10, 7, 10));
		card.accent(Theme.kind(r.getKind()));
		JPanel text = new JPanel(new BorderLayout());
		text.setOpaque(false);
		text.add(Ui.label(map.getTarget().getName(), Theme.title(), Theme.GOLD_BRIGHT), BorderLayout.NORTH);
		String line = r.getKind() == null ? "No way to hurt it yet"
			: r.getKind() + "  ·  max hit " + r.getMaxHit() + "  ·  " + Format.percent(r.getAccuracy()) + " accuracy";
		text.add(Ui.label(line, Theme.small(), Theme.TEXT_MUTED), BorderLayout.SOUTH);
		card.add(text, BorderLayout.CENTER);
		JPanel trail = new JPanel(new BorderLayout());
		trail.setOpaque(false);
		trail.add(new CodexButton("Change", false, openTargets), BorderLayout.NORTH);
		card.add(trail, BorderLayout.EAST);
		return card;
	}
}
