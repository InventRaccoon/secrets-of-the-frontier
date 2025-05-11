package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfHypnagogicIncursionScript extends BaseShipSystemScript {


	public static float SHIP_ALPHA_MULT = 0.25f;
	public static float RELOAD_MULT = 15f;
	public static float DISSIPATION_MULT = 3f;

	public static final float MAX_TURN_BONUS = 50f;
	public static final float TURN_ACCEL_BONUS = 50f;
	public static final float INSTANT_BOOST_FLAT = 500f;
	public static final float INSTANT_BOOST_MULT = 3f;

	private CombatEngineAPI engine = Global.getCombatEngine();
	private boolean shouldUnapply = false;
	private boolean appliedBoost = false;
	private float boostScale = 0.75f;
	private boolean boostForward = false;

	private float alphaLevel = 0f;
	private float cooldown = 0f;
	private IntervalUtil interval = new IntervalUtil(0.03f, 0.035f);

	protected static float ALPHA_RESET_TIME = 0.1f;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (engine.isPaused() || stats.getEntity() == null) return;
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) {
			return;
		}
		float shipRadius = ship.getCollisionRadius();
		float amount = engine.getElapsedInLastFrame();
		if (Global.getCombatEngine().isPaused()) {
			amount = 0f;
		}

		if (state == State.ACTIVE || state == State.IN) {
			alphaLevel = 1f;
			ship.setExtraAlphaMult2(1f - (1f - SHIP_ALPHA_MULT) * effectLevel);
		} else if (state == State.OUT) {
			alphaLevel = 1f;
			cooldown = ALPHA_RESET_TIME;
			ship.setExtraAlphaMult2(1f - (1f - SHIP_ALPHA_MULT));
		} else {
			cooldown -= amount;
			if (cooldown <= 0f) {
				alphaLevel -= amount / 0.25f;
			}
			ship.setExtraAlphaMult2(1f - (1f - SHIP_ALPHA_MULT) * Math.max(alphaLevel, 0f));
		}

		if (effectLevel > 0f) {
			interval.advance(amount);
			if (interval.intervalElapsed()) {
				CombatEngineAPI engine = Global.getCombatEngine();
				float baseDuration = 3f;
				Vector2f vel = new Vector2f();
				float size = ship.getCollisionRadius() * 1f;
				for (int i = 0; i < 10; i++) {
					Vector2f point = new Vector2f(ship.getLocation());
					point = Misc.getPointWithinRadiusUniform(point, ship.getCollisionRadius() * 0.75f, Misc.random);
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
					engine.addNebulaParticle(pt, v, nSize * 1f, 2f,
							0.25f / dur, 0f, dur, new Color(3, 5, 5, 235));
				}
			}
		}

		if (state == State.OUT) {
			shouldUnapply = true;
			/* Black magic to counteract the effects of maneuvering penalties/bonuses on the effectiveness of this system */
			float decelMult = Math.max(0.5f, Math.min(2f, stats.getDeceleration().getModifiedValue() / stats.getDeceleration().getBaseValue()));
			float adjFalloffPerSec = 0.35f * (float) Math.pow(decelMult, 0.5); //0.55f for frigates
			float maxDecelPenalty = 1f / decelMult;

			stats.getMaxTurnRate().unmodify(id);
			stats.getDeceleration().modifyMult(id, (1f - effectLevel) * 1f * maxDecelPenalty);
			stats.getTurnAcceleration().modifyPercent(id, TURN_ACCEL_BONUS * effectLevel);

			if (boostForward) {
				ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
				ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
				ship.blockCommandForOneFrame(ShipCommand.DECELERATE);
			} else {
				ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
			}

			if (amount > 0f) {
				ship.getVelocity().scale((float) Math.pow(adjFalloffPerSec, amount));
			}

			if (!appliedBoost) {
				Vector2f direction = new Vector2f();
				boostForward = false;
				boostScale = 1f;
				if (ship.getEngineController().isAccelerating()) {
					direction.y += 0.6f; //0.75f - 0.2f
					//boostScale -= 0.2f; //0.2f
					boostForward = true;
				} else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
					direction.y -= 0.6f; //0.75f - 0.35f ?
					//boostScale -= 0.2f; //0.35f
				}
				if (ship.getEngineController().isStrafingLeft()) {
					direction.x -= 0.6f;
					//boostScale += 0.25f;
					boostForward = false;
				} else if (ship.getEngineController().isStrafingRight()) {
					direction.x += 0.6f;
					//boostScale += 0.25f;
					boostForward = false;
				}
				if (direction.length() <= 0f) {
					direction.y = 0.6f; //0.75f - 0.2f ?
					//boostScale -= 0.35f;
					boostForward = true;
				}
				Misc.normalise(direction);
				VectorUtils.rotate(direction, ship.getFacing() - 90f, direction);
				direction.scale(((ship.getMaxSpeedWithoutBoost() * INSTANT_BOOST_MULT) + INSTANT_BOOST_FLAT) * boostScale);
				ship.getVelocity().scale(0f);
				Vector2f.add(ship.getVelocity(), direction, ship.getVelocity());

				if (SotfModPlugin.GLIB) {
					RippleDistortion ripple = new RippleDistortion(ship.getLocation(), new Vector2f());
					ripple.setIntensity(ship.getCollisionRadius() * 0.75f);
					ripple.setSize(ship.getShieldRadiusEvenIfNoShield());
					ripple.fadeInSize(0.15f);
					ripple.fadeOutIntensity(0.25f);
					DistortionShader.addDistortion(ripple);
				}

				appliedBoost = true;
			}
			ship.setCollisionClass(CollisionClass.NONE);
			stats.getHullDamageTakenMult().modifyMult(id, 0f);
			stats.getArmorDamageTakenMult().modifyMult(id, 0f);
			stats.getEmpDamageTakenMult().modifyMult(id, 0f);

			stats.getFluxDissipation().modifyMult(id, DISSIPATION_MULT * effectLevel);
			stats.getBallisticAmmoRegenMult().modifyMult(id, RELOAD_MULT * effectLevel);
			stats.getEnergyAmmoRegenMult().modifyMult(id, RELOAD_MULT * effectLevel);
			stats.getBallisticRoFMult().modifyMult(id, RELOAD_MULT * effectLevel);
			stats.getEnergyRoFMult().modifyMult(id, RELOAD_MULT * effectLevel);
			stats.getMissileRoFMult().modifyMult(id, RELOAD_MULT * effectLevel);
		}

		if (state == State.IDLE) {
			if (shouldUnapply) {
				stats.getMaxTurnRate().unmodify(id);
				stats.getDeceleration().unmodify(id);
				stats.getTurnAcceleration().unmodify(id);

				stats.getHullDamageTakenMult().unmodify(id);
				stats.getArmorDamageTakenMult().unmodify(id);
				stats.getEmpDamageTakenMult().unmodify(id);

				stats.getFluxDissipation().unmodify(id);
				stats.getBallisticAmmoRegenMult().unmodify(id);
				stats.getEnergyAmmoRegenMult().unmodify(id);
				stats.getBallisticRoFMult().unmodify(id);
				stats.getEnergyRoFMult().unmodify(id);
				stats.getMissileRoFMult().unmodify(id);

				ship.setCollisionClass(CollisionClass.SHIP);
				shouldUnapply = false;
				appliedBoost = false;
				//boostScale = 0.8f;
				boostForward = false;
			}
		}

		// system prevents firing but need to manually interrupt bursts and active beams
		if (effectLevel > 0f) {
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				weapon.stopFiring();
				//weapon.setForceDisabled(true);
			}
		}

//		if (effectLevel > 0.5f) {
//			for (WeaponAPI weapon : ship.getAllWeapons()) {
//				if (!weapon.getSpec().hasTag("sotf_force_fire_on_out")) {
//					weapon.setForceDisabled(true);
//				}
//			}
//		} else {
//			for (WeaponAPI weapon : ship.getAllWeapons()) {
//				if (!weapon.getSpec().hasTag("sotf_force_fire_on_out")) {
//					weapon.setForceDisabled(false);
//				}
//			}
//		}
//
//		if (state == State.OUT && effectLevel < 0.5f) {
//			for (WeaponAPI weapon : ship.getAllWeapons()) {
//				if (weapon.getSpec().hasTag("sotf_force_fire_on_out")) {
//					weapon.setForceDisabled(false);
//					//weapon.setForceFireOneFrame(true);
//				}
//			}
//		} else {
//			for (WeaponAPI weapon : ship.getAllWeapons()) {
//				if (weapon.getSpec().hasTag("sotf_force_fire_on_out") && weapon.getBurstFireTimeRemaining() <= 0f) {
//					weapon.setForceDisabled(true);
//				}
//			}
//		}
	}
}








