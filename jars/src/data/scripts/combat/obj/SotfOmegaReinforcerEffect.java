package data.scripts.combat.obj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.combat.BaseBattleObjectiveEffect;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.impl.hullmods.ShardSpawner;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import org.magiclib.util.MagicRender;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static data.scripts.combat.obj.SotfReinforcerEffect.REINFORCEMENT_SHIP_KEY;

/**
 *	Fractal Beacon - spawns a single friendly Omega shard for whoever gets it, then vanishes
 */

public class SotfOmegaReinforcerEffect extends BaseBattleObjectiveEffect {

	// the Music is suggested to be anathema to AIs so having it causes the beacon to refuse to aid you
	// capping it is instead only an opportunity to deny it from the enemy
	public static Set<String> MUSIC_HULLMODS = new HashSet<>();
	static {
		MUSIC_HULLMODS.add(SotfIDs.HULLMOD_WISPERSONG);
		MUSIC_HULLMODS.add("ex_phase_coils"); // Ziggy hullmod
	}

	public boolean isDone = false;
	private float facing = 0f;

	public void init(CombatEngineAPI engine, BattleObjectiveAPI objective) {
		super.init(engine, objective);
		facing = (float) Math.random() * 360f;
	}

	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (isDone) return;
		// looks weird if it's transparent and always pointing up
		// so make it opaque and face a random direction
		objective.getSprite().setAlphaMult(1f);
		objective.getSprite().setAngle(facing);
		if (engine.isPaused()) {return;}

		if (objective.getOwner() != 0 && objective.getOwner() != 1) {
			// AI prioritises fighting over the fractal beacon
			if (!objective.getCustomData().containsKey("sotf_didOrderFullAssault")) {
				engine.getFleetManager(1).getTaskManager(false).createAssignment(CombatAssignmentType.ASSAULT, objective, false);
				objective.getCustomData().put("sotf_didOrderFullAssault", true);
				for (CombatFleetManagerAPI.AssignmentInfo assignment : engine.getFleetManager(1).getTaskManager(false).getAllAssignments()) {
					if (assignment.getType().equals(CombatAssignmentType.CAPTURE)
							|| assignment.getType().equals(CombatAssignmentType.ASSAULT)
							&& assignment.getTarget() != objective) {
						engine.getFleetManager(1).getTaskManager(false).removeAssignment(assignment);
					}
				}
			}
			return;
		}

		manifestOmegaShard();
		// fade out the objective
		MagicRender.battlespace(
				objective.getSprite(),
				objective.getLocation(),
				new Vector2f(0,0),
				new Vector2f(objective.getSprite().getWidth(), objective.getSprite().getHeight()),
				new Vector2f(0,0),
				facing - 90f,
				25f,
				Color.WHITE,
				true,
				0f,
				0f,
				0f,
				0f,
				0f,
				0f,
				0f,
				3f,
				CombatEngineLayers.BELOW_SHIPS_LAYER
		);
		// jitter
		MagicRender.battlespace(
				objective.getSprite(),
				objective.getLocation(),
				new Vector2f(0,0),
				new Vector2f(objective.getSprite().getWidth(), objective.getSprite().getHeight()),
				new Vector2f(0,0),
				facing - 90f,
				25f,
				Misc.setAlpha(RiftCascadeEffect.STANDARD_RIFT_COLOR, 90),
				true,
				15,
				5,
				0f,
				0f,
				0f,
				0f,
				0f,
				3f,
				CombatEngineLayers.UNDER_SHIPS_LAYER
		);
		Global.getCombatEngine().removeEntity(objective);

		isDone = true;
	}


	public String getLongDescription() {
		float min = Global.getSettings().getFloat("minFractionOfBattleSizeForSmallerSide");
		int total = Global.getSettings().getBattleSize();
		int maxPoints = (int)Math.round(total * (1f - min));

		return String.format(
				"- device of unknown purpose\n" +
						"- detecting exotic energy fluctuations, unstable physical presence\n" +
						"- signs of complex interface, expect lengthy capture time\n");
	}

	public List<ShipStatusItem> getStatusItemsFor(ShipAPI ship) {
		return null;
	}

	private void manifestOmegaShard() {
		String variant = null;
		FactionAPI faction = pickFaction();
		String name = faction.pickRandomShipName();
		variant = pickShip();
		// shut it all down if we don't end up with a variant to spawn
		if (variant == null) {
			return;
		}
		//emptyFleet.getInflater().setQuality(0.5f);
		//if (emptyFleet.getInflater() instanceof DefaultFleetInflater) {
		//	DefaultFleetInflater dfi = (DefaultFleetInflater) emptyFleet.getInflater();
		//	((DefaultFleetInflaterParams) dfi.getParams()).allWeapons = true;
		//}
		//emptyFleet.inflateIfNeeded();
		CombatFleetManagerAPI fleetManager = engine.getFleetManager(objective.getOwner());
		Color messageColor = Misc.getPositiveHighlightColor();
		String friendlyOrHostile = "friendly";
		if (objective.getOwner() == 1) {
			messageColor = Misc.getNegativeHighlightColor();
			friendlyOrHostile = "hostile";
		}

		List<FleetMemberAPI> shipsToCheck = fleetManager.getDeployedCopy();
		shipsToCheck.addAll(fleetManager.getReservesCopy());
		for (FleetMemberAPI member : shipsToCheck) {
			for (String hullmod : MUSIC_HULLMODS) {
				if (member.getVariant().hasHullMod(hullmod)) {
					engine.getCombatUI().addMessage(0, objective, messageColor, objective.getDisplayName(), Misc.getTextColor(), " has vanished...?");
					return;
				}
			}
		}

		boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
		fleetManager.setSuppressDeploymentMessages(true);

		CampaignFleetAPI emptyFleet = Global.getFactory().createEmptyFleet(faction.getId(), "Reinforcements", true);
		FleetMemberAPI member = emptyFleet.getFleetData().addFleetMember(variant);
		member.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
		emptyFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		member.setShipName(name);
		//member.setAlly(true);
		member.setOwner(objective.getOwner());
		member.setCaptain(Misc.getAICoreOfficerPlugin(Commodities.OMEGA_CORE).createPerson(Commodities.OMEGA_CORE, Factions.OMEGA, new Random()));

		ShipAPI ship = engine.getFleetManager(objective.getOwner()).spawnFleetMember(member, Misc.getPointAtRadius(objective.getLocation(), 750f), 0f, 0f);
		engine.getCombatUI().addMessage(0, ship, Misc.getTextColor(), "Unidentified ", messageColor, friendlyOrHostile, Misc.getTextColor(), " vessel has manifested");
		engine.getCombatUI().addMessage(0, objective, messageColor, objective.getDisplayName(), Misc.getTextColor(), " has vanished");
		fleetManager.setSuppressDeploymentMessages(wasSuppressed);

		ship.getVariant().addTag(REINFORCEMENT_SHIP_KEY);
		ship.getHullSpec().addTag("no_combat_chatter");
		if (ship.getOwner() == 0) {
			ship.setAlly(true);
		}
		//ship.getMutableStats().getSuppliesToRecover().modifyMult("sotf_reinforcementship", 0f);
		ship.getMutableStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(REINFORCEMENT_SHIP_KEY, 0.01f);
		ship.getFleetMember().getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(REINFORCEMENT_SHIP_KEY, 0.01f);

		if (Global.getSettings().getModManager().isModEnabled("automatic-orders")) {
			ship.getVariant().addMod("automatic_orders_no_retreat");
		}

		// if you let the enemy fleet snatch one of these and win, you get a random one of the shard's weapons
		// choice between easier battle or harder battle BUT Omega weapon reward
		if (objective.getOwner() == 1) {
			CampaignFleetAPI mainEnemyFleet = null;
			for (FleetMemberAPI enemyMember : engine.getFleetManager(1).getDeployedCopy()) {
				if (enemyMember.getFleetData() != null) {
					if (enemyMember.getFleetData().getFleet() != null) {
						// Fleet must actually exist in our current location - otherwise, might grab the shard's fleet (which we can't salvage from)
						if (enemyMember.getFleetData().getFleet().getContainingLocation() != null) {
							mainEnemyFleet = enemyMember.getFleetData().getFleet();
						}
					}
				}
			}
			if (mainEnemyFleet != null) {
				Global.getLogger(this.getClass()).info("Successfully found an enemy fleet to add Omega weapon to cargo of: " + mainEnemyFleet.getFullName());
				WeightedRandomPicker<WeaponAPI> weaponPicker = new WeightedRandomPicker<>();
				for (WeaponAPI weapon : ship.getAllWeapons()) {
					weaponPicker.add(weapon, weapon.getSize().ordinal() + 1);
				}
				WeaponAPI picked = weaponPicker.pick();
				if (picked != null) {
					CargoAPI extraLoot = Global.getFactory().createCargo(true);
					extraLoot.addWeapons(picked.getId(), 1);
					BaseSalvageSpecial.addExtraSalvage(mainEnemyFleet, extraLoot);
					//mainEnemyFleet.getCargo().addWeapons(picked.getId(), 1);
					Global.getLogger(this.getClass()).info("Successfully added Omega weapon to cargo of " + mainEnemyFleet.getFullName());
				}
			}
		}

		// dark blue fog like a dying Omega ship
		engine.addPlugin(new SotfObjShardFadeInPlugin(ship, 4f, 180f * objective.getOwner()));
	}

	public static class SotfObjShardFadeInPlugin extends BaseEveryFrameCombatPlugin {
		float elapsed = 0f;

		ShipAPI ship;
		float fadeInTime;
		float angle;

		IntervalUtil interval = new IntervalUtil(0.075f, 0.125f);

		public SotfObjShardFadeInPlugin(ShipAPI ship, float fadeInTime, float angle) {
			this.ship = ship;
			this.fadeInTime = fadeInTime;
			this.angle = angle;
		}


		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			if (Global.getCombatEngine().isPaused()) return;

			elapsed += amount;

			CombatEngineAPI engine = Global.getCombatEngine();

			float progress = (elapsed) / fadeInTime;
			if (progress > 1f) progress = 1f;

			ship.setAlphaMult(progress);

			if (progress < 0.5f) {
				ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
				ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
				ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
				ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
				ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
			}

			ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
			ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
			ship.blockCommandForOneFrame(ShipCommand.FIRE);
			ship.blockCommandForOneFrame(ShipCommand.PULL_BACK_FIGHTERS);
			ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
			ship.setHoldFireOneFrame(true);
			ship.setHoldFire(true);


			ship.setCollisionClass(CollisionClass.NONE);
			ship.getMutableStats().getHullDamageTakenMult().modifyMult("ShardSpawnerInvuln", 0f);
			if (progress < 0.5f) {
				ship.getVelocity().set(new Vector2f());
			} else if (progress > 0.75f){
				ship.setCollisionClass(CollisionClass.SHIP);
				ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
			}

//					Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(source.getLocation(), ship.getLocation()));
//					dir.scale(amount * 50f * progress);
//					Vector2f.add(ship.getLocation(), dir, ship.getLocation());


			float jitterLevel = progress;
			if (jitterLevel < 0.5f) {
				jitterLevel *= 2f;
			} else {
				jitterLevel = (1f - jitterLevel) * 2f;
			}

			float jitterRange = 1f - progress;
			float maxRangeBonus = 50f;
			float jitterRangeBonus = jitterRange * maxRangeBonus;
			Color c = ShardSpawner.JITTER_COLOR;

			ship.setJitter(this, c, jitterLevel, 25, 0f, jitterRangeBonus);

			interval.advance(amount);
			if (interval.intervalElapsed() && progress < 0.8f) {
				c = RiftLanceEffect.getColorForDarkening(RiftCascadeEffect.STANDARD_RIFT_COLOR);
				float baseDuration = 2f;
				Vector2f vel = new Vector2f(ship.getVelocity());
				float size = ship.getCollisionRadius() * 0.35f;
				for (int i = 0; i < 3; i++) {
					Vector2f point = new Vector2f(ship.getLocation());
					point = Misc.getPointWithinRadiusUniform(point, ship.getCollisionRadius() * 0.5f, Misc.random);
					float dur = baseDuration + baseDuration * (float) Math.random();
					float nSize = size;
					Vector2f pt = Misc.getPointWithinRadius(point, nSize * 0.5f);
					Vector2f v = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
					v.scale(nSize + nSize * (float) Math.random() * 0.5f);
					v.scale(0.2f);
					Vector2f.add(vel, v, v);

					float maxSpeed = nSize * 1.5f * 0.2f;
					float minSpeed = nSize * 1f * 0.2f;
					float overMin = v.length() - minSpeed;
					if (overMin > 0) {
						float durMult = 1f - overMin / (maxSpeed - minSpeed);
						if (durMult < 0.1f) durMult = 0.1f;
						dur *= 0.5f + 0.5f * durMult;
					}
					engine.addNegativeNebulaParticle(pt, v, nSize * 1f, 2f,
							0.5f / dur, 0f, dur, c);
				}
			}

			if (elapsed > fadeInTime) {
				ship.setAlphaMult(1f);
				ship.setHoldFire(false);
				ship.setCollisionClass(CollisionClass.SHIP);
				ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
				engine.removePlugin(this);
			}
		}
	}

	private FactionAPI pickFaction() {
		FactionAPI faction = null;
		if (Global.getSector() != null) {
			faction = Global.getSector().getFaction(Factions.OMEGA);
		} else {
			faction = Global.getSettings().createBaseFaction(Factions.OMEGA);
		}
		return faction;
	}

	protected String pickShip() {
		WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
		post.add("shard_left_Armorbreaker", 2f);
		post.add("shard_left_Attack", 0.5f);
		post.add("shard_left_Attack2", 0.5f);
		//post.add("shard_left_Defense"); // uh no thanks
		post.add("shard_left_Missile");
		post.add("shard_left_Shieldbreaker");

		post.add("shard_right_Attack", 1.1f);
		post.add("shard_right_Missile", 1.1f);
		post.add("shard_right_Shieldbreaker", 1.1f);
		//post.add("shard_right_Shock"); // wtf PD variant? lame
		return post.pick();
	}
}