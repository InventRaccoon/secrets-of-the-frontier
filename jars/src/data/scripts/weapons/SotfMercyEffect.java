package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.CryoblasterEffect;
import com.fs.starfarer.api.impl.combat.GravitonBeamEffect;
import com.fs.starfarer.api.util.TimeoutTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class SotfMercyEffect implements EveryFrameWeaponEffectPlugin, DamageDealtModifier {

    private static String BOLT_WEAPON_ID = "sotf_mercy_boltlauncher";
    //public static float HIT_STRENGTH_BONUS = 40f;
    public static float HIT_STRENGTH_BONUS = 0f;
    private static String ARMOR_REDUCTION_MOD_ID = "sotf_mercy_shredded";
    public static float ARMOR_REDUCTION = 50f;
    public static float SHRED_BASE = 40f;
    public static float SHRED_PER_WEAPON = 20f;
    public static float SHRED_DURATION = 0.75f;
    public static float SHIELD_DAMAGE_MULT = 2.5f;

    public static float SPREAD_DEGREES = 2f;
    protected boolean readyToFire = false;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        weapon.getShip().getMutableStats().getHitStrengthBonus().modifyFlat(weapon.getSpec().getWeaponId() + weapon.getSlot().getId(), HIT_STRENGTH_BONUS);

        ShipAPI ship = weapon.getShip();
        if (ship == null) return;
        if (!ship.isAlive()) return;

        if (!ship.hasListenerOfClass(SotfMercyEffect.class)) {
            ship.addListener(this);
        }

        if (weapon.getChargeLevel() == 0f) {
            readyToFire = true;
        } else if (weapon.getChargeLevel() == 1f && readyToFire) {
            Global.getSoundPlayer().playSound("antimatter_blaster_fire", 1f, 1f, weapon.getLocation(), ship.getVelocity());
            for (int i = 0; i <= 12; i++) {
                float disperseMult = (float) Math.random();
                float newMult = disperseMult;
                if (Math.random() > 0.5f) {
                    newMult *= -1f;
                }
                float spread = (SPREAD_DEGREES / 2) * newMult;

                int barrelIndex = i;
                while (barrelIndex > 3) {
                    barrelIndex -= 4;
                }

                Vector2f randomVel = MathUtils.getRandomPointOnCircumference(null, MathUtils.getRandomNumberInRange(20f, 45f));
                randomVel.x += ship.getVelocity().x;
                randomVel.y += ship.getVelocity().y;

                CombatEntityAPI entity = engine.spawnProjectile(ship, weapon, BOLT_WEAPON_ID,
                        weapon.getFirePoint(barrelIndex), weapon.getCurrAngle() + spread, randomVel);
                DamagingProjectileAPI projectile = (DamagingProjectileAPI) entity;

                engine.spawnMuzzleFlashOrSmoke(ship, weapon.getFirePoint(barrelIndex),
                        Global.getSettings().getWeaponSpec(BOLT_WEAPON_ID), weapon.getCurrAngle() + spread);
            }
            readyToFire = false;
        }
    }

    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof BeamAPI beam && target instanceof ShipAPI targetShip) {
            if (shieldHit && beam.getWeapon() != null && beam.getWeapon().getSpec().getWeaponId().equals("sotf_mercy")) {
                damage.getModifier().modifyMult("sotf_mercy_damage_mod", SHIELD_DAMAGE_MULT / targetShip.getShield().getFluxPerPointOfDamage());
                return "sotf_mercy_damage_mod";
            } else if (!shieldHit && beam.getWeapon() != null && beam.getWeapon().getSpec().getWeaponId().equals("sotf_mercy")) {
                if (!targetShip.hasListenerOfClass(SotfMercyDamageTakenMod.class)) {
                    targetShip.addListener(new SotfMercyDamageTakenMod(targetShip));
                }
                List<SotfMercyDamageTakenMod> listeners = targetShip.getListeners(SotfMercyDamageTakenMod.class);
                if (listeners.isEmpty()) return null; // ???

                SotfMercyDamageTakenMod listener = listeners.get(0);
                listener.notifyHit(beam.getWeapon());
                return null;
            }
        }
        // shotgun bolt EMP to damage conversion
        // would do as an on-hit extra damage instance but it would interact weirdly with armor strength
        if (!shieldHit && param instanceof DamagingProjectileAPI proj && target instanceof ShipAPI targetShip) {
            if (proj.getWeapon() != null && proj.getWeapon().getSpec().getWeaponId().equals("sotf_mercy_boltlauncher") && proj.getEmpAmount() > 0) {
                float empDamageMult = targetShip.getMutableStats().getEmpDamageTakenMult().getModifiedValue();
                if (empDamageMult < 0f) empDamageMult = 0f;
                // Dweller has no direct EMP resistance but their weapons/engines can't take damage - count it as 100% resist
                if (targetShip.getHullSpec().hasTag(Tags.DWELLER)) empDamageMult = 0f;
                float empProportion = proj.getEmpAmount() / proj.getDamageAmount();
                damage.getModifier().modifyMult("sotf_daydream_empconvert", 1f + (empProportion * (1f - empDamageMult)));
                return "sotf_daydream_empconvert_mod";
            }
        }
        return null;
    }

    public static class SotfMercyDamageTakenMod implements AdvanceableListener {
        protected TimeoutTracker<WeaponAPI> recentHits = new TimeoutTracker<WeaponAPI>();
        protected ShipAPI ship;

        public SotfMercyDamageTakenMod(ShipAPI ship) {
            this.ship = ship;
        }

        public void notifyHit(WeaponAPI w) {
            recentHits.add(w, SHRED_DURATION, SHRED_DURATION);
        }

        public void advance(float amount) {
            recentHits.advance(amount);

            int beams = recentHits.getItems().size();

            float bonus = (beams * SHRED_PER_WEAPON) + (SHRED_BASE - SHRED_PER_WEAPON);

            if (bonus > 0) {
                ship.getMutableStats().getEffectiveArmorBonus().modifyFlat(ARMOR_REDUCTION_MOD_ID, bonus * -1f);
            } else {
                ship.removeListener(this);
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(ARMOR_REDUCTION_MOD_ID);
            }
        }
    }
}
