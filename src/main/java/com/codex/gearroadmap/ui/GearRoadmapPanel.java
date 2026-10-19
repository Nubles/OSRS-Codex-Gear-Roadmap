package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Roadmap;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.function.Consumer;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The Gear Roadmap side panel: the crest, then three pages. Roadmap is the road of
 * upgrades for the aimed target, Loadout is the gear that road starts from, and Targets
 * compares the player against every target and lets them aim at another.
 */
public class GearRoadmapPanel extends PluginPanel
{
	private static final String[] TABS = {"roadmap", "loadout", "targets"};

	private final ItemIcons icons;
	private final Consumer<String> onAim;
	private final Consumer<Roadmap.Option> onGoal;
	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(true, "Roadmap", "Loadout", "Targets");
	private final Segmented kinds = new Segmented(false, LoadoutView.KINDS);
	private final Segmented filters = new Segmented(false, TargetsView.FILTERS);
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JScrollPane roadmap = scroller();
	private final JScrollPane loadout = scroller();
	private final JScrollPane targets = scroller();
	private final JPanel kindHolder = new JPanel(new BorderLayout());

	private Report report = Report.loggedOut();
	private String playerName;
	/** Whether the road card carries the character's name, from the settings. */
	private boolean showName = true;
	private boolean showWorth;
	private String search = "";
	private Roadmap.Option preview;
	/** The listed monster being fought, and the one the player hid the offer for. */
	private String fighting;
	private String hiddenFight;
	private ShareActions share;
	/** A line under the road card's buttons, such as where it was saved. */
	private String shareNote;
	/** Set when a report arrived while the panel was closed; it is drawn once the panel opens. */
	private boolean stale;

	/**
	 * @param onAim called with a target's name when the player picks it
	 * @param onGoal called with an upgrade to save for, or null to clear the goal
	 */
	public GearRoadmapPanel(ItemIcons icons, Consumer<String> onAim, Consumer<Roadmap.Option> onGoal)
	{
		super(false);
		this.icons = icons;
		this.onAim = onAim;
		this.onGoal = onGoal;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(crest, BorderLayout.NORTH);
		JPanel tabHolder = new JPanel(new BorderLayout());
		tabHolder.setOpaque(false);
		tabHolder.setBorder(new EmptyBorder(8, 8, 2, 8));
		tabHolder.add(tabs, BorderLayout.CENTER);
		north.add(tabHolder, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		JPanel loadoutHolder = new JPanel(new BorderLayout());
		loadoutHolder.setOpaque(false);
		kindHolder.setOpaque(false);
		kindHolder.setBorder(new EmptyBorder(6, 8, 0, 8));
		kindHolder.add(kinds, BorderLayout.CENTER);
		loadoutHolder.add(kindHolder, BorderLayout.NORTH);
		loadoutHolder.add(loadout, BorderLayout.CENTER);

		JPanel targetHolder = new JPanel(new BorderLayout());
		targetHolder.setOpaque(false);
		JPanel tools = new JPanel(new BorderLayout(0, 5));
		tools.setOpaque(false);
		tools.setBorder(new EmptyBorder(6, 8, 0, 8));
		tools.add(new SearchField("Find a target", text ->
		{
			search = text;
			rebuildTargets();
		}), BorderLayout.NORTH);
		tools.add(filters, BorderLayout.SOUTH);
		targetHolder.add(tools, BorderLayout.NORTH);
		targetHolder.add(targets, BorderLayout.CENTER);

		content.setOpaque(false);
		content.add(roadmap, TABS[0]);
		content.add(loadoutHolder, TABS[1]);
		content.add(targetHolder, TABS[2]);
		add(content, BorderLayout.CENTER);

		tabs.onChange(tab ->
		{
			if (tab == 1 && preview != null)
			{
				preview = null;
				rebuildLoadout();
			}
			cards.show(content, TABS[tab]);
		});
		kinds.onChange(i -> rebuildLoadout());
		filters.onChange(i -> rebuildTargets());
		rebuild();
	}

	private static JScrollPane scroller()
	{
		JScrollPane scroll = new JScrollPane();
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		return scroll;
	}

	/** Called on the Swing thread with each new report. */
	public void update(Report report, String playerName)
	{
		if (!java.util.Objects.equals(aimed(this.report), aimed(report)))
		{
			// A note on a card saved for another target no longer applies.
			shareNote = null;
		}
		this.report = report;
		this.playerName = playerName;
		preview = null;
		if (!isShowing())
		{
			// Nothing is built while the panel is closed; it catches up when it opens.
			stale = true;
			return;
		}
		rebuild();
	}

	@Override
	public void onActivate()
	{
		if (stale)
		{
			rebuild();
		}
	}

	private static String aimed(Report report)
	{
		return report == null || report.getRoadmap() == null ? null : report.getRoadmap().getTarget().getName();
	}

	/** Whether the road card shows the player's coins and how far they go; used when the card is next drawn. */
	public void setShowWorth(boolean showWorth)
	{
		this.showWorth = showWorth;
	}

	/** Whether the road card carries the character's name; used when the card is next drawn. */
	public void setShowName(boolean showName)
	{
		this.showName = showName;
	}

	/** What the road card's buttons do. */
	public void setShare(ShareActions share)
	{
		this.share = share;
	}

	/** A line under the road card's buttons; the Roadmap tab is built again to show it. */
	public void shareNote(String note)
	{
		shareNote = note;
		rebuildRoadmap();
	}

	/** Called on the Swing thread with the listed monster the player is fighting, or null. */
	public void setFighting(String fighting)
	{
		if (java.util.Objects.equals(this.fighting, fighting))
		{
			return;
		}
		this.fighting = fighting;
		if (!isShowing())
		{
			stale = true;
			return;
		}
		rebuildRoadmap();
	}

	/** Opens one tab, for previews and tests. */
	public void showTab(int tab)
	{
		tabs.setSelected(tab, false);
		cards.show(content, TABS[tab]);
	}

	/** Shows the loadout an upgrade would build. */
	public void preview(Roadmap.Option option)
	{
		preview = option;
		rebuildLoadout();
		tabs.setSelected(1, false);
		cards.show(content, TABS[1]);
		SwingUtilities.invokeLater(() -> loadout.getVerticalScrollBar().setValue(0));
	}

	public void selectKind(int kind)
	{
		kinds.setSelected(kind, false);
		rebuildLoadout();
	}

	public void selectFilter(int filter)
	{
		filters.setSelected(filter, false);
		rebuildTargets();
	}

	private void rebuild()
	{
		stale = false;
		crest.update(report, playerName);
		rebuildRoadmap();
		rebuildLoadout();
		rebuildTargets();
	}

	private void rebuildRoadmap()
	{
		String fight = fighting == null || fighting.equals(hiddenFight) ? null : fighting;
		StackPanel view = RoadmapView.build(report, icons, () -> showTab(2), this::preview, () -> onGoal.accept(null),
			RoadmapView.fightCard(report, fight, () -> onAim.accept(fight), () ->
			{
				hiddenFight = fight;
				rebuildRoadmap();
			}));
		if (share != null && RoadCard.canDraw(report))
		{
			RoadmapView.shareCard(view, shareNote, this::saveCard, this::copyCard);
		}
		replace(roadmap, view);
	}

	/**
	 * The road card is drawn only when asked for, never as the road changes, and handed over once
	 * the pictures of its loadout and stops are in, so no stop is saved as an empty ring.
	 */
	private void drawCard(java.util.function.Consumer<BufferedImage> deliver)
	{
		Report drawn = report;
		String name = showName ? playerName : null;
		boolean worth = showWorth;
		long now = System.currentTimeMillis();
		CardPictures.draw(icons, pictures -> RoadCard.render(drawn, name, pictures, now, null, worth), deliver);
	}

	private void saveCard()
	{
		if (RoadCard.canDraw(report))
		{
			String file = "Road to " + report.getRoadmap().getTarget().getName().replaceAll("[\\\\/:*?\"<>|]", "") + ".png";
			drawCard(card -> share.save(card, file));
		}
	}

	private void copyCard()
	{
		if (RoadCard.canDraw(report))
		{
			drawCard(share::copy);
		}
	}

	private void rebuildLoadout()
	{
		kindHolder.setVisible(preview == null);
		replace(loadout, LoadoutView.build(report, kinds.getSelected(), preview, icons, () ->
		{
			preview = null;
			rebuildLoadout();
			showTab(0);
		}, option ->
		{
			onGoal.accept(option);
			preview = null;
			rebuildLoadout();
			showTab(0);
		}));
	}

	private void rebuildTargets()
	{
		String focused = report.getRoadmap() == null ? null : report.getRoadmap().getTarget().getName();
		replace(targets, TargetsView.build(report, filters.getSelected(), search, focused, name ->
		{
			showTab(0);
			onAim.accept(name);
		}, icons));
	}

	private static void replace(JScrollPane scroll, JComponent view)
	{
		int position = scroll.getVerticalScrollBar().getValue();
		scroll.setViewportView(view);
		scroll.revalidate();
		SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(position));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(Theme.INK);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
