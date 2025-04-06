package data.scripts.campaign.missions;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.RecoverAPlanetkiller;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.wendigo.SotfWendigoEncounterManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *	A FAVOR FOR WENDIGO
 *	Seems unreasonable? Shouldn't have gotten yourself into this mess in the first place, oh quarry mine.
 */

public class SotfAFavorForWendigo extends HubMissionWithSearch implements FleetEventListener {

	public static float MISSION_DAYS = 365f;
	
	public static enum Stage {
		KILL,
		COMPLETED,
		FAILED,
	}
	
	protected CampaignFleetAPI station;
	protected StarSystemAPI system;
	
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		//genRandom = Misc.random;
		
		PersonAPI person = SotfPeople.getPerson(SotfPeople.WENDIGO);
		if (person == null) return false;
		
		setPersonOverride(person);
		
		setStoryMission();
		setNoAbandon();
		
		if (!setGlobalReference("$sotf_affw_ref", "$sotf_affw_inProgress")) {
			return false;
		}

		CampaignFleetAPI nearestStation = null;
		float nearestDistance = 1000000000f;
		for (CampaignFleetAPI station : getStations()) {
			float dist = Misc.getDistanceToPlayerLY(station.getStarSystem().getHyperspaceAnchor());
			if (dist < nearestDistance) {
				nearestDistance = dist;
				nearestStation = station;
			}
		}
		
		if (nearestStation == null) return false;

		station = nearestStation;
		system = station.getStarSystem();
		for (CampaignFleetAPI station : getStations()) {
			makeImportant(station, "$sotf_affw_target", Stage.KILL);
		}

		setStartingStage(Stage.KILL);
		setSuccessStage(Stage.COMPLETED);
		setFailureStage(Stage.FAILED);
		
		setStageOnMemoryFlag(Stage.COMPLETED, person, "$sotf_affw_completed");
		setTimeLimit(Stage.FAILED, MISSION_DAYS, station.getStarSystem());

		setRepRewardFaction(0.55f);
		setRepPersonChangesVeryHigh();
		return true;
	}
	
	@Override
	protected void endAbandonImpl() {
		super.endAbandonImpl();
		endFailureImpl(null, null);
	}

	@Override
	protected void endFailureImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_FAILED_WENDIGO_FAVOR, true);

		SotfWendigoEncounterManager.sendWendigoFailureHunt();
	}

	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (isDone() || result != null) return;

		// also credit the player if they're in the same location as the fleet and nearby
		float distToPlayer = Misc.getDistance(fleet, Global.getSector().getPlayerFleet());
		boolean playerInvolved = battle.isPlayerInvolved() || (fleet.isInCurrentLocation() && distToPlayer < 2000f);

		if (!battle.isPlayerInvolved()) return;

		//CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		if (!playerInvolved || !battle.isInvolved(fleet) || battle.onPlayerSide(fleet)) {
			return;
		}

		if (fleet.isStationMode()) {
			if (fleet.getFlagship() != null) return;
		}

		getPerson().getMemoryWithoutUpdate().set("$sotf_affw_completed", true);
	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
	}
	
	protected void updateInteractionDataImpl() {
		set("$sotf_affw_systemName", station.getStarSystem().getNameWithLowercaseTypeShort());
	}
	
	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.KILL) {
			info.addPara("Destroy a fully-operational Remnant Nexus.", opad);
			info.addPara("Wintry-Annex-Wendigo last saw one in the " + system.getNameWithLowercaseTypeShort() + ", though you need not destroy it specifically.", opad);
		} else if (currentStage == Stage.FAILED) {
			info.addPara("You are now being hunted by Wintry-Annex-Wendigo, and can expect to meet them the next time you visit the sector's fringe systems.", opad);
		}
	}

	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.KILL) {
			info.addPara("Destroy a fully-operational Remnant Nexus", tc, pad);
			return true;
		}
		return false;
	}
	
	protected String getMissionTypeNoun() {
		return "task";
	}
	
	@Override
	public String getBaseName() {
		return "A Favor for Wendigo";
	}
	
	
	@Override
	public void acceptImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.acceptImpl(dialog, memoryMap);
		for (CampaignFleetAPI station : getStations()) {
			makeImportant(station, "$sotf_affw_target", Stage.KILL);
			station.addEventListener(this);
		}
	}

	@Override
	protected void notifyEnding() {
		super.notifyEnding();
		for (CampaignFleetAPI station : getStations()) {
			station.removeEventListener(this);
		}
	}

	public java.util.List<CampaignFleetAPI> getStations() {
		List<CampaignFleetAPI> stations = new ArrayList<CampaignFleetAPI>();
		for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			if (!system.hasTag(Tags.THEME_REMNANT_MAIN)) continue;
			if (!system.hasTag(Tags.THEME_REMNANT_RESURGENT)) continue;

			for (CampaignFleetAPI fleet : system.getFleets()) {
				if (!fleet.isStationMode()) continue;
				if (!Factions.REMNANTS.equals(fleet.getFaction().getId())) continue;
				if (fleet.getMemoryWithoutUpdate().getBoolean("$damagedStation")) continue;
				stations.add(fleet);
			}
		}
		return stations;
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map, Object currentStage) {
		if (currentStage == Stage.KILL) {
			return getMapLocationFor(system.getCenter());
		}
		return super.getMapLocation(map, currentStage);
	}
	
}





