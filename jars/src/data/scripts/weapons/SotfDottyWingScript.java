package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

/**
 *	Controls Dotty's wings, which fan out when she's in combat
 */

public class SotfDottyWingScript implements EveryFrameWeaponEffectPlugin {

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip().isHulk()) {
            return;
        }

        boolean right = weapon.getId().equals("sotf_dottyrwing");

        float facing = weapon.getShip().getFacing();
        //float fluxLevel = (weapon.getShip().getFluxLevel() + weapon.getShip().getHardFluxLevel()) / 2;
        //if (weapon.getShip().getFluxTracker().isOverloaded()) {
        //    fluxLevel = 1f;
        //} else if (weapon.getShip().getFluxTracker().isVenting()) {
        //    fluxLevel = 0f;
        //}
        //float angle = 70f - (45f * fluxLevel);
        float angle = 70f;
        if (weapon.getShip().getShipTarget() != null) {
            if (Misc.getDistance(weapon.getShip().getShipTarget().getLocation(), weapon.getShip().getLocation()) <= 1400f) {
                angle -= 45f;
            }
        }
        if (right) {
            angle *= -1;
        }
        float desired_angle = facing + angle;
        float dir = MathUtils.getShortestRotation(weapon.getCurrAngle(),
                desired_angle);
        dir = Math.min(dir, 1);
        dir = Math.max(dir, -1);
        weapon.setCurrAngle(
                weapon.getCurrAngle() + (dir * 60f * amount)
        );
    }

}
