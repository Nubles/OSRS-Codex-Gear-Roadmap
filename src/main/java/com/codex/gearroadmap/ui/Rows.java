package com.codex.gearroadmap.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Compact card lines: a verdict mark, a name, a quiet second line and an optional value. */
public final class Rows
{
	private Rows()
	{
	}

	public static Card check(Mark.Kind kind, String title, String detail, String value)
	{
		return line(new Mark(kind, 14), title, detail, value == null ? null : Ui.label(value, Theme.small(), Mark.color(kind)),
			kind == Mark.Kind.BAD ? Theme.BLOOD : null);
	}

	/** A verdict with a sentence that may run to two lines. */
	public static Card reason(Mark.Kind kind, String text)
	{
		Card card = new Card(new BorderLayout(8, 0));
		card.setBorder(new EmptyBorder(6, 8, 6, 8));
		if (kind == Mark.Kind.BAD)
		{
			card.accent(Theme.BLOOD);
		}
		JPanel holder = new JPanel(new BorderLayout());
		holder.setOpaque(false);
		holder.setBorder(new EmptyBorder(1, 0, 0, 0));
		holder.add(new Mark(kind, 14), BorderLayout.NORTH);
		card.add(holder, BorderLayout.WEST);
		card.add(Ui.wrapped(text, Theme.body(), Theme.TEXT, 160), BorderLayout.CENTER);
		return card;
	}

	public static Card line(JComponent lead, String title, String detail, JComponent trailing, Color accent)
	{
		return line(lead, title, detail, trailing, accent, 0);
	}

	/** @param wrap above 0, the width the detail wraps at instead of being cut short */
	public static Card line(JComponent lead, String title, String detail, JComponent trailing, Color accent, int wrap)
	{
		Card card = new Card(new BorderLayout(8, 0));
		card.setBorder(new EmptyBorder(6, 8, 6, 8));
		if (accent != null)
		{
			card.accent(accent);
		}
		if (lead != null)
		{
			JPanel holder = new JPanel(new BorderLayout());
			holder.setOpaque(false);
			holder.setBorder(new EmptyBorder(1, 0, 0, 0));
			holder.add(lead, BorderLayout.NORTH);
			card.add(holder, BorderLayout.WEST);
		}
		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		JLabel name = Ui.label(title, Theme.body(), Theme.TEXT);
		name.setToolTipText(title);
		text.add(name);
		if (detail != null && !detail.isEmpty())
		{
			JLabel sub = wrap > 0 ? Ui.wrapped(detail, Theme.small(), Theme.TEXT_MUTED, wrap) : Ui.label(detail, Theme.small(), Theme.TEXT_MUTED);
			sub.setToolTipText(detail);
			text.add(sub);
		}
		// Long names clip with an ellipsis rather than push the value off the card.
		text.setMinimumSize(new Dimension(10, 10));
		text.setPreferredSize(new Dimension(10, text.getPreferredSize().height));
		card.add(text, BorderLayout.CENTER);
		if (trailing != null)
		{
			JPanel holder = new JPanel(new BorderLayout());
			holder.setOpaque(false);
			holder.add(trailing, BorderLayout.NORTH);
			card.add(holder, BorderLayout.EAST);
		}
		return card;
	}

	/** An empty state as a card: what is missing in gold, and what to do about it beneath. */
	public static Card empty(String title, String text)
	{
		Card card = new Card(new BorderLayout(0, 3));
		card.setBorder(new EmptyBorder(8, 10, 9, 10));
		card.add(Ui.label(title, Theme.body(), Theme.GOLD_BRIGHT), BorderLayout.NORTH);
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, 176), BorderLayout.CENTER);
		return card;
	}

	/** A quiet line of text for empty states. */
	public static JComponent note(String text, int width)
	{
		JPanel holder = new JPanel(new BorderLayout());
		holder.setOpaque(false);
		holder.setBorder(new EmptyBorder(2, 4, 2, 4));
		holder.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_FAINT, width), BorderLayout.CENTER);
		return holder;
	}
}
