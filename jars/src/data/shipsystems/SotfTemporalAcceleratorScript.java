package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.SotfRingTimerVisualScript;
import data.scripts.utils.SotfMisc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfTemporalAcceleratorScript extends BaseShipSystemScript {

	private CombatEngineAPI engine = Global.getCombatEngine();

	public static final Color JITTER_COLOR = new Color(90,165,255,55);
	public static final Color JITTER_UNDER_COLOR = new Color(90,165,255,155);

	private boolean appliedBoost = false;
	protected static float DURATION = 5;
	protected static float DURATION_PER_STACK = 3f;
	public static float TIMEFLOW_MULT = 2f;
	public static float TIMEFLOW_PER_STACK = 0.5f;
	public static float TIMEFLOW_RAMP = 0.5f;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (engine.isPaused() || stats.getEntity() == null) return;
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) {
			return;
		}

		// apply boost the first frame of chargeup, don't run again until the next time the system is used
		if (effectLevel > 0 && !appliedBoost) {
			appliedBoost = true;
			SotfMisc.ewarChargeVisuals(ship);
			// if buff is active, add a stack
			if (ship.hasListenerOfClass(SotfTemporalAccelerationListener.class)) {
				SotfTemporalAccelerationListener listener = ship.getListeners(SotfTemporalAccelerationListener.class).get(0);
				listener.applyCharge();
			} else {
				// otherwise, add the buff
				ship.addListener(new SotfTemporalAccelerationListener(ship));
				Global.getSoundPlayer().playSound("system_temporalshell", 0.85f, 0.75f, ship.getLocation(), ship.getVelocity());
			}
		}

		// while inactive, set var so we can apply the buff next time we
		if (effectLevel <= 0) {
			appliedBoost = false;
		}
	}

	public static class SotfTemporalAccelerationListener implements AdvanceableListener {
		protected ShipAPI ship;
		public int extraStacks = 0;
		public float effectLevel = 0f;
		public float duration = DURATION; // how long the buff is charging up or at full strength
		protected float reloadProgress = 0f;
		public SotfTemporalAccelerationListener(ShipAPI ship) {
			this.ship = ship;
			this.reloadProgress = ship.getSystem().getAmmoReloadProgress();
		}

		public void advance(float amount) {
			boolean player = ship == Global.getCombatEngine().getPlayerShip();

			ship.setJitter(this, JITTER_COLOR, effectLevel, 2 + extraStacks, 0, 10f + (2f * extraStacks) * effectLevel);
			ship.setJitterUnder(this, JITTER_UNDER_COLOR, effectLevel, 25, 0f, 17f + (2f * extraStacks) * effectLevel);

			// freeze the reload progress so it can't regain charges during usage
			ship.getSystem().setAmmoReloadProgress(reloadProgress);
			// no 0-flux boost while active, has to be done this way bcs the buff isn't tied to the system being active
			ship.getFluxTracker().increaseFlux(amount, false);

			duration -= amount;
			if (duration < 0f) {
				duration = 0f;
			}
			if (duration <= 0f) {
				// start ramping down the effect
				effectLevel -= amount / TIMEFLOW_RAMP;
				if (effectLevel <= 0f) {
					ship.getMutableStats().getTimeMult().unmodify("sotf_temporalaccelerator");
					Global.getCombatEngine().getTimeMult().unmodify("sotf_temporalaccelerator");
					ship.removeListener(this);
					effectLevel = 0f;
					return;
				}
			} else {
				effectLevel += amount / TIMEFLOW_RAMP;
			}
			if (effectLevel > 1f) {
				effectLevel = 1f;
			}
			float shipTimeMult = 1f + (TIMEFLOW_MULT + (TIMEFLOW_PER_STACK * extraStacks) - 1f) * effectLevel;
			float perceptionMult = shipTimeMult;
			// perception mult isn't as strong, so player perceives their ship as faster
			if (player) {
				perceptionMult = 1f + ((TIMEFLOW_MULT - 1f) * 0.65f) * effectLevel;
			}
			ship.getMutableStats().getTimeMult().modifyMult("sotf_temporalaccelerator", shipTimeMult);
			if (player) {
				Global.getCombatEngine().getTimeMult().modifyMult("sotf_temporalaccelerator", 1f / perceptionMult);
				Global.getCombatEngine().maintainStatusForPlayerShip("sotf_temporalaccelerator", null, "Temporal Accelerator", "Timeflow increased to " + Math.round(shipTimeMult * 100f) + "%", false);
			}
		}

//		private boolean completelyInactive() {
//			return duration <= 0f && effectLevel <= 0f;
//		}

		// adds an additional stack of the buff and plays the sound effect
		private void applyCharge() {
			duration += DURATION_PER_STACK;
			extraStacks += 1;
			Global.getSoundPlayer().playSound("system_temporalshell", 0.85f + (0.08f * extraStacks), 0.75f + (0.05f * extraStacks), ship.getLocation(), ship.getVelocity());
		}
	}
}








