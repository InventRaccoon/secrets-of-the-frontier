// Phase Anchor except it works on shielded ships with a phase system
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfFervor extends SotfBaseConcordAugment {

    public static float PHASE_DISSIPATION_MULT = 2f;
    public static float ACTIVATION_COST_MULT = 0f;

    public static float CR_LOSS_MULT_FOR_EMERGENCY_DIVE = 1f;

    public static class SotfFervorScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public ShipAPI ship;
        public boolean emergencyDive = false;
        public float diveProgress = 0f;
        public FaderUtil diveFader = new FaderUtil(1f, 1f);
        public SotfFervorScript(ShipAPI ship) {
            this.ship = ship;
        }

        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            //if (ship.getCurrentCR() <= 0) return false;

            if (!emergencyDive && ship.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD)) {
                String key = "sotf_fervor_canDive";
                boolean canDive = !Global.getCombatEngine().getCustomData().containsKey(key);
                float depCost = 0f;
                if (ship.getFleetMember() != null) {
                    depCost = ship.getFleetMember().getDeployCost();
                }
                float crLoss = CR_LOSS_MULT_FOR_EMERGENCY_DIVE * depCost;
                canDive &= ship.getCurrentCR() >= crLoss;
                // ignore phase dive cost during A Memory
                if (Global.getCombatEngine().getCustomData().containsKey("$sotf_AMemory")) canDive = true;

                float hull = ship.getHitpoints();
                if (damageAmount >= hull && canDive) {
                    ship.setHitpoints(1f);

                    if (ship.getFleetMember() != null) { // fleet member is fake during simulation, so this is fine
                        ship.getFleetMember().getRepairTracker().applyCREvent(-crLoss, "Emergency phase dive");
                    }
                    emergencyDive = true;
                    Global.getCombatEngine().getCustomData().put(key, true);

                    if (!ship.isPhased()) {
                        Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    }
                }
            }

            if (emergencyDive) {
                return true;
            }

            return false;
        }

        public void advance(float amount) {
            ShipSystemAPI cloak = ship.getSystem();
            String id = "phase_anchor_modifier";
            if (emergencyDive) {
                Color c = cloak.getSpecAPI().getEffectColor2();
                c = Misc.setAlpha(c, 255);
                c = Misc.interpolateColor(c, Color.white, 0.5f);

                if (diveProgress == 0f) {
                    if (ship.getFluxTracker().showFloaty()) {
                        float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
                        Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
                                "Emergency dive!",
                                NeuralLinkScript.getFloatySize(ship), c, ship, 16f * timeMult, 3.2f/timeMult, 1f/timeMult, 0f, 0f,
                                1f);
                    }
                }

                diveFader.advance(amount);
                ship.setRetreating(true, false);

                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                diveProgress += amount * cloak.getChargeUpDur();
                float curr = ship.getExtraAlphaMult();
                cloak.forceState(ShipSystemAPI.SystemState.IN, Math.min(1f, Math.max(curr, diveProgress)));
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);

                if (diveProgress >= 1f) {
                    if (diveFader.isIdle()) {
                        Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    }
                    diveFader.fadeOut();
                    diveFader.advance(amount);
                    float b = diveFader.getBrightness();
                    ship.setExtraAlphaMult2(b);

                    float r = ship.getCollisionRadius() * 5f;
                    ship.setJitter(this, c, b, 20, r * (1f - b));

                    if (diveFader.isFadedOut()) {
                        ship.getLocation().set(0, -1000000f);
                    }
                }
            }


            boolean phased = ship.isPhased();
            if (cloak != null && cloak.isChargedown()) {
                phased = false;
            }

            MutableShipStatsAPI stats = ship.getMutableStats();
            if (phased) {
                stats.getFluxDissipation().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getBallisticRoFMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getEnergyRoFMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getMissileRoFMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getBallisticAmmoRegenMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getEnergyAmmoRegenMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getMissileAmmoRegenMult().modifyMult(id, PHASE_DISSIPATION_MULT);
            } else {
                stats.getFluxDissipation().unmodifyMult(id);
                stats.getBallisticRoFMult().unmodifyMult(id);
                stats.getEnergyRoFMult().unmodifyMult(id);
                stats.getMissileRoFMult().unmodifyMult(id);
                stats.getBallisticAmmoRegenMult().unmodifyMult(id);
                stats.getEnergyAmmoRegenMult().unmodifyMult(id);
                stats.getMissileAmmoRegenMult().unmodifyMult(id);
            }
        }

    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new SotfFervorScript(ship));
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getPhaseCloakActivationCostBonus().modifyMult(id, ACTIVATION_COST_MULT);
    }

//    @Override
//    public boolean isApplicableToShip(ShipAPI ship) {
//        if (ship.getVariant().hasHullMod(SotfIDs.HULLMOD_SERENITY)) return false;
//        return shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord");
//    }
//
//    @Override
//    public String getUnapplicableReason(ShipAPI ship) {
//        if (ship.getVariant().hasHullMod(SotfIDs.HULLMOD_SERENITY)) {
//            return "Incompatible with Concord - Serenity";
//        }
//        if (!shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord")) {
//            return "Requires a Phase Concord";
//        }
//        return super.getUnapplicableReason(ship);
//    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "zero";
        if (index == 1) return "" + (int)PHASE_DISSIPATION_MULT + Strings.X;
        if (index == 2) return "" + (int)CR_LOSS_MULT_FOR_EMERGENCY_DIVE + Strings.X;
        if (index == 3) return "Phase Anchor";
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

        LabelAPI label = tooltip.addPara("\"Any perceived increase in music tempo is actually caused by a degradation in " +
                "your capacity to correctly perceive the passing of time. It definitely fits the atmosphere, though.\"", SotfMisc.getSierraColor().darker(), opad);
        label.italicize();
        tooltip.addPara("   - \"Fervor\" augment digital handout", gray, opad);
    }

}
