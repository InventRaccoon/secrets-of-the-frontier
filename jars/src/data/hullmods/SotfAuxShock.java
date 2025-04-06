// Shock Package for the Proxy Picket
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import data.scripts.weapons.SotfAuxShockOnHitEffect;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SotfAuxShock extends SotfBaseAuxPackage {

	public static float HULL_TRIGGER_PERCENT = 0.85f;
	public static float EMP_RANGE = 500f;
	private static final Map<ShipAPI.HullSize, Float> SO_SPEED_BOOST = new HashMap<ShipAPI.HullSize, Float>();
	static {
		SO_SPEED_BOOST.put(ShipAPI.HullSize.FIGHTER, 50f); // just in case
		SO_SPEED_BOOST.put(ShipAPI.HullSize.FRIGATE, 50f);
		SO_SPEED_BOOST.put(ShipAPI.HullSize.DESTROYER, 30f);
		SO_SPEED_BOOST.put(ShipAPI.HullSize.CRUISER, 20f);
		SO_SPEED_BOOST.put(ShipAPI.HullSize.CAPITAL_SHIP, 20f); // works out to 10f since halved
	}
	private static final float RANGE_THRESHOLD = 450f;
	private static final float RANGE_MULT = 0.25f;

	private static float EXPLOSION_DAMAGE = 1500f;

	private static Color EMP_COLOR = new Color(45, 120, 200, 255);

	public static class SotfAuxShockSacrificeScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
		public ShipAPI ship;
		public String id;
		public boolean sacrificing = false;
		public float progress = 0f;
		public float timeUntilNextArc = 1f;
		public float timeUntilNextBlink = 0.5f;

		public SotfAuxShockSacrificeScript(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}

		public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
			if (!sacrificing) {
				if (ship.getHitpoints() - damageAmount < (ship.getMaxHitpoints() * HULL_TRIGGER_PERCENT)) {
					sacrificing = true;
					Global.getSoundPlayer().playSound("sotf_lastlaughprimed", 1f, 1f, ship.getLocation(), ship.getVelocity());
					return true;
				}
			}
			return false;
		}

		public void advance(float amount) {
			if (!sacrificing) {
				// random sparking while not yet sacrificing
				timeUntilNextBlink -= amount;
				if (timeUntilNextBlink <= 0) {
					float randomFactor = (float) Math.random();
					Global.getCombatEngine().addHitParticle(ship.getLocation(), ship.getVelocity(), 15f + randomFactor * 45f, 1f, 0.25f + (0.1f * randomFactor), EMP_COLOR.brighter());
					timeUntilNextBlink = 0.25f + (randomFactor * 0.75f);
				}
				if (ship.getFluxLevel() > 0.95f) {
					sacrificing = true;
					Global.getSoundPlayer().playSound("sotf_lastlaughprimed", 1f, 1f, ship.getLocation(), ship.getVelocity());
				}
			} else {
				float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
				//int brighten = 120;
				//Color ventColor = Misc.setBrightness(ship.getVentFringeColor(), 100);
				if (!ship.isAlive()) {
					DamagingExplosionSpec explosionSpec = DamagingExplosionSpec.explosionSpecForShip(ship);
					explosionSpec.setParticleCount(200);

					explosionSpec.setMinDamage(EXPLOSION_DAMAGE * 0.5f);
					explosionSpec.setMaxDamage(EXPLOSION_DAMAGE);
					explosionSpec.setCoreRadius(EMP_RANGE * 0.3f);
					explosionSpec.setRadius(EMP_RANGE * 0.6f);
					explosionSpec.setCollisionClass(CollisionClass.MISSILE_FF);
					explosionSpec.setCollisionClassByFighter(CollisionClass.MISSILE_FF);
					explosionSpec.setDamageType(DamageType.ENERGY);

					explosionSpec.setParticleDuration(1.5f);
					explosionSpec.setParticleCount(75);
					explosionSpec.setParticleColor(EMP_COLOR.brighter());

					explosionSpec.setUseDetailedExplosion(true);
					explosionSpec.setExplosionColor(EMP_COLOR);
					explosionSpec.setDetailedExplosionRadius(EMP_RANGE);
					explosionSpec.setDetailedExplosionFlashRadius(0.5f);
					explosionSpec.setDetailedExplosionFlashColorCore(Color.WHITE);
					explosionSpec.setDetailedExplosionFlashColorFringe(EMP_COLOR);
					explosionSpec.setShowGraphic(true);
					explosionSpec.setSoundSetId("mine_explosion");
					explosionSpec.setEffect(new SotfAuxShockOnHitEffect());
					Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, ship, ship.getLocation(), true);
					for (int i = 0; i < 8; i++) {
						float randomFactor = (float) Math.random();
						Global.getCombatEngine().spawnExplosion(
								Misc.getPointWithinRadius(ship.getLocation(),50f + (1f - randomFactor * 250f)),
								new Vector2f(),
								Misc.setBrightness(EMP_COLOR, 200 + Math.round((randomFactor * 30f))),
								150 + ((1f - randomFactor) * 250f),
								2f + (2f * randomFactor)
						);
					}
					return;
				}
				if (progress == 0f) {
					if (ship.getFluxTracker().showFloaty()) {
						Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
								"Initiated shock sequence",
								NeuralLinkScript.getFloatySize(ship),
								EMP_COLOR.brighter(),
								ship,
								16f * timeMult,
								3.2f/timeMult,
								1f/timeMult,
								0.25f,
								0.25f,
								1f);
					}

					Global.getCombatEngine().addHitParticle(ship.getLocation(), ship.getVelocity(), 160f, 1f, 0.75f, EMP_COLOR.brighter());
					ship.getFluxTracker().beginOverloadWithTotalBaseDuration(4f);

					// assign a new ship AI if needed
					if (ship != Global.getCombatEngine().getPlayerShip()) {
						ShipAIConfig config = new ShipAIConfig();
						config.alwaysStrafeOffensively = true;
						config.backingOffWhileNotVentingAllowed = false;
						config.turnToFaceWithUndamagedArmor = false;
						config.burnDriveIgnoreEnemies = true;
						config.personalityOverride = Personalities.RECKLESS;

						ship.setShipAI(Global.getSettings().createDefaultShipAI(ship, config));
						if (ship.getShipAI() != null) {
							ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, 10000f);
							ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING, 10000f);
						}
					}
				}

				timeUntilNextArc -= amount;
				if (timeUntilNextArc <= 0) {
					float thickness = 20f;
					float coreWidthMult = 0.67f;
					CombatEntityAPI empTarget = findTarget(ship);
					if (empTarget != null) {
						Global.getCombatEngine().spawnEmpArcPierceShields(ship,
								ship.getShieldCenterEvenIfNoShield(),
								ship, empTarget,
								DamageType.ENERGY,
								200f,
								300f,
								100000f,
								"tachyon_lance_emp_impact",
								15f,
								EMP_COLOR,
								Color.white
						);
					} else {
						Vector2f to = Misc.getPointAtRadius(ship.getLocation(), 100f + (50f * (float) Math.random()));
						EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
								ship.getShieldCenterEvenIfNoShield(),
								ship,
								to,
								ship,
								thickness,
								EMP_COLOR,
								Color.white
						);
						arc.setCoreWidthOverride(thickness * coreWidthMult);
						arc.setSingleFlickerMode();
					}
					// self-damage arc
					Global.getCombatEngine().spawnEmpArcPierceShields(ship,
							ship.getShieldCenterEvenIfNoShield(),
							ship,
							ship,
							DamageType.ENERGY,
							100f,
							0f,
							100000f,
							"tachyon_lance_emp_impact",
							15f,
							EMP_COLOR,
							Color.white
					);
					float randomFactor = (float) Math.random();
					timeUntilNextArc = 0.3f + (0.15f * randomFactor);
					Global.getCombatEngine().addHitParticle(ship.getLocation(), ship.getVelocity(), 60f + (30f * randomFactor), 1f, 0.35f, EMP_COLOR.brighter());
				}

				ship.setJitter(this, EMP_COLOR, 0.25f, 10, 3, 5);
				ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
				ship.giveCommand(ShipCommand.ACCELERATE, new Vector2f(0f, 1f), 0);

				// apply Safety Overrides effect
				float sizeMult = 1f;
				if (ship.isCapital()) sizeMult = 0.5f;
				if (!ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES)) {
					ship.getMutableStats().getMaxSpeed().modifyFlat(HullMods.SAFETYOVERRIDES, SO_SPEED_BOOST.get(ship.getHullSize()) * sizeMult);
					ship.getMutableStats().getAcceleration().modifyFlat(HullMods.SAFETYOVERRIDES, SO_SPEED_BOOST.get(ship.getHullSize()) * sizeMult);
					ship.getMutableStats().getDeceleration().modifyFlat(HullMods.SAFETYOVERRIDES, SO_SPEED_BOOST.get(ship.getHullSize()) * sizeMult);
					ship.getMutableStats().getZeroFluxMinimumFluxLevel().modifyFlat(HullMods.SAFETYOVERRIDES, 2f * sizeMult); // set to two, meaning boost is always on

					//ship.getMutableStats().getFluxDissipation().modifyMult(HullMods.SAFETYOVERRIDES, ((FLUX_DISSIPATION_MULT - 1f) * sizeMult) + 1f);
					// capitals can still vent at half the speed
					ship.getMutableStats().getVentRateMult().modifyMult(HullMods.SAFETYOVERRIDES, 1f - sizeMult);
					// capitals: threshold doubled, penalty halved
					ship.getMutableStats().getWeaponRangeThreshold().modifyFlat(HullMods.SAFETYOVERRIDES, RANGE_THRESHOLD / sizeMult);
					ship.getMutableStats().getWeaponRangeMultPastThreshold().modifyMult(HullMods.SAFETYOVERRIDES, RANGE_MULT / sizeMult);

					// engine color change
					float visualProgress = Math.min(progress, 1f) * sizeMult;
					ship.getEngineController().fadeToOtherColor(this, new Color(255,100,255,255), null, visualProgress, 0.4f);
					ship.getEngineController().extendFlame(this, 0.25f * visualProgress, 0.25f * visualProgress, 0.25f * visualProgress);
				}

				ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id, 0.2f);

				if (Global.getCombatEngine().getPlayerShip().equals(ship)) {
					Global.getCombatEngine().maintainStatusForPlayerShip(id, null, "Reactor overloading!", "In sacrifice, meaning", true);
				}

				progress += amount;
			}
		}

		public CombatEntityAPI findTarget(ShipAPI ship) {
			float range = EMP_RANGE;
			Vector2f from = ship.getLocation();

			Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
					range * 2f, range * 2f);
			int owner = ship.getOwner();
			CombatEntityAPI best = null;
			float minScore = Float.MAX_VALUE;

			while (iter.hasNext()) {
				Object o = iter.next();
				if (!(o instanceof MissileAPI) &&
						//!(o instanceof CombatAsteroidAPI) &&
						!(o instanceof ShipAPI)) continue;
				CombatEntityAPI other = (CombatEntityAPI) o;
				if (other.getOwner() == owner) continue;

				if (other instanceof ShipAPI) {
					ShipAPI otherShip = (ShipAPI) other;
					if (otherShip.isHulk()) continue;
					//if (!otherShip.isAlive()) continue;
					if (otherShip.isPhased()) continue;
				}

				if (other.getCollisionClass() == CollisionClass.NONE) continue;

				if (other instanceof MissileAPI) {
					MissileAPI missile = (MissileAPI) other;
					if (missile.isFlare()) continue;
				}

				float radius = Misc.getTargetingRadius(from, other, false);
				float dist = Misc.getDistance(from, other.getLocation()) - radius;
				if (dist > range) continue;

				//float angleTo = Misc.getAngleInDegrees(from, other.getLocation());
				//float score = Misc.getAngleDiff(weapon.getCurrAngle(), angleTo);
				float score = dist;

				if (score < minScore) {
					minScore = score;
					best = other;
				}
			}
			return best;
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new SotfAuxShockSacrificeScript(ship, id));
	}

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getVentRateMult().modifyMult(id, 0f);
		stats.getFluxDissipation().modifyMult(id, 0f);
	}

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "" + Math.round(HULL_TRIGGER_PERCENT * 100f) + "%";
		if (index == 1) return "Safety Overrides";
		if (index == 2) return "" + Math.round(EXPLOSION_DAMAGE);
		if (index == 3) return "" + Math.round(SotfAuxShockOnHitEffect.PERCENT_MAX_FLUX * 100f) + "%";
		return null;
	}
}
