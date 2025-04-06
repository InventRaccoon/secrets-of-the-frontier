package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.secondInCommand.misc.WitchcraftKillingBlowHandler
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class DanceBetweenRealms : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "ships with the \"Sierra's Concord\" hullmod"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("Enables Sierra to simultaneously captain a ship and serve as an executive officer", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("Sierra can now share imprints of herself with other Concord ships", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("   - Sierras imprints are applied to any Concord ship not piloted by her directly", 0f, Misc.getTextColor(), Misc.getHighlightColor())
        tooltip.addPara("   - The imprints have non-elite Helmsmanship, Field Modulation and Systems Expertise", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "Helmsmanship", "Field Modulation", "Systems Expertise")
        tooltip.addPara("   - Imprints enable Concord ships to use their phase systems and avoid penalties to combat readiness", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "shipsystems", "penalties")

        tooltip.addSpacer(10f)

        tooltip.addPara("Sierra will be removed as an executive officer if she is not in the fleet.", Misc.getGrayColor(), 0f)
        tooltip.addPara("When a Concord ship benefiting from this skill is set as the flagship, it will use the imprint's skills instead of your own.", Misc.getGrayColor(), 0f)
    }

    override fun advanceInCampaign(data: SCData?, member: FleetMemberAPI?, amount: Float?) {



    }

    override fun applyEffectsBeforeShipCreation(data: SCData, stats: MutableShipStatsAPI, variant: ShipVariantAPI, hullSize: ShipAPI.HullSize, id: String) {



    }

    override fun applyEffectsAfterShipCreation(data: SCData, ship: ShipAPI, variant: ShipVariantAPI, id: String) {



    }



    override fun advanceInCombat(data: SCData?, ship: ShipAPI?, amount: Float?) {

        var interval = Global.getCombatEngine().customData.get("sotf_witchcraft_killing_blow_interval") as IntervalUtil?
        if (interval == null) {
            interval = IntervalUtil(0.25f, 0.33f)
            Global.getCombatEngine().customData.set("sotf_witchcraft_killing_blow_interval", interval)
        }

        interval.advance(amount!!)
        if (interval.intervalElapsed()) {
            for (toApply in Global.getCombatEngine().ships) {
                if (!toApply.hasListenerOfClass(WitchcraftKillingBlowHandler::class.java)) {
                    toApply.addListener(WitchcraftKillingBlowHandler())
                }
            }
        }

    }

}