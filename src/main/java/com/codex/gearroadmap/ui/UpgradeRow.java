package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
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

	static Card build(Roadmap.Option o, Mode mode, int rank, boolean affordable, Result.Kind before, ItemIcons icons, Runnable onClick)
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

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		JLabel name = Ui.label(o.getName(), Theme.body(), mode == Mode.LOCKED ? Theme.TEXT_MUTED : Theme.TEXT);
		text.add(name);
		text.add(Ui.label(Format.gain(o.getGain()) + " DPS   " + Format.share(o.getGainShare()), Theme.small(),
			mode == Mode.LOCKED ? Theme.mix(tint, Theme.TEXT_FAINT, 0.4) : tint));
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
				detail = Format.value(o.getCost(), o.getGain());
				if (kind != null && before != null && kind != before)
				{
					detail = "Switch to " + kind;
					detailColor = Theme.mix(Theme.TEXT_MUTED, tint, 0.6);
				}
				else if (o.isAmmo())
				{
					detail = "Price per 1,000";
				}
		}
		if (detail != null && !detail.isEmpty())
		{
			JLabel sub = Ui.label(detail, Theme.small(), detailColor);
			sub.setToolTipText(detail);
			text.add(sub);
		}
		text.setMinimumSize(new Dimension(10, 10));
		text.setPreferredSize(new Dimension(10, text.getPreferredSize().height));
		card.add(text, BorderLayout.CENTER);

		if (mode == Mode.STEP || mode == Mode.LEAP || mode == Mode.LOCKED && o.getCost() > 0)
		{
			JPanel trail = new JPanel();
			trail.setOpaque(false);
			trail.setLayout(new BoxLayout(trail, BoxLayout.Y_AXIS));
			JLabel price = Ui.label(Format.coins(o.getCost()), Theme.body(), affordable ? Theme.GOLD_BRIGHT : Theme.GOLD);
			price.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
			trail.add(price);
			if (mode == Mode.STEP && affordable)
			{
				Pill pill = new Pill("In reach", Theme.TEAL);
				pill.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
				trail.add(pill);
			}
			card.add(trail, BorderLayout.EAST);
		}
		StringBuilder tip = new StringBuilder("<html><b>").append(Ui.escape(o.getName())).append("</b>");
		if (o.getNote() != null)
		{
			tip.append("<br>").append(Ui.escape(o.getNote()));
		}
		Result after = o.getAfter().getResult();
		tip.append("<br>").append(Format.dps(after.getDps())).append(" DPS after, max hit ").append(after.getMaxHit());
		if (after.getStyle() != null)
		{
			tip.append(", ").append(Ui.escape(after.getStyle().toLowerCase()));
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
