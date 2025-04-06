// objective effect: periodically spawns droneships allied with the objective's holder
package data.scripts.combat.obj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.combat.BaseBattleObjectiveEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.util.*;

import static data.scripts.SotfModPlugin.OFFICER_CONVO_PATH;
import static data.scripts.SotfModPlugin.REMOTE_AI_JSON;
import static data.scripts.campaign.ids.SotfIDs.STAT_PROXY_REINFORCEMENTS;

public class SotfReinforcerEffect extends BaseBattleObjectiveEffect {

	public static final Map<String, String> OBJ_TO_FACTION_MAP = new HashMap<>();
	static {
		OBJ_TO_FACTION_MAP.put(SotfIDs.OBJ_REINFORCER, Factions.DERELICT);
		OBJ_TO_FACTION_MAP.put(SotfIDs.OBJ_REINFORCER_REM, Factions.REMNANTS);
		OBJ_TO_FACTION_MAP.put(SotfIDs.OBJ_REINFORCER_PROXY, SotfIDs.DUSTKEEPERS_PROXIES);

		// Knights of Ludd crossovers
		OBJ_TO_FACTION_MAP.put(SotfIDs.OBJ_REINFORCER_KOL_DAWN, "zea_dawn");
		OBJ_TO_FACTION_MAP.put(SotfIDs.OBJ_REINFORCER_KOL_DUSK, "zea_dusk");
		OBJ_TO_FACTION_MAP.put(SotfIDs.OBJ_REINFORCER_KOL_ELYSIA, "zea_elysians");
	}

	public ShipAPI ship;
	public Vector2f loc;
	public String factionName = "allied";

	public static float reinforce_timer = 20;
	public static String REINFORCEMENT_RESPAWN_KEY = "sotf_reinforcement_data_key";
	public static String REINFORCEMENT_SHIP_KEY = "$sotf_reinforcementship";

	//public static Map<String, SotfFactionRemoteAIData> REMOTE_AI_FACTIONS = new HashMap<String, SotfFactionRemoteAIData>();

	// Standard key tracks what type of remote AI a side has access to
	public static String REMOTE_AI_KEY = "$sotf_sideRemoteAI_";
	// Max key tracks how many of each AI core a side has to use
	public static String REMOTE_AI_MAX_KEY = "$sotf_sideRemoteAIMax_";
	// Own max key is player-side only, tracks how many cores the player themselves have
	public static String PLAYER_OWN_AI_MAX_KEY = "$sotf_playerOwnRemoteAIMax";
	// Queue key tracks how many AI cores have been assigned but not deployed this wave (i.e earlier this frame)
	public static String REMOTE_AI_QUEUE_KEY = "$sotf_sideRemoteAIQueue_";

	private Object STATUSKEY;

	public static class SotfFactionRemoteAIData {
		public int alphas = 0;
		public int betas = 0;
		public int gammas = 0;
		public SotfFactionRemoteAIData() {
		}
	}

	public static class SotfReinforcementRespawnTimer {
		IntervalUtil interval = new IntervalUtil(reinforce_timer, reinforce_timer);
	}

	public void init(CombatEngineAPI engine, BattleObjectiveAPI objective) {
		super.init(engine, objective);

//		try {
//			loadData();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		} catch (JSONException e) {
//			throw new RuntimeException(e);
//		}
		if (OBJ_TO_FACTION_MAP.containsKey(objective.getType())) {
			factionName += " " + Global.getSettings().createBaseFaction(OBJ_TO_FACTION_MAP.get(objective.getType())).getEntityNamePrefix();
		}


		// determine if the player and enemy sides are capable of using remote AI captains, and how many
		if (!engine.getCustomData().containsKey(REMOTE_AI_KEY + 0)) {
			setUpRemoteAIForSide(0);
			setUpRemoteAIForSide(1);
		}

		// keeps track of how many AI cores have already been assigned earlier in a reinforcement wave
		engine.getCustomData().put(REMOTE_AI_QUEUE_KEY + 0, new float[]{0,0,0});
		engine.getCustomData().put(REMOTE_AI_QUEUE_KEY + 1, new float[]{0,0,0});
	}

//	public void loadData() throws IOException, JSONException {
//		JSONObject remoteAIJson = Global.getSettings().getMergedJSONForMod(REMOTE_AI_JSON, SotfIDs.SOTF);
//		Iterator iter = remoteAIJson.keys();
//		while (iter.hasNext()) {
//			String id = (String) iter.next();
//			JSONObject entryJson = remoteAIJson.getJSONObject(id);
//
//			SotfFactionRemoteAIData data = new SotfFactionRemoteAIData();
//			data.alphas = entryJson.optInt("alphas", 0);
//			data.betas = entryJson.optInt("betas", 1);
//			data.gammas = entryJson.optInt("gammas", 3);
//			REMOTE_AI_FACTIONS.put(id, data);
//		}
//	}

	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused()) {return;}
		// should we show a status on the player's screen?
		boolean status = false;
		String sub = "";
		// number of living reinforcement ships
		int alive = 0;
		// threshold of reinforcement ships alive where the timer should tick, and also how many to spawn
		int threshold = 0;
		// should we spawn a destroyer reinforcement?
		boolean destroyer = false;
		// are there any active ships on the side we're reinforcing?
		boolean alliesToReinforce = false;

		String key = REINFORCEMENT_RESPAWN_KEY + "_" + objective.getOwner();
		SotfReinforcementRespawnTimer data = (SotfReinforcementRespawnTimer) engine.getCustomData().get(key);
		if (data == null) {
			data = new SotfReinforcementRespawnTimer();
			engine.getCustomData().put(key, data);
		}

		if (objective.getOwner() != 0 && objective.getOwner() != 1) {
			return;
		}

		// get the reinforcement threshold - 1 per reinforcer, aka half the maximum
		for (BattleObjectiveAPI obj : engine.getObjectives()) {
			if (obj.getOwner() == objective.getOwner() && OBJ_TO_FACTION_MAP.containsKey(obj.getType())) {
				threshold++;
				if (threshold >= 2) {
					destroyer = true;
				}
			}
		}

		// figure out how many reinforcer ships are alive, and check to see if there is anyone to actually reinforce
		for (ShipAPI ship : engine.getShips()) {
			if (ship.getOwner() == objective.getOwner() && ship.getVariant().hasTag(REINFORCEMENT_SHIP_KEY) && !ship.isHulk()) {
				alive++;
				if (!ship.getHullSize().equals(ShipAPI.HullSize.FRIGATE)) {
					destroyer = false;
				}
			}
			if (ship.getOwner() == objective.getOwner() && !ship.getVariant().hasTag(REINFORCEMENT_SHIP_KEY) && !ship.getVariant().hasTag("$sotf_objship") && !ship.isHulk() && !ship.isFighter()) {
				alliesToReinforce = true;
			}
		}

		if (engine.getFleetManager(objective.getOwner()).getTaskManager(false).isInFullRetreat()) {
			alliesToReinforce = false;
		}

		// if we have less reinforcers alive than the threshold, tick down the reinforcement timer, reinforce up to limit
		if ((objective.getOwner() == 0 || objective.getOwner() == 1) && alive <= threshold && !engine.isPaused() && alliesToReinforce) {
			data.interval.advance(amount / threshold);
			if (data.interval.intervalElapsed()) {
				engine.getCustomData().put(REMOTE_AI_QUEUE_KEY + objective.getOwner(), new float[]{0, 0, 0});
				for (int i = 0; i < threshold; i++) {
					if (destroyer) {
						spawnReinforcerShips(engine, objective, true);
						destroyer = false;
					} else {
						spawnReinforcerShips(engine, objective, false);
					}
				}
			}
		}

		// if all allies are dead, cap reinforcer CR at 10%
		if (!alliesToReinforce) {
			for (ShipAPI ship : engine.getShips()) {
				if (ship.getOwner() == objective.getOwner() && ship.getVariant().hasTag(REINFORCEMENT_SHIP_KEY) && !ship.isHulk() && ship.getCurrentCR() > 0.1f) {
					ship.setCurrentCR(0.1f);
				}
			}
		}

		// status message
		if (objective.getOwner() == 0 && alive <= threshold && !engine.isPaused() && alliesToReinforce) {
			float time = data.interval.getElapsed();
			if (time > reinforce_timer - 1) {
				sub = "Incoming!";
			}
			//else if (time <= 1) {
			//	sub = "Broadcasting...";
			//}
			else {
				sub = "Inbound - " + Math.round(reinforce_timer - time) + " seconds";
			}
		} else {
			int needed = Math.round(alive - threshold);
			if (needed < 0) {
				needed = 0;
			}
			String losses = "losses";
			if (needed == 1) {
				losses = "loss";
			}
			sub = "" + alive + " alive, " + needed + " " + losses + " until next wave";
		}

		if (objective.getOwner() == 0) {
			engine.maintainStatusForPlayerShip(STATUSKEY, Global.getSettings().getSpriteName("objectives",
					"sotf_objective_reinf"), "droneship reinforcements", sub, false);
		}

		// clear fog of war, standard radius
		revealArea(999f);
	}


	public String getLongDescription() {
		float min = Global.getSettings().getFloat("minFractionOfBattleSizeForSmallerSide");
		int total = Global.getSettings().getBattleSize();
		int maxPoints = (int)Math.round(total * (1f - min));
		String playerRemoteAI = (String) engine.getCustomData().get(REMOTE_AI_KEY + 0);
		int[] availableCores = {0,0,0};
		CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
		String remoteAIText = "- with proper expertise, droneships can be remotely captained by AI cores in cargo\n";
		if (playerRemoteAI.equals("player") || playerRemoteAI.equals("hybrid")) {
			availableCores[0] += playerCargo.getCommodityQuantity(Commodities.ALPHA_CORE);
			availableCores[1] += playerCargo.getCommodityQuantity(Commodities.BETA_CORE);
			availableCores[2] += playerCargo.getCommodityQuantity(Commodities.GAMMA_CORE);
			remoteAIText = "- Automated Ships skill is enabling spare AI cores to captain droneships\n";
		}
		if (playerRemoteAI.equals("ally") || playerRemoteAI.equals("hybrid")) {
			availableCores[1] += 1;
			availableCores[2] += 6;
			remoteAIText += "- allied forces are employing remote AI captains\n";
		}
		if (!Arrays.equals(availableCores, new int[]{0, 0, 0})) {
			remoteAIText += "- available alpha/beta/gamma cores: " + availableCores[0] + "/" + availableCores[1] + "/" + availableCores[2] + "\n";
		}
		if (engine.getCustomData().get(REMOTE_AI_KEY + 1).equals("enemy")) {
			remoteAIText += "- enemy forces are employing remote AI captains\n";
		}
		return String.format(
				"- periodically reinforces fleet with " + factionName + " droneships\n" +
						"- capture additional objectives to increase number and strength of drones\n\n" +
						remoteAIText + "\n" +
						"+%d bonus deployment points\n" +
						"up to a maximum of " + maxPoints + " points",
				getBonusDeploymentPoints());
	}

	public List<ShipStatusItem> getStatusItemsFor(ShipAPI ship) {
		return null;
	}

	private void spawnReinforcerShips(CombatEngineAPI engine, BattleObjectiveAPI objective, boolean destroyer) {
		String variant = null;
		FactionAPI faction = pickFaction();
		String name = faction.pickRandomShipName();
		if (destroyer) {
			variant = pickDestroyer();
		} else {
			variant = pickShip();
		}
		// shut it all down if we don't end up with a variant to spawn
		if (variant == null) {
			return;
		}
		CampaignFleetAPI emptyFleet = Global.getFactory().createEmptyFleet(faction.getId(), "Reinforcements", true);
		FleetMemberAPI member = emptyFleet.getFleetData().addFleetMember(variant);
		emptyFleet.setInflater(new DefaultFleetInflater(new DefaultFleetInflaterParams()));
		emptyFleet.getInflater().setQuality(0.5f);
		if (emptyFleet.getInflater() instanceof DefaultFleetInflater) {
			DefaultFleetInflater dfi = (DefaultFleetInflater) emptyFleet.getInflater();
			((DefaultFleetInflaterParams) dfi.getParams()).allWeapons = true;
		}
		emptyFleet.inflateIfNeeded();

		member.setOwner(objective.getOwner());

		// AI core remote control
		boolean addedRemoteAI = false;
		if (member.getVariant().hasHullMod(HullMods.AUTOMATED)) {
			addedRemoteAI = tryAddAIOfficer(member);
		}

		member.setShipName(name);
		member.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(REINFORCEMENT_SHIP_KEY, 0.01f);
		if (member.getOwner() == 0) {
			member.setAlly(!addedRemoteAI);
		}

		ship = engine.getFleetManager(objective.getOwner()).spawnFleetMember(member, loc, 90f, 6f);
		ship.getVariant().addTag(REINFORCEMENT_SHIP_KEY);
		ship.getHullSpec().addTag("no_combat_chatter");
		ship.setCurrentCR(0.7f);
		if (ship.getOwner() == 0 && !addedRemoteAI) {
			ship.setAlly(true);
			DeployedFleetMemberAPI deployed = engine.getFleetManager(0).getDeployedFleetMember(ship);
			if (deployed != null) {
				engine.getFleetManager(objective.getOwner()).getTaskManager(true).orderSearchAndDestroy(deployed, false);
			}
		}

		//ship.getFleetMember().setShipName(name);
		//ship.getFleetMember().setAlly(true);
		//ship.getMutableStats().getSuppliesToRecover().modifyMult("sotf_reinforcementship", 0f);
		//ship.getMutableStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(REINFORCEMENT_SHIP_KEY, 0f);
		//ship.getFleetMember().getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(REINFORCEMENT_SHIP_KEY, 0f);

		if (addedRemoteAI) {
			ship.setCustomData("$sotf_remoteAIOfficer", true);
 		}

		if (Global.getSettings().getModManager().isModEnabled("automatic-orders")) {
			ship.getVariant().addMod("automatic_orders_no_retreat");
		}
	}

	// checks if a side can use remote AI core officers
	private void setUpRemoteAIForSide(int side) {
		String type = "none";

		// e.g we're not in a campaign
		if (Global.getSector().getPlayerPerson() == null) {
			engine.getCustomData().put(REMOTE_AI_KEY + side, type);
			return;
		}

		float[] maxNPCCores = {0,0,0};

		boolean canRemoteAI = false;
		boolean playerRemoteAI = Misc.getAllowedRecoveryTags().contains(Tags.AUTOMATED_RECOVERABLE);;
		for (PersonAPI commander : engine.getFleetManager(side).getAllFleetCommanders()) {
			if (SotfModPlugin.REMOTE_AI_FACTIONS.get(commander.getFaction().getId()) != null) {
				SotfFactionRemoteAIData data = SotfModPlugin.REMOTE_AI_FACTIONS.get(commander.getFaction().getId());
				if (maxNPCCores[0] < data.alphas) {
					maxNPCCores[0] = data.alphas;
				}
				if (maxNPCCores[1] < data.betas) {
					maxNPCCores[1] = data.betas;
				}
				if (maxNPCCores[2] < data.gammas) {
					maxNPCCores[2] = data.gammas;
				}
				// just in case someone does an oopsie, make sure they actually have non-zero cores set
				if (maxNPCCores[0] > 0 || maxNPCCores[1] > 0 || maxNPCCores[2] > 0) {
					canRemoteAI = true;
				}
			}
		}
		if (side == 1 && canRemoteAI) {
			type = "enemy";
		} else if (side == 1) {
			type = "none";
		} else if (canRemoteAI && playerRemoteAI) {
			type = "hybrid";
		} else if (canRemoteAI) {
			type = "ally";
		} else if (playerRemoteAI) {
			type = "player";
		}
		engine.getCustomData().put(REMOTE_AI_KEY + side, type);

		CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
		float[] aiCores = {0,0,0};
		if (type.equals("player") || type.equals("hybrid")) {
			aiCores[0] = playerCargo.getCommodityQuantity(Commodities.ALPHA_CORE);
			aiCores[1] = playerCargo.getCommodityQuantity(Commodities.BETA_CORE);
			aiCores[2] = playerCargo.getCommodityQuantity(Commodities.GAMMA_CORE);
		}
		if (type.equals("ally") || type.equals("hybrid") || type.equals("enemy")) {
			aiCores[0] += maxNPCCores[0];
			aiCores[1] += maxNPCCores[1];
			aiCores[2] += maxNPCCores[2];
		}
		engine.getCustomData().put(REMOTE_AI_MAX_KEY + side, aiCores);
	}

	private boolean tryAddAIOfficer(FleetMemberAPI ship) {
		String type = (String) engine.getCustomData().get(REMOTE_AI_KEY + ship.getOwner());
		if (type.equals("none")) {
			return false;
		}
		String aiCoreId = null;
		boolean conserveAlpha = false;

		String max_key = REMOTE_AI_MAX_KEY + ship.getOwner();
		float[] aiCores = (float[]) engine.getCustomData().get(max_key);

		int numObjectives = 0;
		for (BattleObjectiveAPI obj : engine.getObjectives()) {
			if (obj.getType().equals(objective.getType()) && obj.getOwner() != ship.getOwner()) {
				numObjectives++;
			}
		}
		// if only 1 Alpha, don't waste it on a frigate if destroyers can spawn
		if (numObjectives > 1 && aiCores[0] == 1) {
			conserveAlpha = true;
		}

		String queue_key = REMOTE_AI_QUEUE_KEY + ship.getOwner();
		float[] aiCoreQueue = (float[]) engine.getCustomData().get(queue_key);
		for (int i = 0; i < 2; i++) {
			aiCores[i] -= aiCoreQueue[i];
		}
		for (ShipAPI existing_ship : Global.getCombatEngine().getShips()) {
			if (existing_ship.getOwner() != ship.getOwner() || !existing_ship.isAlive() || !existing_ship.getCustomData().containsKey("$sotf_remoteAIOfficer")) {
				continue;
			}
			switch (existing_ship.getCaptain().getAICoreId()) {
				case Commodities.ALPHA_CORE:
					aiCores[0]--;
					break;
				case Commodities.BETA_CORE:
					aiCores[1]--;
					break;
				case Commodities.GAMMA_CORE:
					aiCores[2]--;
					break;
			}
		}

		if (aiCores[0] > 0 && (!conserveAlpha || !ship.isFrigate())) {
			aiCoreId = Commodities.ALPHA_CORE;
			aiCoreQueue[0] += 1;
			engine.getCustomData().put(queue_key, aiCoreQueue);
		} else if (aiCores[1] > 0) {
			aiCoreId = Commodities.BETA_CORE;
			aiCoreQueue[1] += 1;
			engine.getCustomData().put(queue_key, aiCoreQueue);
		} else if (aiCores[2] > 0) {
			aiCoreId = Commodities.GAMMA_CORE;
			aiCoreQueue[2] += 1;
			engine.getCustomData().put(queue_key, aiCoreQueue);
		}
		if (aiCoreId == null) {
			return false;
		}
		PersonAPI captain = Misc.getAICoreOfficerPlugin(aiCoreId).createPerson(aiCoreId, Factions.REMNANTS, Misc.random);
		ship.setCaptain(captain);
		// remotely piloted by the player = player gains control
		if (type.equals("player") || type.equals("hybrid")) {
			ship.setAlly(false);
		}
		return true;
	}

	private FactionAPI pickFaction() {
		String factionId = null;
		FactionAPI faction = null;
		if (OBJ_TO_FACTION_MAP.containsKey(objective.getType())) {
			factionId = OBJ_TO_FACTION_MAP.get(objective.getType());
		}
		if (factionId == null) factionId = Factions.DERELICT;
		// if Dustkeeper commander or player with Proxy Code Injector, replace Derelicts with upgraded Proxies
		if (factionId.equals(Factions.DERELICT)) {
			for (PersonAPI commander : Global.getCombatEngine().getFleetManager(objective.getOwner()).getAllFleetCommanders()) {
				// if (commander.getFaction().getId().equals(SotfIDs.DUSTKEEPERS) || commander.getStats().hasSkill(SotfIDs.SKILL_PROXYWAR)) {
				if (commander.getFaction().getId().contains(SotfIDs.DUSTKEEPERS) || commander.getStats().getDynamic().getMod(SotfIDs.STAT_PROXY_REINFORCEMENTS).computeEffective(0f) > 0f) {
					factionId = SotfIDs.DUSTKEEPERS_PROXIES;
				}
			}
		}
		if (Global.getSettings().getFactionSpec(factionId) == null) return Global.getSettings().createBaseFaction(factionId);
		if (Global.getSector() != null) {
			faction = Global.getSector().getFaction(factionId);
		} else {
			faction = Global.getSettings().createBaseFaction(factionId);
		}
		return faction;
	}

	private String pickShip() {
		FactionAPI faction = pickFaction();

		WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
		post.addAll(faction.getVariantsForRole(ShipRoles.COMBAT_SMALL));
		post.addAll(faction.getVariantsForRole(ShipRoles.PHASE_SMALL));

		for (String variantId : post.clone().getItems()) {
			if (Global.getSettings().getVariant(variantId).hasHullMod("strikeCraft")) {
				post.remove(variantId);
			}
		}
		return post.pick();
	}

	private String pickDestroyer() {
		FactionAPI faction = pickFaction();

		WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
		post.addAll(faction.getVariantsForRole(ShipRoles.COMBAT_MEDIUM));
		post.addAll(faction.getVariantsForRole(ShipRoles.PHASE_MEDIUM));
		post.addAll(faction.getVariantsForRole(ShipRoles.CARRIER_SMALL));
		for (String variantId : post.clone().getItems()) {
			if (Global.getSettings().getVariant(variantId).hasHullMod("strikeCraft")) {
				post.remove(variantId);
			}
		}
		return post.pick();
	}
}