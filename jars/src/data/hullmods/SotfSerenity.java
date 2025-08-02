// Adaptive Phase Coils for Sierra
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;
import data.shipsystems.scripts.SotfConcordShiftStats;

import java.awt.*;

public class SotfSerenity extends SotfBaseConcordAugment {

    public static float FLUX_THRESHOLD_INCREASE_PERCENT = 100f;

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(
                Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, FLUX_THRESHOLD_INCREASE_PERCENT);
    }

//    @Override
//    public boolean isApplicableToShip(ShipAPI ship) {
//        if (ship.getVariant().hasHullMod(SotfIDs.HULLMOD_FERVOR)) return false;
//        return shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord");
//    }
//
//    @Override
//    public String getUnapplicableReason(ShipAPI ship) {
//        if (ship.getVariant().hasHullMod(SotfIDs.HULLMOD_FERVOR)) {
//            return "Incompatible with Concord - Fervor";
//        }
//        if (!shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord")) {
//            return "Requires a Phase Concord";
//        }
//        return super.getUnapplicableReason(ship);
//    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int) Math.round(FLUX_THRESHOLD_INCREASE_PERCENT) + "%";
        if (index == 1) return "" + (int) Math.round(SotfConcordShiftStats.BASE_FLUX_LEVEL_FOR_MIN_SPEED * 100f) + "%";
        if (index == 2) return "" + (int)Math.round(
                SotfConcordShiftStats.BASE_FLUX_LEVEL_FOR_MIN_SPEED * 100f *
                        (1f + FLUX_THRESHOLD_INCREASE_PERCENT/100f)) + "%";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color good = Misc.getPositiveHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();
        Color gray = Misc.getGrayColor();

        if (Global.getSettings().getCurrentState() == GameState.TITLE) return;
        if (isForModSpec || ship == null) return;

        LabelAPI label = tooltip.addPara("\"Just... take a deep breath. " +
                "In, and out. Get comfy, and enjoy the show.\"", SotfMisc.getSierraColor().darker(), opad);
        label.italicize();
        tooltip.addPara("   - \"Serenity\" augment digital handout", gray, opad);
    }

}
