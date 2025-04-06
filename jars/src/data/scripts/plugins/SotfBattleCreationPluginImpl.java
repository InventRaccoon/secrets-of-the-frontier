package data.scripts.plugins;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAbyssPluginImpl;
import com.fs.starfarer.api.impl.combat.EscapeRevealPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.Misc;

import static data.scripts.SotfModPlugin.OBJECTIVE_CSV;

/**
 *	Titanic replacement for the code that generates battles
 *	Features: more planets, jumppoints in BGs, more objectives
 *	Also: battle objectives picked based on where you're fighting
 */

public class SotfBattleCreationPluginImpl implements BattleCreationPlugin {

	public interface NebulaTextureProvider {
		String getNebulaTex();
		String getNebulaMapTex();
	}

	private static final float ASTEROID_MAX_DIST = 750f;
	public static Logger log = Global.getLogger(SotfBattleCreationPluginImpl.class);

	public static float ABYSS_SHIP_SPEED_PENALTY = 20f;
	public static float ABYSS_MISSILE_SPEED_PENALTY = 20f;
	//public static float ABYSS_MISSILE_FLIGHT_TIME_MULT = 1.25f;
	public static float ABYSS_OVERLAY_ALPHA = 0.2f;

	private float width, height;
	private float sizeMod;

	private int maxFPForObj = (int) Global.getSettings().getFloat("maxNoObjectiveBattleSize");
	private int fpOne = 0;
	private int fpTwo = 0;
	private int fpBoth;

	private float xPad = 2000;
	private float yPad = 2000;

	private float prevXDir = 0;
	private float prevYDir = 0;
	private boolean escape;

	private BattleCreationContext context;
	private MissionDefinitionAPI loader;
	private int nebulaLevel;

	private Random random;

	public void initBattle(final BattleCreationContext context, MissionDefinitionAPI loader) {

		this.context = context;
		this.loader = loader;
		CampaignFleetAPI playerFleet = context.getPlayerFleet();
		CampaignFleetAPI otherFleet = context.getOtherFleet();
		FleetGoal playerGoal = context.getPlayerGoal();
		FleetGoal enemyGoal = context.getOtherGoal();

		// Seeding objective layouts so they're identical if you savescum or retreatscum
		random = Misc.getRandom(Misc.getSalvageSeed(otherFleet) *
				(long)otherFleet.getFleetData().getNumMembers(), 23);

		escape = playerGoal == FleetGoal.ESCAPE || enemyGoal == FleetGoal.ESCAPE;

		for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
			if (member.canBeDeployedForCombat() || playerGoal == FleetGoal.ESCAPE) {
				fpOne += member.getUnmodifiedDeploymentPointsCost();
			}
		}
		for (FleetMemberAPI member : otherFleet.getFleetData().getMembersListCopy()) {
			if (member.canBeDeployedForCombat() || playerGoal == FleetGoal.ESCAPE) {
				fpTwo += member.getUnmodifiedDeploymentPointsCost();
			}
		}

		fpBoth = fpOne + fpTwo;

		int smaller = Math.min(fpOne, fpTwo);

		boolean withObjectives = smaller > maxFPForObj;
		if (!context.objectivesAllowed) {
			withObjectives = false;
		} else if (context.forceObjectivesOnMap) {
			withObjectives = true;
		}

		int numObjectives = 0;
		if (withObjectives) {
			if (fpBoth > maxFPForObj + 480) {
				numObjectives = 6;
			} else if (fpBoth > maxFPForObj + 320) {
				numObjectives = 5;
			} else if (fpBoth > maxFPForObj + 180) {
				numObjectives = 4;
			} else if (fpBoth > maxFPForObj + 80) {
				numObjectives = 3;
			} else {
				numObjectives = 2;
			}
		}

		if (context.getPlayerFleet().getContainingLocation() == Global.getSector().getHyperspace()) {
			numObjectives--;
		}

		if (numObjectives > 6) {
			numObjectives = 6;
		}
		if (numObjectives > 4 && escape) {
			numObjectives = 4;
		}
		if (numObjectives == 1) {
			numObjectives = 2;
		}
		if (numObjectives < 2 && escape) {
			numObjectives = 2;
		}

		int baseCommandPoints = (int) Global.getSettings().getFloat("startingCommandPoints");

		loader.initFleet(FleetSide.PLAYER, "ISS", playerGoal, false,
				context.getPlayerCommandPoints() - baseCommandPoints,
				(int) playerFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);
		loader.initFleet(FleetSide.ENEMY, "", enemyGoal, true,
				(int) otherFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);

		List<FleetMemberAPI> playerShips = playerFleet.getFleetData().getCombatReadyMembersListCopy();
		if (playerGoal == FleetGoal.ESCAPE) {
			playerShips = playerFleet.getFleetData().getMembersListCopy();
		}
		for (FleetMemberAPI member : playerShips) {
			loader.addFleetMember(FleetSide.PLAYER, member);
		}


		List<FleetMemberAPI> enemyShips = otherFleet.getFleetData().getCombatReadyMembersListCopy();
		if (enemyGoal == FleetGoal.ESCAPE) {
			enemyShips = otherFleet.getFleetData().getMembersListCopy();
		}
		for (FleetMemberAPI member : enemyShips) {
			loader.addFleetMember(FleetSide.ENEMY, member);
		}

		// Wow! I don't know what any of this math does, and so I'm just going to... turn it all off
		//float heightMod;
		//if (context.getPlayerFleet().getContainingLocation() == Global.getSector().getHyperspace()) {
		//	heightMod = 1f + 0.5f * (float) (Math.random() * Math.random());
		//} else {
		//	heightMod = Math.min(1.2f, Math.min(1f, Math.max(0.6f, (float) Math.cbrt(getNearbyPlanetFactor(
		//			context.getPlayerFleet()) / PLANET_MAX_DIST))) *
		//			(1.25f + 0.25f * (float) Math.random()));
		//}
		//loat widthMod = 1f / (float) Math.sqrt(heightMod);

		width = 18000f;
		height = 18000f;
		//float baseHeightForEscape = height;

		if (escape) {
			// messing with escape map sizes is very questionable for balance reasons

//			if (numObjectives <= 2) {
//				width = 12000f;
//				height = 14000f;
//			} else if (numObjectives <= 4) {
//				width = 14000f;
//				height = 18000f;
//			} else {
//				width = 16000f;
//				height = 22000f;
//			}
			xPad = 2000f;
			yPad = 4000f;
		} else if (withObjectives) {
			width = 24000f;
			// SS+ had generally narrower maps - might've been squashed by planet factor?
			// idk man I'm just going to do away with it and make them more consistently wider/shorter
			// so: equal to vanilla size EXCEPT for 5/6 objective maps, which are *slightly* longer
			if (numObjectives > 4) {
				//width = 20000f;
				//height = 16000f;
				height = 20000f;
			} else if (numObjectives > 2) {
				//width = 22000f;
				height = 14000f;
			}
			xPad = 2000f;
			yPad = 3000f;
		}

		// used to multiply asteroid count
		sizeMod = (width / 18000f) * (height / 18000f);


		createMap();

		context.setInitialDeploymentBurnDuration(1.5f);
		context.setNormalDeploymentBurnDuration(6f);
		context.setEscapeDeploymentBurnDuration(1.5f);

		if (escape) {
			addObjectives(loader, numObjectives, true);

			context.setInitialEscapeRange(Global.getSettings().getFloat("escapeStartDistance"));
			context.setFlankDeploymentDistance(Global.getSettings().getFloat("escapeFlankDistance"));

			loader.addPlugin(new EscapeRevealPlugin(context));
		} else {
			if (withObjectives) {
				addObjectives(loader, numObjectives, false);
				context.setStandoffRange(height - 4500f);
			} else {
				context.setStandoffRange(6000f);
			}
		}
	}

	public void afterDefinitionLoad(final CombatEngineAPI engine) {
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		if (coronaIntensity > 0 && (corona != null || pulsar != null)) {
			String name = "Corona";
			if (pulsar != null) name = pulsar.getTerrainName();
			else if (corona != null) name = corona.getTerrainName();

			final String name2 = name;

//			CombatFleetManagerAPI manager = engine.getFleetManager(FleetSide.PLAYER);
//			for (FleetMemberAPI member : manager.getReservesCopy()) {
//			}
			final Object key1 = new Object();
			final Object key2 = new Object();
			final String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty");
			engine.addPlugin(new BaseEveryFrameCombatPlugin() {
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					engine.maintainStatusForPlayerShip(key1, icon, name2, "reduced peak time", true);
					engine.maintainStatusForPlayerShip(key2, icon, name2, "faster CR degradation", true);
				}
			});
		}

		if (abyssalDepth > 0) {
			Color color = Misc.scaleColor(Color.white, 1f - abyssalDepth);
			engine.setBackgroundColor(color);

			color = Misc.scaleAlpha(Color.black, abyssalDepth * ABYSS_OVERLAY_ALPHA);
			engine.setBackgroundGlowColor(color);
			engine.setBackgroundGlowColorNonAdditive(true);

			if (abyssalDepth > HyperspaceAbyssPluginImpl.DEPTH_THRESHOLD_FOR_NO_DUST_PARTICLES_IN_COMBAT) {
				engine.setRenderStarfield(false);
			}

			final Object key1 = new Object();
			final Object key2 = new Object();
			final String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_engine_damage");
			final String name = "Abyssal hyperspace";
			engine.addPlugin(new BaseEveryFrameCombatPlugin() {
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					String percentSpeed = "-" + (int)Math.round(ABYSS_SHIP_SPEED_PENALTY) + "%";
					String percentMissile = "-" + (int)Math.round(ABYSS_MISSILE_SPEED_PENALTY) + "%";
					engine.maintainStatusForPlayerShip(key1, icon, name, percentSpeed + " top speed", true);
					engine.maintainStatusForPlayerShip(key2, icon, name, percentMissile + " missle speed / range", true);

					String modId = "abyssal";
					float modW = -0.0f * abyssalDepth;
					float modL = -0.33f * abyssalDepth;
					float modG = -0.5f * abyssalDepth;

					for (ShipAPI curr : engine.getShips()) {
						if (curr.isHulk()) continue;

						curr.getEngineController().fadeToOtherColor(this, Color.black, null, 1f, abyssalDepth * 0.4f);
						curr.getEngineController().extendFlame(this, modL, modW, modG);

						curr.getMutableStats().getMaxSpeed().modifyMult(modId,
								1f - abyssalDepth * ABYSS_SHIP_SPEED_PENALTY * 0.01f);
						curr.getMutableStats().getMissileWeaponRangeBonus().modifyMult(modId,
								1f - abyssalDepth * ABYSS_MISSILE_SPEED_PENALTY * 0.01f);
						curr.getMutableStats().getMissileMaxSpeedBonus().modifyMult(modId,
								1f - abyssalDepth * ABYSS_MISSILE_SPEED_PENALTY * 0.01f);
					}

					for (MissileAPI missile : engine.getMissiles()) {
						missile.getEngineController().fadeToOtherColor(this, Color.black, null, 1f, abyssalDepth * 0.4f);
						missile.getEngineController().extendFlame(this, modL, modW, 0f);
					}

				}
			});

		}
	}

	protected float abyssalDepth = 0f;
	private float coronaIntensity = 0f;
	private StarCoronaTerrainPlugin corona = null;
	private PulsarBeamTerrainPlugin pulsar = null;
	private void createMap() {
		loader.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

		CampaignFleetAPI playerFleet = context.getPlayerFleet();
		String nebulaTex = null;
		String nebulaMapTex = null;
		boolean inNebula = false;

		abyssalDepth = Misc.getAbyssalDepth(playerFleet);

		float numRings = 0;

		Color coronaColor = null;
		// this assumes that all nebula in a system are of the same color
		for (CampaignTerrainAPI terrain : playerFleet.getContainingLocation().getTerrainCopy()) {
			if (terrain.getPlugin() instanceof NebulaTextureProvider) {
				if (terrain.getPlugin().containsEntity(playerFleet)) {
					inNebula = true;
					if (terrain.getPlugin() instanceof NebulaTextureProvider) {
						NebulaTextureProvider provider = (NebulaTextureProvider) terrain.getPlugin();
						nebulaTex = provider.getNebulaTex();
						nebulaMapTex = provider.getNebulaMapTex();
					}
				} else {
					if (nebulaTex == null) {
						if (terrain.getPlugin() instanceof NebulaTextureProvider) {
							NebulaTextureProvider provider = (NebulaTextureProvider) terrain.getPlugin();
							nebulaTex = provider.getNebulaTex();
							nebulaMapTex = provider.getNebulaMapTex();
						}
					}
				}
			} else if (terrain.getPlugin() instanceof StarCoronaTerrainPlugin && pulsar == null) {
				StarCoronaTerrainPlugin plugin = (StarCoronaTerrainPlugin) terrain.getPlugin();
				if (plugin.containsEntity(playerFleet)) {
					float angle = Misc.getAngleInDegrees(terrain.getLocation(), playerFleet.getLocation());
					Color color = plugin.getAuroraColorForAngle(angle);
					float intensity = plugin.getIntensityAtPoint(playerFleet.getLocation());
					intensity = 0.4f + 0.6f * intensity;
					int alpha = (int) (80f * intensity);
					color = Misc.setAlpha(color, alpha);
					if (coronaColor == null || coronaColor.getAlpha() < alpha) {
						coronaColor = color;
						coronaIntensity = intensity;
						corona = plugin;
					}
				}
			} else if (terrain.getPlugin() instanceof PulsarBeamTerrainPlugin) {
				PulsarBeamTerrainPlugin plugin = (PulsarBeamTerrainPlugin) terrain.getPlugin();
				if (plugin.containsEntity(playerFleet)) {
					float angle = Misc.getAngleInDegreesStrict(terrain.getLocation(), playerFleet.getLocation());
					Color color = plugin.getPulsarColorForAngle(angle);
					float intensity = plugin.getIntensityAtPoint(playerFleet.getLocation());
					intensity = 0.4f + 0.6f * intensity;
					int alpha = (int) (80f * intensity);
					color = Misc.setAlpha(color, alpha);
					if (coronaColor == null || coronaColor.getAlpha() < alpha) {
						coronaColor = color;
						coronaIntensity = intensity;
						pulsar = plugin;
						corona = null;
					}
				}
			} else if (terrain.getType().equals(Terrain.RING)) {
				if (terrain.getPlugin().containsEntity(playerFleet)) {
					numRings++;
				}
			}
		}
		if (nebulaTex != null) {
			loader.setNebulaTex(nebulaTex);
			loader.setNebulaMapTex(nebulaMapTex);
		}

		if (coronaColor != null) {
			loader.setBackgroundGlowColor(coronaColor);
		}

		nebulaLevel = 15;
		if (inNebula) {
			nebulaLevel = 100;
		}
		if (!inNebula && playerFleet.isInHyperspace()) {
			nebulaLevel = 0;
		}

		for (int i = 0; i < nebulaLevel; i++) {
			float x = random.nextFloat() * width - width / 2;
			float y = random.nextFloat() * height - height / 2;
			float radius = 100f + random.nextFloat() * 400f;
			if (inNebula) {
				radius += 100f + 500f * random.nextFloat();
			}
			loader.addNebula(x, y, radius);
		}

		float numAsteroidsWithinRange = countNearbyAsteroids(playerFleet);

		int numAsteroids;
		if (context.getPlayerFleet().getContainingLocation() == Global.getSector().getHyperspace()) {
			if (numAsteroidsWithinRange > 0) {
				numAsteroids = Math.min(100, (int) ((numAsteroidsWithinRange + 1f) * 20f));
			} else {
				numAsteroids = 0;
			}
		} else {
			if (numAsteroidsWithinRange > 0) {
				numAsteroids = Math.min(200, (int) ((numAsteroidsWithinRange + 1f) * 20f));
			} else {
				if (random.nextFloat() > 0.5) {
					numAsteroids = (int) (random.nextFloat() * 200.0);
				} else {
					numAsteroids = 0;
				}
			}
		}
		numAsteroids *= sizeMod;

		Vector2f asteroidVelocity = getNearbyAsteroidsVelocity(context.getPlayerFleet());
		float asteroidDirection;
		float asteroidSpeed;
		if (asteroidVelocity.x != 0f || asteroidVelocity.y != 0f) {
			asteroidDirection = VectorUtils.getFacing(asteroidVelocity);
			asteroidSpeed = Math.min(50f, Math.max(10f, asteroidVelocity.length() * 1.5f));
		} else {
			asteroidDirection = random.nextFloat() * 360f;
			asteroidSpeed = random.nextFloat() * 5f + 5f;
		}

		if (numAsteroids > 0) {
			loader.addAsteroidField(0, 0, asteroidDirection, Math.max(width, height), asteroidSpeed * 1.5f,
					asteroidSpeed * 3f + 40f, numAsteroids);
		}

		if (numRings > 0) {
			int numRingAsteroids = (int) (numRings * 300 + (numRings * 600f) * random.nextFloat());
			if (numRingAsteroids > 1500) {
				numRingAsteroids = 1500;
			}
			loader.addRingAsteroids(0, 0, random.nextFloat() * 360f, width, 100f, 200f, numRingAsteroids);
		}

		loader.setBackgroundSpriteName(playerFleet.getContainingLocation().getBackgroundTextureFilename());

		if (playerFleet.getContainingLocation() == Global.getSector().getHyperspace()) {
			loader.setHyperspaceMode(true);
		} else {
			loader.setHyperspaceMode(false);
		}

		addMultiplePlanets();
	}

	private void addMultiplePlanets() {
		float bgWidth = width / 17.5f;
		float bgHeight = height / 17.5f;

		List<NearbyPlanetData> planets = getNearbyPlanets(context.getPlayerFleet());
		List<NearbyJumpPointData> jumpPoints = getNearbyJumpPoints(context.getPlayerFleet());
		float closestSquared = Float.MAX_VALUE;
		SectorEntityToken closest = null;
		for (NearbyPlanetData data : planets) {
			float distanceSquared = data.offset.lengthSquared();
			if (distanceSquared < closestSquared) {
				closestSquared = distanceSquared;
				closest = data.planet;
			}
		}
		for (NearbyJumpPointData data : jumpPoints) {
			float distanceSquared = data.offset.lengthSquared();
			if (distanceSquared < closestSquared) {
				closestSquared = distanceSquared;
				closest = data.jumpPoint;
			}
		}

		JumpPointAPI biggestJumpPoint = null;
		float biggest = 0f;
		for (NearbyJumpPointData data : jumpPoints) {
			float size = data.jumpPoint.getRadius();
			if (size > biggest) {
				biggest = size;
				biggestJumpPoint = data.jumpPoint;
			}
		}

		if ((!planets.isEmpty() || !jumpPoints.isEmpty()) && closest != null) {
			loader.setPlanetBgSize(bgWidth, bgHeight);

			float maxDist = PLANET_MAX_DIST;
			for (NearbyPlanetData data : planets) {
				float dist = Vector2f.sub(context.getPlayerFleet().getLocation(), data.planet.getLocation(),
						new Vector2f()).length();
				float baseRadius = data.planet.getRadius();
				float scaleFactor = 1.5f;
				float distanceFactor = 1f;
				float maxRadius = 500f;

				if (data.planet.getTypeId().equals(StarTypes.WHITE_DWARF)) {
					maxRadius = 200f;
				}

				float f = (maxDist - dist) / maxDist * 0.65f + 0.35f;
				float radius = baseRadius * f * scaleFactor;
				if (data.planet != closest) {
					float otherDist =
							Vector2f.sub(closest.getLocation(), data.planet.getLocation(), new Vector2f()).length();
					radius *= (maxDist - otherDist) / maxDist * 0.65f + 0.35f;
				}
				if (radius > maxRadius) {
					radius = maxRadius;
				}

				float locX = data.offset.x * distanceFactor;
				float locY = data.offset.y * distanceFactor;
				loader.addPlanet(locX, locY, radius, data.planet.getTypeId(), 0f, true);
			}
			for (NearbyJumpPointData data : jumpPoints) {
				float dist = Vector2f.sub(context.getPlayerFleet().getLocation(), data.jumpPoint.getLocation(),
						new Vector2f()).length();
				float baseRadius = data.jumpPoint.getRadius();
				float scaleFactor = 1.5f;
				float distanceFactor = 1f;
				float maxRadius = 500f;

				float f = (maxDist - dist) / maxDist * 0.65f + 0.35f;
				float radius = baseRadius * f * scaleFactor;
				if (data.jumpPoint != closest) {
					float otherDist =
							Vector2f.sub(closest.getLocation(), data.jumpPoint.getLocation(), new Vector2f()).length();
					radius *= (maxDist - otherDist) / maxDist * 0.65f + 0.35f;
				}
				if (radius > maxRadius) {
					radius = maxRadius;
				}

				float locX = data.offset.x * distanceFactor;
				float locY = data.offset.y * distanceFactor;
				loader.addPlanet(locX, locY, radius, "wormholeUnder", 0f, true);
				loader.addPlanet(locX, locY, radius, "wormholeA", 0f, true);
				loader.addPlanet(locX, locY, radius, "wormholeB", 0f, true);
				loader.addPlanet(locX, locY, radius, "wormholeC", 0f, true);
			}
		}
	}

	protected void setRandomBackground(MissionDefinitionAPI loader, Random random) {
		// these have to be loaded using the graphics section in settings.json
		String [] bgs = new String [] {
				"graphics/backgrounds/background1.jpg",
				"graphics/backgrounds/background2.jpg",
				"graphics/backgrounds/background3.jpg",
				"graphics/backgrounds/background4.jpg"
		};
		String pick = bgs[Math.min(bgs.length - 1, (int)(random.nextFloat() * bgs.length))];
		loader.setBackgroundSpriteName(pick);
	}

	private void addObjectives(MissionDefinitionAPI loader, int num, boolean escape) {
		WeightedRandomPicker<JSONObject> objectivePicker = null;
        try {
            objectivePicker = getObjectivePicker(context.getPlayerFleet());
        } catch (IOException | JSONException ex) {
            log.error(ex);
        }

        if (objectivePicker == null) {
            return;
        }

        WeightedRandomPicker<String> centralObjectives = new WeightedRandomPicker<>(random);
        WeightedRandomPicker<String> standardObjectives = new WeightedRandomPicker<>(random);

        //centralObjectives.add("sotf_objective_turret");
		//centralObjectives.add("sotf_objective_turret");
		//standardObjectives.add("comm_relay");
		//standardObjectives.add("comm_relay");

        // sort out objectives we want to try spawning more centrally
        for (int i = 0; i < num; i++) {
            JSONObject obj = objectivePicker.pick(random);
            if (obj.optBoolean("central_spawn", false)) {
                try {
                    centralObjectives.add(obj.getString("obj_id"));
                } catch (JSONException ex) {
                    log.error(ex);
                }
            } else {
                try {
                    standardObjectives.add(obj.getString("obj_id"));
                } catch (JSONException ex) {
                    log.error(ex);
                }
            }
        }

        // ---
        // HEAD-ON BATTLES
        // ---

		float r = random.nextFloat();

        if (!escape) {
            if (num == 2) {
                addObjectiveAt(0.25f, 0.5f, 0f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                addObjectiveAt(0.75f, 0.5f, 0f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
            } else if (num == 3) {
                if (r < 0.33f) {
                    addObjectiveAt(0.75f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));

                    addObjectiveAt(0.25f, 0.7f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.3f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else if (r < 0.67f) {
                    addObjectiveAt(0.75f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));

                    addObjectiveAt(0.25f, 0.7f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.3f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else {
                    addObjectiveAt(0.5f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));

                    addObjectiveAt(0.25f, 0.5f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.5f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                }
            } else if (num == 4) {
                if (r < 0.33f) {
                    addObjectiveAt(0.25f, 0.25f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.75f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.25f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.75f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else if (r < 0.67f) {
                    addObjectiveAt(0.25f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));

                    addObjectiveAt(0.5f, 0.75f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.25f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else {
                    addObjectiveAt(0.2f, 0.5f, 1f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.4f, 0.5f, 0f, 3f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.6f, 0.5f, 0f, 3f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.8f, 0.5f, 1f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                }
            } else if (num == 5) {
                if (r < 0.33f) {
                    addObjectiveAt(0.5f, 0.5f, 2f, 1f, pickOutObjective(centralObjectives, standardObjectives));

                    addObjectiveAt(0.25f, 0.25f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.75f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.25f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.75f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else if (r < 0.67f) {
                    addObjectiveAt(0.25f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));

                    addObjectiveAt(0.5f, 0.25f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.75f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                } else {
                    addObjectiveAt(0.2f, 0.5f, 1f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.35f, 0.5f, 0f, 3f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.65f, 0.5f, 0f, 3f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.8f, 0.5f, 1f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.5f, 0f, 5f, pickAnyObjective(centralObjectives, standardObjectives));
                }
            } else if (num == 6) {
                if (r < 0.33f) {
                    addObjectiveAt(0.25f, 0.5f, 2f, 1f, pickOutObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.5f, 2f, 1f, pickOutObjective(centralObjectives, standardObjectives));

                    addObjectiveAt(0.25f, 0.25f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.25f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.75f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.75f, 2f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else if (r < 0.67f) {
                    addObjectiveAt(0.25f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.5f, 1f, 1f, pickOutObjective(centralObjectives, standardObjectives));

                    addObjectiveAt(0.5f, 0.2f, 1f, 0.5f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.4f, 1f, 0.5f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.6f, 1f, 0.5f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.8f, 1f, 0.5f, pickAnyObjective(centralObjectives, standardObjectives));
                } else {
                    addObjectiveAt(0.4f, 0.5f, 0f, 3f, pickOutObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.6f, 0.5f, 0f, 3f, pickOutObjective(centralObjectives, standardObjectives));

                    addObjectiveAt(0.25f, 0.25f, 1f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.75f, 1f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.25f, 1f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.75f, 1f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                }
            }
        }

        // ---
        // ESCAPE BATTLES
        // ---

        else {
            if (num == 2) {
                if (r < 0.33f) {
                    addObjectiveAt(0.25f, 0.25f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.75f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else if (r < 0.67f) {
                    addObjectiveAt(0.75f, 0.25f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.75f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else {
                    addObjectiveAt(0.5f, 0.25f, 4f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.75f, 4f, 2f, pickAnyObjective(centralObjectives, standardObjectives));
                }
            } else if (num == 3) {
                if (r < 0.33f) {
                    addObjectiveAt(0.25f, 0.75f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.25f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.5f, 1f, 6f, pickAnyObjective(centralObjectives, standardObjectives));
                } else if (r < 0.67f) {
                    addObjectiveAt(0.25f, 0.5f, 1f, 6f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.75f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.25f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else {
                    addObjectiveAt(0.5f, 0.25f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.5f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.75f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                }
            } else if (num == 4) {
                if (r < 0.33f) {
                    addObjectiveAt(0.25f, 0.25f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.75f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.25f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.75f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else if (r < 0.67f) {
                    addObjectiveAt(0.35f, 0.25f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.65f, 0.35f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.6f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.8f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else {
                    addObjectiveAt(0.65f, 0.25f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.35f, 0.35f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.6f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.8f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                }
            } else if (num == 5) {
                if (r < 0.33f) {
                    addObjectiveAt(0.25f, 0.25f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.75f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.25f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.75f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.5f, 1f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                } else if (r < 0.67f) {
                    addObjectiveAt(0.25f, 0.45f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.75f, 0.35f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.6f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.8f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.25f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                } else {
                    addObjectiveAt(0.75f, 0.45f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.25f, 0.35f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.6f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.8f, 4f, 1f, pickAnyObjective(centralObjectives, standardObjectives));
                    addObjectiveAt(0.5f, 0.25f, 2f, 0f, pickAnyObjective(centralObjectives, standardObjectives));
                }
            }
        }
	}

	private void addObjectiveAt(float xMult, float yMult, float xOff, float yOff, String type) {
		float minX = -width/2 + xPad;
		float minY = -height/2 + yPad;

		float x = (width - xPad * 2f) * xMult + minX;
		float y = (height - yPad * 2f) * yMult + minY;

		x = ((int) x / 1000) * 1000f;
		y = ((int) y / 1000) * 1000f;

		float offsetX = Math.round((random.nextFloat() - 0.5f) * xOff * 2f) * 1000f;
		float offsetY = Math.round((random.nextFloat() - 0.5f) * yOff * 2f) * 1000f;

		float xDir = (float) Math.signum(offsetX);
		float yDir = (float) Math.signum(offsetY);

		if (xDir == prevXDir && xOff > 0) {
			xDir = -xDir;
			offsetX = Math.abs(offsetX) * -prevXDir;
		}

		if (yDir == prevYDir && yOff > 0) {
			yDir = -yDir;
			offsetY = Math.abs(offsetY) * -prevYDir;
		}

		prevXDir = xDir;
		prevYDir = yDir;

		x += offsetX;
		y += offsetY;

		if (type != null) {
			loader.addObjective(x, y, type);
		}

		if (random.nextFloat() > 0.6f && loader.hasNebula()) {
			float nebulaSize = random.nextFloat() * 1500f + 500f;
			loader.addNebula(x, y, nebulaSize);
		}
	}

	// pick a central objective if possible
	private String pickOutObjective(WeightedRandomPicker<String> central, WeightedRandomPicker<String> non) {
	    String type = "comm_relay";

	    if (central.getItems().size() > 0) {
	        type = central.pickAndRemove();
        } else {
	        type = non.pickAndRemove();
        }
	    return type;
    }

    // pick anything out of the two pickers. Doesn't matter which it removes from, at this point we don't care what goes where
    private String pickAnyObjective(WeightedRandomPicker<String> central, WeightedRandomPicker<String> non) {
        String type = "comm_relay";
        WeightedRandomPicker<String> new_picker = new WeightedRandomPicker<String>(random);
        new_picker.addAll(central);
        new_picker.addAll(non);

        type = new_picker.pickAndRemove();

        if (central.getItems().contains(type)) {
            central.remove(type);
        } else {
            non.remove(type);
        }
        return type;
    }

	private WeightedRandomPicker<JSONObject> getObjectivePicker(CampaignFleetAPI playerFleet) throws IOException, JSONException {
		LocationAPI loc = playerFleet.getContainingLocation();
		WeightedRandomPicker<JSONObject> post = new WeightedRandomPicker<JSONObject>(random);
		//post.add("nav_buoy", 1f);
		//post.add("sensor_array", 1f);
		//post.add("comm_relay", 1f);

		//JSONArray objectives = Global.getSettings().getMergedSpreadsheetDataForMod("id", OBJECTIVE_CSV, SotfIDs.SOTF);

		JSONArray objectives = SotfModPlugin.OBJECTIVE_DATA;

		//if (objectives == null) {
		//	return post;
		//}
		for(int i = 0; i < objectives.length(); i++) {
			JSONObject row = objectives.getJSONObject(i);
			boolean should_add_objective = true;

			try {
				if (row.getString("hyperspace").equals("true")) {
					if (!loc.isHyperspace()) {
						should_add_objective = false;
					}
				}
				else if (row.getString("hyperspace").equals("false")) {
					if (loc.isHyperspace()) {
						should_add_objective = false;
					}
				}
			} catch (JSONException ex) {
				log.info("no hyperspace setting for objective ID " + row.getString("id"));
			}

			if (!loc.isHyperspace()) {
				StarSystemAPI system = (StarSystemAPI) loc;
				try {
					if (!row.getString("tag").equals("") && !system.hasTag(row.getString("tag"))) {
						should_add_objective = false;
					}
				} catch (JSONException ex) {
					log.info("no tag set for objective ID " + row.getString("id"));
				}
			} else {
				try {
					if (!row.getString("tag").equals("")) {
						should_add_objective = false;
					}
				} catch (JSONException ex) {
					log.info("no tag set for objective ID " + row.getString("id"));
				}
			}

			try {
				if (row.getString("escape").equals("true")) {
					if (!escape) {
						should_add_objective = false;
					}
				}
				else if (row.getString("escape").equals("false")) {
					if (escape) {
						should_add_objective = false;
					}
				}
			} catch (JSONException ex) {
				log.info("no escape setting for objective ID " + row.getString("id"));
			}

			try {
				int minFP = row.getInt("minFP");
					if (fpBoth < (maxFPForObj + minFP)) {
						should_add_objective = false;
					}
				} catch (JSONException ex) {
					log.info("no minFP for objective ID " + row.getString("id"));
				}

			try {
				int maxFP = row.getInt("maxFP");
				if (maxFP == -1) {
					maxFP = 999999;
				}
				if (fpBoth > (maxFPForObj + maxFP)) {
					should_add_objective = false;
				}
			} catch (JSONException ex) {
				log.info("no maxFP for objective ID " + row.getString("id"));
			}

			if (should_add_objective) {
				post.add(row, (float) row.getDouble("weight"));
			}
		}
		return post;
	}

	private float countNearbyAsteroids(CampaignFleetAPI playerFleet) {
		float numAsteroidsWithinRange = 0;
		LocationAPI loc = playerFleet.getContainingLocation();
		if (loc instanceof StarSystemAPI) {
			StarSystemAPI system = (StarSystemAPI) loc;
			List<SectorEntityToken> asteroids = system.getAsteroids();
			for (SectorEntityToken asteroid : asteroids) {
				float range = Vector2f.sub(playerFleet.getLocation(), asteroid.getLocation(), new Vector2f()).length();
				if (range < 300) numAsteroidsWithinRange ++;
			}
		}
		return numAsteroidsWithinRange;
	}

	private static class NearbyPlanetData {
		private Vector2f offset;
		private PlanetAPI planet;
		public NearbyPlanetData(Vector2f offset, PlanetAPI planet) {
			this.offset = offset;
			this.planet = planet;
		}
	}

	private static float PLANET_AREA_WIDTH = 2000;
	private static float PLANET_AREA_HEIGHT = 2000;
	private static float PLANET_MAX_DIST = (float) Math.sqrt(PLANET_AREA_WIDTH/2f * PLANET_AREA_WIDTH/2f + PLANET_AREA_HEIGHT/2f * PLANET_AREA_WIDTH/2f);

	private static float SINGLE_PLANET_MAX_DIST = 1000f;

	private static List<NearbyJumpPointData> getNearbyJumpPoints(CampaignFleetAPI playerFleet) {
		LocationAPI loc = playerFleet.getContainingLocation();
		List<NearbyJumpPointData> result = new ArrayList<>(10);
		List<SectorEntityToken> jumpPoints = loc.getEntitiesWithTag(Tags.JUMP_POINT);
		for (SectorEntityToken token : jumpPoints) {
			JumpPointAPI jumpPoint = (JumpPointAPI) token;
			Vector2f vector = Vector2f.sub(jumpPoint.getLocation(), playerFleet.getLocation(), new Vector2f());
			float range = vector.length();
			if (range <= PLANET_MAX_DIST) {
				result.add(new NearbyJumpPointData(vector, jumpPoint));
			}
		}
		return result;
	}

//	private static float getNearbyPlanetFactor(CampaignFleetAPI playerFleet) {
//		LocationAPI loc = playerFleet.getContainingLocation();
//		float minDist = Float.MAX_VALUE;
//		if (loc instanceof StarSystemAPI) {
//			StarSystemAPI system = (StarSystemAPI) loc;
//			List<PlanetAPI> planets = system.getPlanets();
//			for (PlanetAPI planet : planets) {
//				if (planet.isStar()) {
//					continue;
//				}
//				float dist = Vector2f.sub(playerFleet.getLocation(), planet.getLocation(), new Vector2f()).length();
//				float adjustedDist = (dist + 150f + planet.getRadius()) * (150f / Math.max(50f, planet.getRadius()));
//				if (adjustedDist < minDist && dist < SINGLE_PLANET_MAX_DIST) {
//					minDist = adjustedDist;
//				}
//			}
//		}
//		return minDist;
//	}

	private List<NearbyPlanetData> getNearbyPlanets(CampaignFleetAPI playerFleet) {
		LocationAPI loc = playerFleet.getContainingLocation();
		List<NearbyPlanetData> result = new ArrayList<NearbyPlanetData>();
		if (loc instanceof StarSystemAPI) {
			StarSystemAPI system = (StarSystemAPI) loc;
			List<PlanetAPI> planets = system.getPlanets();
			for (PlanetAPI planet : planets) {
				float diffX = planet.getLocation().x - playerFleet.getLocation().x;
				float diffY = planet.getLocation().y - playerFleet.getLocation().y;

				if (Math.abs(diffX) < PLANET_AREA_WIDTH/2f && Math.abs(diffY) < PLANET_AREA_HEIGHT/2f) {
					result.add(new NearbyPlanetData(new Vector2f(diffX, diffY), planet));
				}
			}
		}
		return result;
	}

	private static Vector2f getNearbyAsteroidsVelocity(CampaignFleetAPI playerFleet) {
		Vector2f sumVec = new Vector2f();
		LocationAPI loc = playerFleet.getContainingLocation();
		int count = 0;
		if (loc instanceof StarSystemAPI) {
			StarSystemAPI system = (StarSystemAPI) loc;
			List<SectorEntityToken> asteroids = system.getAsteroids();
			for (SectorEntityToken asteroid : asteroids) {
				float range = Vector2f.sub(playerFleet.getLocation(), asteroid.getLocation(), new Vector2f()).length();
				if (range <= ASTEROID_MAX_DIST) {
					Vector2f velocity = SSP_AsteroidTracker.getVelocity(asteroid);
					Vector2f.add(sumVec, velocity, sumVec);
					count++;
				}
			}
		}
		if (count > 0) {
			sumVec.scale(1f / count);
		}
		return sumVec;
	}

	private static class NearbyJumpPointData {

		final JumpPointAPI jumpPoint;
		final Vector2f offset;

		NearbyJumpPointData(Vector2f offset, JumpPointAPI jumpPoint) {
			this.offset = offset;
			this.jumpPoint = jumpPoint;
		}
	}
}