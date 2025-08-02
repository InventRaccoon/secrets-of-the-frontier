package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.hullmods.AdaptivePhaseCoils
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.utils.SotfMisc
import second_in_command.SCData
import second_in_command.misc.baseOrModSpec
import second_in_command.specs.SCBaseSkillPlugin

class SealOfAbjuration : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("20% increase to the flux required to reach the maximum effect of phase coil stress", Misc.getHighlightColor(), 0f)
        tooltip.addPara("-10% damage taken by shields", Misc.getHighlightColor(), 0f)

        tooltip.addSpacer(10f)

        tooltip.addPara("Affects: all phase ships*", 0f, Misc.getGrayColor(), Misc.getBasePlayerColor(), "all phase ships")

        tooltip.addSpacer(10f)

        tooltip.addPara("+60 seconds peak operating time", Misc.getHighlightColor(), 0f)

        tooltip.addSpacer(10f)

        tooltip.addPara("*Concord ships are not considered to be phase ships.", Misc.getGrayColor(), 0f)

        tooltip.addSpacer(10f)

        var label = tooltip.addPara("\"Musicians make the best fields officers.\"", SotfMisc.getSierraColor().darker(), 0f)
        label.italicize()
    }

    override fun applyEffectsBeforeShipCreation(data: SCData, stats: MutableShipStatsAPI, variant: ShipVariantAPI, hullSize: ShipAPI.HullSize, id: String) {

        if (variant.baseOrModSpec().isPhase) {
            stats!!.peakCRDuration.modifyFlat(id, 60f)
        } else {
            stats.dynamic.getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, 20f)
            stats.shieldDamageTakenMult.modifyMult(id, 0.9f)
        }

    }

    override fun applyEffectsAfterShipCreation(data: SCData, ship: ShipAPI, variant: ShipVariantAPI, id: String) {



    }

}