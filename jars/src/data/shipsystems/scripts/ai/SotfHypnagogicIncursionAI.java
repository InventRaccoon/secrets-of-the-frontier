package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import static com.fs.starfarer.api.combat.ShipwideAIFlags.*;

/**
 *	AI for Thorn's Hypagogic Incursion dash
 */

public class SotfHypnagogicIncursionAI implements ShipSystemAIScript{

    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private float desire = 0f; // slowly rising desire to use system: faster depending on circumstance
    private IntervalUtil timer = new IntervalUtil(0.25f,0.35f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine){
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target){
        if(engine.isPaused()){
            return;
        }

        timer.advance(amount);
        if (timer.intervalElapsed()) {
            if (!AIUtils.canUseSystemThisFrame(ship)) return;

            if (flags.hasFlag(AIFlags.PURSUING)) {
                desire += 1.25f;
            }
            if (flags.hasFlag(AIFlags.HARASS_MOVE_IN)) {
                desire += 1.5f;
            }
            if (flags.hasFlag(AIFlags.BACKING_OFF)) {
                desire += 0.5f;
            }
            if (flags.hasFlag(AIFlags.DO_NOT_PURSUE)) {
                desire -= 0.5f;
            }

            if (flags.hasFlag(AIFlags.RUN_QUICKLY)) {
                desire += 0.75f;
                if (system.getAmmo() > 1) {
                    desire += 0.5f;
                }
            }
            if (flags.hasFlag(AIFlags.TURN_QUICKLY)) {
                desire += 0.25f;
            }
            if (flags.hasFlag(AIFlags.NEEDS_HELP) || flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)) {
                desire += 3f;
            }
            if (flags.hasFlag(AIFlags.HAS_INCOMING_DAMAGE)) {
                desire += 1.5f;
            }

            if (ship.isRetreating()) {
                desire += 4f;
            }

            if (desire >= MathUtils.getRandomNumberInRange(3f,4f)){
                ship.useSystem();
                desire = 0f;
            }
        }
    }
}