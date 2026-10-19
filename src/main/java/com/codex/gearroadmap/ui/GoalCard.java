package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.core.Savings;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The savings goal on the Roadmap tab: the upgrade, a bar of how far the player's coins go,
 * and when the rest should be saved at the pace Codex Ledger has seen their wealth grow.
 */
final class GoalCard
{
	private GoalCard()
	{
	}

	static Card build(Savings.View view, String target, ItemIcons icons, Runnable onClear)
	{
		Roadmap.Option o = view.getOption();
		boolean done = view.isAffordable();
		Card card = new Card(new BorderLayout(0, 5));
		card.setBorder(new EmptyBorder(7, 9, 8, 9));
		card.accent(done ? Theme.TEAL : Theme.GOLD);

		JPanel top = new JPanel(new BorderLayout(7, 0));
		top.setOpaque(false);
		JPanel badge = new JPanel(new BorderLayout());
		badge.setOpaque(false);
		badge.add(new IconBadge(o.getIcon().getId(), icons, true), BorderLayout.NORTH);
		top.add(badge, BorderLayout.WEST);
		JPanel names = new JPanel();
		names.setOpaque(false);
		names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
		names.add(Ui.label(view.isOwned() ? "SAVED FOR" : "SAVING FOR", Theme.small(), done ? Theme.TEAL : Theme.GOLD));
		JLabel name = Ui.label(o.getName(), Theme.body(), Theme.TEXT);
		name.setToolTipText(o.getName());
		names.add(name);
		names.setMinimumSize(new Dimension(10, 10));
		names.setPreferredSize(new Dimension(10, names.getPreferredSize().height));
		top.add(names, BorderLayout.CENTER);
		JPanel clear = new JPanel(new BorderLayout());
		clear.setOpaque(false);
		clear.add(new CodexButton("Clear", false, onClear), BorderLayout.NORTH);
		top.add(clear, BorderLayout.EAST);
		card.add(top, BorderLayout.NORTH);

		JPanel body = new JPanel();
		body.setOpaque(false);
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		ProgressBar bar = new ProgressBar(6);
		bar.setProgress(view.getProgress(), done, false);
		bar.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		body.add(bar);

		JPanel numbers = new JPanel(new BorderLayout(4, 0));
		numbers.setOpaque(false);
		numbers.setBorder(new EmptyBorder(3, 0, 0, 0));
		String held = view.isOwned() ? "Owned" : Format.coins(Math.min(view.getCoins(), view.getCost())) + " of " + Format.coins(view.getCost());
		numbers.add(Ui.label(held, Theme.small(), done ? Theme.TEAL : Theme.TEXT), BorderLayout.WEST);
		numbers.add(Ui.label(Format.percent(view.getProgress()), Theme.small(), Theme.progressColor(view.getProgress())), BorderLayout.EAST);
		numbers.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		body.add(numbers);

		JLabel pace = Ui.wrapped(paceLine(view), Theme.small(), done ? Theme.TEAL : Theme.TEXT_MUTED, 180);
		pace.setBorder(new EmptyBorder(3, 0, 0, 0));
		pace.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		body.add(pace);

		String[] gain = gainLine(o, target);
		JLabel gainLabel = Ui.label(gain[0], Theme.small(), gain[1] == null ? Theme.kind(o.getAfter().getResult().getKind()) : Theme.EMBER);
		gainLabel.setBorder(new EmptyBorder(3, 0, 0, 0));
		gainLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		body.add(gainLabel);
		card.add(body, BorderLayout.CENTER);

		card.setToolTipText("<html><b>" + Ui.escape(o.getName()) + "</b><br>" + Format.coins(view.getCost())
			+ (o.isAmmo() ? " per 1,000" : "") + " at today's Grand Exchange price</html>");
		return card;
	}

	/** When the rest should be saved, in one plain sentence. */
	static String paceLine(Savings.View view)
	{
		if (view.isOwned())
		{
			return "You own it now. Clear this to pick your next goal.";
		}
		if (view.isAffordable())
		{
			return "Your coins cover it. Buy it whenever you like.";
		}
		String rest = Format.coins(view.getRemaining()) + " to go.";
		if (!view.hasLedger())
		{
			return rest + " With Codex Ledger, this also says when you will get there.";
		}
		String worth = view.getWorth() >= view.getCost()
			? " Your net worth of " + Format.coins(view.getWorth()) + " would cover it if you sold other things." : "";
		Long perDay = view.getPerDay();
		if (perDay == null)
		{
			return rest + " Codex Ledger needs a day of history to tell when." + worth;
		}
		if (perDay <= 0)
		{
			return rest + " Your net worth has not grown this week, so there is no pace to go by." + worth;
		}
		int days = view.getDays();
		String when = days < 0 ? "Not within years at this week's pace"
			: days <= 1 ? "About a day at this week's pace" : "About " + days + " days at this week's pace";
		return rest + " " + when + " (net worth +" + Format.coins(perDay) + " a day)." + worth;
	}

	/** What the goal does against the aimed target; the second value is set when it is out of reach by level. */
	static String[] gainLine(Roadmap.Option o, String target)
	{
		if (o.getMissing() != null)
		{
			return new String[]{"Needs " + o.getMissing() + " to wield", o.getMissing()};
		}
		if (o.getGain() <= 0)
		{
			return new String[]{"Adds nothing against " + target, null};
		}
		return new String[]{Format.gain(o.getGain()) + " DPS against " + target + "  " + Format.share(o.getGainShare()), null};
	}
}
