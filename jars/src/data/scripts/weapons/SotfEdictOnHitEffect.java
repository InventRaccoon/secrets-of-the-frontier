// just a simple on-hit effect for the Edict Stormblaster. Arcs that deal scaling damage and EMP.
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfEdictOnHitEffect implements OnHitEffectPlugin {

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI) {
            ShipAPI target_ship = (ShipAPI) target;
            // vs ghosts, on-hit works through shields, and hull hits deal double damage
            boolean ghostBusting = target_ship.getVariant().hasHullMod("sotf_phantasmalship");
            if (shieldHit && !ghostBusting) {
                return;
            }
            float emp = projectile.getEmpAmount();
            float dam = projectile.getDamageAmount() * 0.4f;
            // strong against ghosts
            if (ghostBusting && !shieldHit) {
                dam = dam * 2f;
            }

            engine.spawnEmpArc(projectile.getSource(), point, target, target,
                    DamageType.ENERGY,
                    dam,
                    emp, // emp
                    600f, // max range
                    "tachyon_lance_emp_impact",
                    15f, // thickness
                    new Color(155,75,200,200),
                    new Color(255,255,255,255)
            );
        }
    }
}