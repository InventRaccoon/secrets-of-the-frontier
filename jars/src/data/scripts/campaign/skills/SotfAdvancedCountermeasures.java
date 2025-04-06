package data.scripts.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.hullmods.SotfNaniteSynthesized;
import data.subsystems.SotfNaniteDronesSubsystem;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.awt.*;
import java.util.*;

import static data.scripts.weapons.SotfLethargyOnFireEffect.lethargyFakeBeam;

public class SotfAdvancedCountermeasures {

	public static String NANITE_SWARM_EXCHANGE_CLASS = "sotf_nanite_swarm_exchange_class";
	public static float EW_PENALTY_MULT = 0.5f;

	public static Color SWARM_ATTACK_COLOR = new Color(100,255,205);
	public static float SWARM_ATTACK_RANGE = 400f;
	public static float SWARM_ATTACK_DAMAGE = 50f;
	public static float SWARM_ATTACK_EMP = 50f;
	public static DamageType SWARM_ATTACK_DAMAGE_TYPE = DamageType.FRAGMENTATION;
	// delay between shots for a frigate
	public static float SWARM_ATTACK_RATE = 2f;

	// Attack swarm attack rate multiplier based on hull size
	public static Map<HullSize, Float> SWARM_ATTACK_RATE_MULT = new HashMap<HullSize, Float>();
	static {
		SWARM_ATTACK_RATE_MULT.put(HullSize.FIGHTER, 0.5f);
		SWARM_ATTACK_RATE_MULT.put(HullSize.FRIGATE, 1f);
		SWARM_ATTACK_RATE_MULT.put(HullSize.DESTROYER, 1.5f);
		SWARM_ATTACK_RATE_MULT.put(HullSize.CRUISER, 2f);
		SWARM_ATTACK_RATE_MULT.put(HullSize.CAPITAL_SHIP, 3f);
	}

	public static class ECCM implements ShipSkillEffect {
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD).modifyMult(id, EW_PENALTY_MULT);
		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD).unmodify(id);
		}
		public String getEffectDescription(float level) {
			return "" + Math.round(EW_PENALTY_MULT * 100f) + "% reduced penalty to weapon range due to superior enemy Electronic Warfare";
		}
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class Drones extends BaseSkillEffectDescription implements ShipSkillEffect, AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			MagicSubsystemsManager.addSubsystemToShip(ship, new SotfNaniteDronesSubsystem(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			MagicSubsystemsManager.removeSubsystemFromShip(ship, SotfNaniteDronesSubsystem.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			initElite(stats, skill);

			info.addPara("Deploys up to 2/4/6/8 interceptor drones to protect the ship, depending on size", hc, 0f
			);
		}
	}

	public static class Swarm extends BaseSkillEffectDescription implements ShipSkillEffect, AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SotfNaniteDefenseSwarmScript(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SotfNaniteDefenseSwarmScript(ship));
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			initElite(stats, skill);

			info.addPara("Deploys an orbiting defense swarm to protect the ship with energy beams", hc, 0f);
			info.addPara("Each fighter craft deployed also gains its own swarm", hc, 0f);
		}

		public static class SotfNaniteDefenseSwarmScript implements AdvanceableListener {

			protected ShipAPI ship;

			IntervalUtil interval = new IntervalUtil(SWARM_ATTACK_RATE, SWARM_ATTACK_RATE + 0.2f);
			float rate;

			public SotfNaniteDefenseSwarmScript(ShipAPI ship) {
				this.ship = ship;
				this.rate = SWARM_ATTACK_RATE_MULT.get(ship.getHullSize());
			}

			public void advance(float amount) {
				if (!Global.getCurrentState().equals(GameState.COMBAT)) {
					return;
				}
				if (amount <= 0f || ship == null) return;

				RoilingSwarmEffect swarm = RoilingSwarmEffect.getSwarmFor(ship);
				// create a swarm for non-nanite-synthesized ships, i.e. for Fel
				if (swarm == null) {
					swarm = SotfNaniteSynthesized.SotfNaniteSynthesizedListener.createSwarmFor(ship);
				}

				interval.advance(amount * rate);
				if (interval.intervalElapsed()) {
					fire(swarm);
				}
			}

			public void fire(RoilingSwarmEffect swarm) {
				CombatEntityAPI target = findTarget();

				if (target == null) return;

				RoilingSwarmEffect.SwarmMember pick = pickFragmentTowardsPointWithinRange(swarm, target.getLocation(), 150f);
				if (pick == null) return;

				pick.setRecentlyPicked(1f);

				lethargyFakeBeam(
						Global.getCombatEngine(),
						pick.loc,
						Misc.getDistance(pick.loc, target.getLocation()),
						Misc.getAngleInDegrees(pick.loc, target.getLocation()),
						5,
						0.05f,
						0.15f,
						8,
						Color.WHITE,
						SWARM_ATTACK_COLOR,
						SWARM_ATTACK_DAMAGE,
						SWARM_ATTACK_DAMAGE_TYPE,
						SWARM_ATTACK_EMP,
						rate,
						ship
				);

				pick.flash();
				pick.flash.forceIn();
				pick.flash.setDurationOut(0.25f);

//				float thickness = 30f;
//				//Color color = weapon.getSpec().getGlowColor();
//				//Color color = new Color(255,0,0,255);
//				Color color = SotfNaniteSynthesized.COLOR;
//				Color coreColor = Color.white;
//
//				float coreWidthMult = 0.75f;
//
//				EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
//				params.segmentLengthMult = 2f;
//				//params.zigZagReductionFactor = 0.25f;
//				params.zigZagReductionFactor = 0.25f;
//				//params.maxZigZagMult = 0f;
//				//params.flickerRateMult = 0.75f;
//				params.flickerRateMult = 1f;
//				params.fadeOutDist = 1000f;
//				params.minFadeOutMult = 1f;
////		params.fadeOutDist = 200f;
////		params.minFadeOutMult = 2f;
//				params.glowSizeMult = 0.5f;
//				params.glowAlphaMult = 0.75f;
//
//				// actually, probably fine given how long it takes to chew through the missile health with low damage per hit
//				//params.flamesOutMissiles = false; // a bit much given the RoF and general prevalence
//
//				pick.flash();
//				pick.flash.forceIn();
//				pick.flash.setDurationOut(0.25f);
//
//				EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArc(ship, pick.loc, ship,
//						target,
//						DamageType.FRAGMENTATION,
//						25f,
//						50f, // emp
//						100000f, // max range
//						"voltaic_discharge_emp_impact",
//						thickness, // thickness
//						color,
//						coreColor,
//						params
//				);
//				arc.setCoreWidthOverride(thickness * coreWidthMult);
//				arc.setSingleFlickerMode();
//				//arc.setUpdateFromOffsetEveryFrame(true);
//				arc.setRenderGlowAtStart(false);
//				arc.setFadedOutAtStart(true);
			}

			public CombatEntityAPI findTarget() {
				float range = (SWARM_ATTACK_RANGE + ship.getShieldRadiusEvenIfNoShield()) * ship.getMutableStats().getEnergyWeaponRangeBonus().getBonusMult();
				Vector2f from = ship.getShieldCenterEvenIfNoShield();

				Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
						range * 2f, range * 2f);
				int owner = ship.getOwner();
				CombatEntityAPI best = null;
				float minScore = Float.MAX_VALUE;

				boolean ignoreFlares = ship.getMutableStats().getDynamic().getValue(Stats.PD_IGNORES_FLARES, 0) >= 1;

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
						if (!otherShip.isTargetable()) continue;
					}

					if (ignoreFlares && other instanceof MissileAPI) {
						MissileAPI missile = (MissileAPI) other;
						if (missile.isFlare()) continue;
					}

					float radius = Misc.getTargetingRadius(from, other, false);
					float dist = Misc.getDistance(from, other.getLocation()) - radius;
					if (dist > range) continue;

					float score = dist;

					if (score < minScore) {
						minScore = score;
						best = other;
					}
				}
				return best;
			}

			public static RoilingSwarmEffect.SwarmMember pickFragmentTowardsPointWithinRange(RoilingSwarmEffect swarm, Vector2f towards, float maxRange) {
				WeightedRandomPicker<RoilingSwarmEffect.SwarmMember> picker = swarm.getPicker(true, true, towards);
				while (!picker.isEmpty()) {
					RoilingSwarmEffect.SwarmMember p = picker.pickAndRemove();
					float dist = Misc.getDistance(p.loc, swarm.getAttachedTo().getLocation());
					if (dist > maxRange) continue;
					return p;
				}
				return null;
			}
		}
	}
}
