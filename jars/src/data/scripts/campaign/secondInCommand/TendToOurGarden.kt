package data.scripts.campaign.secondInCommand

import data.scripts.campaign.secondInCommand.misc.PidController
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.utils.SotfMisc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.plus
import org.lwjgl.util.vector.Vector2f
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin
import java.awt.Color

class TendToOurGarden : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("Disabling or destroying an opposing ship creates 2/2/3/4 guardian wisps, based on hullsize", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("   - Wisps fire a 1000 range energy beam that deals 100 damage per second", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "1000", "energy", "100")
        tooltip.addPara("   - Wisps disappear 30 seconds after being created", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "30")
        tooltip.addPara("   - Wisps prioritize firing at missiles and fighters", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "missiles", "fighters")
        tooltip.addPara("   - Wisps orbit their caller in a defensive orientation", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "")

        tooltip.addSpacer(10f)

        var label = tooltip.addPara("\"The beyond will protect you, if only you know how to ask.\"", SotfMisc.getSierraColor().darker(), 0f)
        label.italicize()
    }

    override fun applyEffectsBeforeShipCreation(data: SCData, stats: MutableShipStatsAPI, variant: ShipVariantAPI, hullSize: ShipAPI.HullSize, id: String) {



    }

    override fun applyEffectsAfterShipCreation(data: SCData, ship: ShipAPI, variant: ShipVariantAPI, id: String) {

        if (!ship.hasListenerOfClass(TendToOurGardenListener::class.java)) {
            ship.addListener(TendToOurGardenListener(ship))
        }

    }

    class TendToOurGardenListener(var ship: ShipAPI) : AdvanceableListener {

        data class GardenWisp(var wisp: ShipAPI, var controller: PidController, var interval: IntervalUtil, var offset: Vector2f)

        var wisps = ArrayList<GardenWisp>()

        override fun advance(amount: Float) {

            for (wisp in ArrayList(wisps)) {
                if (!wisp.wisp.isAlive) {
                    wisps.remove(wisp)
                    continue
                }
            }

            if (wisps.size == 0) return

           /* var angleIncrease = 360 / wisps.size
            var angle = 0f*/


            for (wisp in ArrayList(wisps)) {

                wisp.interval.advance(amount)
                if (wisp.interval.intervalElapsed()) {
                    wisp.offset = MathUtils.getRandomPointOnCircumference(Vector2f(), MathUtils.getRandomNumberInRange(ship.collisionRadius * 1.2f, ship.collisionRadius * 2f))
                }

                //var point = MathUtils.getPointOnCircumference(ship.location, ship.collisionRadius * 1.5f, angle)
                //var point = MathUtils.getPointOnCircumference(ship.location, ship.collisionRadius * 1.5f, angle)

                //point = point.plus(MathUtils.getRandomPointInCircle(Vector2f(), 25f))

                var point = Vector2f(ship.location.plus(wisp.offset))

                wisp.controller.move(point, wisp.wisp)

                //angle += angleIncrease
            }
        }

        //Called by WitchcraftKillingBlowHandler
        fun spawnWisp(target: ShipAPI, count: Int)  {
            for (i in 0 until count) {

                val to = Misc.getPointAtRadius(target.location, target.collisionRadius + 25)

                val engine = Global.getCombatEngine()

                val fleetManager: CombatFleetManagerAPI = engine.getFleetManager(ship.getOriginalOwner())
                val wasSuppressed = fleetManager.isSuppressDeploymentMessages
                fleetManager.isSuppressDeploymentMessages = true
                val wisp = engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing("sotf_wisp_xo_wing", to, Misc.getAngleInDegrees(target.getLocation(), to), 0f)
                Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 1f, wisp.location, Vector2f(0f, 0f))
                Global.getCombatEngine().spawnEmpArcVisual(Misc.getPointWithinRadius(ship.getLocation(), 50f),
                    ship,
                    wisp.location,
                    wisp,
                    30f,
                    Color(100, 25, 155, 255),
                    Color.white)
                fleetManager.isSuppressDeploymentMessages = wasSuppressed

                wisp.shipAI = null

                var interval = IntervalUtil(0.5f, 1.5f)

                var entity = GardenWisp(wisp, PidController(4f, 4f, 6f, 0.5f), interval, Vector2f())
                wisps.add(entity)
            }
        }

    }

}