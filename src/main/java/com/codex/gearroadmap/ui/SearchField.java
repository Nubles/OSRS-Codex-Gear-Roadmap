package com.codex.gearroadmap.ui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** An inked search line with a small lens, matching the rest of the panel. */
class SearchField extends JTextField
{
	private final String hint;

	SearchField(String hint, Consumer<String> onChange)
	{
		this.hint = hint;
		setOpaque(false);
		setFont(Theme.body());
		setForeground(Theme.TEXT);
		setCaretColor(Theme.GOLD_BRIGHT);
		setSelectionColor(Theme.alpha(Theme.GOLD, 90));
		setBorder(new EmptyBorder(4, 24, 4, 8));
		setPreferredSize(new Dimension(10, 26));
		getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				onChange.accept(getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				onChange.accept(getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				onChange.accept(getText());
			}
		});
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(shape);
		g.setColor(isFocusOwner() ? Theme.alpha(Theme.GOLD, 150) : Theme.STROKE_SOFT);
		g.draw(shape);
		g.setStroke(new BasicStroke(1.4f));
		g.setColor(Theme.GOLD_DIM);
		g.draw(new Ellipse2D.Float(8, h / 2f - 5, 8, 8));
		g.drawLine(15, h / 2 + 2, 18, h / 2 + 5);
		if (getText().isEmpty() && !isFocusOwner())
		{
			g.setFont(getFont());
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(hint, 24, (h + g.getFontMetrics().getAscent()) / 2 - 2);
		}
		g.dispose();
		super.paintComponent(graphics);
	}
}
