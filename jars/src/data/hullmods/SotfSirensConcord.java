// stripped-down version of Sierra's Concord for Project SIREN
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;

import java.awt.*;

public class SotfSirensConcord extends com.fs.starfarer.api.combat.BaseHullMod

{

    public static float ZERO_FLUX_BOOST = 10f;
    public static float SHIELD_ACCEL = 30f;
    public static float TIME_MULT = 1.1f;

    public void advanceInCombat(ShipAPI ship, float amount) {
        // time mult
        CombatEngineAPI engine = Global.getCombatEngine();
        boolean player = ship == engine.getPlayerShip();
        if (player && ship.isAlive()) {
            ship.getMutableStats().getTimeMult().modifyMult(spec.getId(), TIME_MULT);
            //Global.getCombatEngine().getTimeMult().modifyMult(spec.getId(), 1f / TIME_MULT);
        } else {
            ship.getMutableStats().getTimeMult().modifyMult(spec.getId(), TIME_MULT);
            //Global.getCombatEngine().getTimeMult().unmodify(spec.getId());
        }
        // would implement a die-if-player-side code but, like, player Myranious or something could do it legitimately
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getZeroFluxSpeedBoost().modifyFlat(id, ZERO_FLUX_BOOST);
        stats.getShieldUnfoldRateMult().modifyPercent(id, SHIELD_ACCEL);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new SotfSierrasConcord.SotfSierraAfterImageScript(ship));
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int) (TIME_MULT * 100f) + "%";
        if (index == 1) return "" + (int) ZERO_FLUX_BOOST;
        if (index == 2) return "" + (int) SHIELD_ACCEL + "%";
        return null;
    }


    public Color getBorderColor() {
        return SotfMisc.getSierraColor();
    }

    public Color getNameColor() {
        return SotfMisc.getSierraColor();
    }

    // show up earlier, before Phase Field etc
    public int getDisplaySortOrder() {
        return 50;
    }
}