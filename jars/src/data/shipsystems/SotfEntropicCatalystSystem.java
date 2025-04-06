package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;

/**
 *  Resonance Catalyst: debuffs the target ship while also self-buffing the user
 */

public class SotfEntropicCatalystSystem extends BaseShipSystemScript {
	public static Object KEY_SHIP = new Object();
	public static Object KEY_TARGET = new Object();

	// top speed/accel bonus while amplifying entropy
	public static float SELF_SPEED_MULT = 1.35f;
	public static float SELF_DAM_MULT = 0.8f;

	public static float DAM_MULT = 1.25f;
	public static float PHASE_FLUX_MULT = 2f;
	protected static float RANGE = 1500f;
	
	public static Color TEXT_COLOR = new Color(215,235,255,255);
	
	public static Color JITTER_COLOR = new Color(125,165,195,75);
	public static Color JITTER_UNDER_COLOR = new Color(255,100,100,155);

	
	public static class TargetData {
		public ShipAPI ship;
		public ShipAPI target;
		public EveryFrameCombatPlugin targetEffectPlugin;
		public float currDamMult;
		public float currFluxCostMult;
		public float elaspedAfterInState;
		public TargetData(ShipAPI ship, ShipAPI target) {
			this.ship = ship;
			this.target = target;
		}
	}
	
	
	public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
		
		final String targetDataKey = ship.getId() + "_entropy_target_data";
		
		Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey); 
		if (state == State.IN && targetDataObj == null) {
			ShipAPI target = findTarget(ship);
			Global.getCombatEngine().getCustomData().put(targetDataKey, new TargetData(ship, target));
			if (target != null) {
				if (target.getFluxTracker().showFloaty() || 
						ship == Global.getCombatEngine().getPlayerShip() ||
						target == Global.getCombatEngine().getPlayerShip()) {
					target.getFluxTracker().showOverloadFloatyIfNeeded("Defenses Disrupted!", TEXT_COLOR, 4f, true);
					// phased? I don't think so
					target.getFluxTracker().beginOverloadWithTotalBaseDuration(0.25f);
				}
			}
		} else if (state == State.IDLE && targetDataObj != null) {
			Global.getCombatEngine().getCustomData().remove(targetDataKey);
			((TargetData)targetDataObj).currDamMult = 1f;
			targetDataObj = null;
		}
		if (targetDataObj == null || ((TargetData) targetDataObj).target == null) return;
		
		final TargetData targetData = (TargetData) targetDataObj;
		float shieldMult = 1f;
		if (targetData.target.getShield() != null) {
			shieldMult = Math.min(targetData.target.getShield().getFluxPerPointOfDamage(), 0.8f);
		}
		targetData.currDamMult = 1f + (DAM_MULT - 1f) * effectLevel / shieldMult;
		targetData.currFluxCostMult = 1f + (PHASE_FLUX_MULT - 1f) * effectLevel;
		//System.out.println("targetData.currDamMult: " + targetData.currDamMult);
		if (targetData.targetEffectPlugin == null) {
			targetData.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					if (Global.getCombatEngine().isPaused()) return;
					if (targetData.target == Global.getCombatEngine().getPlayerShip()) { 
						Global.getCombatEngine().maintainStatusForPlayerShip(KEY_TARGET, 
								targetData.ship.getSystem().getSpecAPI().getIconSpriteName(),
								targetData.ship.getSystem().getDisplayName(), 
								"" + (int)((targetData.currDamMult - 1f) * 100f) + "% more damage taken", true);
					}
					
					if (targetData.currDamMult <= 1f || !targetData.ship.isAlive()) {
						targetData.target.getMutableStats().getHullDamageTakenMult().unmodify(id);
						targetData.target.getMutableStats().getArmorDamageTakenMult().unmodify(id);
						targetData.target.getMutableStats().getShieldDamageTakenMult().unmodify(id);
						targetData.target.getMutableStats().getEmpDamageTakenMult().unmodify(id);
						targetData.target.getMutableStats().getPhaseCloakUpkeepCostBonus().unmodify(id);
						Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
					} else {
						targetData.target.getMutableStats().getHullDamageTakenMult().modifyMult(id, targetData.currDamMult);
						targetData.target.getMutableStats().getArmorDamageTakenMult().modifyMult(id, targetData.currDamMult);
						targetData.target.getMutableStats().getShieldDamageTakenMult().modifyMult(id, targetData.currDamMult);
						targetData.target.getMutableStats().getEmpDamageTakenMult().modifyMult(id, targetData.currDamMult);
						targetData.target.getMutableStats().getPhaseCloakUpkeepCostBonus().modifyMult(id, targetData.currFluxCostMult);
					}
				}
			};
			Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin);
		}
		
		if (effectLevel > 0) {
			if (state != State.IN) {
				targetData.elaspedAfterInState += Global.getCombatEngine().getElapsedInLastFrame();
			}

			ship.getMutableStats().getMaxSpeed().modifyMult(id + "_self", 1f + (SELF_SPEED_MULT - 1f) * effectLevel);
			ship.getMutableStats().getAcceleration().modifyMult(id + "_self", 1f + (SELF_SPEED_MULT - 1f) * effectLevel);
			ship.getMutableStats().getDeceleration().modifyMult(id + "_self", 1f + (SELF_SPEED_MULT - 1f) * effectLevel);
			ship.getMutableStats().getMaxTurnRate().modifyMult(id + "_self", 1f + (SELF_SPEED_MULT - 1f) * effectLevel);
			ship.getMutableStats().getTurnAcceleration().modifyMult(id + "_self", 1f + (SELF_SPEED_MULT - 1f) * effectLevel);

			ship.getMutableStats().getShieldUnfoldRateMult().modifyMult(id + "_self", 1f + (SELF_SPEED_MULT - 1f) * effectLevel);
			ship.getMutableStats().getShieldTurnRateMult().modifyMult(id + "_self", 1f + (SELF_SPEED_MULT - 1f) * effectLevel);

			ship.getMutableStats().getHullDamageTakenMult().modifyMult(id + "_self", 1f + (SELF_DAM_MULT - 1f) * effectLevel);
			ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id + "_self", 1f + (SELF_DAM_MULT - 1f) * effectLevel);
			ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id + "_self", 1f + (SELF_DAM_MULT - 1f) * effectLevel);
			ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id + "_self", 1f + (SELF_DAM_MULT - 1f) * effectLevel);

			//float shipJitterLevel = 0;
//			if (state == State.IN) {
//				shipJitterLevel = effectLevel;
//			} else {
//				float durOut = 0.5f;
//				shipJitterLevel = Math.max(0, durOut - targetData.elaspedAfterInState) / durOut;
//			}
			float targetJitterLevel = effectLevel;
			
			//float maxRangeBonus = 50f;
			//float jitterRangeBonus = shipJitterLevel * maxRangeBonus;
			
			Color color = JITTER_COLOR;
//			if (shipJitterLevel > 0) {
//				//ship.setJitterUnder(KEY_SHIP, JITTER_UNDER_COLOR, shipJitterLevel, 21, 0f, 3f + jitterRangeBonus);
//				ship.setJitter(KEY_SHIP, color, shipJitterLevel, 4, 0f, 0 + jitterRangeBonus * 1f);
//			}
			
			if (targetJitterLevel > 0) {
				//target.setJitterUnder(KEY_TARGET, JITTER_UNDER_COLOR, targetJitterLevel, 5, 0f, 15f);
				targetData.target.setJitter(KEY_TARGET, color, targetJitterLevel, 3, 0f, 5f);
				ship.setJitter(KEY_SHIP, color, targetJitterLevel, 4, 0f, 5f);
			}
		}
	}
	
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		
	}
	
	protected ShipAPI findTarget(ShipAPI ship) {
		float range = getMaxRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();
		
		if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.TARGET_FOR_SHIP_SYSTEM)){
			target = (ShipAPI) ship.getAIFlags().getCustom(AIFlags.TARGET_FOR_SHIP_SYSTEM);
		}
		
		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum) target = null;
		} else {
			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.FIGHTER, range, true);
				} else {
					Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
						float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
						if (dist > range + radSum) target = null;
					}
				}
			}
			if (target == null) {
				target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.FIGHTER, range, true);
			}
		}
		
		return target;
	}
	
	
	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
	}

	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (effectLevel > 0) {
			if (index == 0) {
				float damMult = 1f + (DAM_MULT - 1f) * effectLevel;
				return new StatusData("" + (int)((damMult - 1f) * 100f) + "% more damage to target", false);
			}
		}
		return null;
	}


	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != SystemState.IDLE) return null;
		
		ShipAPI target = findTarget(ship);
		if (target != null && target != ship) {
			return "READY";
		}
		if ((target == null) && ship.getShipTarget() != null) {
			return "OUT OF RANGE";
		}
		return "NO TARGET";
	}

	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		//if (true) return true;
		ShipAPI target = findTarget(ship);
		return target != null && target != ship;
	}

}








