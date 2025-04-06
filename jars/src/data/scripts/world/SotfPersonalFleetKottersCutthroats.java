package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.PersonalFleetScript;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
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

public class SotfPersonalFleetKottersCutthroats extends PersonalFleetScript {

	public SotfPersonalFleetKottersCutthroats() {
		super(SotfPeople.KOTTER);
		setMinRespawnDelayDays(40f);
		setMaxRespawnDelayDays(60f);
	}

	@Override
	protected MarketAPI getSourceMarket() {
		return Global.getSector().getEconomy().getMarket("umbra");
	}

	@Override
	public CampaignFleetAPI spawnFleet() {
		MarketAPI umbra = getSourceMarket();

		if (umbra == null) return null;

		FleetCreatorMission m = new FleetCreatorMission(random);
		m.beginFleet();
		
		Vector2f loc = umbra.getLocationInHyperspace();

		m.triggerCreateFleet(FleetSize.TINY, FleetQuality.DEFAULT, Factions.PIRATES, FleetTypes.PATROL_SMALL, loc);
		m.triggerSetFleetCommander(getPerson());
		m.triggerSetPatrol();
		m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, umbra);
		m.triggerSetFleetMemoryValue(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
		m.triggerSetPirateFleet();
		m.triggerFleetSetNoFactionInName();
		m.triggerMakeNonHostile();
		m.triggerPatrolAllowTransponderOff();
		m.triggerSetFleetMemoryValue(SotfIDs.KOTTERS_FLEET, true);
		m.triggerFleetSetName("Kotter's Cutthroats");
		m.triggerFleetSetPatrolActionText("looking for easy pickings");
		m.triggerOrderFleetPatrol(umbra.getStarSystem());

		// *giggling*
		m.triggerFleetSetFlagship("hound_d_pirates_Shielded");
		m.triggerGetFleetParams().onlyRetainFlagship = true;
		CampaignFleetAPI fleet = m.createFleet();
		fleet.getFleetData().addFleetMember("kite_pirates_Raider");

		fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
		umbra.getContainingLocation().addEntity(fleet);
		fleet.setLocation(umbra.getPlanetEntity().getLocation().x, umbra.getPlanetEntity().getLocation().y);
		fleet.setFacing((float) random.nextFloat() * 360f);
		
		return fleet;
	}

	@Override
	public boolean canSpawnFleetNow() {
		MarketAPI umbra = Global.getSector().getEconomy().getMarket("umbra");
		if (umbra == null || umbra.hasCondition(Conditions.DECIVILIZED)) return false;
		return umbra.getFactionId().equals(Factions.PIRATES);
	}

	@Override
	public boolean shouldScriptBeRemoved() {
		return false;
	}

}




