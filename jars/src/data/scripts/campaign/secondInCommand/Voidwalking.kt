package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.utils.SotfMisc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class Voidwalking : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("+1 to burn level at which the fleet is considered to be moving slowly*", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("-15%% fuel usage", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())

        tooltip.addSpacer(10f)

        tooltip.addPara("*A slow moving fleet is harder to detect in some types of terrain, and can avoid some hazards. Some abilities also make the fleet " +
                "move slowly when activated. A fleet is considered slow-moving at a burn level of half of its slowest ship.", 0f, Misc.getGrayColor(), Misc.getHighlightColor())

        tooltip.addSpacer(10f)

        var label = tooltip.addPara("\"Everyone's got a few tricks out there - swim with me, and I'll show you mine.\"", SotfMisc.getSierraColor().darker(), 0f)
        label.italicize()
    }

    override fun applyEffectsBeforeShipCreation(data: SCData?, stats: MutableShipStatsAPI?, variant: ShipVariantAPI?,   hullSize: ShipAPI.HullSize?, id: String?) {
        stats!!.fuelUseMod.modifyMult(id, 0.85f)
    }

    override fun advanceInCampaign(data: SCData, member: FleetMemberAPI?, amount: Float?) {
        data.fleet.stats.dynamic.getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD).modifyFlat("sotf_voidwalking", 1f, "Void Walking")

    }

    override fun onDeactivation(data: SCData) {
        data.fleet.stats.dynamic.getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD).unmodify("sotf_voidwalking")

    }

}