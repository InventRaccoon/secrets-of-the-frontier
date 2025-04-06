package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 *	On-hit effect for the explosion from Hands of the Drowned's gravitic mines
 */

public class SotfGraviticMineOnHitEffect implements OnHitEffectPlugin {

    // onhit effects trigger once per frame of the explosion
    // so we need to make sure it doesn't trigger multiple times on one ship
    protected List<CombatEntityAPI> targetsHit = new ArrayList<CombatEntityAPI>();

    public static float PERCENT_MAX_FLUX = 0.1f;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (targetsHit.contains(target)) return;
        targetsHit.add(target);
        float mass = target.getMass();

        if (target instanceof ShipAPI) {
            ShipAPI targetShip = (ShipAPI) target;
            mass = targetShip.getMassWithModules();

            // % max flux damage
            targetShip.getFluxTracker().increaseFlux(targetShip.getMaxFlux() * PERCENT_MAX_FLUX, false);
            targetsHit.add(targetShip);
        }

        if (SotfModPlugin.GLIB) {
            RippleDistortion ripple = new RippleDistortion(projectile.getLocation(), target.getVelocity());
            ripple.setIntensity(100f);
            ripple.setSize(300f);
            ripple.fadeInSize(0.25f);
            ripple.fadeOutIntensity(0.5f);
            ripple.flip(true);
            DistortionShader.addDistortion(ripple);
        }

        CombatUtils.applyForce(target, Misc.getAngleInDegrees(target.getLocation(), point), mass);
    }
}