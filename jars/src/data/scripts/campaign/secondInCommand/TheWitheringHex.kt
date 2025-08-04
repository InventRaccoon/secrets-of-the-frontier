package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier
import com.fs.starfarer.api.impl.combat.RiftBeamEffect
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect
import com.fs.starfarer.api.impl.combat.RiftLanceEffect
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

class TheWitheringHex : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("Missile hits against armor or hull apply \"Hex\" to their targets*", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("   - Hex applies damage over time on the impacted location for 7 seconds", 0f, Misc.getTextColor(), Misc.getHighlightColor(),  "7")
        tooltip.addPara("   - Deals 10%% of the missiles base damage over the duration as energy damage, and 20%% as EMP damage", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "10%", "energy", "20%", "EMP" )

        tooltip.addSpacer(10f)

        tooltip.addPara("*DEM missiles do not apply Hex.", Misc.getGrayColor(), 0f)

        tooltip.addSpacer(10f)

        var label = tooltip.addPara("\"... I think this voids every warranty on the ship, but the risk of dimensional " +
                "collapse is near-nil as long as you recite the enclosed rites.\"", SotfMisc.getSierraColor().darker(), 0f)
        label.italicize()
    }

    override fun applyEffectsBeforeShipCreation(data: SCData, stats: MutableShipStatsAPI, variant: ShipVariantAPI, hullSize: ShipAPI.HullSize, id: String) {



    }

    override fun applyEffectsAfterShipCreation(data: SCData, ship: ShipAPI, variant: ShipVariantAPI, id: String) {

        if (!ship.hasListenerOfClass(WitheringHexListener::class.java)) {
            ship.addListener(WitheringHexListener())
        }

    }

    class WitheringHexListener() : DamageDealtModifier {

        override fun modifyDamageDealt(param: Any?, target: CombatEntityAPI?, damage: DamageAPI?, point: Vector2f?, shieldHit: Boolean): String? {

            if (target !is ShipAPI || param !is DamagingProjectileAPI || shieldHit) {
                return null
            }
            if (target.isHulk || !target.isAlive) return null

            if (param !is MissileAPI && !param.isFromMissile) {
                return null
            }

            var hexListener = target.getListeners(HexEffectListener::class.java).firstOrNull()
            if (hexListener == null) {
                hexListener = HexEffectListener(target)
                target.addListener(hexListener)
            }

            var offset = Vector2f.sub(point, target.getLocation(), Vector2f())
            offset = Misc.rotateAroundOrigin(offset, -target.getFacing())

            var mult = 0.3f

            hexListener.effects.add(HexEffectListener.HexEffect(param.weapon?.ship, offset, param.baseDamageAmount * mult, 7f, IntervalUtil(0.25f, 0.25f), IntervalUtil(0.2f, 0.25f)))


            return null

        }

    }

    class HexEffectListener(var target: ShipAPI) : AdvanceableListener {

        data class HexEffect(var ship: ShipAPI?, var offset: Vector2f, var damage: Float, var duration: Float, var interval: IntervalUtil, var vfxInterval: IntervalUtil)

        var effects = ArrayList<HexEffect>()

        override fun advance(amount: Float) {

            for (effect in ArrayList(effects)) {

                effect.duration -= 1 * amount
                if (effect.duration <= 0) {
                    effects.remove(effect)
                    continue
                }

                var loc = Vector2f(effect.offset)
                loc = Misc.rotateAroundOrigin(loc, target.facing)
                Vector2f.add(target.location, loc, loc)

                effect.interval.advance(amount)
                if (effect.interval.intervalElapsed()) {
                    var dam = effect.damage / effect.duration * effect.interval.maxInterval / 3
                    var emp = dam * 2f

                    Global.getCombatEngine().applyDamage(target, loc, dam, DamageType.ENERGY, emp, true, true, effect.ship)
                }

                effect.vfxInterval.advance(amount)
                if (effect.vfxInterval.intervalElapsed()) {

                    //Scale how large the effect should be.
                    var min = 0
                    var max = 4000 * 0.2f
                    var level = (effect.damage - min) / (max - min)

                    level = MathUtils.clamp(level, 0.15f, 1f)
                    var durationLevel = (effect.duration - 0) / (7 - 0) * 2
                    durationLevel = MathUtils.clamp(durationLevel, 0f, 1f)


                    var vfxLoc = Vector2f(loc)
                    loc = loc.plus(MathUtils.getRandomPointInCircle(Vector2f(), 10f * level))

                    Global.getCombatEngine().addNegativeNebulaParticle(loc, Vector2f(), 50f * level * durationLevel,
                        1f, 0.5f, 0f, MathUtils.getRandomNumberInRange(3f * durationLevel, 5f * durationLevel)
                        , RiftLanceEffect.getColorForDarkening(Color(200, 125, 255)));
                }

            }
        }
    }
}