// draws a Neutrino Detector visual around a ship, pointing at another one
package data.scripts.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class SotfNeutrinoLockVisualScript extends BaseCombatLayeredRenderingPlugin {

    public static class NeutrinoParams implements Cloneable {
        public ShipAPI ship = null;
        public ShipAPI target = null;
        public float radius = 0f;
        public Color color = new Color(25,215,255);

        public NeutrinoParams() {
        }

        public NeutrinoParams(ShipAPI ship, ShipAPI target) {
            super();
            this.ship = ship;
            this.target = target;
            radius = ship.getShieldRadiusEvenIfNoShield() + 70f;
        }

        @Override
        protected NeutrinoParams clone() {
            try {
                return (NeutrinoParams) super.clone();
            } catch (CloneNotSupportedException e) {
                return null; // should never happen
            }
        }

    }

    protected SpriteAPI texture;
    protected NeutrinoParams p;

    protected float progress;
    protected float phaseAngle;
    protected float ramp;
    protected boolean rampingDown;

    public SotfNeutrinoLockVisualScript(NeutrinoParams p) {
        this.p = p;
    }

    public float getRenderRadius() {
        return p.radius + 500f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (p.ship == null || p.target == null) {
            return;
        } else if (!p.ship.isAlive() || !p.target.isAlive()) {
            return;
        }

        if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {

            float level = ramp;
            if (level <= 0) return;

            float bandWidthInTexture = 256;
            float bandIndex;

            float radStart = p.radius;
            float radEnd = radStart + 150f;

            float circ = (float) (Math.PI * 2f * (radStart + radEnd) / 2f);
            //float pixelsPerSegment = 10f;
            float pixelsPerSegment = circ / 360f;
            //float pixelsPerSegment = circ / 720;
            float segments = Math.round(circ / pixelsPerSegment);

//		segments = 360;
//		pixelsPerSegment = circ / segments;
            //pixelsPerSegment = 10f;

            float startRad = (float) Math.toRadians(0);
            float endRad = (float) Math.toRadians(360f);
            float spanRad = Math.abs(endRad - startRad);
            float anglePerSegment = spanRad / segments;

            Vector2f loc = p.ship.getLocation();
            float x = loc.x;
            float y = loc.y;


            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0);

            //float zoom = viewport.getViewMult();
            //GL11.glScalef(zoom, zoom, 1);

            GL11.glEnable(GL11.GL_TEXTURE_2D);

            if (texture == null) texture = Global.getSettings().getSprite("abilities", "neutrino_detector");
            texture.bindTexture();

            GL11.glEnable(GL11.GL_BLEND);
            //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            boolean outlineMode = false;
            //outlineMode = true;
            if (outlineMode) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
                //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            }

            float thickness = (radEnd - radStart) * 1f;
            float radius = radStart;

            float texProgress = 0f;
            float texHeight = texture.getTextureHeight();
            float imageHeight = texture.getHeight();
            float texPerSegment = pixelsPerSegment * texHeight / imageHeight * bandWidthInTexture / thickness;

            texPerSegment *= 1f;

            float totalTex = Math.max(1f, Math.round(texPerSegment * segments));
            texPerSegment = totalTex / segments;

            float texWidth = texture.getTextureWidth();
            float imageWidth = texture.getWidth();



            Color color = p.color;

            for (int iter = 0; iter < 2; iter++) {
                if (iter == 0) {
                    bandIndex = 1;
                } else {
                    //color = new Color(255,215,25,255);
                    //color = new Color(25,255,215,255);
                    bandIndex = 0;
                    texProgress = segments/2f * texPerSegment;
                    //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                }
                if (iter == 1) {
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                }
                //bandIndex = 1;

                float leftTX = (float) bandIndex * texWidth * bandWidthInTexture / imageWidth;
                float rightTX = (float) (bandIndex + 1f) * texWidth * bandWidthInTexture / imageWidth - 0.001f;

                GL11.glBegin(GL11.GL_QUAD_STRIP);
                for (float i = 0; i < segments + 1; i++) {

                    float segIndex = i % (int) segments;

                    //float phaseAngleRad = (float) Math.toRadians(phaseAngle + segIndex * 10) + (segIndex * anglePerSegment * 10f);
                    float phaseAngleRad;
                    if (iter == 0) {
                        phaseAngleRad = (float) Math.toRadians(phaseAngle) + (segIndex * anglePerSegment * 29f);
                    } else { //if (iter == 1) {
                        phaseAngleRad = (float) Math.toRadians(-phaseAngle) + (segIndex * anglePerSegment * 17f);
                    }


                    float angle = (float) Math.toDegrees(segIndex * anglePerSegment);
                    //if (iter == 1) angle += 180;


                    float pulseSin = (float) Math.sin(phaseAngleRad);
                    float pulseMax = thickness * 0.5f;

                    pulseMax = thickness * 0.2f;
                    pulseMax = 20f;

                    //pulseMax *= 0.25f + 0.75f * noiseLevel;

                    float pulseAmount = pulseSin * pulseMax;
                    //float pulseInner = pulseAmount * 0.1f;
                    float pulseInner = pulseAmount * 0.1f;

                    float r = radius;

                    float theta = anglePerSegment * segIndex;;
                    float cos = (float) Math.cos(theta);
                    float sin = (float) Math.sin(theta);

                    float rInner = r - pulseInner;
                    //if (rInner < r * 0.9f) rInner = r * 0.9f;

                    //float rOuter = (r + thickness * thicknessMult - pulseAmount + thicknessFlat);
                    float rOuter = r + thickness - pulseAmount;


                    //rOuter += noiseLevel * 25f;

                    float angleBetween = Misc.getAngleInDegreesStrict(p.ship.getLocation(), p.target.getLocation());

                    float grav = 0f;
                    if (Misc.isInArc(angle, anglePerSegment, p.ship.getLocation(), p.target.getLocation())) {
                        grav = 750f;
                    }

                    if (grav > 750) grav = 750;
                    grav *= 250f / 750f;
                    grav *= level;
                    rOuter += grav;

                    float alphaMult = 1f;
                    float alpha = alphaMult;
                    alpha *= 0.25f + Math.min(grav / 100, 0.75f);
                    //alpha *= 0.75f;

                    float x1 = cos * rInner;
                    float y1 = sin * rInner;
                    float x2 = cos * rOuter;
                    float y2 = sin * rOuter;

                    x2 += (float) (Math.cos(phaseAngleRad) * pixelsPerSegment * 0.33f);
                    y2 += (float) (Math.sin(phaseAngleRad) * pixelsPerSegment * 0.33f);


                    GL11.glColor4ub((byte)color.getRed(),
                            (byte)color.getGreen(),
                            (byte)color.getBlue(),
                            (byte)((float) color.getAlpha() * alphaMult * alpha));

                    GL11.glTexCoord2f(leftTX, texProgress);
                    GL11.glVertex2f(x1, y1);
                    GL11.glTexCoord2f(rightTX, texProgress);
                    GL11.glVertex2f(x2, y2);

                    texProgress += texPerSegment * 1f;
                }
                GL11.glEnd();

                //GL11.glRotatef(180, 0, 0, 1);
            }
            GL11.glPopMatrix();

            if (outlineMode) {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            }
        }
    }

    public void init(CombatEntityAPI entity) {
        super.init(entity);
    }

    public void advance(float amount) {
        if (p.ship != null) {
            entity.getLocation().set(p.ship.getLocation().x, p.ship.getLocation().y);
        }
        progress += amount;
        if (progress >= 4f) {
            rampingDown = true;
        }
        if (ramp < 1f && !rampingDown) {
            ramp += amount;
        } else if (ramp > 0f && rampingDown) {
            ramp -= amount;
        }
        phaseAngle += amount * 360f;
        phaseAngle = Misc.normalizeAngle(phaseAngle);
    }

    public boolean isExpired() {
        return p.ship == null || ramp < 0f;
    }

}
