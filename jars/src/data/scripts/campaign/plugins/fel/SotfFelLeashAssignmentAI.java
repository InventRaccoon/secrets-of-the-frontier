package data.scripts.campaign.plugins.fel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.fleet.FleetData;
import data.scripts.campaign.customstart.SotfHauntedDreamCampaignVFX;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.missions.SotfHauntedFinale;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfFelLeashAssignmentAI extends BaseAssignmentAI {

	protected float elapsed = 0f;
	protected float dur = 30f + (float) Math.random() * 20f;
	protected SectorEntityToken toGuard;

	protected IntervalUtil moteSpawn = new IntervalUtil(0.01f, 0.1f);

	public SotfFelLeashAssignmentAI(CampaignFleetAPI fleet, SectorEntityToken toGuard) {
		super();
		this.fleet = fleet;
		this.toGuard = toGuard;
		
		giveInitialAssignments();
	}

	@Override
	protected void giveInitialAssignments() {
		pickNext();
	}

	@Override
	protected void pickNext() {
		fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, toGuard, 100f);
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);
		
//		if (fleet.getCurrentAssignment() == null || fleet.getCurrentAssignment().getAssignment() != FleetAssignment.HOLD) {
//			fleet.addAssignmentAtStart(FleetAssignment.HOLD, null, 1000f, null);
//		}
		
		if (toGuard != null) {
			float dist = Misc.getDistance(fleet.getLocation(), toGuard.getLocation());
			if (dist > toGuard.getRadius() + fleet.getRadius() + 5000 &&
					fleet.getAI().getCurrentAssignmentType() == FleetAssignment.ORBIT_AGGRESSIVE) {
				fleet.addAssignmentAtStart(FleetAssignment.ORBIT_PASSIVE, toGuard, 0.5f, null);
				CampaignFleetAIAPI ai = fleet.getAI();
				if (ai instanceof ModularFleetAIAPI) {
					// needed to interrupt an in-progress pursuit
					ModularFleetAIAPI m = (ModularFleetAIAPI) ai;
					m.getStrategicModule().getDoNotAttack().add(m.getTacticalModule().getTarget(), 0.5f);
					m.getTacticalModule().setTarget(null);
				}
			}
		}

		if (!Misc.getDefeatTriggers(fleet, true).contains("sotfHauntedBeatFel")) {
			Misc.makeUnimportant(fleet, "$sotf_haunted");
			return;
		}

		boolean showVisual = false;
		if (Global.getSector().getPlayerFleet() != null) {
			CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
			if (playerFleet.getContainingLocation() == fleet.getContainingLocation()) {
				if (Misc.getDistance(fleet.getLocation(), playerFleet.getLocation()) < 1500) {
					showVisual = true;
				}
				if (!fleet.getMemoryWithoutUpdate().contains("$sotf_felDidAdaptations")) {
					adapt();
					fleet.getMemoryWithoutUpdate().set("$sotf_felDidAdaptations", true);
				}
			}
		}
		if (showVisual) {
			for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
				curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
			}
			SotfHauntedDreamCampaignVFX.fadeInFromCurrent(0.5f);
		} else {
			SotfHauntedDreamCampaignVFX.fadeOutFromCurrent(0.5f);
		}
	}

	private void adapt() {
		FleetDataAPI fleetData = fleet.getFleetData();
		WeightedRandomPicker<String> adaptPicker = new WeightedRandomPicker<String>(Misc.random);

		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		float playerCombatFP = 0.01f; // just to avoid divide by zero :)
		float playerCruiserFP = 0f;
		float playerCapitalFP = 0f;
		float playerCarrierFP = 0f;

		float omegaWeight = 0f;

		for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
			if (member.isCivilian()) continue;
			playerCombatFP += member.getFleetPointCost();
			if (member.isCruiser()) playerCruiserFP += member.getFleetPointCost();
			if (member.isCapital()) playerCapitalFP += member.getFleetPointCost();

			float carrierBays = member.getHullSpec().getHullSize().ordinal() - 1;
			if (member.isCapital()) carrierBays++;
			playerCarrierFP += (member.getFleetPointCost() * ((float) member.getNumFlightDecks() / carrierBays));

			// two can play at that game
			if (member.getHullSpec().hasTag(Tags.OMEGA)) {
				omegaWeight = 99999f;
			}
		}

		float eidolonWeight = 0f;
		if (SotfMisc.playerHasSierra()) eidolonWeight = 0.5f;

		float preyFP = playerCruiserFP + playerCapitalFP;
		float anticapitalWeight = (2f * (preyFP / playerCombatFP));

		float pdWeight = 0.01f + (playerCarrierFP / playerCombatFP * 2f); // default pick if nothing else applies

		adaptPicker.add("omega", omegaWeight);
		adaptPicker.add("eidolon", eidolonWeight);
		adaptPicker.add("capital_hunters", anticapitalWeight);
		adaptPicker.add("pointdefense", pdWeight);

		String adaptPick = adaptPicker.pickAndRemove();
		SotfHauntedFinale.addFelComposition(fleetData, adaptPick);

		if (Global.getSettings().isDevMode()) {
			Global.getSector().getCampaignUI().addMessage("Fel adaptive pick: " + adaptPick);
		}

		fleetData.sort();
		fleetData.syncIfNeeded();
	}
}












