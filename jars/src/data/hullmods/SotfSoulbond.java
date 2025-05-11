// Kindred souls are ever drawn to each other
// Grants the flagship a MagicLib subsystem that allows it to teleport to Sierra, or teleport Sierra to itself
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.CombatEngine;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.SotfAuraVisualScript;
import data.scripts.utils.SotfMisc;
import data.subsystems.SotfSoulbondSubsystem;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsCombatPlugin;
import org.magiclib.subsystems.MagicSubsystemsManager;
import second_in_command.SCUtils;

import java.awt.*;
import java.util.Iterator;

public class SotfSoulbond extends SotfBaseConcordAugment
{

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!Global.getCurrentState().equals(GameState.COMBAT)) {
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
//        CombatEngine combatEngine = (CombatEngine) engine;
//        combatEngine.add

        if (engine.getPlayerShip() == null) {
            return;
        }

        ShipAPI flagship = engine.getPlayerShip();
        if (flagship.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD)) return;
        if (ship.getVariant().hasTag(SotfIDs.TAG_INERT)) return;
        if (ship.isCapital() && flagship.isCapital()) return;

        MagicSubsystemsManager.addSubsystemToShip(flagship, new SotfSoulbondSubsystem(flagship));
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color good = Misc.getPositiveHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();
        Color gray = Misc.getGrayColor();
        Color sc = SotfMisc.getSierraColor();

        tooltip.addPara("An exotic spatial shunt installed into the ship's phase web, harmonized with an " +
                "accompanying spatial anchor installed in the fleet's flagship. Activation distorts and " +
                "collapses p-space to interdimensionally drag the shunt to the anchor, or vice versa.", opad);

        tooltip.addPara("Adds an additional Soulbond subsystem to the flagship in combat.", pad, sc, "Soulbond");

        tooltip.addSectionHeading("Function: Recall", Alignment.MID, opad);
        tooltip.addPara("Activating the subsystem normally causes Sierra's ship to enter phase space and be dragged " +
                "towards the flagship before unphasing a short distance away.", opad);

        tooltip.addSectionHeading("Function: Kinskip", Alignment.MID, opad);
        tooltip.addPara("Activating the subsystem while Sierra's ship is targeted causes the flagship to remotely phase " +
                "and be dragged towards Sierra's ship before exiting phase nearby.", opad);

        tooltip.addSectionHeading("Notes", Alignment.MID, opad);
        if (Global.getSector() != null && !isForModSpec && ship != null) {
            if (SotfMisc.isSecondInCommandEnabled()) {
                if (SCUtils.getPlayerData().isSkillActive("sotf_dance_between_realms")) {
                    tooltip.addPara("- Does not function with imprints from Dance Between Realms; requires Sierra herself be piloting the other ship.", pad, sc, "Dance Between Realms");
                }
            }
        }

        tooltip.addPara("- Capital ships cannot be dragged with this subsystem.", opad, new Color[]{h, bad}, "Capital ships", "cannot be dragged");
        tooltip.addPara("- Cannot be used if Sierra's ship is also the flagship.", pad);
        if (ship != null) {
            tooltip.addPara("- On activation, Sierra's ship builds hard flux equal to %s of its base flux capacity (%s flux).", pad, h,
                    "" + (int) (SotfSoulbondSubsystem.FLUX_BUILDUP_PERCENT * 100f) + "%", "" + (int) (SotfSoulbondSubsystem.FLUX_BUILDUP_PERCENT * ship.getHullSpec().getFluxCapacity()));
        } else {
            tooltip.addPara("- On activation, Sierra's ship builds hard flux equal to %s of its base flux capacity.", pad, h,
                    "" + (int) (SotfSoulbondSubsystem.FLUX_BUILDUP_PERCENT * 100f) + "%");
        }
        tooltip.addPara("- Must recharge for %s between uses.", pad, h, "" + (int) SotfSoulbondSubsystem.BASE_COOLDOWN + " seconds");
        //tooltip.addPara("- Brief temporal distortion causes both ships to experience time more slowly for a few seconds at the sequence's end", pad, h, "experience time more slowly");

        LabelAPI label = tooltip.addPara(
                "   Where I go, won't you still follow?~\n" +
                        "   Into the darkness, into the hollow?~\n" +
                        "   No tides nor winds to unite us,\n" +
                        "   My soul still yearns for you~", SotfMisc.getSierraColor().darker(), opad);
        label.italicize();
        tooltip.addPara("   - Sierra-Nought-Bravo, \"Soulbond\" lyrical excerpt", gray, opad);
    }
}