package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.data.Target;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** One upgrade as a card: its picture, the damage it adds, what it costs and how to get it. */
final class UpgradeRow
{
	enum Mode
	{
		STEP,
		FREE,
		LOCKED,
		LEAP
	}

	private UpgradeRow()
	{
	}

	static Card build(Roadmap.Option o, Mode mode, int rank, boolean affordable, Result.Kind before, ItemIcons icons, Target target,
		Runnable onClick)
	{
		Card card = new Card(new BorderLayout(7, 0));
		card.setBorder(new EmptyBorder(6, 8, 6, 8));
		Result.Kind kind = o.getAfter().getResult().getKind();
		Color tint = Theme.kind(kind);
		if (mode == Mode.STEP && rank == 1 || mode == Mode.FREE)
		{
			card.accent(mode == Mode.FREE ? Theme.TEAL : Theme.GOLD);
		}

		JPanel lead = new JPanel(new BorderLayout(4, 0));
		lead.setOpaque(false);
		if (mode == Mode.STEP)
		{
			JPanel medal = new JPanel(new BorderLayout());
			medal.setOpaque(false);
			medal.setBorder(new EmptyBorder(4, 0, 0, 0));
			medal.add(new Medal(rank, affordable), BorderLayout.NORTH);
			lead.add(medal, BorderLayout.WEST);
		}
		JPanel badge = new JPanel(new BorderLayout());
		badge.setOpaque(false);
		badge.add(new IconBadge(o.getIcon().getId(), icons, mode != Mode.LOCKED), BorderLayout.NORTH);
		lead.add(badge, BorderLayout.CENTER);
		card.add(lead, BorderLayout.WEST);

		Color priceColor = affordable ? Theme.TEAL : Theme.GOLD;
		boolean priced = mode == Mode.STEP || mode == Mode.LEAP || mode == Mode.LOCKED && o.getCost() > 0;
		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

		// The name gets the whole first line, with the price at its end, so it is rarely cut short.
		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		// A pair bought together ("A + B") names the first on top and the second beneath it.
		int plus = o.getName().indexOf(" + ");
		String title = plus < 0 ? o.getName() : o.getName().substring(0, plus);
		JLabel name = Ui.label(title, Theme.body(), mode == Mode.LOCKED ? Theme.TEXT_MUTED : Theme.TEXT);
		name.setMinimumSize(new Dimension(10, 10));
		name.setPreferredSize(new Dimension(10, name.getPreferredSize().height));
		top.add(name, BorderLayout.CENTER);
		if (priced)
		{
			top.add(Ui.label(Format.coins(o.getCost()), Theme.body(), priceColor), BorderLayout.EAST);
		}
		top.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		text.add(top);
		if (plus >= 0)
		{
			JLabel second = Ui.label("+ " + o.getName().substring(plus + 3), Theme.body(), name.getForeground());
			second.setToolTipText("Neither does anything alone: together they reach the next max hit");
			second.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			text.add(second);
		}

		Color gainColor = mode == Mode.LOCKED ? Theme.mix(tint, Theme.TEXT_FAINT, 0.4) : tint;
		JPanel middle = new JPanel(new BorderLayout(4, 0));
		middle.setOpaque(false);
		middle.add(Ui.label(Format.gain(o.getGain()) + " DPS  " + Format.share(o.getGainShare()), Theme.small(), gainColor), BorderLayout.WEST);
		middle.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		text.add(middle);

		String detail;
		Color detailColor = Theme.TEXT_MUTED;
		switch (mode)
		{
			case FREE:
				detail = o.getEarn() != null ? o.getEarn() : o.getNote();
				detailColor = Theme.TEAL;
				break;
			case LOCKED:
				detail = "Needs " + o.getMissing();
				detailColor = Theme.EMBER;
				break;
			default:
				detail = o.isAmmo() ? "Price per 1,000" : Format.value(o.getCost(), o.getGain());
				if (kind != null && before != null && kind != before)
				{
					detail = "To " + kind.toString().toLowerCase(Locale.ROOT) + (detail.isEmpty() ? "" : " · " + detail);
					detailColor = Theme.mix(Theme.TEXT_MUTED, tint, 0.6);
				}
		}
		if (detail != null && !detail.isEmpty())
		{
			JLabel sub = Ui.label(detail, Theme.small(), detailColor);
			sub.setToolTipText(detail);
			sub.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			text.add(sub);
		}
		text.setMinimumSize(new Dimension(10, 10));
		text.setPreferredSize(new Dimension(10, text.getPreferredSize().height));
		card.add(text, BorderLayout.CENTER);

		StringBuilder tip = new StringBuilder("<html><b>").append(Ui.escape(o.getName())).append("</b>");
		if (priced)
		{
			tip.append("<br>").append(Format.coins(o.getCost())).append(o.isAmmo() ? " per 1,000" : "")
				.append(mode == Mode.STEP ? affordable ? ", your coins cover the road to here" : ", past what your coins cover" : "");
		}
		if (o.getNote() != null)
		{
			tip.append("<br>").append(Ui.escape(o.getNote()));
		}
		Result after = o.getAfter().getResult();
		tip.append("<br>").append(Format.dps(after.getDps())).append(" DPS after, max hit ").append(after.getMaxHit());
		if (after.getStyle() != null)
		{
			tip.append(", ").append(Ui.escape(after.getStyle().toLowerCase(Locale.ROOT)));
		}
		double was = o.getGain() > 0 && o.getAfter().dps() > o.getGain() ? target.getHp() / (o.getAfter().dps() - o.getGain()) : 0;
		double now = after.secondsToKill(target);
		if (was > 0 && !Double.isInfinite(now) && was - now >= 0.5)
		{
			tip.append("<br>Kill time ").append(Format.time(was)).append(" to ").append(Format.time(now))
				.append(", ").append(Math.round((1 - now / was) * 100)).append("% faster");
		}
		if (onClick != null)
		{
			tip.append("<br><i>Click to see the loadout</i>");
		}
		card.setToolTipText(tip.append("</html>").toString());
		if (onClick != null)
		{
			card.onClick(onClick);
		}
		return card;
	}

	/** A small gilt medallion with the step number; dim once the road passes the player's coins. */
	private static final class Medal extends JComponent
	{
		private final int rank;
		private final boolean lit;

		Medal(int rank, boolean lit)
		{
			this.rank = rank;
			this.lit = lit;
			Dimension d = new Dimension(18, 18);
			setPreferredSize(d);
			setMinimumSize(d);
			setMaximumSize(d);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setColor(lit ? Theme.alpha(Theme.GOLD, 60) : Theme.INK_DEEP);
			g.fill(new Ellipse2D.Float(1, 1, 16, 16));
			g.setColor(lit ? Theme.GOLD : Theme.STROKE);
			g.draw(new Ellipse2D.Float(1, 1, 16, 16));
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			String text = String.valueOf(rank);
			ProgressRing.drawShadowed(g, text, 9 - fm.stringWidth(text) / 2, 9 + fm.getAscent() / 2 - 1,
				lit ? Theme.GOLD_BRIGHT : Theme.TEXT_MUTED);
			g.dispose();
		}
	}
}
