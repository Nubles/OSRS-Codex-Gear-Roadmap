package com.codex.gearroadmap.ui;

import java.util.Locale;
import static org.junit.Assert.assertEquals;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Damage figures read the same on a German computer as on an English one, as coins already did. */
public class LocaleFormatTest
{
	private Locale was;

	@Before
	public void german()
	{
		was = Locale.getDefault();
		Locale.setDefault(Locale.GERMANY);
	}

	@After
	public void restore()
	{
		Locale.setDefault(was);
	}

	@Test
	public void dpsUsesAFullStop()
	{
		assertEquals("4.25", Format.dps(4.25));
		assertEquals(Format.dps(1.5).indexOf(','), -1);
		assertEquals(Format.gain(0.37).indexOf(','), -1);
	}
}
