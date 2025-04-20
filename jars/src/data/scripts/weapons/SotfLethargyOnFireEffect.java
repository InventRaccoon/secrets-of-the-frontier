// Lethargy onfire, fake beam weapon
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Pair;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;

import java.awt.*;
import java.util.List;

import static org.magiclib.util.MagicFakeBeam.getCollisionPointOnCircumference;

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
    public static void lethargyFakeBeam(CombatEngineAPI engine, Vector2f from, float range, float angle, float width,
                                        float full, float fading, float impactSize, Color core, Color fringe,
                                        float normalDamage, DamageType type, float emp, float dampMult, float dampVsShields, ShipAPI source) {

        CombatEntityAPI theTarget = null;
        float damage = normalDamage;
        boolean shieldHit = false;

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
                        Pair<Vector2f, Boolean> hitPair = getShipCollisionPoint(from, end, s, angle);
                        if (hitPair.one != null) {
                            col = (Vector2f) hitPair.one;
                            shieldHit = hitPair.two;
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
                if (!shieldHit) {
                    slowdown *= dampVsShields;
                }
                theTarget.getVelocity().scale(1f - (slowdown * dampMult));
            }

            //Add the beam to the plugin
            //public static void addBeam(float duration, float fading, float width, Vector2f from, float angle, float length, Color core, Color fringe)
            MagicFakeBeamPlugin.addBeam(full, fading, width, from, angle, MathUtils.getDistance(from, end) + 10, core, fringe);
        }
    }

    public static void lethargyFakeBeam(CombatEngineAPI engine, Vector2f from, float range, float angle, float width, float full, float fading, float impactSize, Color core, Color fringe, float normalDamage, DamageType type, float emp, ShipAPI source) {
        lethargyFakeBeam(engine, from, range, angle, width, full, fading, impactSize, core, fringe, normalDamage, type, emp, 1f, 0.5f, source);
    }

    // return the collision point of segment segStart to segEnd and a ship (will consider shield).
    // if segment can not hit the ship, will return null.
    // if segStart hit the ship, will return segStart.
    // if segStart hit the shield, will return segStart.
    // RACCOON: returns as a pair, the point hit and whether it was a shield impact
    public static Pair<Vector2f, Boolean> getShipCollisionPoint(Vector2f segStart, Vector2f segEnd, ShipAPI ship, float aim) {

        // if target can not be hit, return null
        if (ship.getCollisionClass() == CollisionClass.NONE) {
            return new Pair<>(null, false);
        }
        ShieldAPI shield = ship.getShield();

        // Check hit point when shield is off.
        if (shield == null || shield.isOff()) {
            return new Pair<>(CollisionUtils.getCollisionPoint(segStart, segEnd, ship), false);
        } // If ship's shield is on, thing goes complicated...
        else {

            Vector2f circleCenter = shield.getLocation();
            float circleRadius = shield.getRadius();
            //the beam already start within the shield radius:
            if (MathUtils.isPointWithinCircle(segStart, circleCenter, circleRadius)) {
                if (shield.isWithinArc(segStart)) {
                    return new Pair<>(MathUtils.getPoint(segStart, 15, aim), true);
                } else {
                    return new Pair<>(CollisionUtils.getCollisionPoint(segStart, segEnd, ship), false);
                }
            } else //the beam start from outside:
            {
                Vector2f tmp1 = getCollisionPointOnCircumference(segStart, segEnd, circleCenter, circleRadius);
                if (tmp1 != null && shield.isWithinArc(tmp1)) {
                    return new Pair<>(MathUtils.getPoint(tmp1, 1, aim), true);
                } else {
                    return new Pair<>(MathUtils.getPoint(
                            CollisionUtils.getCollisionPoint(segStart, segEnd, ship),
                            1,
                            aim
                    ), false);
                }
            }
        }
    }

}
