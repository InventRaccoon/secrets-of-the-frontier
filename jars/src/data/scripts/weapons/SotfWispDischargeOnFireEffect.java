// this is just the Shock Repeater on-fire but also adding +1 flux (so that Wisps burn out faster in combat, but don't conserve fire)
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.ShockRepeaterOnFireEffect;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfWispDischargeOnFireEffect extends ShockRepeaterOnFireEffect {

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        //ARC = 30f;
        float emp = projectile.getEmpAmount();
        float dam = projectile.getDamageAmount();

        CombatEntityAPI target = findTarget(projectile, weapon, engine);
        float thickness = 20f;
        float coreWidthMult = 0.67f;
        Color color = weapon.getSpec().getGlowColor();
        if (target != null) {
            // +1 flux on firing. Done this way so the AI thinks the weapon is flux-free
            if (projectile.getSource() != null) {
                projectile.getSource().getFluxTracker().setCurrFlux(projectile.getSource().getFluxTracker().getCurrFlux() + 1);
            }
            EmpArcEntityAPI arc = engine.spawnEmpArc(projectile.getSource(), projectile.getLocation(), weapon.getShip(),
                    target,
                    DamageType.ENERGY,
                    dam,
                    emp, // emp
                    100000f, // max range
                    "shock_repeater_emp_impact",
                    thickness, // thickness
                    color,
                    new Color(255,255,255,255)
            );
            arc.setCoreWidthOverride(thickness * coreWidthMult);
            arc.setSingleFlickerMode();
        } else {
            Vector2f from = new Vector2f(projectile.getLocation());
            Vector2f to = pickNoTargetDest(projectile, weapon, engine);
            EmpArcEntityAPI arc = engine.spawnEmpArcVisual(from, weapon.getShip(), to, weapon.getShip(), thickness, color, Color.white);
            arc.setCoreWidthOverride(thickness * coreWidthMult);
            arc.setSingleFlickerMode();
            //Global.getSoundPlayer().playSound("shock_repeater_emp_impact", 1f, 1f, to, new Vector2f());
        }
    }

}
