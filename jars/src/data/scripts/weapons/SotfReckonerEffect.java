// UNUSED: Reckoner alt version that was homing bolt shotgun
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import static data.scripts.weapons.SotfLethargyOnFireEffect.lethargyFakeBeam;

public class SotfReckonerEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin, OnHitEffectPlugin {

    public static float ARC = 20f;
    public static float SIDE_OFFSET = 5f;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
//        if (weapon.getAmmo() < weapon.getSpec().getBurstSize()) {
//            weapon.setForceDisabled(true);
//        } else {
//            weapon.setForceDisabled(false);
//        }
    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.addPlugin(new SotfReckonerBoltGuidanceScript(projectile, null));
    }

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI targetShip) {
            float emp = projectile.getEmpAmount();
            float empDamageMult = targetShip.getMutableStats().getEmpDamageTakenMult().getModifiedValue();
            if (empDamageMult < 0f) empDamageMult = 0f;
            if (empDamageMult < 1f) {
                engine.applyDamage(targetShip, point, emp - (emp * empDamageMult), DamageType.ENERGY,
                        0f, false, false, projectile.getSource(), false);
            }
        }
    }
}
