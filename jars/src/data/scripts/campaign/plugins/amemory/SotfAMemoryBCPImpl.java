// battle creation code for the A Memory fight. EXTREMELY cut down to match the Nothing Personal mission as close as possible - no context-based features at all
package data.scripts.campaign.plugins.amemory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.special.SotfAMemoryEFCPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SotfAMemoryBCPImpl implements BattleCreationPlugin {

	private float width, height;

	private BattleCreationContext context;
	private MissionDefinitionAPI loader;

	@Override
	public void initBattle(final BattleCreationContext context, MissionDefinitionAPI loader) {
		this.context = context;
		this.loader = loader;
		CampaignFleetAPI playerFleet = context.getPlayerFleet();
		CampaignFleetAPI otherFleet = context.getOtherFleet();
		FleetGoal playerGoal = context.getPlayerGoal();
		FleetGoal enemyGoal = context.getOtherGoal();
		context.enemyDeployAll = true;
		context.aiRetreatAllowed = false;
		context.fightToTheLast = true;

		int baseCommandPoints = (int) Global.getSettings().getFloat("startingCommandPoints");

		loader.initFleet(FleetSide.PLAYER, "ISS", playerGoal, false,
				context.getPlayerCommandPoints() - baseCommandPoints,
				(int) playerFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);
		loader.initFleet(FleetSide.ENEMY, "HSS", enemyGoal, true,
				(int) otherFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);

		List<FleetMemberAPI> playerShips = playerFleet.getFleetData().getCombatReadyMembersListCopy();
		if (playerGoal == FleetGoal.ESCAPE) {
			playerShips = playerFleet.getFleetData().getMembersListCopy();
		}
		for (FleetMemberAPI member : playerShips) {
			// doesn't work??? adds entire fleet regardless
			//if (member.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) || (member.isPhaseShip() && member.isFrigate() && Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.HARMONIC_TUNING_KEY))) {
				loader.addFleetMember(FleetSide.PLAYER, member);
			//}
		}

		List<FleetMemberAPI> enemyShips = otherFleet.getFleetData().getMembersListCopy();
		if (enemyGoal == FleetGoal.ESCAPE) {
			enemyShips = otherFleet.getFleetData().getMembersListCopy();
		}
		for (FleetMemberAPI member : enemyShips) {
			loader.addFleetMember(FleetSide.ENEMY, member);
		}

		width = 24000;
		height = 18000f;

		createMap();

		context.setInitialDeploymentBurnDuration(1f);
		context.setNormalDeploymentBurnDuration(6f);
		context.setEscapeDeploymentBurnDuration(1.5f);

		addObjectives(loader);
		context.setStandoffRange(12000f);
	}

	public void afterDefinitionLoad(final CombatEngineAPI engine) {
		Set<ShipAPI> ships = new HashSet<>();

		boolean didNothingPersonal = Global.getSettings().getMissionScore("nothingpersonal") >= 100;

		// add ships from Nothing Personal along with some officers
		PersonAPI captain = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(Factions.INDEPENDENT), 5, OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_YES_DEFENSE, Misc.random);
		captain.setGender(FullName.Gender.FEMALE);
		captain.setPortraitSprite(Global.getSector().getFaction(Factions.INDEPENDENT).getPortraits(FullName.Gender.FEMALE).pick());
		FleetMemberAPI athena = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "aurora_Assault");
		athena.setCaptain(captain);
		//athena.setAlly(true);
		athena.setShipName("ISS Athena");
		ShipAPI athena_ship = engine.getFleetManager(0).spawnFleetMember(athena, new Vector2f(0, -4000), 90f, 2f);
		ships.add(athena_ship);

		FleetMemberAPI enki = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "venture_Balanced");
		enki.setCaptain(OfficerManagerEvent.createOfficer(Global.getSector().getFaction(Factions.INDEPENDENT), 1, OfficerManagerEvent.SkillPickPreference.ANY, Misc.random));
		//enki.setAlly(true);
		enki.setShipName("ISS Enki");
		ShipAPI enki_ship = engine.getFleetManager(0).spawnFleetMember(enki, new Vector2f(+500, -4000), 90f, 2f);
		ships.add(enki_ship);

		FleetMemberAPI falcon = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "falcon_Attack");
		falcon.setCaptain(OfficerManagerEvent.createOfficer(Global.getSector().getFaction(Factions.INDEPENDENT), 2, OfficerManagerEvent.SkillPickPreference.ANY, Misc.random));
		//falcon.setAlly(true);
		falcon.setShipName(Global.getSector().getFaction(Factions.INDEPENDENT).pickRandomShipName());
		ShipAPI falcon_ship = engine.getFleetManager(0).spawnFleetMember(falcon, new Vector2f(-500, -4000), 90f, 2f);
		ships.add(falcon_ship);

		FleetMemberAPI heron = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "heron_Attack");
		heron.setCaptain(OfficerManagerEvent.createOfficer(Global.getSector().getFaction(Factions.INDEPENDENT), 2, OfficerManagerEvent.SkillPickPreference.ANY, Misc.random));
		//heron.setAlly(true);
		heron.setShipName(Global.getSector().getFaction(Factions.INDEPENDENT).pickRandomShipName());
		ShipAPI heron_ship = engine.getFleetManager(0).spawnFleetMember(heron, new Vector2f(+1000, -4000), 90f, 2f);
		ships.add(heron_ship);

		FleetMemberAPI tempest = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "tempest_Attack");
		PersonAPI tempestCapt = OfficerManagerEvent.createOfficer(
				Global.getSector().getFaction(Factions.INDEPENDENT),
				1,
				OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_NO_MISSILE_YES_DEFENSE,
				Misc.random);
		tempestCapt.getStats().getSkillsCopy().get(0).setLevel(0);
		tempestCapt.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
		tempestCapt.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
		tempestCapt.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
		tempestCapt.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 1);
		tempest.setCaptain(tempestCapt);
		//tempest.setAlly(true);
		tempest.setShipName(Global.getSector().getFaction(Factions.INDEPENDENT).pickRandomShipName());
		ShipAPI tempest_ship = engine.getFleetManager(0).spawnFleetMember(tempest, new Vector2f(-1000, -4000), 90f, 2f);
		ships.add(tempest_ship);

		for (ShipAPI ship : ships) {
			ship.setInvalidTransferCommandTarget(true);
			float maxCR = 0.7f;
			if (ship.getCaptain().getStats().hasSkill(Skills.COMBAT_ENDURANCE)) {
				maxCR = 0.85f;
			}
			ship.setCurrentCR(maxCR);
			ship.setCRAtDeployment(maxCR);
			ship.setControlsLocked(false);
			ship.getFleetMember().getVariant().addTag("sotf_amemory_immunity");
			ship.getFleetMember().getRepairTracker().setCR(maxCR);
			ship.getFleetMember().getRepairTracker().setMothballed(false);
			ship.getFleetMember().getRepairTracker().setCrashMothballed(false);
			ship.getFleetMember().getCrewComposition().setCrew(ship.getFleetMember().getMinCrew());
		}

		engine.addPlugin(new SotfAMemoryEFCPlugin(context));
	}

	private void createMap() {
		loader.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

		for (int i = 0; i < 25; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 400f + (float) Math.random() * 1000f;
			loader.addNebula(x, y, radius);
		}

		// faint purple glow
		loader.setBackgroundGlowColor(new Color(205, 155, 255, 40));

		loader.setBackgroundSpriteName("graphics/backgrounds/background1.jpg");

		loader.setHyperspaceMode(false);
	}

	private void addObjectives(MissionDefinitionAPI loader) {
		float minX = -width/2;
		float minY = -height/2;
		loader.addObjective(minX + width * 0.2f + 400 + 3000, minY + height * 0.2f + 400 + 2000, "sensor_array");
		loader.addObjective(minX + width * 0.4f + 2000, minY + height * 0.7f, "sensor_array");
		loader.addObjective(minX + width * 0.75f - 2000, minY + height * 0.7f, "comm_relay");
		loader.addObjective(minX + width * 0.2f + 3000, minY + height * 0.5f, "nav_buoy");
		loader.addObjective(minX + width * 0.85f - 3000, minY + height * 0.4f, "nav_buoy");
	}

}