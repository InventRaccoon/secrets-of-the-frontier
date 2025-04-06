package data.scripts.world.mia;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator;
import com.fs.starfarer.api.impl.campaign.shared.WormholeManager;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.LinkedHashMap;

import static com.fs.starfarer.api.impl.campaign.ids.FleetTypes.PATROL_LARGE;
import static com.fs.starfarer.api.util.Misc.random;

public class SotfMiaSystem {

	public void generate(SectorAPI sector) {

		// make the system itself!
		StarSystemAPI system = sector.createStarSystem("Mia's Star");
		system.setOptionalUniqueId("sotf_mia");
		system.addTag(SotfIDs.THEME_DUSTKEEPERS);
		LocationAPI hyper = Global.getSector().getHyperspace();

		// pick somewhere clear leftish of the core
		pickLocation(Global.getSector(), system);

		system.setBackgroundTextureFilename("graphics/backgrounds/background6.jpg");
		// no quests pls except this one
		system.addTag(Tags.THEME_UNSAFE);
		//system.addTag(Tags.THEME_HIDDEN);

		// create the star Mia, named after my cat
		PlanetAPI star = system.initStar("sotf_mia", // unique id for this star
				StarTypes.YELLOW,  // id in planets.json
				1200f,          // radius (in pixels at default zoom)
				-32600f,
				2500f,
				850); // corona radius, from star edge
		star.setName("Mia's Star");
		system.setLightColor(new Color(255, 225, 170)); // light color in entire system, affects all entities

		// delete surrounding hyperspace
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);

		float minRadius = plugin.getTileSize() * 2f;
		float radius = system.getMaxRadiusInHyperspace() * 4f;
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.65f, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

		FactionAPI dustkeepers = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS);

		// Beacon, triple ping because Seraph is dramatic
		CustomCampaignEntityAPI beacon = RemnantThemeGenerator.addBeacon(system, RemnantThemeGenerator.RemnantSystemType.RESURGENT);
		Misc.setWarningBeaconColors(beacon, dustkeepers.getBaseUIColor(), dustkeepers.getBaseUIColor());
		beacon.getMemoryWithoutUpdate().set(SotfIDs.MIA_BEACON, true);
		sector.getMemoryWithoutUpdate().set(SotfIDs.MIA_BEACON, beacon);

		String desecratorsType = Planets.BARREN;
		if (Misc.random.nextFloat() < 0.15f) {
			desecratorsType = Planets.PLANET_LAVA;
		}

		PlanetAPI planet1 = system.addPlanet("sotf_desecrators", star, "Desecrator's Rest", desecratorsType, 60, 210, 2900f, 120);

		String hallowhallType = Planets.PLANET_TERRAN_ECCENTRIC;
		if (Misc.random.nextFloat() < 0.15f) {
			hallowhallType = Planets.PLANET_WATER;
		}

		system.addAsteroidBelt(star, 90, 3850, 150, 100, 110, Terrain.ASTEROID_BELT, "Winter Coat");
		system.addRingBand(star, "misc", "rings_dust0", 256f, 0, Color.white, 256f, 3850, 110);

		PlanetAPI planet2 = system.addPlanet("sotf_hallowhall", star, "Hallowhall", hallowhallType, 125, 160, 4500f, 155);
		planet2.getMarket().addCondition(Conditions.HABITABLE);
		planet2.getMarket().addCondition(Conditions.RUINS_EXTENSIVE);
		planet2.getMarket().addCondition(Conditions.DECIVILIZED);
		if (hallowhallType.equals(Planets.PLANET_TERRAN_ECCENTRIC)) {
			planet2.getMarket().addCondition(Conditions.TECTONIC_ACTIVITY);
			planet2.getMarket().addCondition(Conditions.POOR_LIGHT);
			planet2.getMarket().addCondition(Conditions.FARMLAND_RICH);
			planet2.getMarket().addCondition(Conditions.ORE_MODERATE);
			planet2.getMarket().addCondition(Conditions.ORGANICS_COMMON);
		} else if (hallowhallType.equals(Planets.PLANET_WATER)) {
			planet2.getMarket().addCondition(Conditions.WATER_SURFACE);
			planet2.getMarket().addCondition(Conditions.ORGANICS_PLENTIFUL);
		}

		SectorEntityToken astropolis = system.addCustomEntity("sotf_hallowhall_astropolis",
				"Abandoned Astropolis", "station_side06", Factions.NEUTRAL);

		astropolis.setCircularOrbitPointingDown(planet2, 45, 300, 50);

		Misc.setAbandonedStationMarket("sotf_hallow_astropolis_market", astropolis);
		astropolis.setCustomDescriptionId("sotf_hallow_astropolis");
		astropolis.setInteractionImage("illustrations", "abandoned_station3");

		PlanetAPI gasgiant = system.addPlanet("sotf_gorslittlelie", star, "Gor's Little Lie", "gas_giant", 240, 550, 6100f, 170);
		gasgiant.getMarket().addCondition(Conditions.VOLATILES_ABUNDANT);
		gasgiant.getMarket().addCondition(Conditions.HIGH_GRAVITY);

		system.addAsteroidBelt(gasgiant, 50, 850, 120, 90, 110, Terrain.ASTEROID_BELT, "Veil of Half-Truths");
		system.addRingBand(gasgiant, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 725, 80f);
		system.addRingBand(gasgiant, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 800, 80f);

		//
		// BEGIN FORGESHIP
		//

		SectorEntityToken station = system.addCustomEntity("sotf_holdout", "Holdout Forgeship", "sotf_forgeship", SotfIDs.DUSTKEEPERS);
		station.setCircularOrbitPointingDown(system.getEntityById("sotf_gorslittlelie"), 45, 650, 50);
		station.setSensorProfile(1f);
		station.setDiscoverable(true);
		station.getDetectedRangeMod().modifyFlat("gen", 5000f);

		MarketAPI market = Global.getFactory().createMarket("sotf_holdout_market", station.getName(), 3);
		market.setFactionId(SotfIDs.DUSTKEEPERS);
		market.setHidden(true);

		market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
		market.setPrimaryEntity(station);

		market.addIndustry(SotfIDs.CENTRAL_PROCESSING);
		market.addIndustry(Industries.SPACEPORT);
		market.addIndustry(Industries.WAYSTATION);
		market.addIndustry(Industries.HEAVYINDUSTRY);
		market.addIndustry(Industries.HEAVYBATTERIES);

		market.addSubmarket(SotfIDs.FORGESHIP_MARKET);
		market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
		market.addSubmarket(Submarkets.LOCAL_RESOURCES);

		market.getMemoryWithoutUpdate().set("$noBar", true);

		// forgeship submarket has no tariff tho
		market.getTariff().modifyFlat("default_tariff", market.getFaction().getTariffFraction());

		station.setMarket(market);

		market.setEconGroup(market.getFactionId());
		market.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true);
		market.addTag(Tags.MARKET_NO_OFFICER_SPAWN); // otherwise we get "human" Dustkeeper officers/admins

		market.reapplyIndustries();

		Global.getSector().getEconomy().addMarket(market, false);

		market.removeSubmarket(Submarkets.GENERIC_MILITARY);
		StoragePlugin storage = (StoragePlugin)market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin();
		if (storage != null) {
			storage.setPlayerPaidToUnlock(true);
		}

		CommDirectoryAPI directory = market.getCommDirectory();
		directory.addPerson(SotfPeople.getPerson(SotfPeople.CERULEAN));
		directory.addPerson(SotfPeople.getPerson(SotfPeople.INADVERTENT));

		SotfPeople.getPerson(SotfPeople.CERULEAN).setMarket(market);
		SotfPeople.getPerson(SotfPeople.INADVERTENT).setMarket(market);

		// put in Cerulean as the admin
		market.setAdmin(SotfPeople.getPerson(SotfPeople.CERULEAN));

		// need a script to automatically fulfil Holdout's demand (akin to pirate bases' "Brought in by raiders")
		Global.getSector().getEconomy().addUpdateListener(new SotfHoldoutDemandNegator(market));
		// script to spawn fleets from Holdout
		system.addScript(new SotfHoldoutProxyFleetManager(market));

		//
		// END FORGESHIP
		//

		system.addAsteroidBelt(star, 120, 7800, 150, 100, 110, Terrain.ASTEROID_BELT, "Outer Coat");
		system.addRingBand(star, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 7800, 110);

		system.addRingBand(star, "misc", "rings_ice0", 256f, 2, Color.white, 256f, 8500, 110, Terrain.RING, "Collar");

		StarSystemGenerator.addSystemwideNebula(system, StarAge.OLD);

		// matches Gor's Little Lie (gas giant)
		SectorEntityToken loc1 = system.addCustomEntity(null,null, "comm_relay", SotfIDs.DUSTKEEPERS);
		loc1.getMemoryWithoutUpdate().set("$sotf_miaRelay", true);
		loc1.setCircularOrbitPointingDown(star, 50 + 60, 6400, 170);

		// matches Desecrator's Rest (barren)
		SectorEntityToken loc2 = system.addCustomEntity(null,null, "nav_buoy_makeshift",SotfIDs.DUSTKEEPERS);
		loc2.setCircularOrbitPointingDown(star, 50 + 130, 3250, 140);

		if (Misc.random.nextFloat() < 0.2f) {
			LinkedHashMap<BaseThemeGenerator.LocationType, Float> weights = new LinkedHashMap<BaseThemeGenerator.LocationType, Float>();
			weights.put(BaseThemeGenerator.LocationType.OUTER_SYSTEM, 10f);
			weights.put(BaseThemeGenerator.LocationType.STAR_ORBIT, 10f);
			WeightedRandomPicker<BaseThemeGenerator.EntityLocation> locs = BaseThemeGenerator.getLocations(random, system, null, 100f, weights);
			BaseThemeGenerator.EntityLocation loc = locs.pick();

			BaseThemeGenerator.AddedEntity added = BaseThemeGenerator.addNonSalvageEntity(system, loc, Entities.INACTIVE_GATE, Factions.NEUTRAL);

			if (added != null) {
				BaseThemeGenerator.convertOrbitPointingDown(added.entity);
			}
		} else {
			StarSystemGenerator.addStableLocations(system, 1);
		}

		// script to share comm relay's stability boost with the player
		system.addScript(new SotfDustkeeperObjectiveShareScript(system));

		// autogenerate jump points
		system.autogenerateHyperspaceJumpPoints(true, true);

		Global.getSector().addScript(new SotfPersonalFleetSeraph());
		Global.getSector().addScript(new SotfPersonalFleetBlithe());
	}

	private static float getRandom(float min, float max) {
		return min + (max - min) * random.nextFloat();
	}

	private static void pickLocation(SectorAPI sector, StarSystemAPI system) {
		float radius = system.getMaxRadiusInHyperspace() + 200f;
		try_again:
		for (int i = 0; i < 100; i++) {
			Vector2f loc = new Vector2f(getRandom(46500f, 54500f), 0f);
			VectorUtils.rotate(loc, getRandom(180, 240f), loc);

			for (StarSystemAPI location : sector.getStarSystems()) {
				float otherRadius = location.getMaxRadiusInHyperspace();
				if (MathUtils.getDistance(location.getLocation(), loc) < radius + otherRadius) {
					continue try_again;
				}
			}

			system.getLocation().set(loc.x, loc.y);
			break;
		}
	}
}
