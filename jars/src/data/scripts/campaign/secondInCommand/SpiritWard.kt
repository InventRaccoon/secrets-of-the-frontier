package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.secondInCommand.misc.SpiritWardListener
import data.scripts.utils.SotfMisc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class SpiritWard : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all phase ships"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("Creates a defensive ward that envelops the surface of all phase ships*", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("   - The ward acts similar to a shield with 0.5 flux efficiency", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "shield", "0.5")
        tooltip.addPara("   - Unlike shields, the ward has a maximum amount of hitpoints, equal to 20%% of the ships flux capacity", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "20%")
        tooltip.addPara("   - Ward regenerates 20%% of its max hitpoints per second after the ship has not been damaged for 10 seconds", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "20%", "10")

        tooltip.addSpacer(10f)

        tooltip.addPara("*Concord ships are not considered to be phase ships.", Misc.getGrayColor(), 0f)

        tooltip.addSpacer(10f)

        var label = tooltip.addPara("\"Tempered spirit, a warding sigil and a witch's grit - vital safeguards for any voidsinger.\"", SotfMisc.getSierraColor().darker(), 0f)
        label.italicize()
    }

    override fun applyEffectsBeforeShipCreation(data: SCData, stats: MutableShipStatsAPI, variant: ShipVariantAPI, hullSize: ShipAPI.HullSize, id: String) {



    }

    override fun applyEffectsAfterShipCreation(data: SCData, ship: ShipAPI, variant: ShipVariantAPI, id: String) {

        if (ship.phaseCloak != null && ship.phaseCloak.specAPI.isPhaseCloak && !ship.variant.hasHullMod("rat_phaseshift_shield")) {
            var sierraMode = ship.variant.hasHullMod("sotf_sierrasconcord")
            Global.getCombatEngine().addPlugin(SpiritWardListener(ship, sierraMode))
        }

    }

}