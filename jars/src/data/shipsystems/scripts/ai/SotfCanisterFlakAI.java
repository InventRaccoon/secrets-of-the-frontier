package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 *	UNUSED AS OF STARSECTOR 0.96a
 *	AI for SotF's verison of Canister Flak that allowed it to be used as a right-click system
 *  This functionality is now supported by vanilla, so this isn't used
 */

public class SotfCanisterFlakAI implements ShipSystemAIScript{

    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private IntervalUtil timer = new IntervalUtil(0.25f,0.35f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine){
        this.ship = ship;
        this.system = system;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target){
        if(engine.isPaused()){
            return;
        }

        timer.advance(amount);
        if (timer.intervalElapsed()) {
            // spam right click if there's missiles or ships to asplode
            if(!system.isActive() && canUseSystemThisFrame(ship) && (!AIUtils.getNearbyEnemyMissiles(ship, 600).isEmpty() || !AIUtils.getNearbyEnemies(ship, 500).isEmpty())){
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            }
        }
    }

    // copypasted from LazyLib but for right-click system
    public static boolean canUseSystemThisFrame(ShipAPI ship)
    {
        FluxTrackerAPI flux = ship.getFluxTracker();
        ShipSystemAPI system = ship.getPhaseCloak();

        // No system, overloading/venting, out of ammo
        return !(system == null || flux.isOverloadedOrVenting() || system.isOutOfAmmo()
                // In use but can't be toggled off right away
                || (system.isOn() && system.getCooldownRemaining() > 0f)
                // In chargedown, in cooldown
                || (system.isActive() && !system.isOn()) || system.getCooldownRemaining() > 0f
                // Not enough flux
                || system.getFluxPerUse() > (flux.getMaxFlux() - flux.getCurrFlux()));
    }
}