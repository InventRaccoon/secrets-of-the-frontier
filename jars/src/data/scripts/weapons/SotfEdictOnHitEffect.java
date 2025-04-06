// just a simple on-hit effect for the Edict Stormblaster. Arcs that deal scaling damage and EMP.
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.SharedSettings;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.achievements.SharedCombatDamageListener;

import java.awt.*;

public class SotfEdictOnHitEffect implements OnHitEffectPlugin {

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI) {
            ShipAPI target_ship = (ShipAPI) target;
            // vs ghosts, on-hit works through shields, and hull hits deal double damage
            boolean ghostBusting = target_ship.getVariant().hasHullMod("sotf_phantasmalship");
            // also strong vs demons
            if (target_ship.getHullSpec().hasTag(Tags.DWELLER)) {
                ghostBusting = true;
            }
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