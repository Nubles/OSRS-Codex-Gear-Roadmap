package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Advisor;
import com.codex.gearroadmap.core.Fights;
import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.data.Target;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;

/**
 * The Targets tab: every target with the best the player can do there now; clicking one aims the
 * roadmap at it, and right-clicking one puts it on the shortlist the upgrades are weighed across.
 */
final class TargetsView
{
	static final String[] FILTERS = {"All", "Bosses", "Slayer", "Training"};

	private TargetsView()
	{
	}

	static StackPanel build(Report report, int filter, String search, String focused, Consumer<String> onPick,
		Consumer<String> onShortlist, ItemIcons icons)
	{
		TargetRows rows = new TargetRows(report, focused, onPick, onShortlist);
		StackPanel stack = new StackPanel(4, 8);
		if (!report.isLoggedIn())
		{
			stack.add(Rows.empty("Over 70 targets wait", "Log in to see your damage and kill time against every boss, slayer monster and training spot."));
			return stack;
		}
		if (report.getSurvey().isEmpty())
		{
			stack.add(Rows.note("Weighing your gear against every target...", 190));
			return stack;
		}
		String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
		boolean listed = needle.isEmpty() && filter == 0 && shortlist(stack, report, icons, rows);
		boolean fought = needle.isEmpty() && filter == 0 && fights(stack, report, icons);
		List<String> task = report.getTaskTargets();
		if (!task.isEmpty() && needle.isEmpty() && filter == 0)
		{
			// The task's monsters first, so aiming at what the task covers is one click away.
			stack.add(new SectionHeader("Slayer task", report.getTaskName() == null ? null : Format.task(report.getTaskName())));
			for (String name : task)
			{
				for (Roadmap.Survey row : report.getSurvey())
				{
					if (row.getTarget().getName().equals(name))
					{
						stack.add(rows.row(row));
					}
				}
			}
			stack.add(new SectionHeader("All targets"));
		}
		else if (fought || listed)
		{
			stack.add(new SectionHeader("All targets"));
		}
		int shown = 0;
		for (Roadmap.Survey row : report.getSurvey())
		{
			Target t = row.getTarget();
			if (!matches(t, filter) || !needle.isEmpty() && !t.getName().toLowerCase(Locale.ROOT).contains(needle))
			{
				continue;
			}
			stack.add(rows.row(row));
			shown++;
		}
		if (shown == 0)
		{
			stack.add(Rows.note("No targets match.", 190));
		}
		return stack;
	}

	/**
	 * The targets the player shortlisted, such as the bosses left to finish, and the upgrades that
	 * save the most fighting time across all of them. Shown only once something is shortlisted.
	 */
	static boolean shortlist(StackPanel stack, Report report, ItemIcons icons, TargetRows rows)
	{
		List<String> names = report.getShortlist();
		if (names.isEmpty())
		{
			return false;
		}
		stack.add(new SectionHeader("Your shortlist", names.size() == 1 ? "1 target" : names.size() + " targets"));
		JComponent note = Rows.note("Upgrades weighed over " + Advisor.SHORTLIST_KILLS + " kills of each. Most time saved for the coins first.", 190);
		note.setToolTipText("<html>Right-click any target to add it to the shortlist or take it off.<br>"
			+ "Useful for the bosses left on your collection log, or the stops on a long grind.</html>");
		stack.add(note);
		for (String name : names)
		{
			for (Roadmap.Survey row : report.getSurvey())
			{
				if (row.getTarget().getName().equals(name))
				{
					stack.add(rows.row(row));
				}
			}
		}
		List<Fights.Pick> picks = report.getShortlistPicks();
		if (picks.isEmpty())
		{
			stack.add(Rows.note(report.isIronman() ? "Ironmen are shown the roadmap for each target instead."
				: "Nothing you could buy now would save much time across these.", 190));
			return true;
		}
		String span = Advisor.SHORTLIST_KILLS + " kills of each";
		for (Fights.Pick p : picks)
		{
			stack.add(pick(p, icons, span));
		}
		return true;
	}

	/** Where the player really fights, from Session Chronicle, and the upgrades that save the most time there. */
	static boolean fights(StackPanel stack, Report report, ItemIcons icons)
	{
		Fights.Tally tally = report.getTally();
		if (tally == null || tally.isEmpty())
		{
			return false;
		}
		String span = tally.getDays() == 1 ? "the last day" : "the last " + tally.getDays() + " days";
		stack.add(new SectionHeader("Where you fight", span.substring(4)));
		StringBuilder line = new StringBuilder("Kills from Session Chronicle: ");
		int shown = 0;
		for (Map.Entry<Target, Integer> e : tally.getKills().entrySet())
		{
			if (shown == 3)
			{
				line.append(", and more");
				break;
			}
			line.append(shown == 0 ? "" : ", ").append(e.getKey().getName()).append(' ')
				.append(String.format(Locale.ENGLISH, "%,d", e.getValue()));
			shown++;
		}
		JComponent note = Rows.note(line.append(". Most time saved for the coins first.").toString(), 190);
		note.setToolTipText("<html>Each kill is weighed as it was fought: on a slayer task that covered it with the<br>"
			+ "slayer helmet's bonus, off one without. Kills logged before Chronicle kept the task<br>"
			+ "count Slayer creatures as on task and bosses as off.</html>");
		stack.add(note);
		List<Fights.Pick> picks = report.getFights();
		if (picks.isEmpty())
		{
			stack.add(Rows.note(report.isIronman() ? "Ironmen are shown the roadmap for each target instead."
				: "Nothing you could buy now would have saved you much time there.", 190));
			return true;
		}
		for (Fights.Pick p : picks)
		{
			stack.add(pick(p, icons, span));
		}
		return true;
	}

	/** One upgrade weighed across several targets, with the time it saves over the span named. */
	private static Card pick(Fights.Pick p, ItemIcons icons, String span)
	{
		JLabel cost = Ui.label(Format.coins(p.getCost()), Theme.small(), Theme.GOLD);
		Card row = Rows.line(new IconBadge(p.getIcon().getId(), icons, true), p.getName(),
			"Saves " + Format.span(p.getSeconds()) + " of fighting", cost, null);
		StringBuilder tip = new StringBuilder("<html><b>").append(Ui.escape(p.getName())).append("</b><br>About ")
			.append(Format.span(p.getSeconds())).append(" less fighting over ").append(span).append(", mostly at ");
		List<String> where = p.getWhere();
		for (int i = 0; i < where.size() && i < 3; i++)
		{
			tip.append(i == 0 ? "" : i == where.size() - 1 || i == 2 ? " and " : ", ").append(Ui.escape(where.get(i)));
		}
		row.setToolTipText(tip.append(".</html>").toString());
		return row;
	}

	private static boolean matches(Target t, int filter)
	{
		switch (filter)
		{
			case 1:
				return "Boss".equals(t.getGroup());
			case 2:
				return "Slayer".equals(t.getGroup());
			case 3:
				return "Training".equals(t.getGroup());
			default:
				return true;
		}
	}

	/** Builds target rows: a click aims the roadmap, a right-click puts the target on the shortlist or takes it off. */
	static final class TargetRows
	{
		private final Report report;
		private final String focused;
		private final Consumer<String> onPick;
		private final Consumer<String> onShortlist;

		TargetRows(Report report, String focused, Consumer<String> onPick, Consumer<String> onShortlist)
		{
			this.report = report;
			this.focused = focused;
			this.onPick = onPick;
			this.onShortlist = onShortlist;
		}

		Card row(Roadmap.Survey row)
		{
			Target t = row.getTarget();
			boolean aimed = t.getName().equals(focused);
			boolean listed = report.getShortlist().contains(t.getName());
			Result r = row.getPlan().getResult();
			Card card = new Card(new BorderLayout(8, 0));
			card.setBorder(new EmptyBorder(5, 10, 5, 10));
			card.accent(Theme.kind(r.getKind()));
			card.highlighted(aimed);

			JPanel text = new JPanel();
			text.setOpaque(false);
			text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
			text.add(Ui.label(t.getName(), Theme.body(), aimed ? Theme.GOLD_BRIGHT : Theme.TEXT));
			String line = r.getKind() == null ? "Out of reach" : Format.dps(r.getDps()) + " DPS  ·  " + r.getKind();
			text.add(Ui.label(line, Theme.small(), r.getKind() == null ? Theme.TEXT_FAINT : Theme.mix(Theme.TEXT_MUTED, Theme.kind(r.getKind()), 0.5)));
			text.setMinimumSize(new Dimension(10, 10));
			text.setPreferredSize(new Dimension(10, text.getPreferredSize().height));
			card.add(text, BorderLayout.CENTER);

			JPanel trail = new JPanel();
			trail.setOpaque(false);
			trail.setLayout(new BoxLayout(trail, BoxLayout.Y_AXIS));
			JLabel time = Ui.label(Format.time(r.secondsToKill(t)), Theme.body(), Theme.TEXT);
			time.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
			trail.add(time);
			String captionText = aimed ? "aimed" : listed ? "shortlisted" : "per kill";
			JLabel caption = Ui.label(captionText, Theme.small(), aimed || listed ? Theme.GOLD : Theme.TEXT_FAINT);
			caption.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
			trail.add(caption);
			card.add(trail, BorderLayout.EAST);
			card.setToolTipText("<html><b>" + Ui.escape(t.getName()) + "</b><br>" + t.getHp() + " hitpoints, " + t.getDefence()
				+ " Defence<br>Click to aim your roadmap here<br>Right-click to " + (listed ? "take it off" : "add it to")
				+ " your shortlist</html>");
			card.onClick(() -> onPick.accept(t.getName()));
			card.onMenu(at ->
			{
				JPopupMenu menu = new JPopupMenu();
				JMenuItem item = new JMenuItem(listed ? "Remove from shortlist" : "Add to shortlist");
				item.addActionListener(e -> onShortlist.accept(t.getName()));
				menu.add(item);
				menu.show(card, at.x, at.y);
			});
			return card;
		}
	}
}
