package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.impl.combat.TemporalShellStats
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.utils.SotfMisc
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.setAlpha
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin
import java.awt.Color

class AgonyEncore : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("Disabling or destroying an opposing ship provides a temporary increase in timeflow", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("   - Upon triggering, the ships timeflow is doubled for 5/7/8/10 seconds, based on the target's hull size",
            0f, Misc.getTextColor(), Misc.getHighlightColor(), "doubled", "5", "7", "8", "10")
        tooltip.addPara("   - Defeating another ship while active adds an additional 5/7/8/10 seconds to the current " +
                "timer, up to 15", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "5", "7", "8", "10", "15")
        tooltip.addPara("   - Only the ship dealing the killing blow receives the effect", 0f, Misc.getTextColor(), Misc.getHighlightColor())

        tooltip.addSpacer(10f)

        var label = tooltip.addPara("\"The danse macabre is all about tempo.\"", SotfMisc.getSierraColor().darker(), 0f)
        label.italicize()
    }

    override fun applyEffectsBeforeShipCreation(data: SCData, stats: MutableShipStatsAPI, variant: ShipVariantAPI, hullSize: ShipAPI.HullSize, id: String) {



    }

    override fun applyEffectsAfterShipCreation(data: SCData, ship: ShipAPI, variant: ShipVariantAPI, id: String) {



    }

    //Added through WitchcraftKillingBlowHandler
    class AgonyEncoreListener(var ship: ShipAPI, var time: Float) : AdvanceableListener {

        var duration = time
        var inTimer = 0f

        override fun advance(amount: Float) {

            val player = ship == Global.getCombatEngine().playerShip
            var id = "agony_encore${ship.id}"
            var stats = ship.mutableStats

            inTimer += 1 * amount
            duration -= 1 * amount

            if (player) {
                Global.getCombatEngine().maintainStatusForPlayerShip("agony_encore", "graphics/icons/hullsys/temporal_shell.png",
                    "Agony's Encore", "${duration.toInt()} seconds remaining", false)
            }

            if (duration <= 0) {
                //Unapply

                stats.getTimeMult().unmodify(id)
                if (player) {
                    Global.getCombatEngine().timeMult.unmodify(id)
                }

                ship.removeListener(this)
                return
            }

            //Fade out at the end
            var effectLevel = (duration - 0f) / (0.75f - 0f)
            var inLevel = (inTimer - 0f) / (0.25f - 0f)
            if (inLevel < 0.99f) effectLevel = inLevel

            effectLevel = MathUtils.clamp(effectLevel, 0f, 1f)

            var jitterLevel = effectLevel
            var jitterRangeBonus = 2f
            val maxRangeBonus = 10f
          /*  if (inLevel < 0.99f) {
                jitterLevel = effectLevel / (1f / ship.system.chargeUpDur)
                if (jitterLevel > 1) {
                    jitterLevel = 1f
                }
                jitterRangeBonus = jitterLevel * maxRangeBonus
            }
            else if (effectLevel < 0.99f) {
                jitterRangeBonus = jitterLevel * maxRangeBonus
            } else {
                jitterLevel = 1f
                jitterRangeBonus = maxRangeBonus
            }*/

            //jitterLevel = Math.sqrt(jitterLevel.toDouble()).toFloat()
            //effectLevel *= effectLevel

            ship.setJitter(this, SotfMisc.SIERRA_COLOR.setAlpha(55), jitterLevel, 3, 0f, 0 + jitterRangeBonus)
            ship.setJitterUnder(this, SotfMisc.SIERRA_COLOR.setAlpha(155), jitterLevel, 25, 0f, 7f + jitterRangeBonus)

            val shipTimeMult: Float = 1f + (2f - 1f) * effectLevel
            stats.getTimeMult().modifyMult(id, shipTimeMult)
            if (player) {
                Global.getCombatEngine().timeMult.modifyMult(id, 1f / shipTimeMult)
            } else {
                Global.getCombatEngine().timeMult.unmodify(id)
            }

            ship.engineController.fadeToOtherColor(this,
                TemporalShellStats.JITTER_COLOR,
                Color(0, 0, 0, 0),
                effectLevel,
                0.5f)
            ship.engineController.extendFlame(this, -0.25f, -0.25f, -0.25f)
        }

    }

}