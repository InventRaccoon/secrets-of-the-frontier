package data.scripts.campaign.customstart

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import data.scripts.campaign.misc.PausedTimeAdvancer
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.dark.shaders.util.ShaderLib
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import java.util.*

class HauntedDreamCampaignVFX : LunaCampaignRenderingPlugin {


    companion object {

        @JvmStatic
        fun getInstance() : HauntedDreamCampaignVFX {
            var renderer = LunaCampaignRenderer.getRendererOfClass(HauntedDreamCampaignVFX::class.java) as HauntedDreamCampaignVFX?
            if (renderer == null) {
                renderer = HauntedDreamCampaignVFX()
                LunaCampaignRenderer.addRenderer(renderer)
            }
            return renderer
        }

        @JvmStatic
        fun fadeIn(durationDays: Float) {
            var renderer = getInstance()

            renderer.startDelay = 0.05f

            renderer.maxDuration = durationDays
            renderer.duration = 0f
            renderer.isFadeIn = true
        }

        @JvmStatic
        fun fadeOut(durationDays: Float) {
            var renderer = getInstance()
            renderer.maxDuration = durationDays
            renderer.duration = durationDays
            renderer.isFadeIn = false
        }

    }

    @Transient
    var noise1: SpriteAPI? = Global.getSettings().getSprite("graphics/fx/sotf_haunted_dream_noise1.png")

    @Transient
    var shader: Int? = 0

    var layers = EnumSet.of(CampaignEngineLayers.ABOVE)

    var maxDuration = 10f
    var duration = 0f;

    var startDelay = 0.05f

    var isFadeIn = true

    init {

    }

    override fun isExpired(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        if (!Global.getSector().isPaused) {

            if (startDelay >= 0) {
                startDelay -= Global.getSector().clock.convertToDays(amount)
                return
            }

            if (isFadeIn) {
                duration += Global.getSector().clock.convertToDays(amount)
            } else {
                duration -= Global.getSector().clock.convertToDays(amount)
                if (duration <= 0) {
                    LunaCampaignRenderer.removeRenderer(this)
                }
            }
        }
    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers> {
        return layers
    }

    override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI?) {


        if (shader == null || shader == 0) {
            shader = ShaderLib.loadShader(
                Global.getSettings().loadText("data/shaders/sotf_baseVertex.shader"),
                Global.getSettings().loadText("data/shaders/sotf_haunted_dream.shader"))
            if (shader != 0) {
                GL20.glUseProgram(shader!!)

                GL20.glUniform1i(GL20.glGetUniformLocation(shader!!, "tex"), 0)
                GL20.glUniform1i(GL20.glGetUniformLocation(shader!!, "noiseTex1"), 1)

                GL20.glUseProgram(0)
            } else {
                var test = ""
            }
        }

        if (noise1 == null) {
            noise1 = Global.getSettings().getSprite("graphics/fx/sotf_haunted_dream_noise1.png")
        }


        if (layer == CampaignEngineLayers.ABOVE) {
            var playerfleet = Global.getSector().playerFleet


            //Screen texture can be unloaded if graphicslib shaders are disabled, causing a blackscreen
            if (ShaderLib.getScreenTexture() != 0) {
                //Shader
                var clock = Global.getSector().clock
                //var t = clock.convertToSeconds(clock.elapsedDaysSinceGameStart()) / 5f
                var t = PausedTimeAdvancer.time / 5f

                var level = duration / maxDuration
                level = MathUtils.clamp(level, 0f, 1f)

                ShaderLib.beginDraw(shader!!);
                GL20.glUniform1f(GL20.glGetUniformLocation(shader!!, "level"), level)
                GL20.glUniform1f(GL20.glGetUniformLocation(shader!!, "iTime"), t / 8f)
                GL20.glUniform3f(GL20.glGetUniformLocation(shader!!, "colorMult"), 1f, 1f, 1f)

                GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, ShaderLib.getScreenTexture());

                //Noise1
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + 1)
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, noise1!!.textureId)

                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)

                //Reset Texture
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0)

                //Might Fix Incompatibilities with odd drivers
                GL20.glValidateProgram(shader!!)
                if (GL20.glGetProgrami(shader!!, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
                    ShaderLib.exitDraw()
                    return
                }

                GL11.glDisable(GL11.GL_BLEND);
                ShaderLib.screenDraw(ShaderLib.getScreenTexture(), GL13.GL_TEXTURE0 + 0)
                ShaderLib.exitDraw()

            }
        }
    }
}