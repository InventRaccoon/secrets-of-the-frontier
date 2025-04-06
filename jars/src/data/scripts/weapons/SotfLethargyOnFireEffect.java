// Lethargy onfire, fake beam weapon
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.ShockRepeaterOnFireEffect;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;
import org.magiclib.util.MagicFakeBeam;

import java.awt.*;
import java.util.List;

import static org.magiclib.util.MagicFakeBeam.getCollisionPointOnCircumference;
import static org.magiclib.util.MagicFakeBeam.getShipCollisionPoint;

public class SotfLethargyOnFireEffect implements OnFireEffectPlugin {

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float disperseMult = (float) Math.random();
        float newMult = disperseMult;
        if (Math.random() > 0.5f) {
            newMult *= -1f;
        }
        float spread = (weapon.getCurrSpread() / 2) * newMult;
        lethargyFakeBeam(
                engine,
                weapon.getFirePoint(0),
                weapon.getRange() + 10f - (10f * disperseMult),
                weapon.getCurrAngle() + spread,
                6,
                0.1f,
                0.1f,
                8,
                Color.WHITE,
                weapon.getSpec().getGlowColor(),
                projectile.getDamageAmount(),
                weapon.getDamageType(),
                projectile.getEmpAmount(),
                weapon.getShip()
        );
    }

    /**
     * Code taken from MagicLib, by Tartiflette and Deathfly
     * Almost entirely copypasted, but I need to add an onhit effect!
     */
    public static void lethargyFakeBeam(CombatEngineAPI engine, Vector2f from, float range, float angle, float width, float full, float fading, float impactSize, Color core, Color fringe, float normalDamage, DamageType type, float emp, ShipAPI source) {

        CombatEntityAPI theTarget = null;
        float damage = normalDamage;

        //default end point
        Vector2f end = MathUtils.getPoint(from, range, angle);

        //list all nearby entities that could be hit
        List<CombatEntityAPI> entity = CombatUtils.getEntitiesWithinRange(from, range + 500);
        if (!entity.isEmpty()) {
            for (CombatEntityAPI e : entity) {

                //ignore un-hittable stuff like phased ships
                if (e.getCollisionClass() == CollisionClass.NONE) {
                    continue;
                }

                //damage can be reduced against some modded ships
                float newDamage = normalDamage;

                Vector2f col = new Vector2f(1000000, 1000000);
                //ignore everything but ships...
                if (e instanceof ShipAPI) {
                    if (
                            e != source
                                    &&
                                    ((ShipAPI) e).getParentStation() != e
                                    &&
                                    e.getCollisionClass() != CollisionClass.NONE
                                    &&
                                    !(e.getCollisionClass() == CollisionClass.FIGHTER && e.getOwner() == source.getOwner() && !((ShipAPI) e).getEngineController().isFlamedOut())
                                    &&
                                    CollisionUtils.getCollides(from, end, e.getLocation(), e.getCollisionRadius())
                    ) {

                        //check for a shield impact, then hull and take the closest one
                        ShipAPI s = (ShipAPI) e;

                        //find the collision point with shields/hull
                        Vector2f hitPoint = getShipCollisionPoint(from, end, s, angle);
                        if (hitPoint != null) {
                            col = hitPoint;
                        }

                        //check for modded ships with damage reduction
                        if (s.getHullSpec().getBaseHullId().startsWith("exigency_")) {
                            newDamage = normalDamage / 2;
                        }
                    }
                } else
                    //...and asteroids!
                    if (
                            (e instanceof CombatAsteroidAPI
                                    ||
                                    (e instanceof MissileAPI)
                                            &&
                                            e.getOwner() != source.getOwner()
                            )
                                    &&
                                    CollisionUtils.getCollides(from, end, e.getLocation(), e.getCollisionRadius())
                    ) {
                        Vector2f cAst = getCollisionPointOnCircumference(from, end, e.getLocation(), e.getCollisionRadius());
                        if (cAst != null) {
                            col = cAst;
                        }
                    }

                //if there was an impact and it is closer than the curent beam end point, set it as the new end point and store the target to apply damage later damage
                if (
                        col.x != 1000000 &&
                                MathUtils.getDistanceSquared(from, col) < MathUtils.getDistanceSquared(from, end)) {
                    end = col;
                    theTarget = e;
                    damage = newDamage;
                }
            }

            //if the beam impacted something, apply the damage
            if (theTarget != null) {

                // RACCOON: SPLIT DAMAGE WOO
                engine.applyDamage(
                        theTarget,
                        end,
                        damage / 2,
                        type,
                        emp,
                        false,
                        false,
                        source
                );
                engine.applyDamage(
                        theTarget,
                        end,
                        damage / 2,
                        type,
                        0f,
                        false,
                        true,
                        source
                );
                //impact flash
                engine.addHitParticle(
                        end,
                        new Vector2f(),
                        (float) Math.random() * impactSize / 2 + impactSize,
                        1,
                        full + fading,
                        fringe
                );
                engine.addHitParticle(
                        end,
                        new Vector2f(),
                        (float) Math.random() * impactSize / 4 + impactSize / 2,
                        1,
                        full,
                        core
                );

                // RACCOON'S NOTE: this is my addition. Slow down the hit target (lethargically!)

                float slowdown = 0.1f;
                if (theTarget instanceof MissileAPI) {
                    slowdown = 0.25f;
                } else if (theTarget instanceof ShipAPI) {
                    ShipAPI targetShip = (ShipAPI) theTarget;

                    if (targetShip.isFighter()) {
                        slowdown = 0.15f;
                    }
                }
                theTarget.getVelocity().scale(1f - slowdown);
            }

            //Add the beam to the plugin
            //public static void addBeam(float duration, float fading, float width, Vector2f from, float angle, float length, Color core, Color fringe)
            MagicFakeBeamPlugin.addBeam(full, fading, width, from, angle, MathUtils.getDistance(from, end) + 10, core, fringe);
        }
    }

}
