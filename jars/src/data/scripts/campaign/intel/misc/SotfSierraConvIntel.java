package data.scripts.campaign.intel.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.fel.SotfGuiltTracker;
import data.scripts.dialog.SotfGenericSierraDialogScript;
import data.scripts.utils.SotfMisc;
import org.lwjgl.input.Keyboard;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

import java.awt.*;
import java.util.Set;

import static data.scripts.campaign.ids.SotfIDs.MEM_BARROW_FLEET;

/**
 *	Sierra's contact intel, also tracks what she wants to talk about and the Project SIREN event
 */

public class SotfSierraConvIntel extends BaseIntelPlugin implements FleetEventListener, ColonyPlayerHostileActListener, EconomyTickListener {

	// monthly "maintenance" cost for Sierra in credits
	private static final float MAINTENANCE_COST = 50f;
	private float randThoughtCounter = 0f;
	private final float randThoughtTime = 30f;
	//private static PersonAPI sierra = SotfPeople.getPerson(SotfPeople.SIERRA);

	// so we can hotswap the Sierra officer core for the special item in the cargo screen
	public boolean runWhilePaused() {
		return true;
	}

	public SotfSierraConvIntel() {
		Global.getSector().getPlayerFleet().addEventListener(this);
		Global.getSector().getListenerManager().addListener(this);
		if (!Global.getSector().getScripts().contains(this)) {
			Global.getSector().addScript(this);
		}
	}

	protected void advanceImpl(float amount) {

		// don't want the player to get their hands on Sierra's "real" core (could install it in other ships)
		if (Global.getSector().getPlayerFleet() != null && !SotfModPlugin.NEW_SIERRA_MECHANICS) {
			float numSierraCores = Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(SotfIDs.SIERRA_CORE_OFFICER);
			if (numSierraCores > 0) {
				Global.getSector().getPlayerFleet().getCargo().removeCommodity(SotfIDs.SIERRA_CORE_OFFICER, numSierraCores);
				//Global.getSector().getPlayerFleet().getCargo().addSpecial(new SpecialItemData(SotfIDs.SIERRA_CORE, null), 1);
			}
		}

		// Project SIREN if player sold Sierra to a scientist
		if (Global.getSector().getMemoryWithoutUpdate().getString(SotfIDs.MEM_SIERRA_BETRAYAL_TYPE) != null && !Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_PROJECT_SIREN_COUNTDOWN)) {
			if (Global.getSector().getMemoryWithoutUpdate().getString(SotfIDs.MEM_SIERRA_BETRAYAL_TYPE).equals("science")) {
				float sirenDelay = 120f + (float) Math.random() * 60f;
				if (Global.getSettings().isDevMode()) {
					Global.getSector().getCampaignUI().addMessage("DEV: Project SIREN now available");
					sirenDelay = 0f;
				}
				HubMissionWithTriggers.SetMemoryValueAfterDelay action = new HubMissionWithTriggers.SetMemoryValueAfterDelay(sirenDelay,
						Global.getSector().getMemoryWithoutUpdate(), SotfIDs.MEM_BEGAN_PROJECT_SIREN, true);
				action.doAction(null);
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_PROJECT_SIREN_COUNTDOWN, true);
			}
		}

		// Remove Sierra XO if she is not in the fleet
		if (SotfMisc.isSecondInCommandEnabled()) {
			if (SCUtils.getPlayerData().hasAptitudeInFleet("sotf_witchcraft") && isHidden()) {
				SCOfficer officer = (SCOfficer) Global.getSector().getMemoryWithoutUpdate().get("$sotf_sierraXO");
				SCData data = SCUtils.getPlayerData();
				data.removeOfficerFromFleet(officer);
			}
		}

		if (isHidden()) return; // only if she's in the fleet

		// Add Sierra XO if she is in the fleet and it has been unlocked
		if (SotfMisc.isSecondInCommandEnabled() && Global.getSector().getMemoryWithoutUpdate().getBoolean("$sotf_gotSierraXO")) {
			if (!SCUtils.getPlayerData().hasAptitudeInFleet("sotf_witchcraft")) {
				SCOfficer officer = (SCOfficer) Global.getSector().getMemoryWithoutUpdate().get("$sotf_sierraXO");
				if (officer == null) {
					officer = new SCOfficer(SotfPeople.getPerson(SotfPeople.SIERRA), "sotf_witchcraft");
					Global.getSector().getMemoryWithoutUpdate().set("$sotf_sierraXO", officer);
				}
				SCData data = SCUtils.getPlayerData();
				data.addOfficerToFleet(officer);
				data.setOfficerInEmptySlotIfAvailable(officer);
			}
		}

		if (!Global.getSector().getPlayerMemoryWithoutUpdate().contains("$sotf_metSierra")) {
			Global.getSector().getPlayerMemoryWithoutUpdate().set("$sotf_metSierra", true);
		}

		if (Global.getSector().getMemoryWithoutUpdate().get(SotfIDs.MEM_DAYS_WITH_SIERRA) == null) {
			Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_DAYS_WITH_SIERRA, 0f);
		} else {
			float timeHadSierra = Global.getSector().getMemoryWithoutUpdate().getFloat(SotfIDs.MEM_DAYS_WITH_SIERRA);
			Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_DAYS_WITH_SIERRA, timeHadSierra + Global.getSector().getClock().convertToDays(amount));
		}

		if (!Global.getSettings().getBoolean("sotf_sierraHasRandomThoughts")) return;
		int numSierraThoughts = Global.getSector().getMemoryWithoutUpdate().getInt(SotfIDs.MEM_NUM_SIERRA_THOUGHTS);
		if (numSierraThoughts > 1) {
			return;
		}
		float toIncrement = Global.getSector().getClock().convertToDays(amount);
		if (numSierraThoughts > 0) {
			toIncrement *= 0.5f; // half as fast if she already has something random to say
		}
		randThoughtCounter += toIncrement;
		if (randThoughtCounter > randThoughtTime) {
			String topic = getSierraTopic();
			if (topic != null) {
				Global.getSector().getMemoryWithoutUpdate().set(topic, true);
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_NUM_SIERRA_THOUGHTS, numSierraThoughts + 1);
			}
			randThoughtCounter = 0f;
		}
	}

	public static String getSierraTopic() {
		float timeHadSierra = Global.getSector().getMemoryWithoutUpdate().getFloat(SotfIDs.MEM_DAYS_WITH_SIERRA);
		WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>();
		picker.add("$sierraRandShepherds");
		picker.add("$sierraRandRain");
		picker.add("$sierraRandReaper");
		if (timeHadSierra > 60) {
			picker.add("$sierraRand2ndAIWar");
		}
		if (timeHadSierra > 120) {
			if (Global.getSector().getEconomy().getMarket("tartessus") != null) {
				picker.add("$sierraRandMoths");
			}
			picker.add("$sierraRandMusicSales");
		}
		//picker.add("$sierraRand");
		// no topics Sierra has already wanted to talk about
		for (String topic : picker.clone().getItems()) {
			if (Global.getSector().getMemoryWithoutUpdate().contains(topic)) {
				picker.remove(topic);
			}
		}
		if (picker.isEmpty()) {
			return null;
		}
		return picker.pick();
	}

	// tracks the player's accomplishments with and without Sierra
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (isDone()) return;

		if (!battle.isPlayerInvolved()) {
			return;
		}

		SotfMisc.setSierraHasThoughts();

		MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();

		int biggest = 0;
		for (CampaignFleetAPI otherFleet : battle.getNonPlayerSideSnapshot()) {
			if (otherFleet.getFleetPoints() > biggest) {
				biggest = otherFleet.getFleetPoints();
				if (!otherFleet.getMemoryWithoutUpdate().contains(MemFlags.MEMORY_KEY_NO_REP_IMPACT) && SotfMisc.playerHasSierra()) {
					sector_mem.set("$sierraFactionFought", otherFleet.getFaction().getId(), 60);
				}
			}
			MemoryAPI fleet_mem = otherFleet.getMemoryWithoutUpdate();
			if (fleet_mem.contains("$ziggurat") && SotfMisc.playerHasSierra()) {
				sector_mem.set("$sierraWitnessedZigFight", true);
			}
			for (FleetMemberAPI member : Misc.getSnapshotMembersLost(otherFleet)) {
				if (member.getHullId().equals("tesseract") && SotfMisc.playerHasSierra()) {
					sector_mem.set("$sierraWitnessedOmega", true);
				}
				if (member.getHullId().equals("guardian") && SotfMisc.playerHasSierra()) {
					sector_mem.set("$sierraWitnessedGuardianKill", true);
				}
				if (member.getHullId().equals("remnant_station1") && SotfMisc.playerHasSierra()) {
					sector_mem.set("$sierraWitnessedRemStation1Kill", true);
				}
				if (member.getHullId().equals("remnant_station2") && SotfMisc.playerHasSierra()) {
					sector_mem.set("$sierraWitnessedRemStation2Kill", true);
				}

				if (member.getHullId().equals("sotf_repose") && fleet_mem.contains(MEM_BARROW_FLEET) && SotfMisc.playerHasSierra()) {
					sector_mem.set("$sierraWitnessedBarrowKill", true);
				}

				if (member.getHullId().equals("XHAN_Myrianous") && SotfMisc.playerHasSierra()) {
					sector_mem.set("$sierraWitnessedMyrianous", true);
				}
			}
		}
	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {

	}

	public void reportRaidForValuablesFinishedBeforeCargoShown(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, CargoAPI cargo) {
		//
	}
	public void reportRaidToDisruptFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, Industry industry) {
		//
	}

	public void reportTacticalBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData) {
		//
	}
	public void reportSaturationBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData) {
		if (SotfMisc.getSierraNoSatbombConsequences()) return;
		if (SotfGuiltTracker.GUILTY_FACTIONS.contains(market.getFactionId())) {
			return;
		}

		// must have met Sierra already, but she need not be in the player's fleet
		if (Global.getSector().getPlayerMemoryWithoutUpdate().contains("$sotf_metSierra")) {
			Global.getSector().getMemoryWithoutUpdate().set("$sierraLatestSatbombMkt", market.getName());
			if (market.getSize() >= 4) {
				Global.getSector().getScripts().add(new SotfGenericSierraDialogScript("sotfSierraSatbombExtreme"));
			} else if (!Global.getSector().getMemoryWithoutUpdate().contains("$sierraWitnessedAtrocity")) {
				Global.getSector().getMemoryWithoutUpdate().set("$sierraWitnessedAtrocity", true);
				SotfMisc.setSierraHasThoughts();
			} else {
				Global.getSector().getScripts().add(new SotfGenericSierraDialogScript("sotfSierraSatbombExtreme"));
			}
		}
		// admitting the first satbomb was a mistake removes 0.5 Guilt. Hollow words grant no rewards.
		if (Global.getSector().getMemoryWithoutUpdate().contains("$sierraAdmittedMistake") && !Global.getSector().getMemoryWithoutUpdate().contains("$sierraAdmittedMistakePenalty")) {
			SotfMisc.addGuilt(0.5f);
			Global.getSector().getMemoryWithoutUpdate().set("$sierraAdmittedMistakePenalty", true);
		}
	}

	public void reportEconomyMonthEnd() {

	}

	// Sierra "maintenance"
	public void reportEconomyTick(int iterIndex) {
		if (!SotfMisc.playerHasSierra()) return;
		float numIter = Global.getSettings().getFloat("economyIterPerMonth");
		float f = 1f / numIter;

		//CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		MonthlyReport report = SharedData.getData().getCurrentReport();

		MonthlyReport.FDNode officersNode = report.getNode(MonthlyReport.FLEET, MonthlyReport.OFFICERS);
		float monthly = MAINTENANCE_COST;
		MonthlyReport.FDNode costNode = report.getNode(officersNode, "sotf_node_sierra");

		// Sierra will waive her maintenance fee if you're ridiculously poor
		// Yeah it's only 50 credits but "every little helps!"
		boolean playerIsTooPoor = true;
		if (Global.getSector().getPlayerFleet() != null) {
			playerIsTooPoor = Global.getSector().getPlayerFleet().getCargo().getCredits().get() < 10000;
		}

		if (report.getDebt() <= 0 && report.getPreviousDebt() <= 0 && !playerIsTooPoor) {
			costNode.upkeep += monthly * f;
		}

		costNode.name = "Sierra";
		costNode.icon = SotfPeople.getPerson(SotfPeople.SIERRA).getPortraitSprite();
		costNode.tooltipCreator = new TooltipMakerAPI.TooltipCreator() {
				public boolean isTooltipExpandable(Object tooltipParam) {
					return true;
				}
				public float getTooltipWidth(Object tooltipParam) {
					return 450;
				}
				public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
					tooltip.addPara("\"Maintenance fees\" paid to Sierra in place of a full " +
							"officer's salary.", 10f);
					if (expanded) {
						tooltip.addPara("Spending includes exotic flora, media subscriptions, updated atmospheric " +
								"simulations and the ship \"party fund\".", Misc.getGrayColor(),10f);
						tooltip.addPara("\"Two thousand credits per month? I'm hardly going to be saving for a " +
								"rustic homestead in the Ailmar wilds, knock that down to like... fifty?\"", SotfMisc.getSierraColor().darker(), 10f);
					}
				}
			};
	}

	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		float pad = 3f;
		float opad = 10f;
		
		float initPad = pad;
		if (mode == ListInfoMode.IN_DESC) initPad = opad;
		
		Color tc = getBulletColorForMode(mode);
		
		bullet(info);
		boolean isUpdate = getListInfoParam() != null;
		
		unindent(info);
	}
	
	
	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		Color c = getTitleColor(mode);
		info.addPara(getName(), c, 0f);
		addBulletPoints(info, mode);
	}
	
	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		PersonAPI sierra = SotfPeople.getPerson(SotfPeople.SIERRA);
		FactionAPI sierra_faction = Global.getSector().getFaction(SotfIDs.SIERRA_FACTION);
		FleetMemberAPI member = null;
		for (FleetMemberAPI fleetmem : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
			if (fleetmem.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && !fleetmem.getVariant().hasTag(SotfIDs.TAG_INERT)) {
				member = fleetmem;
			}
		}
		float pad = 3f;
		float opad = 10f;
		
		addBulletPoints(info, ListInfoMode.IN_DESC);

		info.addImage(sierra.getPortraitSprite(), width, 128, opad);

		if (member != null) {
			info.addPara("Sierra is an experimental AI persona under your command, currently operating the " + member.getShipName() + ".", opad, SotfMisc.getSierraColor(), "Sierra", member.getShipName());
		}
		// below text should never display - this intel item is hidden without Sierra around
		else {
			info.addPara("Sierra is an experimental AI persona under your command, currently operating within your fleet.", opad, SotfMisc.getSierraColor(), "Sierra");
		}
		info.addPara("A conversation with her might offer up insights on your recent accomplishments, or an opportunity to offer her feedback on her combat style.", opad);
		if (SotfMisc.playerHasInertConcord()) {
			info.addPara("You can also transfer Sierra to an inert Concord ship via this function.", opad);
		}
		info.addPara("She is currently available to speak to.", opad);
		ButtonAPI button = info.addButton("Request a comm-link", "sotf_sierraConvButton",
				sierra_faction.getBaseUIColor(), sierra_faction.getDarkUIColor(),
				(int)(width), 20f, opad * 2f);
		button.setShortcut(Keyboard.KEY_T, true);
	}
	
	@Override
	public String getIcon() {
		return Global.getSettings().getSpriteName("sotf_characters", "sierra");
	}
	
	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_CONTACTS);
		return tags;
	}
	
	public String getSortString() {
		return "Sierra";
	}
	
	public String getName() {
		return "Contact: Sierra";
	}
	
	@Override
	public FactionAPI getFactionForUIColors() {
		return Global.getSector().getFaction(SotfIDs.SIERRA_FACTION);
	}

	public String getSmallDescriptionTitle() {
		return getName();
	}
	
	@Override
	public boolean shouldRemoveIntel() {
		return false;
	}

	// don't show unless a Concord ship is in our fleet
	@Override
	public boolean isHidden() {
		return !SotfMisc.playerHasSierra();
	}

	@Override
	public String getCommMessageSound() {
		return getSoundMajorPosting();
	}

	public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
		if (buttonId == "sotf_sierraConvButton") {
			ui.showDialog(null, "SierraConvOpen");
		}
	}
}







