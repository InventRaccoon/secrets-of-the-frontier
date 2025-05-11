package data.scripts.campaign.missions.cb;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBStats;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import data.scripts.campaign.ids.SotfIDs;

import static data.scripts.SotfModPlugin.WATCHER;

/**
 *	Regular faction custom bounties vs Dustkeepers (player is not hostile to Dustkeepers edition)
 */

public class SotfCBDustkeeperBurnout extends BaseCustomBountyCreator {
	
	@Override
	public float getFrequency(HubMissionWithBarEvent mission, int difficulty) {
		if (!WATCHER) return 0;
		// only if player is not hostile to Dustkeepers
		if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).isAtBest(Factions.PLAYER, RepLevel.HOSTILE)) {
			return 0;
		}
		// faction must dislike the Dustkeepers, e.g core worlds who ban AI
		else if (mission.getPerson().getFaction().isAtWorst(SotfIDs.DUSTKEEPERS, RepLevel.SUSPICIOUS)) {
			return 0;
		}
		return super.getFrequency(mission, difficulty) * CBStats.REMNANT_FREQ;
	}

	public String getBountyNamePostfix(HubMissionWithBarEvent mission, CustomBountyData data) {
		return " - Dustkeeper Warfleet";
	}
	
	@Override
	public String getIconName() {
		return Global.getSettings().getSpriteName("campaignMissions", "remnant_bounty");
	}
	
	@Override
	public CustomBountyData createBounty(MarketAPI createdAt, HubMissionWithBarEvent mission, int difficulty, Object bountyStage) {
		CustomBountyData data = new CustomBountyData();
		data.difficulty = difficulty;
		
		//mission.requireSystem(this);
		mission.requireSystemTags(ReqMode.NOT_ANY, Tags.THEME_CORE);
		mission.requireSystemTags(ReqMode.NOT_ANY, Tags.THEME_REMNANT);
//		mission.requireSystemTags(ReqMode.ANY, Tags.THEME_RUINS, Tags.THEME_MISC, Tags.THEME_REMNANT,
//				  Tags.THEME_DERELICT, Tags.THEME_REMNANT_DESTROYED);
		mission.preferSystemInteresting();
		mission.preferSystemUnexplored();
		mission.requireSystemNotHasPulsar();
		//mission.preferSystemBlackHoleOrNebula();
		mission.preferSystemOnFringeOfSector();
		
		StarSystemAPI system = mission.pickSystem();
		data.system = system;

		FleetSize size = FleetSize.MEDIUM;
		FleetQuality quality = FleetQuality.DEFAULT;
		String type = FleetTypes.PATROL_MEDIUM;
		OfficerQuality oQuality = OfficerQuality.DEFAULT;
		OfficerNum oNum = OfficerNum.DEFAULT;

		if (difficulty == 7) {
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
			oQuality = OfficerQuality.UNUSUALLY_HIGH;
			oNum = OfficerNum.MORE;
			type = FleetTypes.PATROL_LARGE;
		} else {
			size = FleetSize.HUGE;
			quality = FleetQuality.HIGHER;
			oQuality = OfficerQuality.UNUSUALLY_HIGH;
			oNum = OfficerNum.ALL_SHIPS;
			type = FleetTypes.PATROL_LARGE;
		}

		beginFleet(mission, data);
		mission.triggerCreateFleet(size, quality, SotfIDs.DUSTKEEPERS_BURNOUTS, type, data.system);
		mission.triggerSetFleetOfficers(oNum, oQuality);
		mission.triggerAutoAdjustFleetSize(size, size.next());
		mission.triggerFleetSetShipPickMode(FactionAPI.ShipPickMode.PRIORITY_THEN_ALL);

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
		data.baseReward = CBStats.getBaseBounty(difficulty, CBStats.REMNANT_MULT, mission);
		
		return data;
	}
	

	@Override
	public int getMaxDifficulty() {
		return super.getMaxDifficulty();
	}

	@Override
	public int getMinDifficulty() {
		return 7;
	}

}



