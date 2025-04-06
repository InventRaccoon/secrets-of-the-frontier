// Mission definition for Die By The Sword
package data.missions.diebythesword;

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
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MissionDefinition implements MissionDefinitionPlugin {

    public void defineMission(MissionDefinitionAPI api) {
	    api.initFleet(FleetSide.PLAYER, "ODS", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "LGS", FleetGoal.ATTACK, true);
		
		api.setFleetTagline(FleetSide.PLAYER, "Ardent-Annex-Seraph, proxies, and \"Kotter's Cutthroats\"");
		api.setFleetTagline(FleetSide.ENEMY, "Eris Hazen's grand armada and Diktat anti-piracy patrol");

		api.addBriefingItem("Exact retribution on the fiend Andrada's lapdogs");
		api.addBriefingItem("Use your flagship to break through the enemy line");
		api.addBriefingItem("Utilize your cyberwarfare protocols (using [R]) to create new opportunities");
		api.addBriefingItem("Your unshielded proxies can't fight a battle of attrition - strike fast and hard!");

		FactionAPI dustkeepers = Global.getSettings().createBaseFaction(SotfIDs.DUSTKEEPERS);
		FactionAPI proxies = Global.getSettings().createBaseFaction(SotfIDs.DUSTKEEPERS_PROXIES);
		FactionAPI pirates = Global.getSettings().createBaseFaction(Factions.PIRATES);

		// Seraph
		FleetMemberAPI flagship = api.addToFleet(FleetSide.PLAYER, "sotf_respite_Assault", FleetMemberType.SHIP, "ODS Last Light III",true);
		//FleetMemberAPI flagship = api.addToFleet(FleetSide.PLAYER, "sotf_anamnesis_Artillery", FleetMemberType.SHIP, "ODS Last Light",true);
		PersonAPI seraph = SotfPeople.genSeraph();
		seraph.getStats().refreshCharacterStatsEffects();
		flagship.setCaptain(seraph);
		flagship.getRepairTracker().setCR(0.85f);

		api.getDefaultCommander(FleetSide.PLAYER).setStats(seraph.getFleetCommanderStats());

		List<FleetMemberAPI> proxyShips = new ArrayList<>();
		List<FleetMemberAPI> pirateShips = new ArrayList<>();

		// THE JUNK SQUAD!
		FleetMemberAPI rampart = api.addToFleet(FleetSide.PLAYER, "sotf_rampart_aux_Assault", FleetMemberType.SHIP, false);
		proxyShips.add(rampart);
		proxyShips.add(api.addToFleet(FleetSide.PLAYER, "sotf_rampart_aux_Siege", FleetMemberType.SHIP, false));
		proxyShips.add(api.addToFleet(FleetSide.PLAYER, "sotf_cavalier_aux_Overdriven", FleetMemberType.SHIP, false));
		proxyShips.add(api.addToFleet(FleetSide.PLAYER, "sotf_berserker_aux_Assault", FleetMemberType.SHIP, false));
		proxyShips.add(api.addToFleet(FleetSide.PLAYER, "sotf_berserker_aux_Assault", FleetMemberType.SHIP, false));
		proxyShips.add(api.addToFleet(FleetSide.PLAYER, "sotf_warden_aux_Overdriven", FleetMemberType.SHIP, false));
		proxyShips.add(api.addToFleet(FleetSide.PLAYER, "sotf_warden_aux_Overdriven", FleetMemberType.SHIP, false));
		proxyShips.add(api.addToFleet(FleetSide.PLAYER, "sotf_picket_aux_Attack", FleetMemberType.SHIP, false));
		proxyShips.add(api.addToFleet(FleetSide.PLAYER, "sotf_picket_aux_Attack", FleetMemberType.SHIP, false));
		proxyShips.add(api.addToFleet(FleetSide.PLAYER, "sotf_picket_aux_Attack", FleetMemberType.SHIP, false));
		// Kotter's "professional" popcorn munchers
		FleetMemberAPI pirate1 = api.addToFleet(FleetSide.PLAYER, "hound_d_pirates_Shielded", FleetMemberType.SHIP,false);
		FleetMemberAPI pirate2 = api.addToFleet(FleetSide.PLAYER, "kite_pirates_Raider", FleetMemberType.SHIP,false);
		pirateShips.add(pirate1);
		pirateShips.add(pirate2);

		// proxies get integrated Gammas
		for (FleetMemberAPI proxy : proxyShips) {
			if (!proxy.getHullId().contains("picket")) {
				PersonAPI captain = Global.getFactory().createPerson();
				captain.setAICoreId(Commodities.GAMMA_CORE);
				captain.setName(new FullName(Global.getSettings().getCommoditySpec(Commodities.GAMMA_CORE).getName(), "", FullName.Gender.ANY));
				captain.setFaction(SotfIDs.DUSTKEEPERS_PROXIES);
				captain.setPortraitSprite("graphics/portraits/portrait_ai1b.png");
				captain.setPersonality(Personalities.RECKLESS);
				captain.getStats().setLevel(4);
				captain.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
				captain.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
				captain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
				captain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
				captain.getMemoryWithoutUpdate().set("$chatterChar", "robotic");
				proxy.setCaptain(captain);
			}
			proxy.getRepairTracker().setCR(0.85f);
			proxy.setFleetCommanderForStats(seraph, null);

			proxy.getVariant().addPermaMod(HullMods.COMP_ARMOR);
			proxy.getVariant().addPermaMod(HullMods.COMP_HULL);
			proxy.getVariant().addPermaMod(HullMods.FAULTY_GRID);
			proxy.getVariant().addPermaMod(HullMods.DEGRADED_ENGINES);
			proxy.setShipName(proxies.pickRandomShipName());
			proxy.updateStats();
		}

		rampart.getCaptain().setId("sotf_dbtsGamma");

		// pirate ships get Buffalo hunters with no formal training
		int iter = 1;
		for (FleetMemberAPI pirate : pirateShips) {
			PersonAPI captain = pirates.createRandomPerson();
			// wouldn't want to scratch the paint while the robots do all the work
			captain.setPersonality(Personalities.TIMID);
			captain.getStats().setLevel(1);
			captain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
			captain.setId("sotf_dbtsPirate" + iter);
			// Kotter's default portrait
			if (iter == 1) {
				captain.setPortraitSprite("graphics/portraits/portrait_pirate13.png");
			}

			pirate.getVariant().addPermaMod(HullMods.COMP_ARMOR);
			pirate.getVariant().addPermaMod(HullMods.DEGRADED_ENGINES);
			pirate.setShipName(pirates.pickRandomShipName());
			pirate.setCaptain(captain);
			pirate.getRepairTracker().setCR(0.7f);
			pirate.updateStats();
			iter++;
		}

		FactionAPI lionsGuard = Global.getSettings().createBaseFaction(Factions.LIONS_GUARD);
		FactionAPI diktat = Global.getSettings().createBaseFaction(Factions.DIKTAT);

		// "Seasoned" commander Eris Hazen at the helm of the LGS Ascension of Destiny
		FleetMemberAPI ascension = api.addToFleet(FleetSide.ENEMY, "executor_Elite", FleetMemberType.SHIP, "LGS Ascension of Destiny", false);
		PersonAPI hazen = lionsGuard.createRandomPerson(FullName.Gender.FEMALE);
		hazen.setName(new FullName("Eris", "Hazen", FullName.Gender.FEMALE));
		hazen.getStats().setLevel(2);
		hazen.setPortraitSprite("graphics/portraits/portrait_diktat13.png");
		// has Andrada quote
		hazen.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 1);
		// all the better to flee from real fights
		hazen.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);

		// lots of drills... not much in the way of actually shooting real ships
		hazen.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
		hazen.setPersonality(Personalities.AGGRESSIVE);
		ascension.setCaptain(hazen);

		api.getDefaultCommander(FleetSide.ENEMY).setStats(hazen.getFleetCommanderStats());

		List<FleetMemberAPI> lionsGuardShips = new ArrayList<>();
		List<FleetMemberAPI> diktatShips = new ArrayList<>();

		lionsGuardShips.add(api.addToFleet(FleetSide.ENEMY, "eagle_LG_Assault", FleetMemberType.SHIP, false));
		lionsGuardShips.add(api.addToFleet(FleetSide.ENEMY, "falcon_LG_CS", FleetMemberType.SHIP, false));
		lionsGuardShips.add(api.addToFleet(FleetSide.ENEMY, "hammerhead_LG_Elite", FleetMemberType.SHIP, false));
		lionsGuardShips.add(api.addToFleet(FleetSide.ENEMY, "sunder_LG_Assault", FleetMemberType.SHIP, false));
		lionsGuardShips.add(api.addToFleet(FleetSide.ENEMY, "brawler_LG_Elite", FleetMemberType.SHIP, false));
		lionsGuardShips.add(api.addToFleet(FleetSide.ENEMY, "brawler_LG_Elite", FleetMemberType.SHIP, false));
		lionsGuardShips.add(api.addToFleet(FleetSide.ENEMY, "centurion_LG_Assault", FleetMemberType.SHIP, false));

		// pirate hunting's a good job, mate
		// guarantee you won't go hungry
		// 'cuz long as there's two people left in this sector
		// someone's gonna want someone raided
		PersonAPI diktatCommander = diktat.createRandomPerson(FullName.Gender.MALE);
		diktatCommander.getStats().setLevel(4);
		diktatCommander.setPersonality(Personalities.AGGRESSIVE);
		diktatCommander.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
		diktatCommander.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
		diktatCommander.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
		diktatCommander.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);

		diktatCommander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
		diktatCommander.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
		diktatCommander.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);

		FleetMemberAPI diktatFlagship =api.addToFleet(FleetSide.ENEMY, "eagle_Assault", FleetMemberType.SHIP, false);
		diktatShips.add(diktatFlagship);
		diktatShips.add(api.addToFleet(FleetSide.ENEMY, "falcon_CS", FleetMemberType.SHIP, false));
		diktatShips.add(api.addToFleet(FleetSide.ENEMY, "hammerhead_Balanced", FleetMemberType.SHIP, false));
		diktatShips.add(api.addToFleet(FleetSide.ENEMY, "hammerhead_Support", FleetMemberType.SHIP, false));
		diktatShips.add(api.addToFleet(FleetSide.ENEMY, "centurion_Assault", FleetMemberType.SHIP, false));
		diktatShips.add(api.addToFleet(FleetSide.ENEMY, "centurion_Assault", FleetMemberType.SHIP, false));

		// loyalty, not skill
		for (FleetMemberAPI lionsGuardShip : lionsGuardShips) {
			PersonAPI captain = lionsGuard.createRandomPerson();
			captain.getStats().setLevel(1);
			captain.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 1);
			captain.setPersonality(Personalities.STEADY);

			if (lionsGuardShip.isCruiser()) {
				captain.getStats().setLevel(2);
				captain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
			}
			lionsGuardShip.setFleetCommanderForStats(hazen, null);

			lionsGuardShip.setShipName(lionsGuard.pickRandomShipName());
			lionsGuardShip.setCaptain(captain);
		}

		// ok these guys actually aren't shit
		for (FleetMemberAPI diktatShip : diktatShips) {
			PersonAPI captain = diktat.createRandomPerson();
			captain.getStats().setLevel(3);
			captain.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
			captain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
			captain.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
			captain.setPersonality(Personalities.AGGRESSIVE);

			if (diktatShip.isCruiser()) {
				captain.getStats().setLevel(4);
				captain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
			}
			diktatShip.setFleetCommanderForStats(diktatCommander, null);
			diktatShip.getRepairTracker().setCR(0.85f);

			diktatShip.setShipName(diktat.pickRandomShipName());
			diktatShip.setCaptain(captain);
		}

		diktatFlagship.setCaptain(diktatCommander);

		api.addPlugin(new SotfDieByTheSwordPlugin());

		float width = 12000f;
		float height = 14000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;

		api.setBackgroundGlowColor(Misc.setAlpha(Color.RED, 35));
		api.addPlanet(0, -150, 125f, StarTypes.RED_GIANT, 250f, true);

		api.addPlanet(-1000, 0, 300f, "rocky_ice", 100f, true);
	}

	public final static class SotfDieByTheSwordPlugin extends BaseEveryFrameCombatPlugin {

		private boolean reallyStarted = false;
		private boolean started = false;

		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			// Seraph can't "transfer command", that's not how it works
			for (ShipAPI ship : Global.getCombatEngine().getShips()) {
				ship.setInvalidTransferCommandTarget(true);
			}
			if (!started) {
				started = true;
				return;
			}
			if (!reallyStarted) {
				reallyStarted = true;
				Global.getCombatEngine().getCustomData().put(SotfIDs.DIEBYTHESWORD_KEY, true);
				Global.getCombatEngine().getCustomData().put(SotfIDs.ALWAYS_CONVO_KEY, true);
				//Global.getSoundPlayer().playCustomMusic(0, 0, "sotf_weightlessthoughts", true); // just a test
			}
		}

		@Override
		public void init(CombatEngineAPI engine) {
		}
	}
}
