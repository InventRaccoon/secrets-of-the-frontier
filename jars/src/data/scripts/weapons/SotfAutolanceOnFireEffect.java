// Reckoner onfire, fake beam weapon
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.combat.CombatEngine;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

import static data.scripts.weapons.SotfLethargyOnFireEffect.lethargyFakeBeam;
import static org.magiclib.util.MagicFakeBeam.getCollisionPointOnCircumference;

public class SotfAutolanceOnFireEffect implements OnFireEffectPlugin {

    public static float ARC = 25f;
    public static float SIDE_OFFSET = 5f;

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float disperseMult = (float) Math.random();
        float newMult = disperseMult;
        if (Math.random() > 0.5f) {
            newMult *= -1f;
        }
        float spread = (weapon.getCurrSpread() / 2) * newMult;
        Color color = weapon.getSpec().getGlowColor();
        lethargyFakeBeam(
                engine,
                weapon.getFirePoint(0),
                weapon.getRange() + 10f - (10f * disperseMult),
                weapon.getCurrAngle() + spread,
                8,
                0f,
                0.2f,
                8,
                Color.WHITE,
                color,
                projectile.getDamageAmount() * 0.4f,
                weapon.getDamageType(),
                projectile.getEmpAmount() * 0.4f,
                0.5f,
                0.5f,
                weapon.getShip()
        );
        Vector2f vel = new Vector2f();
        if (weapon.getShip() != null) {
            vel = weapon.getShip().getVelocity();
        }
        engine.addHitParticle(weapon.getFirePoint(0), vel, 10f, 1f, 0.2f, color);
        engine.addHitParticle(weapon.getFirePoint(1), vel, 5f, 1f, 0.15f, color);
        engine.addHitParticle(weapon.getFirePoint(2), vel, 5f, 1f, 0.15f, color);
        engine.addHitParticle(weapon.getFirePoint(3), vel, 5f, 1f, 0.15f, color);
        engine.addHitParticle(weapon.getFirePoint(4), vel, 5f, 1f, 0.15f, color);
        // front left
        fireBeamForBarrel(engine, weapon, projectile, 1, SIDE_OFFSET * 2f);
        // front right
        fireBeamForBarrel(engine, weapon, projectile, 2, -SIDE_OFFSET * 2f);
        // back left
        fireBeamForBarrel(engine, weapon, projectile, 3, SIDE_OFFSET);
        // back right
        fireBeamForBarrel(engine, weapon, projectile, 4, -SIDE_OFFSET);
    }

    public void fireBeamForBarrel(CombatEngineAPI engine, WeaponAPI weapon, DamagingProjectileAPI projectile, int index, float offset) {
        float disperseMult = (float) Math.random();
        float newMult = disperseMult;
        if (Math.random() > 0.5f) {
            newMult *= -1f;
        }
        float spread = (weapon.getCurrSpread() / 2) * newMult;
        CombatEntityAPI target = findTarget(weapon.getFirePoint(index), weapon, offset);
        Vector2f targetLoc = null;
        if (target != null) {
            targetLoc = target.getLocation();
        } else {
            targetLoc = pickNoTargetDest(weapon.getFirePoint(index), weapon, offset);
        }
        lethargyFakeBeam(
                engine,
                weapon.getFirePoint(index),
                weapon.getRange() + 10f - (10f * disperseMult),
                Misc.getAngleInDegrees(weapon.getFirePoint(index), targetLoc) + spread,
                4,
                0f,
                0.15f,
                4,
                Color.WHITE,
                weapon.getSpec().getGlowColor(),
                projectile.getDamageAmount() * 0.15f,
                weapon.getDamageType(),
                projectile.getEmpAmount() * 0.15f,
                0.5f,
                0.5f,
                weapon.getShip()
        );
    }

    public Vector2f pickNoTargetDest(Vector2f from, WeaponAPI weapon, float angleOffset) {
        float range = weapon.getRange();
        Vector2f dir = Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle() + angleOffset);
        dir.scale(range);
        Vector2f.add(from, dir, dir);
        return dir;
    }

    public CombatEntityAPI findTarget(Vector2f from, WeaponAPI weapon, float angleOffset) {
        float range = weapon.getRange();

        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                range * 2f, range * 2f);
        int owner = weapon.getShip().getOwner();
        CombatEntityAPI best = null;
        float minScore = Float.MAX_VALUE;

        ShipAPI ship = weapon.getShip();
        boolean ignoreFlares = ship != null && ship.getMutableStats().getDynamic().getValue(Stats.PD_IGNORES_FLARES, 0) >= 1;
        ignoreFlares |= weapon.hasAIHint(WeaponAPI.AIHints.IGNORES_FLARES);

        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof MissileAPI) &&
                    //!(o instanceof CombatAsteroidAPI) &&
                    !(o instanceof ShipAPI)) continue;
            CombatEntityAPI other = (CombatEntityAPI) o;
            if (other.getOwner() == owner) continue;

            if (other instanceof ShipAPI) {
                ShipAPI otherShip = (ShipAPI) other;
                if (otherShip.isHulk()) continue;
                //if (!otherShip.isAlive()) continue;
                if (otherShip.isPhased()) continue;
                if (!otherShip.isTargetable()) continue;
            }

            if (other.getCollisionClass() == CollisionClass.NONE) continue;

            if (ignoreFlares && other instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) other;
                if (missile.isFlare()) continue;
            }

            float radius = Misc.getTargetingRadius(from, other, false);
            float dist = Misc.getDistance(from, other.getLocation()) - radius;
            if (dist > range) continue;

            float arc = ARC;
//            if (other instanceof ShipAPI) {
//                dist += 500f;
//                arc += 5f;
//            }
            if (!Misc.isInArc(weapon.getCurrAngle() + angleOffset, arc, from, other.getLocation())) continue;

            float angleTo = Misc.getAngleInDegrees(from, other.getLocation());
            float score = Misc.getAngleDiff(weapon.getCurrAngle(), angleTo);
            //float score = dist;

            if (score < minScore) {
                minScore = score;
                best = other;
            }
        }
        return best;
    }

}
