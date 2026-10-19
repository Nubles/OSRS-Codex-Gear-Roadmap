package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Loadout;
import com.codex.gearroadmap.core.Planner;
import com.codex.gearroadmap.core.Levels;
import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.core.Savings;
import com.codex.gearroadmap.data.Gear;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** The Loadout tab: the best setup the player owns for each kind of attack, or the one an upgrade builds. */
final class LoadoutView
{
	static final String[] KINDS = {"Best", "Melee", "Ranged", "Magic"};

	private LoadoutView()
	{
	}

	static StackPanel build(Report report, int kindIndex, Roadmap.Option preview, ItemIcons icons, Runnable clearPreview,
		Consumer<Roadmap.Option> saveFor)
	{
		StackPanel stack = new StackPanel(6, 8);
		Roadmap map = report.getRoadmap();
		if (!report.isLoggedIn() || map == null)
		{
			stack.add(Rows.note(report.isLoggedIn() ? "Reading your gear..." : "Log in to see your best loadouts.", 190));
			return stack;
		}
		Planner.Plan plan;
		Set<Gear> fresh = new HashSet<>();
		if (preview != null)
		{
			plan = preview.getAfter();
			fresh.addAll(preview.getPieces());
			Card banner = new Card(new BorderLayout(8, 0));
			banner.accent(Theme.TEAL);
			banner.setBorder(new EmptyBorder(6, 10, 6, 10));
			JPanel text = new JPanel(new BorderLayout());
			text.setOpaque(false);
			JLabel with = Ui.label("With " + preview.getName(), Theme.body(), Theme.TEXT);
			with.setToolTipText(preview.getName());
			with.setMinimumSize(new Dimension(10, 10));
			with.setPreferredSize(new Dimension(10, with.getPreferredSize().height));
			text.add(with, BorderLayout.NORTH);
			text.add(Ui.label("New pieces glow teal", Theme.small(), Theme.TEAL), BorderLayout.SOUTH);
			banner.add(text, BorderLayout.CENTER);
			JPanel trail = new JPanel(new BorderLayout());
			trail.setOpaque(false);
			trail.add(new CodexButton("Back", false, clearPreview), BorderLayout.NORTH);
			banner.add(trail, BorderLayout.EAST);
			stack.add(banner);
			// Anything bought can be saved for; ironmen cannot buy, and earned upgrades cost nothing.
			if (!report.isIronman() && preview.getCost() > 0)
			{
				Savings.View savings = report.getSavings();
				boolean already = savings != null && savings.getOption().getName().equals(preview.getName());
				JPanel save = new JPanel(new BorderLayout(8, 0));
				save.setOpaque(false);
				save.setBorder(new EmptyBorder(0, 2, 0, 0));
				String line = already ? "This is your savings goal." : savings != null
					? "Replaces " + savings.getOption().getName() + " as your goal." : "Track your coins toward it on the Roadmap.";
				save.add(Ui.wrapped(line, Theme.small(), already ? Theme.GOLD : Theme.TEXT_MUTED, 110), BorderLayout.CENTER);
				if (!already)
				{
					JPanel holder = new JPanel(new BorderLayout());
					holder.setOpaque(false);
					holder.add(new CodexButton("Save for it", true, () -> saveFor.accept(preview)), BorderLayout.NORTH);
					save.add(holder, BorderLayout.EAST);
				}
				stack.add(save);
			}
		}
		else if (kindIndex == 0)
		{
			plan = map.getNow().best();
		}
		else
		{
			plan = map.getNow().get(Result.Kind.values()[kindIndex - 1]);
		}
		Result r = plan == null ? null : plan.getResult();
		if (r == null || r.getKind() == null || r.getDps() <= 0)
		{
			Result.Kind wanted = kindIndex == 0 ? null : Result.Kind.values()[kindIndex - 1];
			String name = map.getTarget().getName();
			stack.add(Rows.note(wanted != null && !map.getTarget().allows(wanted.name())
				? name + " can't be hurt with " + wanted.toString().toLowerCase() + "."
				: "You own nothing that can attack " + name + " this way.", 190));
			return stack;
		}
		Color tint = Theme.kind(r.getKind());
		stack.add(new SectionHeader(r.getKind() + " vs " + map.getTarget().getName()));
		Card frame = new Card(new BorderLayout());
		frame.setBorder(new EmptyBorder(8, 6, 8, 6));
		frame.add(new GearGrid(plan.getLoadout(), fresh, tint, icons), BorderLayout.CENTER);
		stack.add(frame);

		JPanel tiles = new JPanel(new GridLayout(2, 2, 5, 5));
		tiles.setOpaque(false);
		double before = map.getNow().dps();
		tiles.add(new StatTile(Format.dps(r.getDps()), "DPS", preview != null ? Format.gain(r.getDps() - before) + " vs now" : null, tint));
		tiles.add(new StatTile(String.valueOf(r.getMaxHit()), "Max hit", "every " + r.getTicks() + " ticks", Theme.GOLD));
		tiles.add(new StatTile(Format.percent(r.getAccuracy()), "Accuracy", null, Theme.TEAL));
		tiles.add(new StatTile(Format.time(r.secondsToKill(map.getTarget())), "Kill time", map.getTarget().getHp() + " hitpoints",
			Theme.EMBER));
		stack.add(tiles);

		stack.add(Rows.check(Mark.Kind.NOTE, styleLine(r), r.getPrayer() == null ? "No prayer" : r.getPrayer(), null));
		stack.add(Rows.check(Mark.Kind.NOTE, report.getBoosts() == null ? "No boosts" : report.getBoosts(),
			pieces(plan.getLoadout()) + " pieces worn", null));
		int uptime = map.getTarget().uptime(r.getKind().name());
		if (uptime < 100)
		{
			stack.add(Rows.check(Mark.Kind.NOTE, r.getKind() + " counted at " + uptime + "%",
				map.getTarget().getName() + "'s phases keep you off", null));
		}
		Boolean slayer = report.getSlayer();
		if (slayer != null)
		{
			stack.add(Rows.check(slayer ? Mark.Kind.GOOD : Mark.Kind.NOTE, slayer ? "On task" : "Off task",
				slayer ? "Slayer helmet bonus counts" : "No slayer helmet bonus", null));
		}
		if (preview == null && kindIndex == 0 && !report.getLevels().isEmpty())
		{
			// Training is an upgrade too: the next level in each skill that adds damage, in the same gear.
			stack.add(new SectionHeader("Levels that pay", "same gear"));
			boolean priced = false;
			for (Levels.Step step : report.getLevels())
			{
				String what = (step.isNewMaxHit() ? "Max hit " + step.getMaxHit() : "Lands more often") + ", " + Format.fine(step.getShare()) + " DPS";
				boolean worth = step.getWorth() > 0 && !report.isIronman();
				priced |= worth;
				Card row = Rows.line(new Mark(Mark.Kind.GOOD, 14), step.getSkill() + " " + step.getTo(), what,
					worth ? Ui.label(Format.coins(step.getWorth()), Theme.small(), Theme.GOLD) : null, null);
				row.setToolTipText("<html>" + step.getSkill() + " " + step.getFrom() + " to " + step.getTo() + " adds "
					+ Format.gain(step.getGain()) + " DPS against " + Ui.escape(map.getTarget().getName()) + " in this loadout."
					+ (worth ? "<br>The road ahead pays about that much for the same damage." : "") + "</html>");
				stack.add(row);
			}
			if (priced)
			{
				stack.add(Rows.note("Beside each, what the road ahead would charge in gear for the same damage.", 190));
			}
		}
		if (preview == null)
		{
			stack.add(Rows.note("Built from the " + report.getOwned() + " pieces of combat gear you own. Click any upgrade on the Roadmap to see the loadout it makes.", 190));
		}
		return stack;
	}

	private static String styleLine(Result r)
	{
		if (r.getSpell() != null)
		{
			return r.getSpell() + ", " + r.getStyle().toLowerCase();
		}
		return r.getStyle();
	}

	private static int pieces(Loadout lo)
	{
		int n = 0;
		for (int i = 0; i < Gear.SLOTS; i++)
		{
			if (lo.get(i) != null)
			{
				n++;
			}
		}
		return n;
	}
}
