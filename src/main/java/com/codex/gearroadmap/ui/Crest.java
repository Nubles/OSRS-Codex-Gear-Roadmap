package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Path2D;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The crest at the top of the panel: the dial, the player and their target, and three
 * ledgers for the kill time now, how much faster the road ends, and the coins to spend.
 */
public class Crest extends JPanel
{
	private final DpsDial dial = new DpsDial(74);
	private final JLabel title = Ui.label("Gear Roadmap", Theme.title(), Theme.GOLD_BRIGHT);
	private final JLabel target = Ui.label("Log in to plan your gear", Theme.small(), Theme.TEXT_MUTED);
	private final Ledger kill = new Ledger("Kill time");
	private final Ledger road = new Ledger("Road ahead");
	private final Ledger coins = new Ledger("Coins");
	private Color glow = Theme.GOLD_DIM;

	public Crest()
	{
		super(new BorderLayout(10, 0));
		setOpaque(false);
		setBorder(new EmptyBorder(12, 10, 12, 10));

		JPanel dialHolder = new JPanel(new BorderLayout());
		dialHolder.setOpaque(false);
		dialHolder.add(dial, BorderLayout.NORTH);
		add(dialHolder, BorderLayout.WEST);

		JPanel right = new JPanel();
		right.setOpaque(false);
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		title.setAlignmentX(LEFT_ALIGNMENT);
		target.setAlignmentX(LEFT_ALIGNMENT);
		right.add(title);
		right.add(target);
		JPanel ledgers = new JPanel(new GridLayout(3, 1, 0, 2));
		ledgers.setOpaque(false);
		ledgers.setBorder(new EmptyBorder(4, 0, 0, 0));
		ledgers.add(kill);
		ledgers.add(road);
		ledgers.add(coins);
		ledgers.setAlignmentX(LEFT_ALIGNMENT);
		right.add(ledgers);
		add(right, BorderLayout.CENTER);
		dial.set("-", 0, 0, Theme.GOLD, false);
	}

	public void update(Report report, String playerName)
	{
		Roadmap map = report.getRoadmap();
		if (!report.isLoggedIn() || map == null)
		{
			title.setText(playerName == null ? "Gear Roadmap" : playerName);
			target.setText(report.isLoggedIn() ? "Reading your gear..." : "Log in to plan your gear");
			target.setForeground(Theme.TEXT_MUTED);
			dial.set("-", 0, 0, Theme.GOLD, true);
			kill.set("-", Theme.TEXT);
			road.set("-", Theme.TEXT);
			coins.set("-", Theme.TEXT);
			glow = Theme.GOLD_DIM;
			repaint();
			return;
		}
		Result now = map.getNow().best().getResult();
		Color color = Theme.kind(now.getKind());
		title.setText(playerName == null ? "Gear Roadmap" : playerName);
		target.setText("vs " + map.getTarget().getName() + (now.getKind() == null ? "" : "  ·  " + now.getKind()));
		target.setForeground(Theme.mix(Theme.TEXT_MUTED, color, 0.55));
		double end = map.finalDps();
		double affordable = now.getDps();
		for (Roadmap.Step step : map.getPath())
		{
			if (step.isAffordable())
			{
				affordable = step.getOption().getAfter().dps();
			}
		}
		dial.set(Format.dps(now.getDps()), end <= 0 ? 0 : now.getDps() / end, end <= 0 ? 0 : affordable / end, color, true);
		kill.set(Format.time(now.secondsToKill(map.getTarget())), Theme.TEXT);
		road.set(map.getPath().isEmpty() ? "Complete" : Format.share(end / Math.max(0.01, now.getDps()) - 1), Theme.TEAL);
		if (report.isIronman())
		{
			coins.label("Road cost");
			coins.set(map.getPath().isEmpty() ? "-" : Format.coins(map.getPath().get(map.getPath().size() - 1).getSpent()), Theme.GOLD);
		}
		else
		{
			coins.label("Coins");
			coins.set(Format.coins(map.getCoins()).replace("Free", "0"), Theme.GOLD);
		}
		glow = Theme.mix(Theme.GOLD_DIM, color, 0.4);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK, glow, 0.2), 0, h, Theme.INK));
		g.fillRect(0, 0, w, h);

		// Gilt rule along the bottom with a small diamond at its centre.
		int y = h - 2;
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.GOLD, 0), w / 2f, 0, Theme.alpha(Theme.GOLD, 150), true));
		g.drawLine(0, y, w, y);
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(w / 2f, y - 3);
		diamond.lineTo(w / 2f + 3, y);
		diamond.lineTo(w / 2f, y + 3);
		diamond.lineTo(w / 2f - 3, y);
		diamond.closePath();
		g.setColor(Theme.GOLD);
		g.fill(diamond);
		g.dispose();
		super.paintComponent(graphics);
	}

	/** One labelled value. */
	private static final class Ledger extends JPanel
	{
		private final JLabel value = Ui.label("-", Theme.small(), Theme.TEXT);
		private final JLabel name;

		Ledger(String label)
		{
			super(new BorderLayout());
			setOpaque(false);
			name = Ui.label(label, Theme.small(), Theme.TEXT_MUTED);
			add(name, BorderLayout.WEST);
			add(value, BorderLayout.EAST);
			setPreferredSize(new Dimension(10, 15));
		}

		void label(String text)
		{
			name.setText(text);
		}

		void set(String text, Color color)
		{
			value.setText(text);
			value.setForeground(color);
		}
	}
}
