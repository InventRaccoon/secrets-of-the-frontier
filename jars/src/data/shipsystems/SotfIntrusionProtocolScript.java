package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.skills.SotfCyberwarfare;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;

import java.awt.*;
import java.util.*;
import java.util.List;

public class SotfIntrusionProtocolScript extends BaseShipSystemScript {

	public static float CWAR_COOLDOWN_MULT = 0.85f;
	
	public static float DISRUPTION_DUR = 0.1f;
	protected static float RANGE = 1600f;
	
	public static final Color OVERLOAD_COLOR = new Color(255,155,255,255);
	
	public static final Color JITTER_COLOR = new Color(155,255,255,45);
	public static final Color JITTER_UNDER_COLOR = new Color(155,255,255,155);

	public static Map<HullSize, Integer> VFX_BEAMS = new HashMap<HullSize, Integer>();
	static {
		VFX_BEAMS.put(ShipAPI.HullSize.FIGHTER, 2);
		VFX_BEAMS.put(ShipAPI.HullSize.FRIGATE, 3);
		VFX_BEAMS.put(ShipAPI.HullSize.DESTROYER, 4);
		VFX_BEAMS.put(ShipAPI.HullSize.CRUISER, 5);
		VFX_BEAMS.put(ShipAPI.HullSize.CAPITAL_SHIP, 7);
	}
	public static float VFX_BEAM_FULL = 0.25f;
	public static float VFX_BEAM_FADEOUT = 0.5f;

	public boolean shownSparks = false;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		//boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			//player = ship == Global.getCombatEngine().getPlayerShip();
		} else {
			return;
		}
		
		stats.getDynamic().getStat(SotfIDs.STAT_CYBERWARFARE_COOLDOWN_MULT).modifyMult(id, CWAR_COOLDOWN_MULT);

		if (effectLevel == 0)  {
			shownSparks = false;
			return;
		}
		
		float jitterLevel = effectLevel;
		if (state == State.OUT) {
			jitterLevel *= jitterLevel;
		}
		float jitterRangeBonus = jitterLevel * 50f;
		
		ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 21, 0f, 3f + jitterRangeBonus);
		//ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus * 0.67f);
		ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);

		if (effectLevel > 0 && !shownSparks) {
			shownSparks = true;
			SotfMisc.ewarChargeVisuals(ship);
		}
		
		String targetKey = ship.getId() + "_sotf_incursionprotocol_target";
		Object foundTarget = Global.getCombatEngine().getCustomData().get(targetKey); 
		if (state == State.IN) {
			if (foundTarget == null) {
				ShipAPI target = findTarget(ship);
				if (target != null) {
					Global.getCombatEngine().getCustomData().put(targetKey, target);
				}
			}
		} else if (effectLevel >= 1) {
			if (foundTarget instanceof ShipAPI) {
				ShipAPI target = (ShipAPI) foundTarget;
				if (target.getFluxTracker().isOverloadedOrVenting()) target = ship;
				applyEffectToTarget(ship, target);
			}
		} else if (state == State.OUT && foundTarget != null) {
			Global.getCombatEngine().getCustomData().remove(targetKey);
		}
	}
	
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponDamageMult().unmodify(id);
	}
	
	protected ShipAPI findTarget(ShipAPI ship) {
		Misc.FindShipFilter filter = new Misc.FindShipFilter() {
			public boolean matches(ShipAPI ship) {
				return !ship.getHullSpec().hasTag(Tags.MONSTER);
			}
		};
		float range = getMaxRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();
		if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.TARGET_FOR_SHIP_SYSTEM)){
			target = (ShipAPI) ship.getAIFlags().getCustom(AIFlags.TARGET_FOR_SHIP_SYSTEM);
			if (target != null && target.getOriginalOwner() == ship.getOriginalOwner()) target = null;
		}
		
		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum) target = null;
		} else {
			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.FRIGATE, range, true, filter);
				} else {
					Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
						float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
						if (dist > range + radSum || target.isFighter()) target = null;
						if (target != null && target.getOriginalOwner() == ship.getOriginalOwner()) target = null;
					}
				}
			}
		}
		
		if (target != null && target.isFighter()) target = null;
		if (target == null) {
			target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.FRIGATE, range, true, filter);
		}
		if (target == null || target.getFluxTracker().isOverloadedOrVenting()) target = ship;
		
		return target;
	}
	
	
	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
	}

	
	protected void applyEffectToTarget(final ShipAPI ship, final ShipAPI target) {
		if (target == ship) return;

		Color hackColor = Global.getSettings().getSkillSpec(SotfIDs.SKILL_CYBERWARFARE).getGoverningAptitudeColor().brighter();

		float effectiveness = SotfCyberwarfare.getHackEffectiveness(ship, target);
		effectiveness *= 1.35f;
		effectiveness = Math.max(effectiveness, 0.75f);

		List<WeaponAPI> hackable = SotfCyberwarfare.getHackableWeapons(target);
		WeightedRandomPicker weaponPicker = new WeightedRandomPicker<WeaponAPI>();
		for (WeaponAPI weapon : hackable) {
			weaponPicker.add(weapon, weapon.getSize().ordinal() + 1);
		}
		float fractionToHack = 0.3f * effectiveness;

		int numToHack = Math.round(hackable.size() * fractionToHack);
		for (int i = 0; i < numToHack + 1; i++) {
			if (weaponPicker.isEmpty()) break;
			WeaponAPI targetWeapon = (WeaponAPI) weaponPicker.pickAndRemove();
			Vector2f from = Misc.getPointWithinRadius(ship.getShieldCenterEvenIfNoShield(), ship.getCollisionRadius() * 0.35f);
			// skip the collision detection/damage parts of the fake beam, we don't want them
			MagicFakeBeamPlugin.addBeam(
					VFX_BEAM_FULL,
					VFX_BEAM_FADEOUT,
					3f,
					from,
					Misc.getAngleInDegrees(from, targetWeapon.getLocation()),
					Misc.getDistance(from, targetWeapon.getLocation()),
					Color.WHITE,
					Misc.getNegativeHighlightColor());
			targetWeapon.disable(false);
		}

		ShipEngineControllerAPI ec = target.getEngineController();
		float limit = ec.getFlameoutFraction() * 0.75f * effectiveness;

		float disabledSoFar = 0f;
		boolean disabledAnEngine = false;
		List<ShipEngineControllerAPI.ShipEngineAPI> engines = new ArrayList<ShipEngineControllerAPI.ShipEngineAPI>(ec.getShipEngines());
		Collections.shuffle(engines);

		for (ShipEngineControllerAPI.ShipEngineAPI engine : engines) {
			if (engine.isDisabled()) continue;
			float contrib = engine.getContribution();
			if (disabledSoFar + contrib <= limit) {
				Vector2f from = Misc.getPointWithinRadius(ship.getShieldCenterEvenIfNoShield(), ship.getCollisionRadius() * 0.35f);
				// skip the collision detection/damage parts of the fake beam, we don't want them
				MagicFakeBeamPlugin.addBeam(
						VFX_BEAM_FULL,
						VFX_BEAM_FADEOUT,
						5f,
						from,
						Misc.getAngleInDegrees(from, engine.getLocation()),
						Misc.getDistance(from, engine.getLocation()),
						Color.WHITE,
						hackColor);
				disabledSoFar += contrib;
				disabledAnEngine = true;
				engine.disable(false);
			}
		}
		if (!disabledAnEngine) {
			for (ShipEngineControllerAPI.ShipEngineAPI engine : engines) {
				if (engine.isDisabled()) continue;
				engine.disable(false);
				break;
			}
		}
		ec.computeEffectiveStats(target == Global.getCombatEngine().getPlayerShip());

		if (!target.getFluxTracker().isOverloadedOrVenting()) {
			target.getFluxTracker().beginOverloadWithTotalBaseDuration(DISRUPTION_DUR);

			if (target.getFluxTracker().showFloaty() ||
					ship == Global.getCombatEngine().getPlayerShip() ||
					target == Global.getCombatEngine().getPlayerShip()) {
				target.getFluxTracker().playOverloadSound();
			}
		}

		float numBeams = VFX_BEAMS.get(target.getHullSize());
		if (effectiveness <= 0.65f) {
			numBeams--;
		}
		if (effectiveness >= 1.35f) {
			numBeams++;
		}

		for (int i = 0; i < numBeams; i++) {
			Vector2f from = Misc.getPointWithinRadius(ship.getShieldCenterEvenIfNoShield(), ship.getCollisionRadius() * 0.35f);
			Vector2f to = Misc.getPointWithinRadius(target.getShieldCenterEvenIfNoShield(), target.getCollisionRadius() * 0.35f);
			MagicFakeBeamPlugin.addBeam(
					1f,
					VFX_BEAM_FADEOUT,
					5f,
					from,
					Misc.getAngleInDegrees(from, to),
					Misc.getDistance(from, to),
					Color.WHITE,
					hackColor
			);
		}
		if (target.getSystem().isActive()) {
			target.getSystem().deactivate();
		}
		// remove active system charges
		if (target.getSystem().getAmmoPerSecond() > 0f && target.getSystem().getAmmoPerSecond() > 0f) {
			target.getSystem().setAmmo(0);
		}
		target.getSystem().setCooldownRemaining(6f * effectiveness);

		target.getFluxTracker().showOverloadFloatyIfNeeded("Intrusion Protocol!", hackColor, 4f, true);
	}
	
//	public StatusData getStatusData(int index, State state, float effectLevel) {
//		//float percent = (1f - ENERGY_DAM_PENALTY_MULT) * effectLevel * 100;
//		float percent = (1f - ENERGY_DAM_PENALTY_MULT) * 100;
//		if (index == 0 && percent > 0) {
//			return new StatusData((int)percent + "% less energy damage", false);
//		}
//		return null;
//	}


	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != SystemState.IDLE) return null;
		
		ShipAPI target = findTarget(ship);
		if (target != null && target != ship) {
			return "READY";
		}
		if ((target == null || target == ship) && ship.getShipTarget() != null) {
			return "OUT OF RANGE";
		}
		return "NO TARGET";
		//return super.getInfoText(system, ship);
	}

	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		ShipAPI target = findTarget(ship);
		return target != null && target != ship;
		//return super.isUsable(system, ship);
	}
	

	
}








