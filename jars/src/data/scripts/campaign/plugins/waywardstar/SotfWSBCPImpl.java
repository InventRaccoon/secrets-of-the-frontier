// battle creation code for the Wayward Star fight
package data.scripts.campaign.plugins.waywardstar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.special.SotfWSEFCPlugin;

import java.awt.*;
import java.util.List;

public class SotfWSBCPImpl implements BattleCreationPlugin {

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
		//context.aiRetreatAllowed = false;
		context.fightToTheLast = true;

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

		List<FleetMemberAPI> enemyShips = otherFleet.getFleetData().getMembersListCopy();
		if (enemyGoal == FleetGoal.ESCAPE) {
			enemyShips = otherFleet.getFleetData().getMembersListCopy();
		}
		for (FleetMemberAPI member : enemyShips) {
			loader.addFleetMember(FleetSide.ENEMY, member);
		}

		width = 20000;
		height = 18000f;

		createMap();

		context.setInitialDeploymentBurnDuration(1f);
		context.setNormalDeploymentBurnDuration(6f);
		context.setEscapeDeploymentBurnDuration(1.5f);

		addObjectives(loader);
		context.setStandoffRange(12000f);
	}

	public void afterDefinitionLoad(final CombatEngineAPI engine) {
		engine.addPlugin(new SotfWSEFCPlugin(context));
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

		loader.setBackgroundSpriteName("graphics/backgrounds/hyperspace1.jpg");

		loader.setHyperspaceMode(true);
	}

	private void addObjectives(MissionDefinitionAPI loader) {
		float minX = -width/2;
		float minY = -height/2;
		loader.addObjective(minX + width * 0.2f, minY + height * 0.3f, SotfIDs.OBJ_REINFORCER);
		loader.addObjective(minX + width * 0.6f, minY + height * 0.3f, "nav_buoy");

		loader.addObjective(minX + width * 0.4f, minY + height * 0.7f, "nav_buoy");
		loader.addObjective(minX + width * 0.8f, minY + height * 0.7f, SotfIDs.OBJ_REINFORCER);

		loader.addObjective(minX + width * 0.5f, minY + height * 0.5f, "sensor_array");
	}

}