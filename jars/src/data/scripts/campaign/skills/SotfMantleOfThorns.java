// EVERY ROSE ITS THORN. Ship fires EMP arcs when taking hits to shields, and builds charge to release as an EMP storm
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.*;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.SotfRingTimerVisualScript;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SotfMantleOfThorns {

	public static float REFLECT_COOLDOWN = 2f;

	public static float REFLECT_FRACTION = 0.4f;

	// maximum charge = should be multiple of DISCHARGE_DAMAGE
	public static Map<HullSize, Float> MAX_CHARGE = new HashMap<HullSize, Float>();
	static {
		MAX_CHARGE.put(HullSize.FIGHTER, 1000f);
		MAX_CHARGE.put(HullSize.FRIGATE, 1500f);
		MAX_CHARGE.put(HullSize.DESTROYER, 2500f);
		MAX_CHARGE.put(HullSize.CRUISER, 3500f);
		MAX_CHARGE.put(HullSize.CAPITAL_SHIP, 5000f);
	}
	public static float DISCHARGE_DAMAGE = 500f;
	public static float DISCHARGE_INTERVAL = 0.3f;
	public static float ABSORPTION_FRACTION = 0.75f;
	public static Color EMP_COLOR = new Color(75, 75, 105, 255);

	public static final String VISUAL_KEY = "sotf_crownofbriars_visual";

	public static class CrownOfBriars extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.addListener(new SotfCrownOfBriarsDTM(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.removeListenerOfClass(SotfCrownOfBriarsDTM.class);
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

			info.addPara("Incoming hits on shields from non-missiles are countered with an EMP arc that deals %s energy damage and %s EMP damage", 0f, hc, hc,
					"" + (int) (DISCHARGE_DAMAGE * REFLECT_FRACTION), "" + (int) (DISCHARGE_DAMAGE * REFLECT_FRACTION));
			info.addPara("Minimum delay of %s seconds between arcs", 0f, hc, hc, "" + new DecimalFormat("#.##").format(REFLECT_COOLDOWN));
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfCrownOfBriarsDTM implements DamageTakenModifier, AdvanceableListener {
		protected ShipAPI ship;
		protected float arcTimer = 0f;
		public SotfCrownOfBriarsDTM(ShipAPI ship) { this.ship = ship; }

		public void advance(float amount) {
			// shouldn't happen but let's be safe
			if (ship.getShield() == null) {
				return;
			}
			ship.getShield().setInnerColor(EMP_COLOR);
			ship.getShield().setInnerRotationRate(3f);

			arcTimer -= amount * (Global.getCombatEngine().getTimeMult().getModifiedValue() * ship.getMutableStats().getTimeMult().getModifiedValue());

			if (arcTimer < 0) {
				arcTimer = 0;
			}
		}

		public String modifyDamageTaken(Object param,
										CombatEntityAPI target, DamageAPI damage,
										Vector2f point, boolean shieldHit) {
			if (!ship.isAlive()) {
				return null;
			}
			if (!shieldHit || param == null || ship.getShield() == null || arcTimer > 0) {
				return null;
			}
			// not while Seething Outburst is triggering
			if (ship.hasListenerOfClass(SotfTheSeethingCurseDTM.class)) {
				SotfTheSeethingCurseDTM outburst = ship.getListenerManager().getListeners(SotfTheSeethingCurseDTM.class).get(0);
				if (outburst.discharging) {
					return null;
				}
			}
			ShipAPI lastAttacker = null;
			if (param instanceof DamagingProjectileAPI) {
				DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
				if (proj.getSource() != null && !(proj instanceof MissileAPI) && !proj.isFromMissile()) {
					lastAttacker = proj.getSource();
				}
			} else if (param instanceof BeamAPI) {
				BeamAPI beam = (BeamAPI) param;
				if (beam.getSource() != null) {
					lastAttacker = beam.getSource();
				}
			}
			if (lastAttacker == null) {
				lastAttacker = findTarget(ship, 1500f);
			}
			if (lastAttacker == null) {
				return null;
			}
			Global.getCombatEngine().spawnEmpArc(ship,
					point,
					ship, lastAttacker,
					DamageType.ENERGY,
					DISCHARGE_DAMAGE * REFLECT_FRACTION,
					DISCHARGE_DAMAGE * REFLECT_FRACTION,
					100000f,
					"tachyon_lance_emp_impact",
					25f,
					EMP_COLOR,
					Color.white
			);
			arcTimer = REFLECT_COOLDOWN;
			return null;
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
					if (otherShip.isFighter() && (otherShip.getEngineController().isFlamedOut() || otherShip.getEngineController().isFlamingOut())) {
						continue;
					}
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
	}

	public static class TheSeethingCurse extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.addListener(new SotfTheSeethingCurseDTM(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.removeListenerOfClass(SotfTheSeethingCurseDTM.class);
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

			info.addPara("Incoming hits on shields store %s of damage taken as charge to a maximum of %s/%s/%s/%s based on hull size", 0f, hc, hc,
					"" + (int) (ABSORPTION_FRACTION * 100f) + "%",
					"" + MAX_CHARGE.get(ShipAPI.HullSize.FRIGATE).intValue(),
					"" + MAX_CHARGE.get(ShipAPI.HullSize.DESTROYER).intValue(),
					"" + MAX_CHARGE.get(ShipAPI.HullSize.CRUISER).intValue(),
					"" + MAX_CHARGE.get(ShipAPI.HullSize.CAPITAL_SHIP).intValue());
			info.addPara("At maximum charge, ship's shield begins consuming all charge to rapidly fire shield-piercing EMP arcs at nearby ships and fighters", hc, 0f);
			info.addPara("Disables \"Crown of Briars\" effect while discharging", hc, 0f);
			info.addPara("Discharge ends immediately if the ship overloads", hc, 0f);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfTheSeethingCurseDTM implements DamageListener, AdvanceableListener {
		protected ShipAPI ship;
		protected ShipAPI lastAttacker;
		protected float charge = 0f;
		protected float maxCharge = 750f;
		protected boolean ranOnce = false;
		protected boolean discharging = false;
		protected float arcTimer = DISCHARGE_INTERVAL;
		public SotfTheSeethingCurseDTM(ShipAPI ship) { this.ship = ship; }

		public void advance(float amount) {
			if (!ranOnce) {
				maxCharge = MAX_CHARGE.get(ship.getHullSize());
				ranOnce = true;
			}
			// shouldn't happen but let's be safe
			if (ship.getShield() == null) {
				return;
			}

			if (!ship.getCustomData().containsKey(VISUAL_KEY)) {
				SotfRingTimerVisualScript.AuraParams p = new SotfRingTimerVisualScript.AuraParams();
				p.color = Misc.setAlpha(EMP_COLOR.brighter().brighter(), 255);
				p.ship = ship;
				p.radius = ship.getShieldRadiusEvenIfNoShield() + 15f;
				p.thickness = 12f;
				p.maxArc = 60f;
				p.baseAlpha = 0.75f;
				//p.followFacing = true;
				p.renderDarkerCopy = true;
				p.degreeOffset = 30f;
				p.layer = CombatEngineLayers.JUST_BELOW_WIDGETS;
				SotfRingTimerVisualScript plugin = new SotfRingTimerVisualScript(p);
				Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
				ship.setCustomData(VISUAL_KEY, plugin);
			} else {
				SotfRingTimerVisualScript visual = (SotfRingTimerVisualScript) ship.getCustomData().get(VISUAL_KEY);
				visual.p.totalArc = charge / maxCharge;
			}
			if (discharging) {
				if (ship.getFluxTracker().isOverloaded()) {
					discharging = false;
					charge = 0f;
				}
				if (ship.getFluxLevel() < 0.8f && ship.getShipAI() != null) {
					ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON);
				}
				arcTimer -= amount * (Global.getCombatEngine().getTimeMult().getModifiedValue() * ship.getMutableStats().getTimeMult().getModifiedValue());
				if (arcTimer < 0) {
					charge -= DISCHARGE_DAMAGE;
					if (ship.getShield().isOn()) {
						ShipAPI empTarget = findTarget(ship, 1200f);
						float shieldAngle = ship.getShield().getFacing();
						float bonus = Math.min(ship.getShield().getActiveArc(), 160f) / 2;
						bonus *= Math.random();
						if (Math.random() > 0.5f) {
							bonus *= -1;
						}
						Vector2f from = MathUtils.getPointOnCircumference(ship.getShieldCenterEvenIfNoShield(), ship.getShieldRadiusEvenIfNoShield(), shieldAngle + bonus);
						if (empTarget == null) {
							Vector2f to = Misc.getPointAtRadius(ship.getLocation(), 100f + (50f * (float) Math.random()));
							EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
									from,
									ship,
									to,
									ship,
									20f,
									EMP_COLOR,
									Color.white
							);
						} else {
							if (empTarget.isFighter()) {
								Global.getCombatEngine().spawnEmpArcVisual(from, ship, empTarget.getLocation(), empTarget, 20f,
										EMP_COLOR,
										Color.white);
								empTarget.getEngineController().forceFlameout();
							} else if (Math.random() > 0.5f) {
								Global.getCombatEngine().spawnEmpArcPierceShields(ship,
										from,
										ship, empTarget,
										DamageType.ENERGY,
										DISCHARGE_DAMAGE * 0.25f,
										DISCHARGE_DAMAGE,
										100000f,
										"tachyon_lance_emp_impact",
										20f,
										EMP_COLOR,
										Color.white
								);
							} else {
								Global.getCombatEngine().spawnEmpArc(ship,
										from,
										ship, empTarget,
										DamageType.ENERGY,
										DISCHARGE_DAMAGE * 0.25f,
										DISCHARGE_DAMAGE,
										100000f,
										"tachyon_lance_emp_impact",
										20f,
										EMP_COLOR,
										Color.white
								);
							}
						}
					}
					arcTimer += DISCHARGE_INTERVAL;
					if (charge <= 0) {
						discharging = false;
						charge = 0f;
					}
				}
			} else {
				arcTimer = DISCHARGE_INTERVAL;
			}
		}

		public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
			if (!ship.isAlive()) {
				return;
			}
			if (result.getDamageToShields() == 0 || source == null || discharging) {
				return;
			}
			if (source instanceof DamagingProjectileAPI) {
				DamagingProjectileAPI proj = (DamagingProjectileAPI) source;
				if (proj.getSource() != null && !(proj instanceof MissileAPI) && !proj.isFromMissile()) {
					lastAttacker = proj.getSource();
				}
			} else if (source instanceof BeamAPI) {
				BeamAPI beam = (BeamAPI) source;
				if (beam.getSource() != null) {
					lastAttacker = beam.getSource();
				}
			}
			charge += result.getDamageToShields() * ABSORPTION_FRACTION;
			if (charge >= maxCharge) {
				charge = maxCharge;
				discharging = true;
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
					if (otherShip.isFighter() && (otherShip.getEngineController().isFlamedOut() || otherShip.getEngineController().isFlamingOut())) {
						continue;
					}
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
	}

}
