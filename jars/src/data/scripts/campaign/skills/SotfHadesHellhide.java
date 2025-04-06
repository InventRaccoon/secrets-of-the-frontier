// DRAG THEM TO THE GATES OF HADES. Ship gains a damage-mitigating skinshield that recharges out of combat
// also kills everyone on board when taking hull damage
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import data.scripts.combat.SotfAuraVisualScript;
import data.scripts.combat.SotfRingTimerVisualScript;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SotfHadesHellhide {

	// skinshield max integrity
	public static Map<HullSize, Float> MAX_CAPACITY = new HashMap<HullSize, Float>();
	static {
		MAX_CAPACITY.put(HullSize.FIGHTER, 350f);
		MAX_CAPACITY.put(HullSize.FRIGATE, 750f);
		MAX_CAPACITY.put(HullSize.DESTROYER, 1250f);
		MAX_CAPACITY.put(HullSize.CRUISER, 2500f);
		MAX_CAPACITY.put(HullSize.CAPITAL_SHIP, 4000f);
	}
	public static float MIN_DAMAGE_THRESHOLD = 0f; // nevermind, reworked to work against everything
	//public static float MIN_DAMAGE_THRESHOLD = 750f; // old default: at least as hard-hitting as a Harpoon
	public static float DAMAGE_REDUCTION = 0.65f; // percent reduced
	public static float RECHARGE_TIME = 15f; // in seconds
	public static float OUT_OF_COMBAT_TIME = 6f; // in seconds

	public static float CREW_LOSS_PERCENT = 2000f; // the Hellhide demands its tribute

	public static Color SKINSHIELD_JITTER = new Color(0,255,200,155);
	//public static Color SCRAPHIDE_JITTER_UNDER = new Color(255,90,75,55);

	public static String HADESHELLHIDE_KEY = "sotf_hadeshellhide";

	public static final String HELLHIDE_CD_KEY = "sotf_hadeshellhide_cdvisual";

	public static class Level0 implements DescriptionSkillEffect {
		public String getString() {
			return
					"*If the blocked damage would be greater than its current integrity, the skinshield's damage reduction is " +
							"reduced proportionally"
					;
		}
		public Color[] getHighlightColors() {
			return null;
		}
		public String[] getHighlights() {
			return null;
		}
		public Color getTextColor() {
			return null;
		}
	}

	public static class InfernalArmor extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SotfHideOfHadesDamageTakenMod(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(SotfHideOfHadesDamageTakenMod.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			stats.getCrewLossMult().modifyPercent(id, CREW_LOSS_PERCENT);
		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getCrewLossMult().unmodify(id);
		}

		public String getEffectDescription(float level) {
			return null;
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			init(stats, skill);

			info.addPara("Piloted ship gains a skinshield that blocks %s of damage taken to hull and armor, excluding EMP damage", 0f, hc, hc,
					"" + (int) (DAMAGE_REDUCTION * 100f) + "%");
			info.addPara("Skinshield has a max capacity of %s/%s/%s/%s damage (based on hull size), and loses integrity equal to the " +
							"damage it blocks", 0f, hc, hc,
					"" + MAX_CAPACITY.get(HullSize.FRIGATE).intValue(),
					"" + MAX_CAPACITY.get(HullSize.DESTROYER).intValue(),
					"" + MAX_CAPACITY.get(HullSize.CRUISER).intValue(),
					"" + MAX_CAPACITY.get(HullSize.CAPITAL_SHIP).intValue());
			info.addPara("Skinshield shuts down while overloaded or venting, and regenerates over %s seconds if it has not been harmed for %s seconds", 0f, hc, hc,
					"" + (int) RECHARGE_TIME, "" + (int) OUT_OF_COMBAT_TIME);
			info.addPara("+%s crew lost due to hull damage in combat", 0f, hc, hc,
					"" + (int) (CREW_LOSS_PERCENT) + "%");
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfHideOfHadesDamageTakenMod implements DamageTakenModifier, AdvanceableListener {
		protected ShipAPI ship;
		protected float integrity = 0f;
		protected float maxCapacity = 1000f;
		protected float timeOutOfCombat = 0f;
		protected boolean ranOnce = false;
		protected boolean active = true;
		protected float arcTimer = 0.6f;
		public SotfHideOfHadesDamageTakenMod(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (integrity > maxCapacity) {
				integrity = maxCapacity;
			} else if (integrity < 0) {
				integrity = 0;
			}
			if (!ship.isAlive()) {
				integrity = 0f;
				return;
			}
			if (!ranOnce) {
				//maxCapacity = (ship.getHullSize().ordinal() - 1) * 1000f;
				//if (ship.isCapital()) {
				//	maxCapacity += 1000f;
				//}
				maxCapacity = MAX_CAPACITY.get(ship.getHullSize());
				integrity = maxCapacity;
				ranOnce = true;
			}
			float integrityFraction = integrity / maxCapacity;
			if (integrityFraction > 1) {
				integrityFraction = 1;
			} else if (integrityFraction < 0) {
				integrityFraction = 0;
			}
			float timeMult = (Global.getCombatEngine().getTimeMult().getModifiedValue() * ship.getMutableStats().getTimeMult().getModifiedValue());
			float realTimePassed = amount * timeMult;
			// Overload EMP arcs
			if (!active) {
				arcTimer -= realTimePassed;
				if (arcTimer <= 0) {
					Global.getCombatEngine().spawnEmpArcPierceShields(ship,
							ship.getShieldCenterEvenIfNoShield(),
							ship,
							ship,
							DamageType.ENERGY,
							0f,
							0f,
							100000f,
							"tachyon_lance_emp_impact",
							15f,
							SKINSHIELD_JITTER,
							Color.white
					);
					float randomFactor = (float) Math.random();
					arcTimer = 0.5f + (0.3f * randomFactor);
				}
			}
			// Recharge hellhide if out of combat
			if (integrityFraction < 1f) {
				timeOutOfCombat += realTimePassed;
				if (timeOutOfCombat >= OUT_OF_COMBAT_TIME) {
					integrity += ((maxCapacity / RECHARGE_TIME) * realTimePassed);
					if (integrity > maxCapacity) {
						integrity = maxCapacity;
					}
					if (!active && integrityFraction >= 0.5f) {
						ship.getFluxTracker().showOverloadFloatyIfNeeded("Hellhide reactivated", Misc.setAlpha(SKINSHIELD_JITTER, 255), 6f, true);
						Global.getSoundPlayer().playSound("system_phase_cloak_collision", 1.3f, 1f, ship.getLocation(), ship.getVelocity());
						active = true;
					}
				}
			}
			// Jitter visual
			if (integrity > 0 && !ship.getFluxTracker().isOverloadedOrVenting()) {
				Color jitterColor = SKINSHIELD_JITTER;
				if (!active) {
					jitterColor = SKINSHIELD_JITTER.darker().darker();
				}
				ship.setJitterUnder(this, jitterColor, Math.min(integrityFraction * ship.getCombinedAlphaMult(), 1f), Math.min(10 + (int) (15 * integrityFraction), 25), 3, 5);
			}
			// Status on screen if player ship
			if (Global.getCombatEngine().getPlayerShip().equals(ship)) {
				String status = "Skinshield";
				if (ship.getFluxTracker().isOverloadedOrVenting()) {
					status += " inactive";
				} else if (integrity > 0f) {
					status += " active - integrity " + (int) integrity + "/" + (int) maxCapacity;
				} else {
					status += " rebooting - " + Math.round(OUT_OF_COMBAT_TIME - timeOutOfCombat) + "s";
				}
				Global.getCombatEngine().maintainStatusForPlayerShip(HADESHELLHIDE_KEY, "graphics/icons/hullsys/damper_field.png", "Hellhide of Hades", status, false);
			}
			// Recharge timer visual
			if (!ship.getCustomData().containsKey(HELLHIDE_CD_KEY)) {
				SotfRingTimerVisualScript.AuraParams p = new SotfRingTimerVisualScript.AuraParams();
				p.color = Misc.setAlpha(SKINSHIELD_JITTER, 125);
				p.ship = ship;
				p.radius = ship.getShieldRadiusEvenIfNoShield() + 35f;
				p.thickness = 9f;
				p.maxArc = 100f;
				p.baseAlpha = 0.65f;
				//p.followFacing = true;
				p.renderDarkerCopy = true;
				p.degreeOffset = 40f;
				p.layer = CombatEngineLayers.JUST_BELOW_WIDGETS;
				SotfRingTimerVisualScript plugin = new SotfRingTimerVisualScript(p);
				Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
				ship.setCustomData(HELLHIDE_CD_KEY, plugin);
			} else {
				SotfRingTimerVisualScript visual = (SotfRingTimerVisualScript) ship.getCustomData().get(HELLHIDE_CD_KEY);
				visual.p.totalArc = integrityFraction;
				if (!active) {
					visual.p.color = Misc.getNegativeHighlightColor();
				} else {
					visual.p.color = SKINSHIELD_JITTER;
				}
			}
		}

		public String modifyDamageTaken(Object param, CombatEntityAPI target,
										DamageAPI damage, Vector2f point,
										boolean shieldHit) {
			if (!ship.isAlive()) {
				return null;
			}
			if (!shieldHit && timeOutOfCombat >= (OUT_OF_COMBAT_TIME - 2f)) {
				timeOutOfCombat = (OUT_OF_COMBAT_TIME - 2f);
			}
			if (!shieldHit && integrity > 0f && active && damage.getBaseDamage() >= MIN_DAMAGE_THRESHOLD && !ship.getFluxTracker().isOverloadedOrVenting()) {
				timeOutOfCombat = 0f;
				float integrityMod = integrity / (damage.getDamage() * DAMAGE_REDUCTION);
				// e.g 400 / 800 = 0.5x effectiveness
				if (integrityMod > 1f) {
					integrityMod = 1f;
				}
				integrity -= damage.getDamage() * DAMAGE_REDUCTION;
				damage.getModifier().modifyMult(HADESHELLHIDE_KEY, (1f - (DAMAGE_REDUCTION * integrityMod)));
				if (integrity <= 0f) {
					integrity = 0f;
					active = false;
					ship.getFluxTracker().showOverloadFloatyIfNeeded("Hellhide overloaded!", Misc.setAlpha(SKINSHIELD_JITTER, 255), 6f, true);
					Global.getSoundPlayer().playSound("ui_downgrade_industry", 1f, 1f, ship.getLocation(), ship.getVelocity());
					if (SotfModPlugin.GLIB) {
						RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
						ripple.setIntensity(30f);
						ripple.setSize(ship.getShieldRadiusEvenIfNoShield());
						ripple.fadeInSize(0.25f);
						ripple.fadeOutIntensity(0.5f);
						DistortionShader.addDistortion(ripple);
					}
				}
				return HADESHELLHIDE_KEY + "_dam_mod";
			}
			return null;
		}
	}

}
