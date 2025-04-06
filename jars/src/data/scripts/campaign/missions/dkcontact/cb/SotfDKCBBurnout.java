package data.scripts.campaign.missions.dkcontact.cb;

import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBStats;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;

/**
 *	Dustkeeper "deserters", aka burning-out instances
 */

public class SotfDKCBBurnout extends SotfBaseDKCBCreator {

	@Override
	public float getFrequency(HubMissionWithBarEvent mission, int difficulty) {
		float extraMult = 1f;
		// probably makes sense for Inadvertent to give these out less than Wendigo or the Twins...
		// ... but Wendigo doesn't give bounties atm and Twins are unimplemented, so let's keep a good source of
		// burnout bounties for now since they can be unreliable to get from normal contacts
		if (mission.getPerson().getId().equals(SotfPeople.INADVERTENT)) {
			//extraMult = 0.5f;
			extraMult = 1f;
		}
		return super.getFrequency(mission, difficulty) * CBStats.DESERTER_FREQ * extraMult;
	}
	
	public String getBountyNamePostfix(HubMissionWithBarEvent mission, CustomBountyData data) {
		return " - Burnout Warshard";
	}
	
	@Override
	public CustomBountyData createBounty(MarketAPI createdAt, HubMissionWithBarEvent mission, int difficulty, Object bountyStage) {
		CustomBountyData data = new CustomBountyData();
		data.difficulty = difficulty;
		
//		mission.requireSystemTags(ReqMode.ANY, Tags.THEME_RUINS, Tags.THEME_MISC, Tags.THEME_REMNANT_SECONDARY,
//								  Tags.THEME_DERELICT, Tags.THEME_REMNANT_DESTROYED);
		mission.requireSystemInterestingAndNotUnsafeOrCore();
		mission.requireSystemNotHasPulsar();
		StarSystemAPI system = mission.pickSystem();
		data.system = system;
	
		FleetSize size = FleetSize.MEDIUM;
		FleetQuality quality = FleetQuality.DEFAULT;
		String type = FleetTypes.PATROL_MEDIUM;
		OfficerQuality oQuality = OfficerQuality.DEFAULT;
		OfficerNum oNum = OfficerNum.DEFAULT;
		
		if (difficulty <= 4) {
			size = FleetSize.SMALL;
			quality = FleetQuality.DEFAULT;
			oQuality = OfficerQuality.DEFAULT;
			oNum = OfficerNum.DEFAULT;
			type = FleetTypes.PATROL_SMALL;
		} else if (difficulty <= 5) {
			size = FleetSize.MEDIUM;
			quality = FleetQuality.DEFAULT;
			oQuality = OfficerQuality.DEFAULT;
			oNum = OfficerNum.DEFAULT;
			type = FleetTypes.PATROL_MEDIUM;
		} else if (difficulty == 6) {
			size = FleetSize.LARGE;
			quality = FleetQuality.DEFAULT;
			oQuality = OfficerQuality.DEFAULT;
			oNum = OfficerNum.DEFAULT;
			type = FleetTypes.PATROL_LARGE;
		} else if (difficulty == 7) {
			size = FleetSize.LARGE;
			quality = FleetQuality.HIGHER;
			oQuality = OfficerQuality.DEFAULT;
			oNum = OfficerNum.MORE;
			type = FleetTypes.PATROL_LARGE;
		} else if (difficulty == 8) {
			size = FleetSize.VERY_LARGE;
			quality = FleetQuality.HIGHER;
			oQuality = OfficerQuality.DEFAULT;
			oNum = OfficerNum.MORE;
			type = FleetTypes.PATROL_LARGE;
		} else if (difficulty == 9) {
			size = FleetSize.HUGE;
			quality = FleetQuality.HIGHER;
			oQuality = OfficerQuality.HIGHER;
			oNum = OfficerNum.MORE;
			type = FleetTypes.PATROL_LARGE;
		} else {
			size = FleetSize.MAXIMUM;
			quality = FleetQuality.HIGHER;
			oQuality = OfficerQuality.HIGHER;
			oNum = OfficerNum.MORE;
			type = FleetTypes.PATROL_LARGE;
		}
		
		beginFleet(mission, data);
		mission.triggerCreateFleet(size, quality, SotfIDs.DUSTKEEPERS_BURNOUTS, type, data.system);
		mission.triggerSetFleetOfficers(oNum, oQuality);
		mission.triggerAutoAdjustFleetSize(size, size.next());
		mission.triggerFleetSetShipPickMode(ShipPickMode.PRIORITY_THEN_ALL);
		
		//mission.triggerFleetSetNoFactionInName();
		//mission.triggerFleetSetName("Rogue Splinter");
		
		mission.triggerMakeHostileAndAggressive();
		mission.triggerMakeNoRepImpact();
		mission.triggerPickLocationAtInSystemJumpPoint(data.system);
		mission.triggerSpawnFleetAtPickedLocation(null, null);
		//mission.triggerOrderFleetPatrol(data.system);
		mission.triggerFleetSetPatrolActionText("wandering erratically");
		mission.triggerOrderFleetPatrol(data.system, true, Tags.JUMP_POINT, Tags.SALVAGEABLE, Tags.PLANET);
		data.fleet = createFleet(mission, data);
		if (data.fleet == null) return null;

		data.fleet.getMemoryWithoutUpdate().set("$sotf_dkcbRogueWarnet", true);
		setRepChangesBasedOnDifficulty(data, difficulty);
		data.baseReward = CBStats.getBaseBounty(difficulty, CBStats.DERELICT_MULT, mission);
		
		return data;
	}
	

	@Override
	public int getMaxDifficulty() {
		return super.getMaxDifficulty();
	}

	@Override
	public int getMinDifficulty() {
		return 4;
	}

}






