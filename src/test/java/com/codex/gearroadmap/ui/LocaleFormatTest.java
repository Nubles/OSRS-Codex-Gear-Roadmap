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

	@Test
	public void pricesSideBySideCarryTheSamePlaces()
	{
		// The road card once priced its stops "1.4M", "12M", "9.9M", "25M".
		assertEquals("12.0M", Format.coins(12_000_000));
		assertEquals("9.9M", Format.coins(9_900_000));
		assertEquals("156.0M", Format.coins(156_000_000));
		assertEquals("60.0k", Format.coins(60_000));
		assertEquals("160.4k", Format.coins(160_400));
		assertEquals("850", Format.coins(850));
		assertEquals("1.0M", Format.coins(999_960));
		assertEquals("1.6B", Format.coins(1_600_000_000L));
		assertEquals("Free", Format.coins(0));
	}
}
