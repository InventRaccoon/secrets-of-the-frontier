package data.scripts.campaign.secondInCommand.misc

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.combat.listeners.DamageListener
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.utils.SotfMisc
import org.dark.shaders.util.ShaderLib
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.minus
import org.lazywizard.lazylib.ext.plus
import org.lazywizard.lazylib.ui.LazyFont
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.cleanBuffer
import org.magiclib.kotlin.setAlpha
import org.magiclib.util.MagicUI
import java.awt.Color
import java.nio.FloatBuffer
import java.util.*
import kotlin.collections.ArrayList


class SpiritWardListener(var ship: ShipAPI, var sierraMode: Boolean) : BaseEveryFrameCombatPlugin() {

    var text: LazyFont.DrawableString
    var uiColor = Misc.getPositiveHighlightColor()

    var appliedShield = false

    var converter = SpiritWardDamageConverter(this)

    init {

        ship.addListener(SpiritWardDamageModifier(this))
        ship.addListener(converter)
        Global.getCombatEngine().addLayeredRenderingPlugin(SpiritWardRenderer(ship, this))

        val font = LazyFont.loadFont("graphics/fonts/victor14.fnt")
        text = font.createText()
        text.baseColor = uiColor

        ship.setCustomData("sotf_spirit_ward_listener", this)
    }

    companion object {
        var shieldEfficiency = 0.5f
        var shieldPercent = 0.2f
        var timeTilRechargeMax = 10f

        var priorDamageInstancesMax = 16
    }

    var maxShieldHP = ship.fluxTracker.maxFlux * shieldPercent
    var regenerationRate = maxShieldHP / 5

    var timeTilRecharge = 0f

    var shieldHP = maxShieldHP

    var effectLevel = 1f
    var mostRecentDamage: Float? = null
    var mostRecentDamageHardflux: Boolean? = null
    var mostRecentDamagePoint: Vector2f? = null

    data class SpiritPriorDamage(var level: Float, var radius: Float, var duration: Float, var offset: Vector2f) {
        var maxDuration = duration
    }

    var priorDamage = ArrayList<SpiritPriorDamage>()

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {


        if (!Global.getCombatEngine().isPaused) {
            for (prior in ArrayList(priorDamage)) {

                prior.duration -= 1 * amount
                if (prior.duration <= 0) {
                    priorDamage.remove(prior)
                    continue
                }
            }
        }


        if (!appliedShield) {
            appliedShield = true
            ship.setShield(ShieldAPI.ShieldType.FRONT, 0f, shieldEfficiency, 360f) //may otherwise crash some onhits
        }

        if (!ship.isAlive) return

        ship.aiFlags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS, 999f)

        if (ship.shield?.isOn == true) {
            ship.shield?.toggleOff()
        }

        var cloakLevel = ship.phaseCloak.effectLevel

        /*if (ship.fluxTracker.isVenting && !Global.getCombatEngine().isPaused) {
            shieldHP -= regenerationRate * amount * 2 //Punish Venting
        }*/

        if (!Global.getCombatEngine().isPaused) {
            timeTilRecharge -= 1 * amount * ship.mutableStats.timeMult.modifiedValue

            if (timeTilRecharge <= 0) {
                var regen = regenerationRate * amount * ship.mutableStats.timeMult.modifiedValue
                shieldHP += regen
            }
        }


        shieldHP = MathUtils.clamp(shieldHP, 0f, maxShieldHP)

        var shieldLevel = shieldHP / maxShieldHP
        shieldLevel = MathUtils.clamp(shieldLevel, 0f, 1f)



        if (ship.isPhased) {
            effectLevel -= 3f * amount
        } else {
            effectLevel += 1 * amount
        }
        effectLevel = MathUtils.clamp(effectLevel, 0f, 1f)

        //var renderLevel = shieldHP.levelBetween(-100f, maxShieldHP*0.2f)
        var renderLevel = (shieldHP - 100f) / (maxShieldHP * 0.2f - 100f)
        renderLevel = MathUtils.clamp(renderLevel, 0.7f, 1f)
        if (shieldHP <= 0) renderLevel = 0f

        //var colorShiftLevel = shieldLevel * shieldLevel * shieldLevel
        var color = Misc.interpolateColor(SotfMisc.getSierraColor(), SotfMisc.SIERRA_COLOR.darker().darker(), 0f + ((1f-shieldLevel) * 0.4f))

       /* ship.setJitter(this, color, 0.1f * effectLevel * renderLevel, 3, 0f, 0 + 2f)
        ship.setJitterUnder(this,  color, 0.5f * effectLevel * renderLevel, 25, 0f, 7f + 2)*/

        if (shieldHP > 0.1) {
            ship.mutableStats.armorDamageTakenMult.modifyMult("sotf_spirit_ward", 0.00001f)
            ship.mutableStats.hullDamageTakenMult.modifyMult("sotf_spirit_ward", 0.00001f)
            ship.mutableStats.empDamageTakenMult.modifyMult("sotf_spirit_ward", 0.00001f)
            ship.mutableStats.weaponDamageTakenMult.modifyMult("sotf_spirit_ward", 0.00001f)
            ship.mutableStats.engineDamageTakenMult.modifyMult("sotf_spirit_ward", 0.00001f)
        } else {
            ship.mutableStats.armorDamageTakenMult.unmodify("sotf_spirit_ward")
            ship.mutableStats.hullDamageTakenMult.unmodify("sotf_spirit_ward")
            ship.mutableStats.empDamageTakenMult.unmodify("sotf_spirit_ward")
            ship.mutableStats.weaponDamageTakenMult.unmodify("sotf_spirit_ward")
            ship.mutableStats.engineDamageTakenMult.unmodify("sotf_spirit_ward")
        }

        //MagicUI.drawInterfaceStatusBar(ship, shieldLevel, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(), 1f, "Shield", shieldHP.toInt())


    }

    fun getShieldLevel() = MathUtils.clamp(shieldHP / maxShieldHP, 0f, 1f)


    override fun renderInUICoords(viewport: ViewportAPI?) {
        super.renderInUICoords(viewport)

        if (ship != Global.getCombatEngine().playerShip) return
        if (!Global.getCombatEngine().isUIShowingHUD || Global.getCombatEngine().combatUI?.isShowingCommandUI == true) return


        var scale = Global.getSettings().screenScaleMult

        var loc = MagicUI.getInterfaceOffsetFromStatusBars(ship, ship.variant)
        var x = (loc.x + 176f)
        var y = (loc.y + 130f)

        //var x = 232f
        //var y = 200f

        var shadowC = Color(0,0,0)

        uiColor = Misc.getPositiveHighlightColor()
        if (ship.owner == 1) uiColor = Misc.getNegativeHighlightColor()
        if (ship.owner == 100) uiColor = Misc.getDarkPlayerColor()

        uiColor = SotfMisc.SIERRA_COLOR

        //Text Shadow
        text.baseColor = Color(0, 0, 0)
        text.text = "Ward"
        text.draw(x+1, y- 1)

        //Text Draw
        text.baseColor = uiColor
        text.text = "Ward"
        text.draw(x, y)


        var barX = x + 48f
        var barY = y - 3f

        var barW = 80f
        var barH = 7f

        text.baseColor = Color(0, 0, 0)
        text.text = "${shieldHP.toInt()}"
        text.draw(x + 180 - text.width + 1, y - 1)

        text.baseColor = uiColor
        text.text = "${shieldHP.toInt()}"
        text.draw(x + 180 - text.width, y)

        var barLevel = getShieldLevel()

        var indicatorWidthMin = barX + (barW * barLevel) - 1
        var indicatorWidthMax = barX + (barW * barLevel) + 1
        indicatorWidthMin = MathUtils.clamp(indicatorWidthMin, barX, barX + barW)
        indicatorWidthMax = MathUtils.clamp(indicatorWidthMax, barX, barX + barW)

        if (barW * barLevel <= 2) {
            //Start Top Shadow
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)

            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(shadowC.red / 255f,
                shadowC.green / 255f,
                shadowC.blue / 255f,
                shadowC.alpha / 255f)

            GL11.glRectf(barX + 1, barY - 1 , barX + 3 + 1, barY)

            GL11.glPopMatrix()

            //Start Top
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)

            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(uiColor.red / 255f,
                uiColor.green / 255f,
                uiColor.blue / 255f,
                uiColor.alpha / 255f)

            GL11.glRectf(barX, barY - 1 + 1, barX + 3, barY + 1)

            GL11.glPopMatrix()





            //Start Bottom Shadow
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)

            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(shadowC.red / 255f,
                shadowC.green / 255f,
                shadowC.blue / 255f,
                shadowC.alpha / 255f)

            GL11.glRectf(barX + 1, barY - 1 - barH -1, barX + 3 + 1, barY - barH - 1)

            GL11.glPopMatrix()


            //Start Bottom
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)

            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(uiColor.red / 255f,
                uiColor.green / 255f,
                uiColor.blue / 255f,
                uiColor.alpha / 255f)

            GL11.glRectf(barX, barY - 1 - barH, barX + 3, barY - barH )

            GL11.glPopMatrix()
        }






        //End Shadow
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)

        GL11.glDisable(GL11.GL_BLEND)

        GL11.glColor4f(shadowC.red / 255f,
            shadowC.green / 255f,
            shadowC.blue / 255f,
            shadowC.alpha / 255f)

        GL11.glRectf(barX + barW - 1 + 1, barY - barH - 1, barX + barW + 1, barY - 1)

        GL11.glPopMatrix()

        //End
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)

        GL11.glDisable(GL11.GL_BLEND)

        GL11.glColor4f(uiColor.red / 255f,
            uiColor.green / 255f,
            uiColor.blue / 255f,
            uiColor.alpha / 255f)

        GL11.glRectf(barX + barW - 1, barY - barH, barX + barW, barY)

        GL11.glPopMatrix()


        if (barLevel > 0f) {
            //Bar Shadow
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)

            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(shadowC.red / 255f,
                shadowC.green / 255f,
                shadowC.blue / 255f,
                shadowC.alpha / 255f)

            GL11.glRectf(barX + 1, barY - barH - 1 , barX + ((barW + 1) * barLevel), barY - 1 )

            GL11.glPopMatrix()

            //Bar

            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)

            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(uiColor.red / 255f,
                uiColor.green / 255f,
                uiColor.blue / 255f,
                uiColor.alpha / 255f)

            GL11.glRectf(barX, barY - barH , barX + (barW * barLevel), barY )


            //Indicator Shadow
            GL11.glPopMatrix()

            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)

            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(shadowC.red / 255f,
                shadowC.green / 255f,
                shadowC.blue / 255f,
                shadowC.alpha / 255f)


            GL11.glRectf(indicatorWidthMin + 1, barY - barH - 1 -1 , indicatorWidthMax + 1, barY + 1 -1)


            GL11.glPopMatrix()

            //Indicator
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)

            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(uiColor.red / 255f,
                uiColor.green / 255f,
                uiColor.blue / 255f,
                uiColor.alpha / 255f)

            GL11.glRectf(indicatorWidthMin, barY - barH - 1 , indicatorWidthMax, barY + 1 )

            GL11.glPopMatrix()

        }


    }

}

class SpiritWardRenderer(var ship: ShipAPI, var listener: SpiritWardListener) : BaseCombatLayeredRenderingPlugin() {

    //var sprite = ship.spriteAPI
    var noise1 = Global.getSettings().getSprite("graphics/fx/sotf_spirit_ward_noise1.png")
    var noise2 = Global.getSettings().getSprite("graphics/fx/sotf_spirit_ward_noise2.png")
    var damage = Global.getSettings().getSprite("graphics/fx/sotf_spirit_ward_damage.png")

    var shader: Int = 0

    init {
        if (shader == 0) {
            shader = ShaderLib.loadShader(
                Global.getSettings().loadText("data/shaders/sotf_baseVertex.shader"),
                Global.getSettings().loadText("data/shaders/sotf_spirit_ward.shader"))
            if (shader != 0) {
                GL20.glUseProgram(shader)

                GL20.glUniform1i(GL20.glGetUniformLocation(shader, "tex"), 0)
                GL20.glUniform1i(GL20.glGetUniformLocation(shader, "noiseTex1"), 1)
                GL20.glUniform1i(GL20.glGetUniformLocation(shader, "noiseTex2"), 2)
                GL20.glUniform1i(GL20.glGetUniformLocation(shader, "damageTex"), 3)

                GL20.glUseProgram(0)
            } else {
                var test = ""
            }
        }
    }

    override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER)
    }

    override fun getRenderRadius(): Float {
        return 1000000f
    }

    override fun advance(amount: Float) {

    }


    override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {

        if (!ship.isAlive) return



        var phaseLevel = 1-(ship.phaseCloak.effectLevel * ship.phaseCloak.effectLevel * ship.phaseCloak.effectLevel * ship.phaseCloak.effectLevel)
        if (ship.phaseCloak.state != ShipSystemAPI.SystemState.IN) {
            phaseLevel = listener.effectLevel
        }
        phaseLevel = MathUtils.clamp(phaseLevel, 0.33f, 1f)

        //var renderLevel = listener.shieldHP.levelBetween(-100f, PhaseshiftShieldListener.maxShieldHP *0.2f)
        var renderLevel = (listener.shieldHP - 100f) / (listener.maxShieldHP * 0.2f - 100f)
        renderLevel = MathUtils.clamp(renderLevel, 0.7f, 1f)
        if (listener.shieldHP <= 0) renderLevel = 0f



        var sprite = ship.spriteAPI
        if (sprite != null) {
            renderGlow(sprite, ship.location, ship.facing, 1f * renderLevel * phaseLevel, 1f, true)
        }






        //Slight Deco Support, maybe just do it for all weapons
        for (weapon in ship.allWeapons) {
            if (weapon.slot.isHidden) continue
            if (weapon.slot.isSystemSlot) continue
            if (weapon.sprite == null) continue
            if (weapon.sprite.width <= 0f) continue
            if (weapon.sprite.height <= 0f) continue
            renderGlow(weapon.sprite, weapon.location, weapon.currAngle, 1f * renderLevel * phaseLevel, 1f, false)
        }

    }

    fun reRenderDeco(sprite: SpriteAPI, loc: Vector2f, angle: Float, alpha: Float) {
        sprite.setNormalBlend()
        sprite.alphaMult = alpha
        sprite.angle = angle - 90f
        //sprite.setSize(width, h -20f)
        sprite.renderAtCenter(loc.x, loc.y)
    }

    var floatBuffer = BufferUtils.createFloatBuffer(SpiritWardListener.priorDamageInstancesMax * 4)

    fun renderGlow(sprite: SpriteAPI, loc: Vector2f, angle: Float, alpha: Float, intensity: Float, renderDamage: Boolean) {

        var level = listener.getShieldLevel()

        //level = 1f - ((1f-level) * 0.4f)

        level = (level - 0.0f) / (0.2f - 0f)
        level = MathUtils.clamp(level, 0.25f, 1f)


        GL20.glUseProgram(shader)

       /* var prior = ArrayList<SpiritWardListener.SpiritPriorDamage>()
        prior.add(SpiritWardListener.SpiritPriorDamage(1f, 100f, Vector2f(sprite.width/2, sprite.height/2)))*/


        var viewport = Global.getCombatEngine().viewport


        //var shieldColor = SotfMisc.getSierraColor()
        //var shieldColor = Color(145,125,255)
        var shieldColor = ship.hullSpec.shieldSpec.innerColor.setAlpha(75)

        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "iTime"), Global.getCombatEngine().getTotalElapsedTime(false) / 32f)
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "alphaMult"),  alpha)
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "shieldLevel"),  level)
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "intensity"),  intensity)
        GL20.glUniform3f(GL20.glGetUniformLocation(shader, "shieldColor"),  shieldColor.red / 255f, shieldColor.green / 255f, shieldColor.blue / 255f)
        GL20.glUniform2f(GL20.glGetUniformLocation(shader, "size"),  sprite.textureWidth, sprite.textureHeight)



        floatBuffer.clear()

        if (renderDamage) {

            //for (prior in listener.priorDamage) {
            for (i in 0 until SpiritWardListener.priorDamageInstancesMax) {

                var prior = listener.priorDamage.getOrNull(i)

                var offset = i /** 4*/

                if (prior != null) {
                    var uvRadius = ShaderLib.unitsToUV(prior.radius) * viewport.viewMult

                    var locTest = prior.offset.plus(Vector2f(ship.spriteAPI.width/2, ship.spriteAPI.height/2))

                    var loc = Vector2f(prior.offset)
                    loc = Misc.rotateAroundOrigin(loc, 90f)
                    //loc = Misc.rotateAroundOrigin(loc, ship.facing)
                    loc = loc.plus(Vector2f(ship.spriteAPI.width/2, ship.spriteAPI.height/2))

                    /*var size = Vector2f(sprite.width, sprite.height)
                    size = Misc.rotateAroundOrigin(size, ship.facing-90)*/

                    /*var loc = Vector2f(prior.offset)
                    loc = Misc.rotateAroundOrigin(loc, ship.facing)
                    Vector2f.add(ship.location, loc, loc)*/

                    var locUv = Vector2f(loc.x / sprite.width, loc.y / sprite.height)
                    //locUv = Misc.rotateAroundOrigin(locUv, ship.facing)
                    //locUv = Misc.rotateAroundOrigin(locUv, 45f)


                    var level = prior.level
                    level *= prior.duration / prior.maxDuration

                    floatBuffer.put(floatArrayOf(locUv.x, locUv.y, uvRadius, level))
                } else{
                    floatBuffer.put(floatArrayOf(0f, 0f, 0f, 0f))
                }


            }

            /*var uvRadius = ShaderLib.unitsToUV(600f) * viewport.viewMult
            floatBuffer.put(floatArrayOf(0.25f, 0.5f, uvRadius, 1f))

            uvRadius = ShaderLib.unitsToUV(600f) * viewport.viewMult
            floatBuffer.put(floatArrayOf(0.7f, 0.25f, uvRadius, 0.75f))*/

        }

        floatBuffer.flip()

        GL20.glUniform4(GL20.glGetUniformLocation(shader, "damageInfo"), floatBuffer);



        //Bind Texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.textureId)

        //Setup Noise1
        //Noise texture needs to be power of two or it wont repeat correctly! (32x32, 64x64, 128x128)
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 1)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, noise1!!.textureId)

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)

        //Setup Noise2, actually just uses Noise 1 for now
        //Noise texture needs to be power of two or it wont repeat correctly! (32x32, 64x64, 128x128)
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, noise1!!.textureId)

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)


        //Setup Damage
        //Noise texture needs to be power of two or it wont repeat correctly! (32x32, 64x64, 128x128)
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 3)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, damage!!.textureId)

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)



        //Reset Texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0)

        var width = sprite.width
        var height = sprite.width


        sprite.setAdditiveBlend()
        sprite.alphaMult = 1f
        sprite.angle = angle - 90f
        //sprite.setSize(width, h -20f)
        sprite.renderAtCenter(loc.x, loc.y)

        sprite.setNormalBlend()


        GL20.glUseProgram(0)
    }

}

class SpiritWardDamageConverter(var listener: SpiritWardListener) : DamageListener {
    override fun reportDamageApplied(source: Any?, target: CombatEntityAPI?, result: ApplyDamageResultAPI?) {

        if (!listener.ship.isAlive) return

        var recent = listener.mostRecentDamage ?: return
        var hardflux = listener.mostRecentDamageHardflux ?: return
        var point = listener.mostRecentDamagePoint ?: return

        var damage = recent * SpiritWardListener.shieldEfficiency

        listener.timeTilRecharge = SpiritWardListener.timeTilRechargeMax

        var active = false
        //Check if Shield is active
        if (listener.shieldHP > 0.1) {
            active = true
        }

        if (active) {

            //Apply Damage
            var ship = listener.ship
            var tracker = ship.fluxTracker

            tracker.increaseFlux(damage, hardflux)

            listener.shieldHP -= damage
            listener.shieldHP = MathUtils.clamp(listener.shieldHP, 0f, listener.maxShieldHP)

            //Spawn Distortions if the damage was significant


            //Ensure onhits are triggered as shield hits
            result?.damageToShields = damage

            if (listener.shieldHP <= 0) {
                Global.getSoundPlayer().playSound("sotf_spirit_ward_burnout", 0.65f + MathUtils.getRandomNumberInRange(-0.2f, 0.2f), 1.3f, ship.location, ship.velocity)

                GraphicLibEffects.CustomRippleDistortion(ship.location,
                    Vector2f(),
                    ship.collisionRadius + 200f,
                    3f,
                    false,
                    0f,
                    360f,
                    1f,
                    0f,0f,3f,
                    0.3f,0f
                )
            }


            //var rippleLevel = damage.levelBetween(100f, 400f)
            var rippleLevel = (damage - 100f) / (400f - 100f)

            if (rippleLevel >= 0.05) {
                GraphicLibEffects.CustomRippleDistortion(point,
                    Vector2f(),
                    300f * rippleLevel,
                    2f,
                    false,
                    0f,
                    360f,
                    1f,
                    0f,0f,0.6f,
                    0.3f,0f
                )
            }



            if (damage > 0) {
                //Prior Damage for Shield DMG VFX

                var offset = Vector2f.sub(point, ship.location, Vector2f())
                offset = Misc.rotateAroundOrigin(offset, -ship.facing)

                var min = 0f
                var max = 300f

                var level = (damage - min) / (max - min)
                //level = MathUtils.clamp(level, 0f, 1f)
                level = MathUtils.clamp(level, 0.4f, 1f)

                var radius = 900 * level
                var duration = MathUtils.getRandomNumberInRange(4f, 5f) * level

                var prior = SpiritWardListener.SpiritPriorDamage(level, radius, duration, offset)

                listener.priorDamage.add(prior)

                while (listener.priorDamage.size >= SpiritWardListener.priorDamageInstancesMax) {
                    var smallest = listener.priorDamage.sortedBy { it.level }.first()
                    listener.priorDamage.remove(smallest)
                }
            }









        }

        listener.mostRecentDamage = null
        listener.mostRecentDamageHardflux = null
        listener.mostRecentDamagePoint = null

    }
}

class SpiritWardDamageModifier(var listener: SpiritWardListener) : DamageTakenModifier {

    override fun modifyDamageTaken(param: Any?, target: CombatEntityAPI?, damage: DamageAPI?, point: Vector2f?,
                                   shieldHit: Boolean): String? {

        if (!listener.ship.isAlive) return null

        //Transfer Damage to next listener
        var dam = damage!!.damage
        if (param is BeamAPI) {
            dam = damage.damage * damage.dpsDuration
        }

        dam *= damage.type.shieldMult //Since this value is only used if the shield is active, apply the shield mult in this place already.

        listener.mostRecentDamage = dam
        listener.mostRecentDamageHardflux = !damage.isSoftFlux || damage.isForceHardFlux
        listener.mostRecentDamagePoint = point

        return null
    }

}