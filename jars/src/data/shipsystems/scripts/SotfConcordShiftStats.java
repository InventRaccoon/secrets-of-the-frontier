package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.magiclib.util.MagicRender;
import data.scripts.utils.SotfMisc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SotfConcordShiftStats extends BaseShipSystemScript {
	
	public static final float SHIP_ALPHA_MULT = 0.25f;
	public static final float VULNERABLE_FRACTION = 0f;
	public static float BASE_FLAT_SPEED_BOOST = 50f;
	
	public static final float MAX_TIME_MULT = 3f;
	public static boolean FLUX_LEVEL_AFFECTS_SPEED = true;
	public static float MIN_SPEED_MULT = 0.5f;
	public static float BASE_FLUX_LEVEL_FOR_MIN_SPEED = 0.65f;

	// no, ship-system scripts are not one-per-spec like hullmods are
	//private List<ShipAPI> have_phased = new ArrayList<>();
	private boolean isPhased = false;

	private float afterImageTimer = 0f;

	protected Object STATUSKEY1 = new Object();
	protected Object STATUSKEY2 = new Object();
	protected Object STATUSKEY3 = new Object();
	protected Object STATUSKEY4 = new Object();
	
	
	public static float getMaxTimeMult(MutableShipStatsAPI stats) {
		return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
	}

	protected boolean isDisruptable(ShipSystemAPI cloak) {
		return cloak.getSpecAPI().hasTag(Tags.DISRUPTABLE);
	}

	protected float getDisruptionLevel(ShipAPI ship) {
		if (FLUX_LEVEL_AFFECTS_SPEED) {
			float threshold = ship.getMutableStats().getDynamic().getMod(
					Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).computeEffective(BASE_FLUX_LEVEL_FOR_MIN_SPEED);
			if (threshold <= 0) return 1f;
			float level = ship.getHardFluxLevel() / threshold;
			if (level > 1f) level = 1f;
			return level;
		}
		return 0f;
	}
	
	protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
		float level = effectLevel;
		float f = VULNERABLE_FRACTION;
		
		ShipSystemAPI cloak = playerShip.getPhaseCloak();
		if (cloak == null) cloak = playerShip.getSystem();
		if (cloak == null) return;

		if (level > f) {
//			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
//					cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "can not be hit", false);
			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
					cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
		} else {
//			float INCOMING_DAMAGE_MULT = 0.25f;
//			float percent = (1f - INCOMING_DAMAGE_MULT) * getEffectLevel() * 100;
//			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
//					spec.getIconSpriteName(), cloak.getDisplayName(), "damage mitigated by " + (int) percent + "%", false);
		}

		if (FLUX_LEVEL_AFFECTS_SPEED) {
			if (level > f) {
				if (getDisruptionLevel(playerShip) <= 0f) {
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
							cloak.getSpecAPI().getIconSpriteName(), "concord stable", "top speed at 100%", false);
				} else {
					//String disruptPercent = "" + (int)Math.round((1f - disruptionLevel) * 100f) + "%";
					//String speedMultStr = Strings.X + Misc.getRoundedValue(getSpeedMult());
					String speedPercentStr = (int) Math.round(getSpeedMult(playerShip, effectLevel) * 100f) + "%";
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
							cloak.getSpecAPI().getIconSpriteName(),
							//"phase coils at " + disruptPercent,
							"concord stress",
							"top speed at " + speedPercentStr, true);
				}
			}
		}
	}

	public float getSpeedMult(ShipAPI ship, float effectLevel) {
		if (getDisruptionLevel(ship) <= 0f) return 1f;
		return MIN_SPEED_MULT + (1f - MIN_SPEED_MULT) * (1f - getDisruptionLevel(ship) * effectLevel);
	}
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

		if (player) {
			maintainStatus(ship, state, effectLevel);
		}

		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		if (state == State.COOLDOWN || state == State.IDLE) {
			isPhased = false;
			afterImageTimer = 0f;
			unapply(stats, id);
			return;
		}

		float baseSpeedBonus = BASE_FLAT_SPEED_BOOST;

		//if (ship.getVariant().hasHullMod(SotfIDs.EIDOLONS_CONCORD)) {
		//	baseSpeedBonus = 25f;
		//}

		float level = effectLevel;
		//float f = VULNERABLE_FRACTION;

		float jitterLevel = 0f;
		float jitterRangeBonus = 0f;
		float levelForAlpha = level;

		ShipSystemAPI cloak = ship.getSystem();
		if (!cloak.getSpecAPI().isPhaseCloak()) cloak = ship.getPhaseCloak();

		if (FLUX_LEVEL_AFFECTS_SPEED) {
			if (state == State.ACTIVE || state == State.OUT || state == State.IN) {
				float mult = getSpeedMult(ship, effectLevel);
				if (mult < 1f) {
					stats.getMaxSpeed().modifyMult(id + "_2", mult);
				} else {
					stats.getMaxSpeed().unmodifyMult(id + "_2");
				}
				((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(getDisruptionLevel(ship));
			}
		}

		float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(baseSpeedBonus);
		float accelPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(baseSpeedBonus);
		stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
		stats.getMaxTurnRate().modifyPercent(id, accelPercentMod * effectLevel);
		stats.getTurnAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
		stats.getAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
		stats.getDeceleration().modifyPercent(id, accelPercentMod * effectLevel);

		float speedMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult();
		float accelMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult();
		stats.getMaxSpeed().modifyMult(id, speedMultMod * effectLevel);
		stats.getMaxTurnRate().modifyPercent(id, accelMultMod * effectLevel);
		stats.getTurnAcceleration().modifyPercent(id, accelMultMod * effectLevel);
		stats.getAcceleration().modifyMult(id, accelMultMod * effectLevel);
		stats.getDeceleration().modifyMult(id, accelMultMod * effectLevel);

		stats.getShieldUpkeepMult().modifyMult(id, 0.5f);

		if (state == State.ACTIVE) {
			boolean nearbyTargets = false;
			for (ShipAPI target : CombatUtils.getShipsWithinRange(ship.getLocation(), 1500)) {
				if ((ship.getOwner() != target.getOwner()) && (target.getOwner() == 0 || target.getOwner() == 1)) {
					nearbyTargets = true;
				}
			}
			if (nearbyTargets) {
				for (WeaponAPI weapon : ship.getAllWeapons()) {
					if (weapon.getSpec().hasTag("sotf_phaseweapon")) {
						weapon.setForceFireOneFrame(true);
					}
				}
			}
		}

		if (state == State.IN || state == State.ACTIVE) {
			ship.setPhased(true);
			levelForAlpha = level;
			if (!isPhased) {

				NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(new Color(200,125,255,155), ship.getShieldRadiusEvenIfNoShield() * 0.25f);
				p.fadeOut = 0.15f;
				p.hitGlowSizeMult = 0.25f;
				p.underglow = new Color(255,175,255, 50);
				p.withHitGlow = false;
				p.noiseMag = 1.25f;

				if (ship.getVariant().hasHullMod(SotfIDs.HULLMOD_SERENITY)) {
					p.noiseMag *= 0.25f;
				}

				if (ship.getVariant().hasHullMod(SotfIDs.HULLMOD_FERVOR)) {
					p.thickness += 10f;
					p.noiseMag *= 2f;
				}

				CombatEntityAPI e = Global.getCombatEngine().addLayeredRenderingPlugin(new NegativeExplosionVisual(p));
				e.getLocation().set(ship.getLocation());

				//if (SotfModPlugin.GLIB) {
				if (SotfModPlugin.GLIB && !ship.getVariant().hasHullMod(SotfIDs.HULLMOD_SERENITY)) {
					RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
					ripple.setIntensity(ship.getCollisionRadius() * 0.75f);
					ripple.setSize(ship.getShieldRadiusEvenIfNoShield());
					ripple.fadeInSize(0.15f);
					ripple.fadeOutIntensity(0.5f);
					DistortionShader.addDistortion(ripple);
				}

				isPhased = true;

				String whisperType = "playful";
				if (Global.getSector() != null) {
					if (SotfMisc.getPlayerGuilt() >= 4f) {
						whisperType = "angry";
					}
				}

				// phase ghost whispers
				if (player && Math.random() < 0.1f) {
					Global.getSoundPlayer().playUISound("sotf_ghost_" + whisperType, 1f, 1f);
				}
			}
		} else if (state == State.OUT) {
			if (level > 0.5f) {
				ship.setPhased(true);
			} else {
				ship.setPhased(false);
			}
			levelForAlpha = level;
		}

		ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
		ship.setApplyExtraAlphaToEngines(true);

		float shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * levelForAlpha;
		float perceptionMult = shipTimeMult;
		if (player) {
			perceptionMult = 1f + ((getMaxTimeMult(stats) - 1f) * 0.65f) * levelForAlpha;
		}
		stats.getTimeMult().modifyMult(id, shipTimeMult);
		if (player) {
			Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / perceptionMult);
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}
	}


	public void unapply(MutableShipStatsAPI stats, String id) {
		
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			//player = ship == Global.getCombatEngine().getPlayerShip();
			//if (player) {
			//	ship.getSystem().setCooldownRemaining(2f);
			//}
			//id = id + "_" + ship.getId();
		} else {
			return;
		}
		
		Global.getCombatEngine().getTimeMult().unmodify(id);
		stats.getTimeMult().unmodify(id);
		
		ship.setPhased(false);
		ship.setExtraAlphaMult(1f);

		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getShieldUpkeepMult().unmodify(id);

		// get system first before checking for right-click because Sierra uses F key for phase
		ShipSystemAPI cloak = ship.getSystem();
		if (!cloak.getSpecAPI().isPhaseCloak()) cloak = ship.getPhaseCloak();
		if (cloak != null) {
			((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(0f);
		}
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
//		if (index == 0) {
//			return new StatusData("time flow altered", false);
//		}
//		float percent = (1f - INCOMING_DAMAGE_MULT) * effectLevel * 100;
//		if (index == 1) {
//			return new StatusData("damage mitigated by " + (int) percent + "%", false);
//		}
		return null;
	}

	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return !ship.getVariant().hasTag(SotfIDs.TAG_INERT) || SotfMisc.isSiCNonInert();
	}
}
