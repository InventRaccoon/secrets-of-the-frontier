package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicAnim;
import org.magiclib.util.MagicRender;

import java.awt.*;

/**
 *	Controls the Polychronios' lidar dish
 */

public class SotfLidarDecoScript implements EveryFrameWeaponEffectPlugin {

    protected float cooldown = 0f;
    protected static float RESET_TIME = 0.25f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip().isHulk()) {
            return;
        }

        if (weapon.getShip().getShipTarget() != null) {
            if (WeaponUtils.isWithinArc(weapon.getShip().getShipTarget(), weapon)) {
                cooldown = RESET_TIME;
                weapon.setSuspendAutomaticTurning(true);
                WeaponUtils.aimTowardsPoint(weapon, weapon.getShip().getShipTarget().getShieldCenterEvenIfNoShield(), amount);
                return;
            }
        }
        cooldown -= amount;
        if (cooldown > 0) {
            return;
        }
        weapon.setSuspendAutomaticTurning(true);
    }

}
