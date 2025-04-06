package data.scripts.world.mia;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.PersonalFleetScript;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

public class SotfPersonalFleetBlithe extends PersonalFleetScript {

	public SotfPersonalFleetBlithe() {
		super(SotfPeople.BLITHE);
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

		// Blithe has only a few skilled Dustkeeper officers in larger ships - isolated for a while
		FactionDoctrineAPI doctrine = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getDoctrine().clone();
		doctrine.setShipSize(5);
		doctrine.setCombatFreighterCombatUseFraction(0.65f);
		
		m.triggerCreateFleet(FleetSize.VERY_LARGE, FleetQuality.DEFAULT, SotfIDs.DUSTKEEPERS, FleetTypes.PATROL_LARGE, loc);
		m.triggerFleetSetFlagship("sotf_anamnesis_CS");
		m.triggerSetFleetOfficers(OfficerNum.DEFAULT, OfficerQuality.HIGHER);
		m.triggerSetFleetCommander(getPerson());
		m.triggerSetPatrol();
		m.triggerGetFleetParams().doctrineOverride = doctrine;
		m.triggerFleetSetNoFactionInName();
		m.triggerPatrolAllowTransponderOff();
		m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_NO_JUMP, true);
		m.triggerSetFleetMemoryValue(SotfIDs.BLITHE_FLEET, true);
		if (!Global.getSector().getMemoryWithoutUpdate().contains("$sotf_HHtOffDone")) {
			m.triggerSetFleetHasslePlayer("sotf_HHtOff");
		}
		m.triggerFleetSetName("Blithe's Neutron Wyrms");
		//m.triggerFleetSetPatrolActionText("patrolling");
		m.triggerOrderFleetPatrol(holdout.getStarSystem());
		
		CampaignFleetAPI fleet = m.createFleet();

		// give Blithe a proper skills list (otherwise would be default Alpha skills)
		SotfMisc.reassignAICoreSkills(SotfPeople.getPerson(SotfPeople.BLITHE), fleet.getFlagship(), fleet, Misc.random);

		int shipIndex = 1;
		if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.BLITHE_FLEET + "_timesKilled")) {
			shipIndex += Global.getSector().getMemoryWithoutUpdate().getInt(SotfIDs.BLITHE_FLEET + "_timesKilled");
		}
		if (shipIndex == 14) {
			shipIndex = 15;
		}
		// Even now you mark my steps~
		// Lovely, bitter water~
		// All the days of our delights~
		// Are poison in my veins~
		String shipName = "ODS Bitter Water";
		if (shipIndex > 1) {
			shipName += " " + Global.getSettings().getRoman(shipIndex);
		}
		fleet.getFleetData().ensureHasFlagship();
		fleet.getFlagship().setShipName(shipName);
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
		if (fleet.getFlagship() != null && fleet.getFlagship().getCaptain() == getPerson()) return;

		Misc.giveStandardReturnToSourceAssignments(fleet);
	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
		super.reportFleetDespawnedToListener(fleet, reason, param);
		if (reason == CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE) {
			if (!Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.BLITHE_FLEET + "_timesKilled")) {
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.BLITHE_FLEET + "_timesKilled", 1);
			} else {
				int timesKilled = Global.getSector().getMemoryWithoutUpdate().getInt(SotfIDs.BLITHE_FLEET + "_timesKilled");
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.BLITHE_FLEET + "_timesKilled", timesKilled + 1);
			}
		}
	}

	@Override
	public boolean canSpawnFleetNow() {
		MarketAPI holdout = Global.getSector().getEconomy().getMarket("sotf_holdout_market");
		if (holdout == null) return false;
		return true;
	}

	@Override
	public boolean shouldScriptBeRemoved() {
		return false;
	}

}