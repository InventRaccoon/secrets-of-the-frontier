// Mission definition for Good Hunting
// Walter Feros fights impossible odds... until a "ghost" shows up to join the hunt
package data.missions.goodhunting;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.combat.special.SotfGHWarhornIntrusionPlugin;

import java.util.ArrayList;
import java.util.List;

public class MissionDefinition implements MissionDefinitionPlugin {

    public void defineMission(MissionDefinitionAPI api) {
	    api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, true);
		
		api.setFleetTagline(FleetSide.PLAYER, "\"Walter's Wolfpack\", advanced mercenary raiders");

		if (Global.getSettings().getMissionScore("goodhunting") < 1) {
			api.setFleetTagline(FleetSide.ENEMY, "Pirate enforcers headed by Vacha Rask");

			api.addBriefingItem("Put on a good show");
			api.addBriefingItem("Feros is an exceptional captain and commander skilled in wolfpack tactics");
			api.addBriefingItem("ISS Once in a Lullaby has been heavily upgraded and is a top-spec frigate");
			api.addBriefingItem("That won't be enough");
		} else {
			api.setFleetTagline(FleetSide.ENEMY, "### EA SY PRE Y");

			api.addBriefingItem("UNRECOGNIZED INPUT // Fell the beasties, kah-hah-hah");
			api.addBriefingItem("Your flagship is a sharp knife to stick into their backsides");
			api.addBriefingItem("Time your assaults with the Hidecracker's cryoharpoons");
			api.addBriefingItem("The Hidecracker loads faster when you're near large prey");
		}

		List<FleetMemberAPI> playerShips = new ArrayList<>();

		// Walter Feros in his modified Tempest
		FleetMemberAPI flagship = api.addToFleet(FleetSide.PLAYER, "sotf_tempest_Modified", FleetMemberType.SHIP, "ISS Once in a Lullaby",true);
		flagship.getRepairTracker().setCR(0.85f);
		PersonAPI feros = SotfPeople.genFeros();
		flagship.setCaptain(feros);
		api.getDefaultCommander(FleetSide.PLAYER).setStats(feros.getStats());
		playerShips.add(flagship);

		playerShips.add(api.addToFleet(FleetSide.PLAYER, "aurora_Assault", FleetMemberType.SHIP, false));
		playerShips.add(api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false));
		playerShips.add(api.addToFleet(FleetSide.PLAYER, "shrike_Attack", FleetMemberType.SHIP, false));
		playerShips.add(api.addToFleet(FleetSide.PLAYER, "scarab_Experimental", FleetMemberType.SHIP, false));
		playerShips.add(api.addToFleet(FleetSide.PLAYER, "omen_PD", FleetMemberType.SHIP, false));
		playerShips.add(api.addToFleet(FleetSide.PLAYER, "brawler_tritachyon_Standard", FleetMemberType.SHIP, "HSS Come Get Some IV",false));
		playerShips.add(api.addToFleet(FleetSide.PLAYER, "wolf_Assault", FleetMemberType.SHIP, "TTS Violet Star",false));

		// Vacha Rask, distant Kanta cousin and family enforcer
		FleetMemberAPI blackfire = api.addToFleet(FleetSide.ENEMY, "atlas2_Standard", FleetMemberType.SHIP, "Blackfire Souleater", false);

		// doms get 2/3 Pilums so they aren't as liable to eviscerate player fleet instantly with Harpoon spam
		FleetMemberAPI dom1 = api.addToFleet(FleetSide.ENEMY, "dominator_d_Assault", FleetMemberType.SHIP, false);
		dom1.getVariant().addWeapon("WS 014", "pilum");
		dom1.getVariant().addWeapon("WS 016", "pilum");

		FleetMemberAPI dom2 = api.addToFleet(FleetSide.ENEMY, "dominator_d_Assault", FleetMemberType.SHIP, false);
		dom2.getVariant().addWeapon("WS 014", "pilum");
		dom2.getVariant().addWeapon("WS 016", "pilum");

		FleetMemberAPI dom3 = api.addToFleet(FleetSide.ENEMY, "dominator_d_Assault", FleetMemberType.SHIP, false);
		dom3.getVariant().addWeapon("WS 014", "pilum");
		dom3.getVariant().addWeapon("WS 016", "pilum");

		api.addToFleet(FleetSide.ENEMY, "eradicator_pirates_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "venture_Outdated", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "venture_Outdated", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_d_pirates_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_d_pirates_Attack", FleetMemberType.SHIP, false);
		//api.addToFleet(FleetSide.ENEMY, "vanguard_pirates_Attack", FleetMemberType.SHIP, "Bait", false); // too strong lol
		api.addToFleet(FleetSide.ENEMY, "cerberus_d_pirates_Shielded", FleetMemberType.SHIP, "Better Than Talon Duty", false);
		api.addToFleet(FleetSide.ENEMY, "cerberus_d_pirates_Shielded", FleetMemberType.SHIP, "Talon Duty", false);

		api.setHyperspaceMode(true);
		api.addPlugin(new SotfGoodHuntingPlugin());

		api.defeatOnShipLoss("ISS Once in a Lullaby");

		float width = 12000f;
		float height = 14000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
	}

	// Warhorn's interruption
	public final static class SotfGoodHuntingPlugin extends BaseEveryFrameCombatPlugin {

		private boolean reallyStarted = false;
		private boolean started = false;
		private float timer = 0f;
		private float warhornSpawn = 11f;

		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			if (!started) {
				started = true;
				return;
			}
			if (!reallyStarted) {
				reallyStarted = true;
				Global.getCombatEngine().getCustomData().put(SotfIDs.GOODHUNTING_KEY, true);
			}

			if (Global.getCombatEngine().isPaused()) {
				return;
			}

			timer += amount;

			if (timer >= warhornSpawn) {
				Global.getCombatEngine().addLayeredRenderingPlugin(new SotfGHWarhornIntrusionPlugin(Misc.getPointAtRadius(Global.getCombatEngine().getPlayerShip().getLocation(), 1000f)));
				Global.getCombatEngine().removePlugin(this);
			}
		}

		@Override
		public void init(CombatEngineAPI engine) {
		}
	}

}
