package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.utils.SotfMisc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class Accelerando : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("+30% increase to the ships speed while venting", Misc.getHighlightColor(), 0f)
        tooltip.addPara("+10% increase to the ships active vent rate", Misc.getHighlightColor(), 0f)

        tooltip.addSpacer(10f)

        var label = tooltip.addPara("\"The fray won't wait for you to take a breather. Keep - in - time!\"", SotfMisc.getSierraColor().darker(), 0f)
        label.italicize()
    }

    override fun advanceInCombat(data: SCData, ship: ShipAPI, amount: Float?) {
        if (ship.fluxTracker.isVenting) {

            if (ship == Global.getCombatEngine().playerShip) {

                Global.getCombatEngine().maintainStatusForPlayerShip("sotf_accelerando_1", "graphics/icons/hullsys/entropy_amplifier.png",
                    "Accelerando", "+30% speed", false)

                Global.getCombatEngine().maintainStatusForPlayerShip("sotf_accelerando_2", "graphics/icons/hullsys/entropy_amplifier.png",
                    "Accelerando", "+10% vent rate", false)
            }

            ship.mutableStats.ventRateMult.modifyPercent("sotf_accelerando", 10f)

            ship.mutableStats.maxSpeed.modifyMult("sotf_accelerando", 1.3f)
            ship.mutableStats.acceleration.modifyMult("sotf_accelerando", 1.3f)
            ship.mutableStats.deceleration.modifyMult("sotf_accelerando", 1.3f)
            ship.mutableStats.turnAcceleration.modifyMult("sotf_accelerando", 1.3f)
            ship.mutableStats.maxTurnRate.modifyMult("sotf_accelerando", 1.3f)
        }
        else {
            ship.mutableStats.ventRateMult.unmodify("sotf_accelerando")

            ship.mutableStats.maxSpeed.unmodify("sotf_accelerando")
            ship.mutableStats.acceleration.unmodify("sotf_accelerando")
            ship.mutableStats.deceleration.unmodify("sotf_accelerando")
            ship.mutableStats.turnAcceleration.unmodify("sotf_accelerando")
            ship.mutableStats.maxTurnRate.unmodify("sotf_accelerando")
        }
    }

}