// Hidecracker on-hit effect(s)
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.scripts.SotfModPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfHidecrackerOnHitEffect implements OnHitEffectPlugin, OnFireEffectPlugin {

    public static float PERCENT_MAX_FLUX = 0.2f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        // BLAM! Vanilla impact SFX don't quite cut it
        Global.getSoundPlayer().playSound("sotf_impact_heavy", 1f, 1f, point, target.getVelocity());

        //GraphicsLib distortion effect on impact
        if (SotfModPlugin.GLIB) {
            RippleDistortion ripple = new RippleDistortion(projectile.getLocation(), target.getVelocity());
            ripple.setIntensity(200f);
            ripple.setSize(500f);
            ripple.fadeInSize(0.25f);
            ripple.fadeOutIntensity(0.5f);
            DistortionShader.addDistortion(ripple);
        }

        if (target instanceof ShipAPI) {
            ShipAPI targetShip = (ShipAPI) target;

            //int arcCount = 4 + ((targetShip.getHullSize().ordinal() - 1) * 2);
            //int arcCount = 4;

            // nice shields, nerd
            //for (int i = 0; i < arcCount; i++) {
//                Global.getCombatEngine().spawnEmpArcPierceShields(projectile.getSource(),
//                        point,
//                        targetShip, targetShip,
//                        DamageType.ENERGY,
//                        60f,
//                        350f,
//                        100000f,
//                        "tachyon_lance_emp_impact",
//                        15f,
//                        new Color(120,200,255),
//                        Color.white
//                );
            //}

            // % max flux damage
            targetShip.getFluxTracker().increaseFlux(targetShip.getMaxFlux() * PERCENT_MAX_FLUX, true);
            if (shieldHit) {
                if (targetShip.isFrigate()) {
                    targetShip.getFluxTracker().beginOverloadWithTotalBaseDuration(4f);
                } else if (targetShip.isDestroyer()) {
                    targetShip.getFluxTracker().beginOverloadWithTotalBaseDuration(1f);
                }
            }
        }
    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        // the projectile glows as it flies
        if (SotfModPlugin.GLIB) {
            StandardLight light = new StandardLight(projectile.getLocation(),
                    new Vector2f(0f,0f),
                    new Vector2f(0f,0f),
                    projectile,
                    1f,
                    200f);
            light.setColor(new Color(120,200,255));
            light.setLifetime(8f);
            LightShader.addLight(light);
        }
    }

}