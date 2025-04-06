package data.scripts.campaign.intel.quests;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ghosts.BaseSensorGhost;
import com.fs.starfarer.api.impl.campaign.ghosts.GBDartAround;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.plugins.waywardstar.SotfWSCheckScript;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;
import java.util.Set;

import static com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.computeSystemData;

/**
 *	Intel report and system generation for Wayward Star
 */

public class SotfWaywardStarIntel extends BaseIntelPlugin {

	protected StarSystemAPI system;

	protected static Random random = new Random();
	
	public SotfWaywardStarIntel() {
		SectorAPI sector = Global.getSector();
		system = sector.createStarSystem("Wayward Star");
		system.setOptionalUniqueId("sotf_waywardstar");
		system.setName("Wayward Star System");
		system.getMemoryWithoutUpdate().set("$sotf_waywardstar", true);
		system.addTag(Tags.THEME_HIDDEN);
        pickLocation(sector, system);

		Global.getSector().getMemoryWithoutUpdate().set("$sotf_waywardstar", system);

		system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_campaign_alpha_site");
		system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");

		// create the star
		PlanetAPI star = system.initStar("sotf_waywardstar", // unique id for this star
				StarTypes.BLUE_GIANT,  // id in planets.json
				1100f, // radius (in pixels at default zoom)
                system.getLocation().x,
				system.getLocation().y,
				200); // corona radius, from star edge
		system.setLightColor(new Color(75, 75, 255)); // light color in entire system, affects all entities

		// get rid of the hyperspace around the star
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);

		float minRadius = plugin.getTileSize() * 2f;
		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

		// add the creepy eye of the storm
		SectorEntityToken storms = Misc.addNebulaFromPNG("data/campaign/terrain/sotf_waywardstar_storms.png",
				star.getLocation().x, star.getLocation().y,
				system,
				"terrain", "deep_hyperspace",
				4, 4, "hyperspace", StarAge.OLD);

		PlanetAPI giant1 = system.addPlanet("sotf_waywardstar_giant1", star, "I", "gas_giant", 235, 200, 3500, 100);
		PlanetAPI volcanic1 = system.addPlanet("sotf_waywardstar_volcanic1", giant1, "I Alpha", "lava_minor", 135, 50, 400, 20);
		PlanetAPI barren1 = system.addPlanet("sotf_waywardstar_barren1", giant1, "I Beta", "barren", 90, 70, 700, 30);

		system.addRingBand(giant1, "misc", "rings_dust0", 256f, 1, Color.white, 126f, 1000, 20f, null, null);
		system.addRingBand(star, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 1500, 200f, null, null);
		system.addRingBand(star, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 2000, 220f, null, null);
		system.addRingBand(star, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 2500, 240f, null, null);

		system.addAsteroidBelt(star, 40, 4600, 100, 30, 40, Terrain.ASTEROID_BELT, null);

		BaseThemeGenerator.StarSystemData data = computeSystemData(system);
		SectorEntityToken station = BaseThemeGenerator.addSalvageEntity(system, Entities.STATION_RESEARCH_REMNANT, Factions.NEUTRAL);

		// add some volatiles for player Neutrino (because nobody thinks to use Distress Call apparently :P)
		CargoAPI extraVolatiles = Global.getFactory().createCargo(true);
		extraVolatiles.addCommodity(Commodities.VOLATILES, 8 + (random.nextInt(4)));
		BaseSalvageSpecial.addExtraSalvage(station, extraVolatiles);

		station.setCircularOrbit(giant1, 160, 600f, 60);
		// sensor ghosts
		for (int i = 0; i < 16; i++) {
			BaseSensorGhost g = new BaseSensorGhost(null, 0);
			g.initEntity(g.genMediumSensorProfile(), g.genSmallRadius(), 0, system);
			g.addBehavior(new GBDartAround(star, 1500f, 16 + Misc.random.nextInt(4), star.getRadius() + 800f, 6000));
			g.setDespawnRange(-1500f);
			g.getEntity().addTag("sotf_AMDancingGhost");
			g.setLoc(Misc.getPointAtRadius(star.getLocation(), 1200f));
			//g.placeNearEntity(tia.getHyperspaceAnchor(), 800, 3200);
			system.addScript(g);
		}

		//RuinsFleetRouteManager fleets = new RuinsFleetRouteManager(system);
		//system.addScript(fleets);

		// script that triggers the Eidolon fight on using Distress Call or Neutrino Detector
		sector.addScript(new SotfWSCheckScript());

		system.autogenerateHyperspaceJumpPoints(false, true, true);
	}

	private static float getRandom(float min, float max) {
		float radius = min + (max - min) * random.nextFloat();
		return radius;
	}

	/**
	 * Adds Reverie's Meal to a provided system, ideally the Wayward Star
	 * @param system
	 */
	public static void addReveriesMeal(StarSystemAPI system) {
		SectorEntityToken barren = system.getEntityById("sotf_waywardstar_barren1");

		if (barren == null) barren = system.getPlanets().get(0);
		if (barren == null) return;

		SectorEntityToken fragment = BaseThemeGenerator.addSalvageEntity(system, "sotf_eradfragment", Factions.NEUTRAL);
		fragment.setCircularOrbit(barren, 120f, barren.getRadius() + 50f, 20);
		fragment.getMemoryWithoutUpdate().set(SotfIDs.MEM_REVERIES_MEAL, true);
	}

	private static void pickLocation(SectorAPI sector, StarSystemAPI system) {
		float radius = system.getMaxRadiusInHyperspace() + 200f;
		try_again:
		for (int i = 0; i < 100; i++) {
			Vector2f loc = new Vector2f(getRandom(30000f, 40000f), 0f);
			VectorUtils.rotate(loc, getRandom(0f, 360f), loc);

			for (LocationAPI location : sector.getAllLocations()) {
				if (location instanceof StarSystemAPI) {
					float otherRadius = ((StarSystemAPI) location).getMaxRadiusInHyperspace();
					if (MathUtils.getDistance(location.getLocation(), loc) < radius + otherRadius) {
						continue try_again;
					}
				}
			}

			system.getLocation().set(loc.x, loc.y);
			break;
		}
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
		float pad = 3f;
		float opad = 10f;
		
		addBulletPoints(info, ListInfoMode.IN_DESC);
		
		info.addPara("Communications among explorers, scavengers and prospectors in " +
				"the Sector have become recently focused on an unusual occurrence - " +
				"the recent discovery of star system that exists on no records, " +
				"with several relatively trustworthy sources stating that its current " +
				"location was known to be empty hyperspace. ", opad);
		info.addPara("Reports are confused and conflicted. If you want a clear answer, " +
				"the only choice would be to head to its location and investigate yourself. ", opad);
	}
	
	@Override
	public String getIcon() {
		return Global.getSettings().getSpriteName("intel", "sotf_waywardstar");
	}
	
	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_STORY);
		tags.add(Tags.INTEL_EXPLORATION);
		return tags;
	}
	
	public String getSortString() {
		return "The Wayward Star";
	}
	
	public String getName() {
		return "The Wayward Star";
	}
	
	@Override
	public FactionAPI getFactionForUIColors() {
		return super.getFactionForUIColors();
	}

	public String getSmallDescriptionTitle() {
		return getName();
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return system.getStar();
	}

	@Override
	public String getCommMessageSound() {
		return getSoundMajorPosting();
	}
	
}