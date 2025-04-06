package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.utils.SotfMisc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class OneWitchWaltz : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "ships with the \"Sierra's Concord\" hullmod"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("Halves the speed at which combat readiness degrades after peak performance time runs out", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("Powers up imprints from the \"Dance between Realms\" skill", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("   - Turns Helmsmanship, Field Modulation and Systems Expertise Elite", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "Helmsmanship", "Field Modulation", "Systems Expertise")
        tooltip.addPara("   - Provides imprints with non-elite Energy Weapon Mastery and Gunnery Implants", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "Energy Weapon Mastery", "Gunnery Implants")

        tooltip.addSpacer(10f)

        var label = tooltip.addPara("\"Me's a crowd.\"", SotfMisc.getSierraColor().darker(), 0f)
        label.italicize()
    }

    override fun applyEffectsBeforeShipCreation(data: SCData, stats: MutableShipStatsAPI, variant: ShipVariantAPI, hullSize: ShipAPI.HullSize, id: String) {

        stats.crLossPerSecondPercent.modifyMult(id, 0.5f)

    }

    override fun applyEffectsAfterShipCreation(data: SCData, ship: ShipAPI, variant: ShipVariantAPI, id: String) {



    }

}