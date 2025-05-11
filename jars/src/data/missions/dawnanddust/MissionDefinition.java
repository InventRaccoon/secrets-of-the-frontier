// Mission definition for Dawn and Dust
// Player (as Sierra) defends a Dustkeeper station against Hegemony/Luddic forces
// Featuring: officer spam
package data.missions.dawnanddust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;

import java.util.ArrayList;
import java.util.List;

public class MissionDefinition implements MissionDefinitionPlugin {

    public void defineMission(MissionDefinitionAPI api) {
	    api.initFleet(FleetSide.PLAYER, "ODS", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "HSS", FleetGoal.ATTACK, true);
		
		api.setFleetTagline(FleetSide.PLAYER, "Dustkeeper's Sanctum, recon shard, and Sierra-Nought-Bravo");
		api.setFleetTagline(FleetSide.ENEMY, "Joint Hegemony and Knights of Ludd assault force");

		api.addBriefingItem("Dance");
		api.addBriefingItem("The Sanctum must survive - it is highly vulnerable to the enemy's strike weapons");
		api.addBriefingItem("Use the Voidwitch's mobility to strike at the enemy's rear or intercept fire for allies");
		api.addBriefingItem("Leverage your inhumanly skilled forces in a well-timed full assault");

		FactionAPI dustkeepers = Global.getSettings().createBaseFaction(SotfIDs.DUSTKEEPERS);

		// Sierra
		FleetMemberAPI flagship = api.addToFleet(FleetSide.PLAYER, "sotf_vow_rem_Sierras", FleetMemberType.SHIP, "Voidwitch",true);
		PersonAPI sierra = SotfPeople.genSierra(7);
		//sierra.getStats().setSkillLevel(SotfIDs.SKILL_SCRAPSCREEN, 2); // for testing
		flagship.setCaptain(sierra);
		flagship.getCrewComposition().setCrew(0);
		flagship.getRepairTracker().setCR(0.85f);

		// Sanctum, operated by Haven-Affix-Wellspring
		FleetMemberAPI sanctum = api.addToFleet(FleetSide.PLAYER, "sotf_sanctum_Vigilant", FleetMemberType.SHIP, "Dustkeeper's Sanctum",false);
		PersonAPI haven = SotfPeople.genHaven();
		//haven.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 0);
		sanctum.setCaptain(haven);
		sanctum.getRepairTracker().setCR(0.7f);
		api.getDefaultCommander(FleetSide.PLAYER).setStats(haven.getFleetCommanderStats());
		flagship.setFleetCommanderForStats(haven, null);

		// the ODS Dusklight operated by Watchful-Echo-Courser (defined later), only on ship 2 at this stage
		List<FleetMemberAPI> dustkeeperShips = new ArrayList<>();
		FleetMemberAPI dusklight = api.addToFleet(FleetSide.PLAYER, "sotf_brilliant_Courser", FleetMemberType.SHIP, "ODS Dusklight II", false);
		dustkeeperShips.add(dusklight);

		//api.addToFleet(FleetSide.PLAYER, "sotf_picket_aux_Hull", FleetMemberType.SHIP, "TEST", false);
		dustkeeperShips.add(api.addToFleet(FleetSide.PLAYER, "fulgent_Assault", FleetMemberType.SHIP, "ODS Sorrow of Acedia II", false));
		dustkeeperShips.add(api.addToFleet(FleetSide.PLAYER, "fulgent_Support", FleetMemberType.SHIP, "ODS Memory of Maairath IV", false));
		// "oh come on dude you're not helping our image"
		dustkeeperShips.add(api.addToFleet(FleetSide.PLAYER, "scintilla_Strike", FleetMemberType.SHIP, "ODS Bloodsoaked Soulrender II", false));
		// HAHA GET IT BECAUSE IT HAS A BUNCH OF SPARKS
		dustkeeperShips.add(api.addToFleet(FleetSide.PLAYER, "scintilla_Support", FleetMemberType.SHIP, "ODS Spark in the Abyss III", false));

		// gen Dustkeeper warmind subroutines. Beta-level echoes
		for (FleetMemberAPI dustkeeperShip : dustkeeperShips) {
			PersonAPI captain = dustkeepers.createRandomPerson();
			captain.setAICoreId(Commodities.BETA_CORE);
			SotfMisc.dustkeeperifyAICore(captain);

			captain.setPersonality(Personalities.STEADY);
			captain.getStats().setLevel(5);
			captain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
			captain.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
			captain.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);

			if (dustkeeperShip.getNumFlightDecks() > 0) {
				captain.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
				captain.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
			} else {
				captain.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
				captain.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
			}

			// early Courser before being regenned as an affix - has Cyberwarfare
			if (dustkeeperShip.equals(dusklight)) {
				//captain.setAICoreId(Commodities.ALPHA_CORE);
				//captain.getStats().setLevel(7);
				//captain.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
				//captain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
				captain.getStats().setLevel(6);
				captain.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2);
				captain.getName().setFirst("Watchful-Echo-Courser");
				captain.getName().setLast("");
				captain.getMemoryWithoutUpdate().set("$chatterChar", "robotic");
				captain.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "courser"));
			}

			dustkeeperShip.setFleetCommanderForStats(haven, null);
			dustkeeperShip.setCaptain(captain);
			dustkeeperShip.getRepairTracker().setCR(0.85f);
			dustkeeperShip.updateStats();
		}

		FactionAPI hegemony = Global.getSettings().createBaseFaction(Factions.HEGEMONY);
		FactionAPI church = Global.getSettings().createBaseFaction(Factions.LUDDIC_CHURCH);

		// Victory of Mayasura (isn't that a little on the nose, Heggies?) and its battle-hardened commander
		// hey, this is where the random XIV Legion derelicts come from!
		FleetMemberAPI mayasura = api.addToFleet(FleetSide.ENEMY, "legion_xiv_Elite", FleetMemberType.SHIP, "HSS Victory of Mayasura", false);
		PersonAPI mayasuraCaptain = hegemony.createRandomPerson(FullName.Gender.MALE);
		mayasuraCaptain.getStats().setLevel(7);
		mayasuraCaptain.setPortraitSprite("graphics/portraits/portrait_hegemony05.png");
		mayasuraCaptain.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
		mayasuraCaptain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
		mayasuraCaptain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
		mayasuraCaptain.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 1);
		mayasuraCaptain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
		mayasuraCaptain.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
		mayasuraCaptain.getStats().setSkillLevel(Skills.POINT_DEFENSE, 1);

		mayasuraCaptain.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
		mayasuraCaptain.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
		mayasuraCaptain.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
		mayasuraCaptain.setPersonality(Personalities.AGGRESSIVE);
		mayasura.setFleetCommanderForStats(mayasuraCaptain, null);
		mayasura.setCaptain(mayasuraCaptain);
		mayasura.getRepairTracker().setCR(0.85f);

		api.getDefaultCommander(FleetSide.ENEMY).setStats(mayasuraCaptain.getFleetCommanderStats());

		// Apocalypse Waning and her seasoned knight-commander
		FleetMemberAPI apocalypse = api.addToFleet(FleetSide.ENEMY, "retribution_Standard", FleetMemberType.SHIP, "CGR Apocalypse Waning", false);
		PersonAPI apocalypseCaptain = church.createRandomPerson(FullName.Gender.FEMALE);
		apocalypseCaptain.getStats().setLevel(6);
		apocalypseCaptain.setPortraitSprite("graphics/portraits/portrait_luddic07.png");

		apocalypseCaptain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
		apocalypseCaptain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
		apocalypseCaptain.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
		apocalypseCaptain.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
		apocalypseCaptain.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
		apocalypseCaptain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);

		apocalypseCaptain.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
		apocalypseCaptain.setPersonality(Personalities.RECKLESS);
		apocalypse.setCaptain(apocalypseCaptain);
		apocalypse.getRepairTracker().setCR(0.85f);

		List<FleetMemberAPI> hegemonyShips = new ArrayList<>();
		List<FleetMemberAPI> luddicShips = new ArrayList<>();

		// Cruiser Crew headed by Big Renai
		hegemonyShips.add(api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, "HSS Big Renai", false));
		hegemonyShips.add(api.addToFleet(FleetSide.ENEMY, "eagle_xiv_Elite", FleetMemberType.SHIP, "HSS Jolene", false));
		//luddicShips.add(api.addToFleet(FleetSide.ENEMY, "sotf_selene_d_Redeemed", FleetMemberType.SHIP, "CGR Unity of Self", false));
		hegemonyShips.add(api.addToFleet(FleetSide.ENEMY, "eagle_xiv_Elite", FleetMemberType.SHIP, "HSS Fia", false));
		luddicShips.add(api.addToFleet(FleetSide.ENEMY, "eradicator_Fighter_Support", FleetMemberType.SHIP, "CGR Reclamation", false));
		luddicShips.add(api.addToFleet(FleetSide.ENEMY, "eradicator_Fighter_Support", FleetMemberType.SHIP, "CGR Condemnation", false));

		// couple of heavy escort destroyers
		hegemonyShips.add(api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, "HSS Blackhill", false));
		hegemonyShips.add(api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, "HSS Morrow", false));
		luddicShips.add(api.addToFleet(FleetSide.ENEMY, "manticore_Support", FleetMemberType.SHIP, "CGR Blossom of July", false));
		luddicShips.add(api.addToFleet(FleetSide.ENEMY, "manticore_Support", FleetMemberType.SHIP, "CGR Solitude of November", false));

		// peace be with you, spawn of Moloch *releases Hammers*
		FleetMemberAPI blessings = api.addToFleet(FleetSide.ENEMY, "mule_Fighter_Support", FleetMemberType.SHIP, "CGR Blessings For All", false);
		luddicShips.add(blessings);
		blessings.getVariant().setWingId(0, "perdition_wing");
		blessings.getVariant().setNumFluxVents(14);

		FleetMemberAPI gift = api.addToFleet(FleetSide.ENEMY, "mule_Fighter_Support", FleetMemberType.SHIP, "CGR Gift of Tommorrow", false);
		luddicShips.add(gift);
		gift.getVariant().setWingId(0, "perdition_wing");
		gift.getVariant().setNumFluxVents(14);

		// these guys know they're cannon fodder but by Ludd they're proud of it
		hegemonyShips.add(api.addToFleet(FleetSide.ENEMY, "vanguard_Attack", FleetMemberType.SHIP, "HSS Shark Bait", false));
		hegemonyShips.add(api.addToFleet(FleetSide.ENEMY, "vanguard_Attack", FleetMemberType.SHIP, "HSS Good Day To Die", false));
		luddicShips.add(api.addToFleet(FleetSide.ENEMY, "vanguard_Strike", FleetMemberType.SHIP, "CGR Sacrifice of Body", false));
		luddicShips.add(api.addToFleet(FleetSide.ENEMY, "vanguard_Strike", FleetMemberType.SHIP, "CGR Glory of Hereafter", false));

		// gen Hegemony captains. Stalwart, disciplined, GUNS, GUNS, GUNS!
		for (FleetMemberAPI hegemonyShip : hegemonyShips) {
			PersonAPI captain = hegemony.createRandomPerson();
			captain.getStats().setLevel(3);
			captain.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
			captain.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
			captain.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 1);
			captain.setPersonality(Personalities.AGGRESSIVE);

			if (hegemonyShip.isCruiser()) {
				captain.getStats().setLevel(4);
				captain.getStats().setSkillLevel(Skills.POINT_DEFENSE, 1);
			}

			hegemonyShip.setFleetCommanderForStats(mayasuraCaptain, null);
			hegemonyShip.setCaptain(captain);
			hegemonyShip.getRepairTracker().setCR(0.85f);
		}
		// gen Luddic captains. Determined, zealous, HAMMERS!
		for (FleetMemberAPI luddicShip : luddicShips) {
			PersonAPI captain = church.createRandomPerson();
			captain.getStats().setLevel(2);
			captain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
			captain.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 1);
			captain.setPersonality(Personalities.RECKLESS);

			if (luddicShip.isCruiser()) {
				captain.getStats().setLevel(3);
				captain.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
			}

			// Selene captains are generally the more tech-friendly ones
			//if (luddicShip.getHullId().contains("selene")) {
			//	captain.getStats().setLevel(5);
			//	captain.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
			//	captain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
			//	captain.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
			//	captain.getStats().setSkillLevel(SotfIDs.SKILL_OATHSWORNZEAL, 1);
			//}

			luddicShip.setFleetCommanderForStats(apocalypseCaptain, null);
			luddicShip.setCaptain(captain);
			luddicShip.getRepairTracker().setCR(0.85f);
		}

		api.defeatOnShipLoss("Dustkeeper's Sanctum");
		//api.defeatOnShipLoss("Voidwitch");

		api.addPlugin(new SotfDawnAndDustPlugin());

		float width = 12000f;
		float height = 14000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
	}

	public final static class SotfDawnAndDustPlugin extends BaseEveryFrameCombatPlugin {

		private boolean reallyStarted = false;
		private boolean started = false;

		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			// Sierra is confined to her own ship
			for (ShipAPI ship : Global.getCombatEngine().getShips()) {
				ship.setInvalidTransferCommandTarget(true);
			}
			if (!started) {
				started = true;
				return;
			}
			if (!reallyStarted) {
				reallyStarted = true;
				Global.getCombatEngine().getCustomData().put(SotfIDs.DAWNANDDUST_KEY, true);
				Global.getCombatEngine().getCustomData().put(SotfIDs.ALWAYS_CONVO_KEY, true);
				Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getTaskManager(false).setFullAssault(true);
				//Global.getSoundPlayer().playCustomMusic(0, 0, "sotf_weightlessthoughts", true); // just a test
			}

			if (Global.getCombatEngine().isPaused()) {
				return;
			}
		}

		@Override
		public void init(CombatEngineAPI engine) {
		}
	}

}
