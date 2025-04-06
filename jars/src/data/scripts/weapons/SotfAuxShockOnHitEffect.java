package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 *	On-hit effect for the explosion from the Shock Package reactor detonation
 */

public class SotfAuxShockOnHitEffect implements OnHitEffectPlugin {

    // onhit effects trigger once per frame of the explosion
    // so we need to make sure it doesn't trigger multiple times on one ship
    protected List<CombatEntityAPI> targetsHit = new ArrayList<CombatEntityAPI>();

    public static float PERCENT_MAX_FLUX = 0.3f;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI) {
            if (targetsHit.contains(target)) return;
            targetsHit.add(target);
            ShipAPI targetShip = (ShipAPI) target;

            //int arcCount = 4 + ((targetShip.getHullSize().ordinal() - 1) * 2);

            // nice shields, nerd
            //for (int i = 0; i < arcCount; i++) {
                //Global.getCombatEngine().spawnEmpArcPierceShields(null,
                //        point,
                //        target, target,
                //        DamageType.ENERGY,
                //        50f,
                //        800f,
                //        100000f,
                //        "tachyon_lance_emp_impact",
                //        15f,
                //        new Color(120,200,255),
                //        Color.white
                //);
            //}

            // % max flux damage
            targetShip.getFluxTracker().increaseFlux(targetShip.getMaxFlux() * PERCENT_MAX_FLUX * 0.5f, false);
            targetShip.getFluxTracker().increaseFlux(targetShip.getMaxFlux() * PERCENT_MAX_FLUX * 0.5f, true);
        }
    }
}