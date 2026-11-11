package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.shipsystems.SotfTemporalAcceleratorScript;
import data.shipsystems.SotfTemporalAcceleratorScript.SotfTemporalAccelerationListener;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import static com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;

/**
 *	AI for Temporal Accelerator system
 */

public class SotfTemporalAcceleratorAI implements ShipSystemAIScript{

    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private float desire = 0f; // slowly rising desire to use system: faster depending on circumstance
    private IntervalUtil timer = new IntervalUtil(0.25f,0.35f);
    private boolean stopUsingChargesThisChain = false;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine){
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target){
        if (engine.isPaused()){
            return;
        }

        if (!AIUtils.canUseSystemThisFrame(ship)) return;

        boolean chaining = ship.hasListenerOfClass(SotfTemporalAccelerationListener.class);
        if (!chaining) {
            stopUsingChargesThisChain = false;
        } else if (stopUsingChargesThisChain) {
            return;
        }
        // don't chain charges if we're not in combat
        if (chaining && AIUtils.getNearbyEnemies(ship, 2000f).isEmpty()) {
            return;
        }
        if (chaining) {
            SotfTemporalAccelerationListener listener = ship.getListeners(SotfTemporalAccelerationListener.class).get(0);
            if (listener.duration < 3f) {
                stopUsingChargesThisChain = true;
            }
            // burn all charges when in danger
            else if (ship.getFluxLevel() > 0.85f && flags.hasFlag(AIFlags.HAS_INCOMING_DAMAGE)) {
                ship.useSystem();
            } else if (flags.hasFlag(AIFlags.PURSUING) && ship.getShipTarget() != null) {
                // if the target's far away, don't burn all charges
                if (Misc.getDistance(ship.getLocation(), ship.getShipTarget().getLocation()) > 1800f) return;
                // don't use all charges when they're probably dead anyway
                if (ship.getShipTarget().getHullLevel() < 0.25f && ship.getShipTarget().getFluxLevel() > 0.95f) {
                    stopUsingChargesThisChain = true;
                }
                // do burn charges if they're under pressure and we've an opening
                else if (ship.getShipTarget().getFluxLevel() > 0.65f) {
                    ship.useSystem();
                }
            }
        } else {
            timer.advance(amount);
            if (timer.intervalElapsed()) {
                // if we're at max ammo, spend a charge
                if (system.getAmmo() == system.getMaxAmmo()) {
                    ship.useSystem();
                    return;
                }

                if (system.getAmmo() > 1) {
                    if (flags.hasFlag(AIFlags.PURSUING)) {
                        desire += 2f;
                    }
                    if (flags.hasFlag(AIFlags.HARASS_MOVE_IN)) {
                        desire += 1f;
                    }
                    if (flags.hasFlag(AIFlags.BACKING_OFF)) {
                        desire += 2f;
                    }
                    if (flags.hasFlag(AIFlags.DO_NOT_PURSUE)) {
                        desire -= 0.5f;
                    }

                    if (flags.hasFlag(AIFlags.NEEDS_HELP) || flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)) {
                        desire += 4f;
                    }
                    if (flags.hasFlag(AIFlags.HAS_INCOMING_DAMAGE)) {
                        desire += 1.5f;
                    }
                } else {
                    // burn the last charge if it might let us overwhelm an enemy
                    if (ship.getShipTarget() != null) {
                        if (Misc.getDistance(ship.getLocation(), ship.getShipTarget().getLocation()) < 2000f && ship.getShipTarget().getFluxLevel() > 0.9f) {
                            desire += 8f;
                        }
                    }
                }

                if (ship.isRetreating()) {
                    desire += 4f;
                }

                if (desire >= MathUtils.getRandomNumberInRange(5f, 6f)) {
                    ship.useSystem();
                    desire = 0f;
                }
            }
        }
    }
}