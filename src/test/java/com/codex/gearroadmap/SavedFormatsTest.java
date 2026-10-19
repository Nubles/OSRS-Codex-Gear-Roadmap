package com.codex.gearroadmap;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Other Codex plugins read what Gear Roadmap saves, and Gear Roadmap reads Codex Ledger: these
 * pin the group, keys and JSON shape so a change here cannot quietly break them.
 */
public class SavedFormatsTest
{
	/** The shape Codex Plunder Pilot and Pest Control Pilot read the bank in: only the item ids. */
	private static final class ReaderBank
	{
		List<Integer> items;
	}

	@Test
	public void bankIsSavedWhereAndHowOtherPluginsReadIt()
	{
		assertEquals("codexgearroadmap", CodexGearRoadmapConfig.GROUP);
		assertEquals("bank", CodexGearRoadmapPlugin.BANK_KEY);

		CodexGearRoadmapPlugin.Bank bank = new CodexGearRoadmapPlugin.Bank();
		bank.items.addAll(Arrays.asList(9050, 4151, 13072));
		bank.coins = 1_234_567;
		Gson gson = new Gson();
		String json = gson.toJson(bank);
		assertEquals("{\"items\":[4151,9050,13072],\"coins\":1234567}", json);
		assertEquals(Arrays.asList(4151, 9050, 13072), gson.fromJson(json, ReaderBank.class).items);
	}

	@Test
	public void ledgerIsReadFromItsOwnGroupAndKey()
	{
		assertEquals("codexledger", CodexGearRoadmapPlugin.LEDGER_GROUP);
		assertEquals("ledger", CodexGearRoadmapPlugin.LEDGER_KEY);
	}
}
