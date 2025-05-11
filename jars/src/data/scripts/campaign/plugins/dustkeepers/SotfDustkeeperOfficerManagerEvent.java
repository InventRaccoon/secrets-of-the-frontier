package data.scripts.campaign.plugins.dustkeepers;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.CallEvent.CallableEvent;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.TimeoutTracker;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import org.apache.log4j.Logger;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;

/**
 *	Altered version of vanilla's OfficerManagerEvent to spawn recruitable Dustkeeper warminds
 */

public class SotfDustkeeperOfficerManagerEvent implements CallableEvent, ColonyInteractionListener, EveryFrameScript {

	public static float WARMIND_SPAWN_PROB = 1f;
	public static float ADDITIONAL_WARMIND_SPAWN_PROB = 0.5f;

	public static class AvailableOfficer {
		public PersonAPI person;
		public String marketId;
		public float timeRemaining = 0f;
		public AvailableOfficer(PersonAPI person, String marketId) {
			this.person = person;
			this.marketId = marketId;
		}
	}

	public static Logger log = Global.getLogger(SotfDustkeeperOfficerManagerEvent.class);

	private IntervalUtil removeTracker = new IntervalUtil(1f, 3f);

	private List<AvailableOfficer> available = new ArrayList<AvailableOfficer>();

	private TimeoutTracker<String> recentlyChecked = new TimeoutTracker<String>();

	protected long seed = 0;

	public SotfDustkeeperOfficerManagerEvent() {
		readResolve();
		Global.getSector().getListenerManager().addListener(this);
	}
	
	Object readResolve() {
		if (recentlyChecked == null) {
			recentlyChecked = new TimeoutTracker<String>();
		}
		if (seed == 0) {
			seed = Misc.random.nextLong();
		}
//		Global.getSector().getListenerManager().addListener(this);
		return this;
	}
	
	public void reportPlayerClosedMarket(MarketAPI market) {}

	public void reportPlayerOpenedMarket(MarketAPI market) {
		//recentlyChecked = new TimeoutTracker<String>();
		if (recentlyChecked.contains(market.getId())) return;
		
		if (market.isPlanetConditionMarketOnly()) return;
		if (!market.isInEconomy()) return;
		if (!market.getFactionId().equals(SotfIDs.DUSTKEEPERS)) return;
		//if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getRelationshipLevel(Factions.PLAYER).isAtBest(RepLevel.FAVORABLE)) return;
		
		pruneFromRemovedMarkets();
		
		float officerProb = 0.5f;

		CampaignClockAPI clock = Global.getSector().getClock();
		long mult = clock.getCycle() * 12L + clock.getMonth();
		
		//Random random = new Random(seed + market.getId().hashCode() * mult);
		Random random = Misc.getRandom(seed + market.getId().hashCode() * mult, 11);
		//random = new Random();
		
		float dur = getOfficerDuration(random);
		recentlyChecked.add(market.getId(), dur * 0.5f);
		
		if (random.nextFloat() < WARMIND_SPAWN_PROB) {
			AvailableOfficer officer = createOfficer(market);
			if (officer != null) {
				officer.timeRemaining = dur;
				addAvailable(officer);
			}
		}

		if (random.nextFloat() < ADDITIONAL_WARMIND_SPAWN_PROB) {
			AvailableOfficer officer = createOfficer(market);
			if (officer != null) {
				officer.timeRemaining = dur;
				addAvailable(officer);
			}
		}
	}
	
	protected float getOfficerDuration(Random random) {
		return 60f + 60f * random.nextFloat();
	}
	
	public void advance(float amount) {
		float days = Global.getSector().getClock().convertToDays(amount);

		if (Global.getSettings().isDevMode()) {
			days *= 15f;
		}
		
		recentlyChecked.advance(days);
		
		removeTracker.advance(days);
		if (removeTracker.intervalElapsed()) {
			pruneFromRemovedMarkets();
			
			float interval = removeTracker.getIntervalDuration();
			
			for (AvailableOfficer curr : new ArrayList<AvailableOfficer>(available)) {
				curr.timeRemaining -= interval;
				if (curr.timeRemaining <= 0) {
					removeAvailable(curr);
				}
			}
		}
		
	}
	
	public void pruneFromRemovedMarkets() {
		for (AvailableOfficer curr : new ArrayList<AvailableOfficer>(available)) {
			if (Global.getSector().getEconomy().getMarket(curr.marketId) == null) {
				removeAvailable(curr);
			}
		}
	}
	
	public void addAvailable(AvailableOfficer officer) {
		if (officer == null) return;
		
		available.add(officer);
		
		setEventDataAndAddToMarket(officer);
	}

	
	protected void setEventDataAndAddToMarket(AvailableOfficer officer) {
		MarketAPI market = Global.getSector().getEconomy().getMarket(officer.marketId);
		if (market == null) return;
		market.getCommDirectory().addPerson(officer.person);
		market.addPerson(officer.person);
		
		officer.person.getMemoryWithoutUpdate().set("$sotf_dkome_hireable", true);
		officer.person.getMemoryWithoutUpdate().set("$sotf_dkome_eventRef", this);
	}
	
	public void removeAvailable(AvailableOfficer officer) {
		if (officer == null) return;
		
		available.remove(officer);
		
		MarketAPI market = Global.getSector().getEconomy().getMarket(officer.marketId);
		if (market != null) {
			market.getCommDirectory().removePerson(officer.person);
			market.removePerson(officer.person);
		}
		
		officer.person.getMemoryWithoutUpdate().unset("$sotf_dkome_hireable");
		officer.person.getMemoryWithoutUpdate().unset("$sotf_dkome_eventRef");
	}

	protected AvailableOfficer createOfficer(MarketAPI market) {
		if (market == null) return null;

		PersonAPI person = pickOfficer(market);
		if (person == null) return null;
		person.setPostId(Ranks.POST_OFFICER_FOR_HIRE);

		return new AvailableOfficer(person, market.getId());
	}

	// they're all pregenerated (because that's kind of how they need to be in order to be consistent per-chip
	// so just pick one who's available
	// Sliver/Echo 3 & Annex 2 are reserved for The Iron Wolves Nex mercenary fleet
	public static PersonAPI pickOfficer(MarketAPI market) {
		WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>();
		if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getRelationshipLevel(Factions.PLAYER).isAtWorst(RepLevel.WELCOMING)) {
			picker.add(SotfPeople.SLIVER_1);
			picker.add(SotfPeople.SLIVER_2);
		}

		if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getRelationshipLevel(Factions.PLAYER).isAtWorst(RepLevel.FRIENDLY)) {
			picker.add(SotfPeople.ECHO_1);
			picker.add(SotfPeople.ECHO_2);
		}

		// only if player has beaten him and turned in his chip to Haven
		if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_turnedInBarrow")) {
			picker.add(SotfPeople.BARROW, 10f);
		}

		for (String potential : picker.clone().getItems()) {
			if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_dkomeHired_" + potential)) {
				picker.remove(potential);
			}
		}
		if (picker.isEmpty()) {
			return null;
		}
		return SotfPeople.getPerson(picker.pick());
	}

	public boolean callEvent(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		String action = params.get(0).getString(memoryMap);
		
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		CargoAPI cargo = playerFleet.getCargo();
		
		if (action.equals("printSkills")) {
			String personId = params.get(1).getString(memoryMap);
			AvailableOfficer officer = getOfficer(personId);
			
			if (officer != null) {
				MutableCharacterStatsAPI stats = officer.person.getStats();
				TextPanelAPI text = dialog.getTextPanel();
				
				Color hl = Misc.getHighlightColor();
				Color red = Misc.getNegativeHighlightColor();

				text.addSkillPanel(officer.person, false);
				
				text.setFontSmallInsignia();
				String personality = Misc.lcFirst(Misc.getPersonalityName(officer.person));
				String autoMultString = new DecimalFormat("#.##").format(officer.person.getMemoryWithoutUpdate().getFloat(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT));

				text.addParagraph("Personality: " + personality + ", level: " + stats.getLevel() + " (fixed)" + ", automated ship points multiplier: " + autoMultString + "x");
				text.highlightInLastPara(hl, personality, "" + stats.getLevel(), autoMultString + "x");
				switch (officer.person.getPersonalityAPI().getId()) {
					// displays as Fearless so use that instead
					// ... though they aren't actually "more aggressive than Reckless" like Gamma/Beta/Alphas are
					case Personalities.RECKLESS:
						text.addParagraph("In combat, this warmind is single-minded and determined. " +
								"In a human captain, their traits might be considered reckless. In a machine, they're terrifying.");
						break;
					case Personalities.AGGRESSIVE:
						text.addParagraph("In combat, this warmind will prefer to engage at a range that allows the use of " +
								"all of their ship's weapons and will employ any fighters under their command aggressively.");
						break;
					case Personalities.STEADY:
						text.addParagraph("In combat, this warmind will favor a balanced approach with " +
								"tactics matching the current situation.");
						break;
					// not going to happen normally, but for completeness' sake...
					case Personalities.CAUTIOUS:
						text.addParagraph("In combat, this warmind will prefer to stay out of enemy range, " +
								"only occasionally moving in if out-ranged by the enemy.");
						break;
					// ... though I'm pretty sure if one popped out the RNG as Timid they'd just reroll them
					case Personalities.TIMID:
						text.addParagraph("In combat, this warmind will attempt to avoid direct engagements if at all " +
								"possible, even if commanding a combat vessel.");
						break;
				}

				if (officer.person.getStats().getLevel() < 8) {
					text.addPara("Warminds act similarly to AI cores. They can only be assigned to automated ships, " +
							"their skills can be reassigned at will, and they can be fully integrated into ships to " +
							"gain an extra level.", Misc.getGrayColor().brighter());
				} else {
					text.addPara("Warminds act similarly to AI cores. They can only be assigned to automated ships, " +
							"their non-unique skills can be reassigned at will, and they can be fully integrated " +
							"into ships to gain an extra level.", Misc.getGrayColor().brighter());
				}

				text.setFontInsignia();
			}
		} else if (action.equals("hireOfficer")) {
			String personId = params.get(1).getString(memoryMap);
			AvailableOfficer officer = getOfficer(personId);
			if (officer != null) {
				removeAvailable(officer);
				cargo.addCommodity(officer.person.getAICoreId(), 1);
				officer.person.setPostId(Ranks.POST_OFFICER);
				AddRemoveCommodity.addCommodityGainText(officer.person.getAICoreId(), 1, dialog.getTextPanel());
				Global.getSector().getMemoryWithoutUpdate().set("$sotf_dkomeHired_" + officer.person.getId(), true);
			}
		} else if (action.equals("hasAutomated")) {
			return Misc.getAllowedRecoveryTags().contains(Tags.AUTOMATED_RECOVERABLE) || SotfMisc.playerHasNoAutoPenaltyShip();
		}
		
		return true;
	}
	
	private AvailableOfficer getOfficer(String personId) {
		for (AvailableOfficer officer: available) {
			if (officer.person.getId().equals(personId)) {
				return officer;
			}
		}
		return null;
	}

	public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		
	}
	
	public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
		
	}

	public boolean runWhilePaused() {
		return false;
	}
	
	public boolean isDone() {
		return false;
	}
}










