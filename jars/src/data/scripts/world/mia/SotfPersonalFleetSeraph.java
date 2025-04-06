package data.scripts.world.mia;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.PersonalFleetScript;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import org.lwjgl.util.vector.Vector2f;

public class SotfPersonalFleetSeraph extends PersonalFleetScript {

	public int pointsAtSpawn = 0;

	public SotfPersonalFleetSeraph() {
		super(SotfPeople.SERAPH);
		setMinRespawnDelayDays(20f);
		setMaxRespawnDelayDays(30f);
	}

	@Override
	protected MarketAPI getSourceMarket() {
		return Global.getSector().getEconomy().getMarket("sotf_holdout_market");
	}

	@Override
	public CampaignFleetAPI spawnFleet() {
		MarketAPI holdout = getSourceMarket();
		SectorEntityToken hallowhall = holdout.getStarSystem().getEntityById("sotf_hallowhall");

		if (hallowhall == null) return null;

		FleetCreatorMission m = new FleetCreatorMission(random);
		m.beginFleet();
		
		Vector2f loc = holdout.getLocationInHyperspace();

		// just Seraph and a bunch of proxies
		m.triggerCreateFleet(FleetSize.HUGE, FleetQuality.DEFAULT, SotfIDs.DUSTKEEPERS_PROXIES, FleetTypes.PATROL_LARGE, loc);
		m.triggerFleetSetFlagship("sotf_respite_Assault");
		m.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.DEFAULT);
		m.triggerSetFleetCommander(getPerson());
		m.triggerSetFleetFaction(SotfIDs.DUSTKEEPERS);
		m.triggerSetPatrol();
		m.triggerFleetSetNoFactionInName();
		m.triggerPatrolAllowTransponderOff();
		m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_NO_JUMP, true);
		m.triggerSetFleetMemoryValue(SotfIDs.SERAPH_FLEET, true);
		if (!Global.getSector().getMemoryWithoutUpdate().contains("$sotf_HHtOffDone")) {
			m.triggerSetFleetHasslePlayer("sotf_HHtOff");
		}
		m.triggerFleetSetName("Seraph's Astral Castellans");
		m.triggerFleetSetPatrolActionText("defending");
		m.triggerOrderFleetPatrol(hallowhall);
		m.triggerFleetSetPatrolLeashRange(1000f);
		
		CampaignFleetAPI fleet = m.createFleet();
		pointsAtSpawn = fleet.getFleetPoints();
		int shipIndex = 3;
		if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.SERAPH_FLEET + "_timesKilled")) {
			shipIndex += Global.getSector().getMemoryWithoutUpdate().getInt(SotfIDs.SERAPH_FLEET + "_timesKilled");
		}
		// unlikely to crop up, but...
		if (shipIndex == 14) {
			shipIndex = 15;
		}
		fleet.getFleetData().ensureHasFlagship();
		fleet.getFlagship().setShipName("ODS Last Light " + Global.getSettings().getRoman(shipIndex));
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SOURCE_MARKET, "sotf_holdout_market");
		fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
		holdout.getContainingLocation().addEntity(fleet);
		fleet.setLocation(holdout.getPrimaryEntity().getLocation().x, holdout.getPrimaryEntity().getLocation().y);
		fleet.setFacing((float) random.nextFloat() * 360f);
		
		return fleet;
	}

	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (fleet == null) return;
		if (fleet.getFleetPoints() < (pointsAtSpawn * 0.5f)) {
			Misc.giveStandardReturnToSourceAssignments(fleet);
		}
	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
		super.reportFleetDespawnedToListener(fleet, reason, param);
		if (reason == CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE) {
			if (!Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.SERAPH_FLEET + "_timesKilled")) {
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.SERAPH_FLEET + "_timesKilled", 1);
			} else {
				int timesKilled = Global.getSector().getMemoryWithoutUpdate().getInt(SotfIDs.SERAPH_FLEET + "_timesKilled");
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.SERAPH_FLEET + "_timesKilled", timesKilled + 1);
			}
		}
	}

	@Override
	public boolean canSpawnFleetNow() {
		if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_recruitedSeraph")) return false;
		MarketAPI holdout = Global.getSector().getEconomy().getMarket("sotf_holdout_market");
		if (holdout == null) return false;
		return true;
	}

	@Override
	public boolean shouldScriptBeRemoved() {
		return false;
	}

}




