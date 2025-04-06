package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import static data.shipsystems.scripts.SotfConcordShiftStats.SHIP_ALPHA_MULT;

public class SotfConcordEntryStats extends BaseShipSystemScript {

    // no, ship-system scripts are not one-per-spec like hullmods are
    //private List<ShipAPI> have_phased = new ArrayList<>();
    private boolean isPhased = false;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        if (Global.getCombatEngine().isPaused() || state == State.IDLE) {
            return;
        }

        if (state == State.OUT) {
            unapply(stats, id);
            ship.getSystem().setCooldownRemaining(2f);
        }

        float level = effectLevel;
        float levelForAlpha = level;

        if (state == State.IN && Global.getCombatEngine().getTotalElapsedTime(false) > 1f) {
            isPhased = true;
        }

        if (state == State.IN || state == State.ACTIVE) {
            ship.setPhased(true);
            levelForAlpha = level;
            stats.getMaxSpeed().modifyFlat(id, 600f * effectLevel);
            stats.getAcceleration().modifyFlat(id, 600f * effectLevel);
        } else if (state == State.OUT) {
            if (level > 0.5f) {
                ship.setPhased(true);
            } else {
                ship.setPhased(false);
            }
            levelForAlpha = level;
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
        }

        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
        ship.setApplyExtraAlphaToEngines(false);

        float shipTimeMult = 1f + (2f) * levelForAlpha;
        stats.getTimeMult().modifyMult(id, shipTimeMult);
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        //boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            //player = ship == Global.getCombatEngine().getPlayerShip();
            //id = id + "_" + ship.getId();
        } else {
            return;
        }

        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);

        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("here we go", false);
        }
        return null;
    }

}
