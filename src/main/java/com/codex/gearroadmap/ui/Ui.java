package com.codex.gearroadmap.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import javax.swing.JLabel;

/** Small helpers shared by the panel's components. */
public final class Ui
{
	private Ui()
	{
	}

	public static JLabel label(String text, Font font, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(font);
		label.setForeground(color);
		return label;
	}

	/**
	 * A label that wraps inside the side panel's narrow width. Swing's HTML renderer
	 * treats CSS pixels as points, so the width is converted to keep it in screen pixels.
	 */
	public static JLabel wrapped(String text, Font font, Color color, int width)
	{
		int css = Math.round(width * 0.75f);
		return label("<html><div style='width:" + css + "px'>" + escape(text) + "</div></html>", font, color);
	}

	public static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** Lets a clickable card keep its hover and click while the pointer is over its children. */
	static void forwardMouse(Container root, MouseAdapter adapter)
	{
		for (Component child : root.getComponents())
		{
			child.addMouseListener(adapter);
			if (child instanceof Container)
			{
				forwardMouse((Container) child, adapter);
			}
		}
	}
}
