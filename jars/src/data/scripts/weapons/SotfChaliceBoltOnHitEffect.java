package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;



public class SotfChaliceBoltOnHitEffect implements OnHitEffectPlugin {

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI targetShip) {
            float emp = projectile.getEmpAmount();
            float empDamageMult = targetShip.getMutableStats().getEmpDamageTakenMult().getModifiedValue();
            if (empDamageMult < 0f) empDamageMult = 0f;
            // Dweller has no direct EMP resistance but their weapons/engines can't take damage - count it as 100% resist
            if (targetShip.getHullSpec().hasTag(Tags.DWELLER)) empDamageMult = 0f;
            if (empDamageMult < 1f) {
                engine.applyDamage(targetShip, point, emp - (emp * empDamageMult), DamageType.ENERGY,
                        0f, false, false, projectile.getSource(), false);
            }
        }
    }
}