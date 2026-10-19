package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.data.Target;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** The Targets tab: every target with the best the player can do there now; clicking one aims the roadmap at it. */
final class TargetsView
{
	static final String[] FILTERS = {"All", "Bosses", "Slayer", "Training"};

	private TargetsView()
	{
	}

	static StackPanel build(Report report, int filter, String search, String focused, Consumer<String> onPick)
	{
		StackPanel stack = new StackPanel(4, 8);
		if (!report.isLoggedIn())
		{
			stack.add(Rows.note("Log in to compare your gear against every target.", 190));
			return stack;
		}
		if (report.getSurvey().isEmpty())
		{
			stack.add(Rows.note("Weighing your gear against every target...", 190));
			return stack;
		}
		String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
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
						stack.add(row(row, name.equals(focused), onPick));
					}
				}
			}
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
			stack.add(row(row, t.getName().equals(focused), onPick));
			shown++;
		}
		if (shown == 0)
		{
			stack.add(Rows.note("No targets match.", 190));
		}
		return stack;
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

	private static Card row(Roadmap.Survey row, boolean focused, Consumer<String> onPick)
	{
		Target t = row.getTarget();
		Result r = row.getPlan().getResult();
		Card card = new Card(new BorderLayout(8, 0));
		card.setBorder(new EmptyBorder(5, 10, 5, 10));
		card.accent(Theme.kind(r.getKind()));
		card.highlighted(focused);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.add(Ui.label(t.getName(), Theme.body(), focused ? Theme.GOLD_BRIGHT : Theme.TEXT));
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
		JLabel caption = Ui.label(focused ? "aimed" : "per kill", Theme.small(), focused ? Theme.GOLD : Theme.TEXT_FAINT);
		caption.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
		trail.add(caption);
		card.add(trail, BorderLayout.EAST);
		card.setToolTipText("<html><b>" + Ui.escape(t.getName()) + "</b><br>" + t.getHp() + " hitpoints, " + t.getDefence()
			+ " Defence<br>Click to aim your roadmap here</html>");
		card.onClick(() -> onPick.accept(t.getName()));
		return card;
	}
}
