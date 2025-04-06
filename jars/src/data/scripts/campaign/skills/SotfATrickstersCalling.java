// ... I'LL RUN YOU DOWN ALL THE SAME.
// Ship periodically steals enemy missile ammo to refill its own, and gets a one-time emergency bugout phase
package data.scripts.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.impl.combat.LowCRShipDamageSequence;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.combat.SotfRingTimerVisualScript;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;

public class SotfATrickstersCalling {

	public static float COVETOUS_COOLDOWN = 12f;
	public static float COVETOUS_RANGE = 1400;
	public static float SHUNT_PERCENT = 0.6f;

	public static Color COVETOUS_COLOR = new Color(125, 255, 55, 255);

	public static String SKEDDADLE_KEY = "sotf_atricksterscalling_skedaddle";
	public static float SKEDADDLE_TRIGGER_PERCENT = 0.5f;
	public static float SKEDADDLE_DURATION = 9f;
	public static float SKEDADDLE_SPEED_MULT = 3f;

	public static final String VISUAL_KEY = "sotf_covetous_visual";

	public static class Covetous extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.addListener(new SotfCovetousScript(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.addListener(new SotfCovetousScript(ship));
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {

		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {

		}

		public String getEffectDescription(float level) {
			return null;
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			init(stats, skill);

			info.addPara("Every %s seconds, gains the ability to remotely shunt missile ammo from a random* " +
							"weapon of a targeted hostile ship within %s units", 0f, hc, hc,
					"" + (int) (COVETOUS_COOLDOWN), "" + (int) COVETOUS_RANGE);
			info.addPara("Steals an amount up to %s of the target weapon's base ammo, and restores an equivalent " +
					"percentage of a random* missile weapon's ammo", 0f, hc, hc, "" + Math.round(SHUNT_PERCENT * 100f) + "%");
			info.addPara("\n*Larger missile weapons are more likely to be stolen from or refilled", dtc, 0f);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static boolean isValidCovetousShip(ShipAPI ship) {
		boolean isValidCovetous = false;
		for (WeaponAPI weapon : ship.getUsableWeapons()) {
			// limited-ammo missiles only
			if (!weapon.getType().equals(WeaponAPI.WeaponType.MISSILE) || !weapon.usesAmmo() || weapon.getAmmoPerSecond() > 0) {
				continue;
			}
			isValidCovetous = true;
		}
		return isValidCovetous;
	}

	public static class SotfCovetousScript implements AdvanceableListener {
		protected ShipAPI ship;
		protected float internalCDTimer = COVETOUS_COOLDOWN / 2f;
		public SotfCovetousScript(ShipAPI ship) { this.ship = ship; }

		public void advance(float amount) {
			if (!Global.getCurrentState().equals(GameState.COMBAT)) {
				return;
			}
			if (!ship.isAlive() || ship.isFighter() || ship.isStationModule()) {
				return;
			}

			if (!ship.getCustomData().containsKey(VISUAL_KEY)) {
				SotfRingTimerVisualScript.AuraParams p = new SotfRingTimerVisualScript.AuraParams();
				p.color = COVETOUS_COLOR;
				p.ship = ship;
				p.radius = ship.getShieldRadiusEvenIfNoShield() + 18f;
				p.thickness = 11f;
				p.baseAlpha = 0.4f;
				p.maxArc = 60f;
				//p.followFacing = true;
				p.renderDarkerCopy = true;
				p.reverseRing = true;
				p.degreeOffset = 60f;
				p.layer = CombatEngineLayers.JUST_BELOW_WIDGETS;
				SotfRingTimerVisualScript plugin = new SotfRingTimerVisualScript(p);
				Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
				ship.setCustomData(VISUAL_KEY, plugin);
			} else {
				SotfRingTimerVisualScript visual = (SotfRingTimerVisualScript) ship.getCustomData().get(VISUAL_KEY);
				visual.p.totalArc = 1f - (internalCDTimer / COVETOUS_COOLDOWN);
				if (internalCDTimer <= 0) {
					visual.p.baseAlpha = 0.8f;
				}
			}

			float timeMult = (Global.getCombatEngine().getTimeMult().getModifiedValue() * ship.getMutableStats().getTimeMult().getModifiedValue());
			internalCDTimer -= amount * timeMult;

			if (internalCDTimer <= 0) {
				internalCDTimer = 0;
			}

			if (internalCDTimer > 0f || getRefillWeapon(ship) == null) {
				return;
			}

			ShipAPI target = findTarget(ship, COVETOUS_RANGE);
            if (target != null) {
				WeaponAPI refillWeapon = getRefillWeapon(ship);
				WeaponAPI stealWeapon = getStealWeapon(target);

				if (refillWeapon == null || stealWeapon == null) return;

				// refilled weapon gains up to half its base max ammo rounded up
				int amountToRefill = Math.max((int) Math.ceil(refillWeapon.getSpec().getMaxAmmo() * SHUNT_PERCENT), refillWeapon.getMaxAmmo() - refillWeapon.getAmmo());
				// stolen weapon loses up to half its base max ammo rounded up
				int amountToSteal = (int) Math.ceil(stealWeapon.getSpec().getMaxAmmo() * SHUNT_PERCENT);
				int amountLeft = stealWeapon.getAmmo() - Math.min(amountToSteal, stealWeapon.getAmmo());
				refillWeapon.setAmmo(refillWeapon.getAmmo() + amountToRefill);
				stealWeapon.setAmmo(amountLeft);

				Global.getCombatEngine().spawnEmpArcVisual(refillWeapon.getLocation(), ship, stealWeapon.getLocation(), target, 15f, COVETOUS_COLOR, Color.black);
				Global.getCombatEngine().addFloatingText(refillWeapon.getLocation(), "Ammo refilled!", 12f, Misc.getPositiveHighlightColor(), ship, 1f, 0f);
				Global.getCombatEngine().addFloatingText(stealWeapon.getLocation(), "Ammo stolen!", 12f, Misc.getNegativeHighlightColor(), target, 1f, 0f);

				internalCDTimer = COVETOUS_COOLDOWN;
			}
		}

		public ShipAPI findTarget(ShipAPI ship, float range) {
			Vector2f from = ship.getLocation();

			Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
					range * 2f, range * 2f);
			int owner = ship.getOwner();
			ShipAPI best = null;
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
					if (otherShip.isFighter()) continue;
					if (getStealWeapon(otherShip) == null) continue;
				} else {
					continue;
				}

				if (other.getCollisionClass() == CollisionClass.NONE) continue;

				float radius = Misc.getTargetingRadius(from, other, false);
				float dist = Misc.getDistance(from, other.getLocation()) - radius;
				if (dist > range) continue;

				//float angleTo = Misc.getAngleInDegrees(from, other.getLocation());
				//float score = Misc.getAngleDiff(weapon.getCurrAngle(), angleTo);
				float score = dist;

				if (score < minScore) {
					minScore = score;
					best = (ShipAPI) other;
				}
			}
			return best;
		}

		public WeaponAPI getRefillWeapon(ShipAPI ship) {
			WeaponAPI picked = null;
			WeightedRandomPicker<WeaponAPI> post = new WeightedRandomPicker<WeaponAPI>();
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				// limited-ammo missiles that need refills only. Slightly lenient because stealing ammo is always good, even if it's inefficient
				if (!weapon.getType().equals(WeaponAPI.WeaponType.MISSILE) || !weapon.usesAmmo() || weapon.getAmmoPerSecond() > 0 || ((float) weapon.getAmmo() / (float) weapon.getMaxAmmo() > 0.75f)) {
					continue;
				}
				int weaponWeight = weapon.getSize().ordinal() + 1;
				weaponWeight = weaponWeight * weaponWeight;
				// 1/3/9 weight
				post.add(weapon, weaponWeight);
			}
			if (!post.isEmpty()) {
				picked = post.pick();
			}
			return picked;
		}

		public WeaponAPI getStealWeapon(ShipAPI ship) {
			WeightedRandomPicker<WeaponAPI> post = new WeightedRandomPicker<WeaponAPI>();
			for (WeaponAPI weapon : ship.getUsableWeapons()) {
				// limited-ammo missiles that need refills only
				if (!weapon.getType().equals(WeaponAPI.WeaponType.MISSILE) || !weapon.usesAmmo() || weapon.getAmmoPerSecond() > 0 || weapon.getAmmo() == 0) {
					continue;
				}
				int weaponWeight = weapon.getSize().ordinal() + 1;
				weaponWeight = weaponWeight * weaponWeight;
				// 1/3/9 weight
				post.add(weapon, weaponWeight);
			}
			return post.pick();
		}
	}

	public static class Skedaddle extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.addListener(new SotfSkedaddleScript(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.removeListenerOfClass(SotfSkedaddleScript.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			//stats.getBreakProb().modifyMult(id, 0f);
		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			//stats.getBreakProb().unmodify(id);
		}

		public String getEffectDescription(float level) {
			return null;
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			init(stats, skill);

			Color c = hc;
			info.addPara("Once per battle, when first reduced to %s hull points, piloted ship retreats into phase space for %s seconds, " +
							"during which it experiences time at a faster rate and its maximum speed and flux dissipation is increased by %s",
					0f, c, c, "" + (int) (SKEDADDLE_TRIGGER_PERCENT * 100f) + "%", "" + (int) SKEDADDLE_DURATION, "" + (int) (SKEDADDLE_SPEED_MULT * 100f) + "%");
			info.addPara("Emergency phase also instantly repairs engines and cancels any overload or active venting", c, 0f);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	// At half hull, phase for several seconds and move into a new position
	public static class SotfSkedaddleScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
		public ShipAPI ship;
		public boolean triggered = false;
		public float progress = 0f;

		public SotfSkedaddleScript(ShipAPI ship) {
			this.ship = ship;
		}

		public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
			if (ship.isFighter() || ship.isStationModule()) {
				return false;
			}
			if (!triggered) {
				float hull = ship.getHitpoints();
				if (hull - damageAmount < (ship.getMaxHitpoints() * SKEDADDLE_TRIGGER_PERCENT)) {
					for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
						engine.setHitpoints(engine.getMaxHitpoints());
						engine.repair();
					}
					if (ship.getFluxTracker().isOverloaded()) {
						ship.getFluxTracker().stopOverload();
					} else if (ship.getFluxTracker().isVenting()) {
						ship.getFluxTracker().stopVenting();
					}
					if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getSpecAPI().isPhaseCloak()) {
						ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.IDLE, 0f);
						ship.setDefenseDisabled(true);
					}
					triggered = true;
				}
			}
			return false;
		}

		public void advance(float amount) {
			if (triggered && ship.isAlive() && progress < SKEDADDLE_DURATION) {
				float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue() * Global.getCombatEngine().getTimeMult().getModifiedValue();

				if (progress == 0f) {
					if (ship.getFluxTracker().showFloaty()) {
						Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
								"Emergency Phase",
								NeuralLinkScript.getFloatySize(ship) + 10f,
								COVETOUS_COLOR,
								ship,
								16f * timeMult,
								1f/timeMult,
								1f/timeMult,
								0.5f,
								0.5f,
								1f);
					}
				}

				progress += amount * timeMult;

				float rampUp = 0.25f;
				float rampDown = 0.25f;
				// in
				float intensity = progress / rampUp;
				if (intensity > 1f) {
					intensity = 1f;
				}
				// out
				if (progress > (SKEDADDLE_DURATION - rampDown)) {
					intensity = 1f - ((progress - (SKEDADDLE_DURATION - rampDown)) / rampDown);
				}

				boolean usingSystem = false;
				if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getSpecAPI().isPhaseCloak()) {
					usingSystem = true;
				} else if (ship.getSystem() != null && ship.getSystem().getSpecAPI().isPhaseCloak()) {
					usingSystem = true;
				}

				if (!usingSystem) {
					ship.setJitter(this, new Color(255, 0, 255, 100), intensity, 7, 0, 10f * intensity);
					ship.setExtraAlphaMult2(1f - (0.75f * intensity));
				}
				ship.getMutableStats().getFluxDissipation().modifyMult(SKEDDADLE_KEY, SKEDADDLE_SPEED_MULT * intensity);
				ship.getMutableStats().getPhaseCloakUpkeepCostBonus().modifyMult(SKEDDADLE_KEY, 0f);
				ship.getMutableStats().getMaxSpeed().modifyMult(SKEDDADLE_KEY, SKEDADDLE_SPEED_MULT * intensity);
				ship.getMutableStats().getAcceleration().modifyMult(SKEDDADLE_KEY, SKEDADDLE_SPEED_MULT * intensity);
				ship.getMutableStats().getDeceleration().modifyMult(SKEDDADLE_KEY, SKEDADDLE_SPEED_MULT * intensity);

				float shipTimeMult = 1f + 3f * intensity;
				float perceptionMult = 1f + 1.3f * intensity;
				ship.getMutableStats().getTimeMult().modifyMult(SKEDDADLE_KEY, shipTimeMult);
				if (Global.getCombatEngine().getPlayerShip() == ship) {
					Global.getCombatEngine().getTimeMult().modifyMult(SKEDDADLE_KEY, 1f / perceptionMult);
				} else {
					Global.getCombatEngine().getTimeMult().unmodify(SKEDDADLE_KEY);
				}

				if (intensity > 0.25f) {
					// phase cloak systems tend to force non-phased status when inactive,
					if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getSpecAPI().isPhaseCloak()) {
						ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, intensity);
					} else if (ship.getSystem() != null && ship.getSystem().getSpecAPI().isPhaseCloak()) {
						ship.getSystem().forceState(ShipSystemAPI.SystemState.ACTIVE, intensity);
						ship.setDefenseDisabled(true);
					} else {
						ship.setPhased(true);
						ship.setDefenseDisabled(true);
					}

					ship.setHoldFireOneFrame(true);
					ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
					ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
					ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);

					if (ship.getShipAI() != null) {
						ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF, 0.1f);
					}
				} else {
					ship.setPhased(false);
					ship.setExtraAlphaMult2(1f);

					ship.setDefenseDisabled(false);

					ship.getMutableStats().getFluxDissipation().unmodify(SKEDDADLE_KEY);
					ship.getMutableStats().getPhaseCloakUpkeepCostBonus().unmodify(SKEDDADLE_KEY);
					ship.getMutableStats().getMaxSpeed().unmodify(SKEDDADLE_KEY);
					ship.getMutableStats().getAcceleration().unmodify(SKEDDADLE_KEY);
					ship.getMutableStats().getDeceleration().unmodify(SKEDDADLE_KEY);
				}
			}
		}
	}
}
