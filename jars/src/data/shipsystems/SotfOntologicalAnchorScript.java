package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SotfOntologicalAnchorScript extends BaseShipSystemScript {

	public static float MAX_LASH_RANGE = 1600f;
	public static float OVERLOAD_DURATION = 1.5f;
	public static float IMPAIR_TIME = 0.5f;
	public static float IMPAIR_MAX_ACCEL_MULT = 0f;

	// There's no escape!
	public static List<String> ALWAYS_PULL = new ArrayList<>();
	static {
		ALWAYS_PULL.add("fabricator_unit");
		ALWAYS_PULL.add("overseer_unit");
		ALWAYS_PULL.add("shrouded_eye");
	}

	private CombatEngineAPI engine = Global.getCombatEngine();

	private int stage = 0;
	private ShipAPI target;

	public static final Color LASH_COLOR = new Color(205, 225, 255,255);
	public static final Color COLLAPSE_COLOR = new Color(155,205,255,255);

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (engine.isPaused() || stats.getEntity() == null) return;
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) {
			return;
		}
		float amount = engine.getElapsedInLastFrame();

		if (state == State.COOLDOWN || state == State.IDLE || state == State.OUT) {
			stage = 0;
			target = null;
			return;
		}

		if (target == null) {
			target = findTarget(ship, false);
		} else if (!engine.isEntityInPlay(target)) {
			stage = 3;
			return;
		}
		if (target == null) {
			stage = 3;
			return;
		}

		//String lashSelfSound = "energy_lash_activate";
		String lashSelfSound = "energy_lash_fire_at_enemy";
		String lashEnemySound = "energy_lash_enemy_impact";

		//
		// LASH 1
		//
		if (effectLevel > 0.33f && stage == 0) {
			stage = 1;

			if (target != null) {
				Vector2f point = CollisionUtils.getNearestPointOnBounds(
						Misc.getPointAtRadius(target.getShieldCenterEvenIfNoShield(), target.getShieldRadiusEvenIfNoShield() * 0.35f),
						target
				);

				EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.15f;
				params.fadeOutDist = 500f;
				params.minFadeOutMult = 2f;
				params.flickerRateMult = 0.4f;

				engine.addHitParticle(ship.getShieldCenterEvenIfNoShield(), ship.getVelocity(), 40f, 1f, 0.5f, LASH_COLOR);

				EmpArcEntityAPI arc = engine.spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
						ship,
						point,
						target,
						30f,
						LASH_COLOR,
						Color.WHITE,
						params
				);
				arc.setCoreWidthOverride(15f);
				arc.setSingleFlickerMode(true);
				arc.setFadedOutAtStart(true);

				Global.getSoundPlayer().playSound(lashSelfSound, 0.6f, 1f, target.getLocation(), target.getVelocity());
				Global.getSoundPlayer().playSound(lashEnemySound, 0.6f, 1f, ship.getLocation(), ship.getVelocity());

				if (SotfModPlugin.GLIB) {
					RippleDistortion ripple = new RippleDistortion(target.getLocation(), target.getVelocity());
					ripple.setIntensity(50f);
					ripple.setSize(50f);
					ripple.fadeInSize(0.15f);
					ripple.fadeOutIntensity(0.5f);
					DistortionShader.addDistortion(ripple);
				}

				// get out of phase
				if (target.isPhased()) {
					target.setOverloadColor(LASH_COLOR);
					target.getFluxTracker().beginOverloadWithTotalBaseDuration(OVERLOAD_DURATION * 0.1f);
					final ShipAPI overloaded = target;
					Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
						@Override
						public void advance(float amount, List<InputEventAPI> events) {
							if (!overloaded.getFluxTracker().isOverloadedOrVenting()) {
								overloaded.resetOverloadColor();
								Global.getCombatEngine().removePlugin(this);
							}
						}
					});
				}

				// Halt!
				applyOntologicalAnchorSlow(target);

				// DIE!
				if (target.isFighter()) {
					anchorObliterate(target);
					target = findTarget(ship, true);
					if (target == null) {
						stage = 3;
					}
				}
			}
		}

		//
		// LASH 2
		//
		if (effectLevel > 0.66f && stage == 1) {
			stage = 2;

			if (target != null) {
				Vector2f point = CollisionUtils.getNearestPointOnBounds(
						Misc.getPointAtRadius(target.getShieldCenterEvenIfNoShield(), target.getShieldRadiusEvenIfNoShield() * 0.25f),
						target
				);

				EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.15f;
				params.fadeOutDist = 500f;
				params.minFadeOutMult = 2f;
				params.flickerRateMult = 0.4f;

				engine.addHitParticle(ship.getShieldCenterEvenIfNoShield(), ship.getVelocity(), 40f, 1f, 0.5f, LASH_COLOR);

				EmpArcEntityAPI arc = engine.spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
						ship,
						point,
						target,
						45f,
						LASH_COLOR,
						Color.WHITE,
						params
				);
				arc.setCoreWidthOverride(20f);
				arc.setSingleFlickerMode(true);
				arc.setFadedOutAtStart(true);

				Global.getSoundPlayer().playSound(lashSelfSound, 0.85f, 1f, target.getLocation(), target.getVelocity());
				Global.getSoundPlayer().playSound(lashEnemySound, 0.85f, 1f, ship.getLocation(), ship.getVelocity());

				if (SotfModPlugin.GLIB) {
					RippleDistortion ripple = new RippleDistortion(target.getLocation(), target.getVelocity());
					ripple.setIntensity(50f);
					ripple.setSize(75f);
					ripple.fadeInSize(0.15f);
					ripple.fadeOutIntensity(0.5f);
					DistortionShader.addDistortion(ripple);
				}

				if (target.isPhased()) {
					target.setOverloadColor(LASH_COLOR);
					target.getFluxTracker().beginOverloadWithTotalBaseDuration(OVERLOAD_DURATION * 0.1f);
					final ShipAPI overloaded = target;
					Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
						@Override
						public void advance(float amount, List<InputEventAPI> events) {
							if (!overloaded.getFluxTracker().isOverloadedOrVenting()) {
								overloaded.resetOverloadColor();
								Global.getCombatEngine().removePlugin(this);
							}
						}
					});
				}

				// Halt!
				applyOntologicalAnchorSlow(target);

				// DIE!
				if (target.isFighter()) {
					anchorObliterate(target);
					target = findTarget(ship, true);
					if (target == null) {
						stage = 3;
					}
				}
			}
		}

		//
		// LASH 3
		//
		if (effectLevel >= 1f && stage == 2) {
			stage = 3;

			if (target != null) {
				Vector2f point = CollisionUtils.getNearestPointOnBounds(
						Misc.getPointAtRadius(target.getShieldCenterEvenIfNoShield(), target.getShieldRadiusEvenIfNoShield() * 0.15f),
						target
				);

				EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.15f;
				params.fadeOutDist = 500f;
				params.minFadeOutMult = 2f;
				params.flickerRateMult = 0.4f;

				engine.addHitParticle(ship.getShieldCenterEvenIfNoShield(), ship.getVelocity(), 40f, 1f, 0.5f, LASH_COLOR);

				EmpArcEntityAPI arc = engine.spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
						ship,
						point,
						target,
						75f,
						LASH_COLOR,
						Color.WHITE,
						params
				);
				arc.setCoreWidthOverride(20f);
				arc.setWarping(0.5f);
				arc.setSingleFlickerMode(true);
				arc.setFadedOutAtStart(true);

				if (SotfModPlugin.GLIB) {
					RippleDistortion ripple = new RippleDistortion(target.getLocation(), target.getVelocity());
					ripple.setIntensity(50f);
					ripple.setSize(100f);
					ripple.fadeInSize(0.15f);
					ripple.fadeOutIntensity(0.5f);
					DistortionShader.addDistortion(ripple);
				}

				Global.getSoundPlayer().playSound(lashSelfSound, 1.1f, 1.2f, target.getLocation(), target.getVelocity());
				Global.getSoundPlayer().playSound(lashEnemySound, 1.1f, 1.2f, ship.getLocation(), ship.getVelocity());

				target.setOverloadColor(LASH_COLOR);
				target.getFluxTracker().beginOverloadWithTotalBaseDuration(OVERLOAD_DURATION);
				final ShipAPI overloaded = target;
				Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
					@Override
					public void advance(float amount, List<InputEventAPI> events) {
						if (!overloaded.getFluxTracker().isOverloadedOrVenting()) {
							overloaded.resetOverloadColor();
							Global.getCombatEngine().removePlugin(this);
						}
					}
				});

				// Halt!
				applyOntologicalAnchorSlow(target);

				float flux = target.getFluxLevel();
				if (flux > 0.6f || ALWAYS_PULL.contains(target.getHullSpec().getHullId())) {
					// Pull!
					CombatUtils.applyForce(target, Misc.getAngleInDegrees(target.getLocation(), ship.getLocation()), 200f + target.getMassWithModules() * 2f);
				} else if (flux < 0.3f) {
					// Push!
					CombatUtils.applyForce(target, Misc.getAngleInDegrees(ship.getLocation(), target.getLocation()), 250f + target.getMassWithModules() * 3f);
				}

				// DIE!
				if (target.isFighter()) {
					anchorObliterate(target);
				}
			}
		}
	}

	public static class SotfOntologicalAnchorImpairmentScript implements AdvanceableListener {
		protected ShipAPI ship;

		public float timer = 0f;
		public SotfOntologicalAnchorImpairmentScript(ShipAPI ship) {
			this.ship = ship;
			//ship.getFluxTracker().showOverloadFloatyIfNeeded("Collapsing!", COLLAPSE_COLOR, 5f, true);
		}

		public void advance(float amount) {
			timer += amount;
			float prog = timer / IMPAIR_TIME;
			if (prog > 1) {
				ship.getMutableStats().getAcceleration().unmodify("sotf_ontologicalanchor");
				ship.getMutableStats().getDeceleration().unmodify("sotf_ontologicalanchor");
				ship.getMutableStats().getTurnAcceleration().unmodify("sotf_ontologicalanchor");
				ship.removeListener(this);
				return;
			}

			ship.getMutableStats().getAcceleration().modifyMult("sotf_ontologicalanchor", IMPAIR_MAX_ACCEL_MULT * (1f - prog));
			ship.getMutableStats().getDeceleration().modifyMult("sotf_ontologicalanchor", IMPAIR_MAX_ACCEL_MULT * (1f - prog));
			ship.getMutableStats().getTurnAcceleration().modifyMult("sotf_ontologicalanchor", IMPAIR_MAX_ACCEL_MULT * (1f - prog));
		}
	}

//	public static class SotfRealityScourgeCollapseScript implements AdvanceableListener {
//		protected ShipAPI ship;
//
//		public static float TIME_UNTIL_COLLAPSE = 1f;
//		protected float timer = TIME_UNTIL_COLLAPSE;
//		public SotfRealityScourgeCollapseScript(ShipAPI ship) {
//			this.ship = ship;
//			//ship.getFluxTracker().showOverloadFloatyIfNeeded("Collapsing!", COLLAPSE_COLOR, 5f, true);
//		}
//
//		public void advance(float amount) {
//			float timeMult = Global.getCombatEngine().getTimeMult().getModifiedValue();
//			timer -= amount * timeMult;
//			if (timer < 0) {
//				timer = 0;
//			}
//
//			float prog = (TIME_UNTIL_COLLAPSE - timer) / TIME_UNTIL_COLLAPSE;
//			if (prog > 1) {
//				prog = 1;
//			} else if (prog < 0) {
//				prog = 0;
//			}
//			ship.setJitter(this, COLLAPSE_COLOR, 2f * prog, 16, 2f, 4f);
//
//			if (timer > 0) {
//				return;
//			}
//
//			NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(COLLAPSE_COLOR, ship.getShieldRadiusEvenIfNoShield());
//			p.fadeOut = 0.15f;
//			p.hitGlowSizeMult = 0.25f;
//			p.underglow = new Color(255,255,255, 50);
//			p.withHitGlow = false;
//			p.noiseMag = 0f;
//
//			if (SotfModPlugin.GLIB) {
//				RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
//				ripple.setIntensity(50f);
//				ripple.setSize(50f);
//				ripple.fadeInSize(0.15f);
//				ripple.fadeOutIntensity(0.5f);
//				DistortionShader.addDistortion(ripple);
//			}
//
//			Global.getCombatEngine().removeEntity(ship);
//		}
//	}

	public static void applyOntologicalAnchorSlow(ShipAPI ship) {
		ship.getVelocity().scale(0f);
		if (ship.hasListenerOfClass(SotfOntologicalAnchorImpairmentScript.class)) {
			SotfOntologicalAnchorImpairmentScript script = ship.getListeners(SotfOntologicalAnchorImpairmentScript.class).get(0);
			script.timer = 0f;
		} else {
			ship.addListener(new SotfOntologicalAnchorImpairmentScript(ship));
		}
	}

	public void anchorObliterate(ShipAPI ship) {
		NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(COLLAPSE_COLOR, ship.getShieldRadiusEvenIfNoShield());
		p.fadeOut = 0.15f;
		p.hitGlowSizeMult = 0.25f;
		p.underglow = new Color(255,255,255, 50);
		p.withHitGlow = false;
		p.noiseMag = 0f;

		if (SotfModPlugin.GLIB) {
			RippleDistortion ripple = new RippleDistortion(ship.getLocation(), new Vector2f());
			ripple.setIntensity(50f);
			ripple.setSize(50f);
			ripple.fadeInSize(0.15f);
			ripple.fadeOutIntensity(0.5f);
			DistortionShader.addDistortion(ripple);
		}

		engine.addFloatingText(ship.getLocation(), "ERASED",
				12f, COLLAPSE_COLOR, ship, 0f, 0f);

		Global.getCombatEngine().removeEntity(ship);
	}

	protected ShipAPI findTarget(ShipAPI ship, boolean onlyFighters) {
		float range = getRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();

		float extraRange = 0f;
		if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.CUSTOM1)){
			target = (ShipAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.CUSTOM1);
			extraRange += 500f;
		}


		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum + extraRange) target = null;
		} else {
			Misc.FindShipFilter filter = s -> isValidLashTarget(ship, s, onlyFighters);

			if (player) {
				target = Misc.findClosestShipTo(ship, ship.getMouseTarget(), ShipAPI.HullSize.FIGHTER, range, true, false, filter);
			} else {
				Object test = ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
				if (test instanceof ShipAPI) {
					target = (ShipAPI) test;
					float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
					float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
					if (dist > range + radSum) target = null;
				}
			}

			if (target == null) {
				target = Misc.findClosestShipTo(ship, ship.getLocation(), ShipAPI.HullSize.FIGHTER, range, true, false, filter);
			}
		}

		return target;
	}

	public boolean isInRange(ShipAPI ship, ShipAPI target) {
		float range = getRange(ship);
		float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
		float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
		return dist <= range + radSum;
	}

	public boolean isValidLashTarget(ShipAPI ship, ShipAPI other, boolean onlyFighters) {
		if (other == null) return false;
		if (other.getOwner() == ship.getOwner()) return false;
		if (other.isHulk() || other.getOwner() == 100) return false;
		if (onlyFighters && !other.isFighter()) return false;
		if (other.isShuttlePod()) return false;
		return true;
	}

	public static float getRange(ShipAPI ship) {
		if (ship == null) return MAX_LASH_RANGE;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_LASH_RANGE);
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;

		ShipAPI target = findTarget(ship, false);
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
		ShipAPI target = findTarget(ship, false);
		return target != null && target != ship;
	}
}








