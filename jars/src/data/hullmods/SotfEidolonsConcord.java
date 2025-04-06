// Won't you dance with me?
// Concord stat buffs and Eidolon's status HUD ""buff""
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.SotfMisc;

import java.awt.*;

public class SotfEidolonsConcord extends com.fs.starfarer.api.combat.BaseHullMod

{

    public void advanceInCombat(ShipAPI ship, float amount) {
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
        // Eidolon chats through the status HUD if she's near the player
        if (ship.isAlive() && ship.getOwner() == 1 && playerShip != null) {
            float dist_to_player = Misc.getDistance(ship.getLocation(), playerShip.getLocation());
            if (dist_to_player <= 700f) {
                Global.getCombatEngine().maintainStatusForPlayerShip("$sotf_eidolonhello",
                        Global.getSettings().getSpriteName("sotf_characters", "eidolon"),
                        "Eyes on us",
                        "Look at us go!",
                        false);
            } else if (dist_to_player <= 1300f) {
                Global.getCombatEngine().maintainStatusForPlayerShip("$sotf_eidolonhello",
                        Global.getSettings().getSpriteName("sotf_characters", "eidolon"),
                        "Eyes on you",
                        "Show me how it's done!",
                        false);
            } else if (dist_to_player <= 2400f) {
                Global.getCombatEngine().maintainStatusForPlayerShip("$sotf_eidolonhello",
                        Global.getSettings().getSpriteName("sotf_characters", "eidolon"),
                        "Oh hello",
                        "Fancy seeing you here",
                        false);
            }
        }

        // time mult. Shares ID with boost from Phantasmal, so they don't stack
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        if (player && ship.isAlive()) {
            ship.getMutableStats().getTimeMult().modifyMult(spec.getId(), 1.1f);
            Global.getCombatEngine().getTimeMult().modifyMult(spec.getId(), 1f / 1.1f);
        } else {
            ship.getMutableStats().getTimeMult().modifyMult(spec.getId(), 1.1f);
            Global.getCombatEngine().getTimeMult().unmodify(spec.getId());
        }
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getZeroFluxSpeedBoost().modifyFlat(id, 10f);
        stats.getShieldUnfoldRateMult().modifyPercent(id, 30f);
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new SotfSierrasConcord.SotfSierraAfterImageScript(ship, SotfMisc.getEidolonColor()));
    }

    public Color getBorderColor() {
        return SotfMisc.getSierraColor();
    }

    public Color getNameColor() {
        return SotfMisc.getSierraColor();
    }
}