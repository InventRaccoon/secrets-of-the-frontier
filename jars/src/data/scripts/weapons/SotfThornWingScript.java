package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicAnim;
import org.magiclib.util.MagicRender;

import java.awt.*;

/**
 *	Controls the Thorn-class' wings which fold back when it uses its system
 *  Left wing also controls the glowing bits underneath the wings
 */

public class SotfThornWingScript implements EveryFrameWeaponEffectPlugin {

    SpriteAPI glow = Global.getSettings().getSprite("fx", "sotf_thorn_glow");
    public static float SHIP_ALPHA_MULT = 0.25f;

    protected float level = 0f;
    protected float cooldown = 0f;

    protected static float RESET_TIME = 0.25f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip().isHulk()) {
            return;
        }

        boolean right = weapon.getId().equals("sotf_deco_thorn_rwing");

        if (weapon.getShip().getSystem() == null) {
            return;
        }

        ShipSystemAPI system = weapon.getShip().getSystem();
        if (system.isActive() || system.isChargeup() || system.isChargedown()) {
            level += amount / 0.25f;
            cooldown = RESET_TIME;
        } else {
            cooldown -= amount;
        }
        if (cooldown <= 0f) {
            level -= amount / 0.75f;
        }
        if (level > 1f) level = 1f;

        if (level <= 0f) {
            level = 0f;
            weapon.setRenderOffsetForDecorativeBeamWeaponsOnly(new Vector2f());
//            weapon.getShip().setExtraAlphaMult2(1f);
            return;
        }
        float xoffset = -16f;
        float yoffset = -5f;
        if (right) {
            yoffset *= -1f;
        }

//        if (system.getState() == ShipSystemAPI.SystemState.IN) {
//            xoffset *= 1f - system.getEffectLevel() * system.getEffectLevel();
//            yoffset *= 1f - system.getEffectLevel() * system.getEffectLevel();
//        } else {
//            xoffset *= system.getEffectLevel() * system.getEffectLevel();
//            yoffset *= system.getEffectLevel() * system.getEffectLevel();
//        }
        xoffset *= MagicAnim.smooth(level);
        yoffset *= MagicAnim.smooth(level);

        weapon.setRenderOffsetForDecorativeBeamWeaponsOnly(new Vector2f(xoffset, yoffset));

        if (right) {
            return;
        }

//        weapon.getShip().setExtraAlphaMult2(1f - (1f - SHIP_ALPHA_MULT) * level);
        //weapon.getShip().setExtraAlphaMult(0.25f);

        MagicRender.singleframe(
                glow,
                MathUtils.getPoint(weapon.getShip().getLocation(), 5f, weapon.getShip().getFacing() - 180f),
                new Vector2f(glow.getWidth(), glow.getHeight()),
                weapon.getShip().getFacing() - 90f,
                Misc.setAlpha(new Color(105,255,195), Math.round(255 * (level * level))),
                true
        );

        MagicRender.battlespace(
                glow,
                MathUtils.getPoint(weapon.getShip().getLocation(), 5f, weapon.getShip().getFacing() - 180f),
                new Vector2f(),
                new Vector2f(glow.getWidth(), glow.getHeight()),
                new Vector2f(),
                weapon.getShip().getFacing() - 90f,
                0f,
                Misc.setAlpha(new Color(115,255,215), Math.round(55 * (level * level))),
                true,
                0f,
                0f,
                0.4f
        );
    }

}
