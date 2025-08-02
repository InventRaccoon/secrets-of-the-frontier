package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.hullmods.SotfSierrasConcord.SotfSierraAfterImageScript
import data.scripts.SotfModPlugin
import data.scripts.campaign.ids.SotfIDs
import data.scripts.utils.SotfMisc
import data.shipsystems.scripts.SotfConcordShiftStats
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lwjgl.util.vector.Vector2f
import org.magiclib.subsystems.MagicSubsystem
import org.magiclib.subsystems.MagicSubsystemsManager
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin
import java.awt.Color

class DanceWithMe : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all fighters"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara("Fighters deployed from your fleet receive the \"Concord Shift\" subsystem", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("   - The subsystem moves the fighter into phase space and increases its speed", 0f, Misc.getTextColor(), Misc.getHighlightColor())
        tooltip.addPara("   - It is only active for a very short duration and recharges every 14 seconds", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "very short", "14")
        tooltip.addPara("   - Fighters will phase to avoid fire and to return quickly to their carrier", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "defensive")

        tooltip.addSpacer(10f)

        var label = tooltip.addPara("\"Follow my lead, and the stars will be in awe of us all!\"", SotfMisc.getSierraColor().darker(), 0f)
        label.italicize()
    }



    override fun applyEffectsToFighterSpawnedByShip(data: SCData?, fighter: ShipAPI, ship: ShipAPI?, id: String?) {
        fighter!!.addListener(SotfSierraAfterImageScript(fighter))
        MagicSubsystemsManager.addSubsystemToShip(fighter, PhaseShiftSubsystem(fighter))
    }
}

class PhaseShiftSubsystem(ship: ShipAPI?) : MagicSubsystem(ship) {

    override fun getBaseActiveDuration(): Float {
        return 2.25f
    }

    override fun getBaseCooldownDuration(): Float {
        return 14f
    }

    override fun getBaseInDuration(): Float {
        return 0.2f
    }

    override fun getBaseOutDuration(): Float {
        return 0.25f
    }

    var interval = IntervalUtil(0.01f, 0.15f)

    override fun shouldActivateAI(amount: Float): Boolean {
        if (ship.isLanding) return false
        interval.advance(amount)
        if (interval.intervalElapsed()) {
            return ship.aiFlags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE) || (ship.wing != null && ship.wing.isReturning(ship))
        }
        return false
    }

    override fun getDisplayText(): String {
        return "Concord Shift"
    }

    override fun onActivate() {
        super.onActivate()

        Global.getSoundPlayer().playSound("sotf_system_concordshift_fire", 1.1f, 0.6f, ship.location, Vector2f())

        val p = RiftCascadeMineExplosion.createStandardRiftParams(Color(200, 125, 255, 155),
            ship.shieldRadiusEvenIfNoShield * 1.33f)
        p.fadeOut = 0.15f
        p.hitGlowSizeMult = 0.25f
        p.underglow = Color(255, 175, 255, 50)
        p.withHitGlow = false
        p.noiseMag = 1.25f

        val e = Global.getCombatEngine().addLayeredRenderingPlugin(NegativeExplosionVisual(p))
        e.location.set(ship.location)

        if (SotfModPlugin.GLIB && !ship.variant.hasHullMod(SotfIDs.HULLMOD_SERENITY)) {
            val ripple = RippleDistortion(ship.location, ship.velocity)
            ripple.intensity = ship.collisionRadius * 1.25f
            ripple.size = ship.shieldRadiusEvenIfNoShield * 1.25f
            ripple.fadeInSize(0.15f)
            ripple.fadeOutIntensity(0.5f)
            DistortionShader.addDistortion(ripple)
        }
    }

    override fun advance(amount: Float, isPaused: Boolean) {
        super.advance(amount, isPaused)

        var ship: ShipAPI? = null
        var player = false
        var id = "sotf_phase_shift"
        if (stats.entity is ShipAPI) {
            ship = stats.entity as ShipAPI
            player = ship === Global.getCombatEngine().playerShip
            id = id + "_" + ship!!.id
        } else {
            return
        }

        if (Global.getCombatEngine().isPaused) {
            return
        }

        if (state == State.COOLDOWN || state == State.READY) {
            onFinished()
            return
        }

        val baseSpeedBonus = SotfConcordShiftStats.BASE_FLAT_SPEED_BOOST * 0.5f //A bit lower than standard Concord

        val level = effectLevel

        val jitterLevel = 0f
        val jitterRangeBonus = 0f
        var levelForAlpha = level

        val speedPercentMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(baseSpeedBonus)
        val accelPercentMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(baseSpeedBonus)
        stats.maxSpeed.modifyPercent(id, speedPercentMod * effectLevel)
        stats.maxTurnRate.modifyPercent(id, accelPercentMod * effectLevel)
        stats.turnAcceleration.modifyPercent(id, accelPercentMod * effectLevel)
        stats.acceleration.modifyPercent(id, accelPercentMod * effectLevel)
        stats.deceleration.modifyPercent(id, accelPercentMod * effectLevel)

        val speedMultMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult()
        val accelMultMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult()
        stats.maxSpeed.modifyMult(id, speedMultMod * effectLevel)
        stats.maxTurnRate.modifyPercent(id, accelMultMod * effectLevel)
        stats.turnAcceleration.modifyPercent(id, accelMultMod * effectLevel)
        stats.acceleration.modifyMult(id, accelMultMod * effectLevel)
        stats.deceleration.modifyMult(id, accelMultMod * effectLevel)


        if (state == State.IN || state == State.ACTIVE) {
            ship.isPhased = true
            levelForAlpha = level
        } else if (state == State.OUT) {
            if (level > 0.5f) {
                ship.isPhased = true
            } else {
                ship.isPhased = false
            }
            levelForAlpha = level
        }

        ship.extraAlphaMult = 1f - (1f - SotfConcordShiftStats.SHIP_ALPHA_MULT) * levelForAlpha
        ship.setApplyExtraAlphaToEngines(true)

        val shipTimeMult = 1f + ((SotfConcordShiftStats.getMaxTimeMult(stats) - 1f) * 0.5f) * levelForAlpha
        var perceptionMult = shipTimeMult
        if (player) {
            perceptionMult = 1f + (SotfConcordShiftStats.getMaxTimeMult(stats) - 1f) * 0.65f * levelForAlpha
        }
        stats.timeMult.modifyMult(id, shipTimeMult)
        if (player) {
            Global.getCombatEngine().timeMult.modifyMult(id, 1f / perceptionMult)
        } else {
            Global.getCombatEngine().timeMult.unmodify(id)
        }
    }

    override fun onFinished() {
        super.onFinished()

        var ship: ShipAPI? = null
        var player = false
        var id = "sotf_phase_shift"
        if (stats.entity is ShipAPI) {
            ship = stats.entity as ShipAPI
            player = ship === Global.getCombatEngine().playerShip
            id = id + "_" + ship!!.id
        } else {
            return
        }

        Global.getCombatEngine().timeMult.unmodify(id)
        stats.timeMult.unmodify(id)

        ship!!.isPhased = false
        ship!!.extraAlphaMult = 1f

        stats.maxSpeed.unmodify(id)
        stats.maxTurnRate.unmodify(id)
        stats.turnAcceleration.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)
        stats.shieldUpkeepMult.unmodify(id)
    }

}