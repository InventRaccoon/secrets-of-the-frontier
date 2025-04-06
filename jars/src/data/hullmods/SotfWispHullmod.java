// hullmod applied to the wisps spawned by the Wispersong and Wisptender hullmods
// explode at max flux/on death, invincible engines
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;

import java.awt.*;

public class SotfWispHullmod extends com.fs.starfarer.api.combat.BaseHullMod

{
    public void advanceInCombat(ShipAPI ship, float amount) {
        ship.getEngineController().forceShowAccelerating();
        // explode at max flux or when killed
        if (ship.isHulk() || ship.getFluxTracker().getFluxLevel() >= 0.95f) {

            float mult = 1f;

            if (ship.getWing() != null && ship.getWing().getWingId().equals("sotf_wisp_xo_wing")) {
                mult = 0f;
            }

            Global.getCombatEngine().spawnDamagingExplosion(new DamagingExplosionSpec(0.5f,
                    ship.getMaxFlux() * 1.5f,
                    ship.getMaxFlux() * 0.75f,
                    ship.getMaxFlux() * 3f * mult,
                    ship.getMaxFlux() * 1.5f * mult,
                    CollisionClass.PROJECTILE_FIGHTER,
                    CollisionClass.PROJECTILE_FIGHTER,
                    2f,
                    2f,
                    1f,
                    100,
                    Color.white,  new Color(155,125,225)), ship, ship.getLocation());

            Global.getCombatEngine().removeEntity(ship);
        }
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // invincible "engines"
        stats.getEngineDamageTakenMult().modifyMult(id, 0f);
        stats.getCombatEngineRepairTimeMult().modifyMult(id,0f);
    }
}