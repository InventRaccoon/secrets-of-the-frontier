package data.scripts.world.mia;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase.PatrolFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

/**
 * Essentially a military base but in script form so it can just be added to any market
 * Used for Holdout Forgeship to handle its Proxy patrols
 */

public class SotfHoldoutProxyFleetManager implements RouteManager.RouteFleetSpawner, EveryFrameScript, FleetEventListener {

	protected boolean industryActive = false;

	public MarketAPI market;

	protected IntervalUtil tracker = new IntervalUtil(Global.getSettings().getFloat("averagePatrolSpawnInterval") * 0.7f,
			Global.getSettings().getFloat("averagePatrolSpawnInterval") * 1.3f);

	// so that patrols immediately begin spawning on game start/mod loaded
	// otherwise the game starts with no patrols because Mia doesn't exist during accelerated time
	protected float returningPatrolValue = 60f;

	public SotfHoldoutProxyFleetManager(MarketAPI source) {
		this.market = source;
	}
	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void advance(float amount) {
		if (market == null) return;
		industryActive = false;
		for (Industry ind : market.getIndustries()) {
			if (ind.isFunctional() && ind.getSpec().hasTag(Industries.TAG_HEAVYINDUSTRY)) {
				industryActive = true;
			}
		}

		if (!industryActive) return;

		float days = Global.getSector().getClock().convertToDays(amount);

		float spawnRate = 1f;
		float rateMult = market.getStats().getDynamic().getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).getModifiedValue();
		spawnRate *= rateMult;

		if (Global.getSector().isInNewGameAdvance()) {
			spawnRate *= 3f;
		}

		float extraTime = 0f;
		if (returningPatrolValue > 0) {
			// apply "returned patrols" to spawn rate, at a maximum rate of 1 interval per day
			float interval = tracker.getIntervalDuration();
			extraTime = interval * days;
			returningPatrolValue -= days;
			if (returningPatrolValue < 0) returningPatrolValue = 0;
		}
		tracker.advance(days * spawnRate + extraTime);

		if (DebugFlags.FAST_PATROL_SPAWN) {
			tracker.advance(days * spawnRate * 100f);
		}

		if (tracker.intervalElapsed()) {
			String sid = getRouteSourceId();

			int light = getCount(FleetFactory.PatrolType.FAST);
			int medium = getCount(FleetFactory.PatrolType.COMBAT);
			int heavy = getCount(FleetFactory.PatrolType.HEAVY);

			int maxLight = getMaxPatrols(FleetFactory.PatrolType.FAST);
			int maxMedium = getMaxPatrols(FleetFactory.PatrolType.COMBAT);
			int maxHeavy = getMaxPatrols(FleetFactory.PatrolType.HEAVY);

			WeightedRandomPicker<FleetFactory.PatrolType> picker = new WeightedRandomPicker<FleetFactory.PatrolType>();
			picker.add(FleetFactory.PatrolType.HEAVY, maxHeavy - heavy);
			picker.add(FleetFactory.PatrolType.COMBAT, maxMedium - medium);
			picker.add(FleetFactory.PatrolType.FAST, maxLight - light);

			if (picker.isEmpty()) return;

			FleetFactory.PatrolType type = picker.pick();
			PatrolFleetData custom = new PatrolFleetData(type);

			RouteManager.OptionalFleetData extra = new RouteManager.OptionalFleetData(market);
			extra.fleetType = type.getFleetType();

			RouteManager.RouteData route = RouteManager.getInstance().addRoute(sid, market, Misc.genRandomSeed(), extra, this, custom);
			extra.strength = (float) getPatrolCombatFP(type, route.getRandom());
			extra.strength = Misc.getAdjustedStrength(extra.strength, market);


			float patrolDays = 35f + (float) Math.random() * 10f;
			route.addSegment(new RouteManager.RouteSegment(patrolDays, market.getPrimaryEntity()));
		}
	}

	public void reportAboutToBeDespawnedByRouteManager(RouteManager.RouteData route) {

	}

	public boolean shouldRepeat(RouteManager.RouteData route) {
		return false;
	}

	public int getCount(FleetFactory.PatrolType... types) {
		int count = 0;
		for (RouteManager.RouteData data : RouteManager.getInstance().getRoutesForSource(getRouteSourceId())) {
			if (data.getCustom() instanceof PatrolFleetData) {
				PatrolFleetData custom = (PatrolFleetData) data.getCustom();
				for (FleetFactory.PatrolType type : types) {
					if (type == custom.type) {
						count++;
						break;
					}
				}
			}
		}
		return count;
	}

	public int getMaxPatrols(FleetFactory.PatrolType type) {
		int light = 3;
		int medium = 2;
		int heavy = 0;
		if (market.getSize() <= 3) {
			light = 3;
			medium = 2;
			heavy = 1;
		} else if (market.getSize() == 4) {
			light = 3;
			medium = 3;
			heavy = 1;
		} else if (market.getSize() == 5) {
			light = 4;
			medium = 3;
			heavy = 1;
		} else if (market.getSize() >= 6) {
			light = 4;
			medium = 3;
			heavy = 2;
		}
		if (type == FleetFactory.PatrolType.FAST) {
			return light + (int) market.getStats().getDynamic().getMod(Stats.PATROL_NUM_LIGHT_MOD).computeEffective(0);
		}
		if (type == FleetFactory.PatrolType.COMBAT) {
			return medium + (int) market.getStats().getDynamic().getMod(Stats.PATROL_NUM_MEDIUM_MOD).computeEffective(0);
		}
		if (type == FleetFactory.PatrolType.HEAVY) {
			return heavy + (int) market.getStats().getDynamic().getMod(Stats.PATROL_NUM_HEAVY_MOD).computeEffective(0);
		}
		return 0;
	}

	public String getRouteSourceId() {
		return "sotf_" + market.getId() + "_" + "proxies";
	}

	public static int getPatrolCombatFP(FleetFactory.PatrolType type, Random random) {
		float combat = 0;
		switch (type) {
			case FAST:
				combat = Math.round(4f + (float) random.nextFloat() * 2f) * 5f;
				break;
			case COMBAT:
				combat = Math.round(8f + (float) random.nextFloat() * 3f) * 5f;
				break;
			case HEAVY:
				combat = Math.round(14f + (float) random.nextFloat() * 5f) * 5f;
				break;
		}
		return (int) Math.round(combat);
	}

	public CampaignFleetAPI spawnFleet(RouteManager.RouteData route) {

		PatrolFleetData custom = (PatrolFleetData) route.getCustom();
		FleetFactory.PatrolType type = custom.type;

		Random random = route.getRandom();

		CampaignFleetAPI fleet = createPatrol(type, market.getFactionId(), route, market, null, random);

		if (fleet == null || fleet.isEmpty()) return null;

		fleet.addEventListener(this);

		market.getContainingLocation().addEntity(fleet);
		fleet.setFacing((float) Math.random() * 360f);
		// this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
		fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);

		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true, 0.3f);

		fleet.addScript(new PatrolAssignmentAIV4(fleet, route));

		if (custom.spawnFP <= 0) {
			custom.spawnFP = fleet.getFleetPoints();
		}

		return fleet;
	}

	public static CampaignFleetAPI createPatrol(FleetFactory.PatrolType type, String factionId, RouteManager.RouteData route, MarketAPI market, Vector2f locInHyper, Random random) {
		if (random == null) random = new Random();

		float combat = getPatrolCombatFP(type, random);
		String fleetType = type.getFleetType();

		FleetParamsV3 params = new FleetParamsV3(
				market,
				locInHyper,
				SotfIDs.DUSTKEEPERS_PROXIES,
				route == null ? null : route.getQualityOverride(),
				fleetType,
				combat, // combatPts
				0f, // freighterPts
				0f, // tankerPts
				0f, // transportPts
				0f, // linerPts
				0f, // utilityPts
				0f // qualityMod
		);
		if (route != null) {
			params.timestamp = route.getTimestamp();
		}
		params.random = random;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

		if (fleet == null || fleet.isEmpty()) return null;

		if (!fleet.getFaction().getCustomBoolean(Factions.CUSTOM_PATROLS_HAVE_NO_PATROL_MEMORY_KEY)) {
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
			if (type == FleetFactory.PatrolType.FAST || type == FleetFactory.PatrolType.COMBAT) {
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_CUSTOMS_INSPECTOR, true);
			}
		}

		fleet.setFaction(SotfIDs.DUSTKEEPERS, true);
		fleet.removeAbility(Abilities.GO_DARK);

		// bug about transponders
		fleet.getMemoryWithoutUpdate().set("$sotf_holdoutProxy", true);
		if (!Global.getSector().getMemoryWithoutUpdate().contains("$sotf_HHtOffDone")) {
			fleet.getMemoryWithoutUpdate().set(MemFlags.WILL_HASSLE_PLAYER, true);
			fleet.getMemoryWithoutUpdate().set(MemFlags.HASSLE_TYPE, "sotf_HHtOff");
		}
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);

		addProxyInteractionConfig(fleet);

		long salvageSeed = random.nextLong();
		fleet.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, salvageSeed);

		String postId = Ranks.POST_GENERIC_MILITARY; // Warfleet Commander
		String rankId = Ranks.GROUND_PRIVATE; // Shardcore

		fleet.getCommander().setPostId(postId);
		fleet.getCommander().setRankId(rankId);

		return fleet;
	}

	public boolean shouldCancelRouteAfterDelayCheck(RouteManager.RouteData route) {
		return false;
	}

	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {

	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		if (market == null || !industryActive) return;

		if (reason == FleetDespawnReason.REACHED_DESTINATION) {
			RouteManager.RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
			if (route.getCustom() instanceof PatrolFleetData) {
				PatrolFleetData custom = (PatrolFleetData) route.getCustom();
				if (custom.spawnFP > 0) {
					float fraction  = fleet.getFleetPoints() / custom.spawnFP;
					returningPatrolValue += fraction;
				}
			}
		}
	}

	public static void addProxyInteractionConfig(CampaignFleetAPI fleet) {
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
				new SotfProxyFleetInteractionConfigGen());
	}

	public static class SotfProxyFleetInteractionConfigGen implements FleetInteractionDialogPluginImpl.FIDConfigGen {
		public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
			FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
			config.showTransponderStatus = false;
			config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
				public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
					bcc.aiRetreatAllowed = false;
				}
			};
			return config;
		}
	}
	
}




