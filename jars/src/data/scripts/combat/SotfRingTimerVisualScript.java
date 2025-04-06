// more extensive version of AuraVisualScript - modified to create curved timers
// can also be used for auras and etc
package data.scripts.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.EnumSet;

public class SotfRingTimerVisualScript extends BaseCombatLayeredRenderingPlugin {

    public static class AuraParams implements Cloneable {
        //public float fadeIn = 0.25f;
        public float radius = 20f;
        public float thickness = 20f;
        public float baseAlpha = 1f;
        public float playerAlpha = 1f;
        public float allyAlpha = 0.5f;
        public float enemyAlpha = 0.5f;
        public float totalArc = 1f;
        public float maxArc = 360f;
        public float degreeOffset = 0f;
        public CombatEngineLayers layer = CombatEngineLayers.BELOW_SHIPS_LAYER;
        public boolean hardTexture = false;
        public boolean followFacing = false;
        public boolean followsPlayerShip = false;
        public boolean reverseRing = false;
        public boolean renderDarkerCopy = false;
        public float darkerColorAlpha = 1f;
        public ShipAPI ship = null;
        public Color color = new Color(100,100,255);

        public AuraParams() {
        }

        public AuraParams(ShipAPI ship, float radius, float thickness, Color color) {
            super();
            this.radius = radius;
            this.thickness = thickness;
            this.color = color;
            this.ship = ship;
        }

        @Override
        protected AuraParams clone() {
            try {
                return (AuraParams) super.clone();
            } catch (CloneNotSupportedException e) {
                return null; // should never happen
            }
        }

    }

    protected SpriteAPI atmosphereTex;
    public AuraParams p;
    protected int segments;

    public SotfRingTimerVisualScript(AuraParams p) {
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
        if (p.ship == null) {
            return;
        } else if (!p.ship.isAlive()) {
            return;
        }
        ShipAPI shipToFollow = p.ship;
        if (p.followsPlayerShip) {
            shipToFollow = Global.getCombatEngine().getPlayerShip();
        }
        float x = shipToFollow.getLocation().x;
        float y = shipToFollow.getLocation().y;

        float r = p.radius;
        float tSmall = p.thickness;
        float a = 1f;
        if (p.ship.getOwner() == 0) {
            if (p.ship.equals(Global.getCombatEngine().getPlayerShip())) {
                a = p.playerAlpha;
            } else {
                a = p.allyAlpha;
            }
        } else {
            a = p.enemyAlpha;
        }
        a *= p.baseAlpha;
        if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
            //renderCircle(x, y, r, 1f, segments, p.color);
            renderRing(x, y, r, p.thickness, a, segments, atmosphereTex, p.color, false, false);
            if (p.renderDarkerCopy) {
                Color darkerColor = p.color.darker().darker();
                renderRing(x, y, r, p.thickness, a, segments, atmosphereTex, Misc.setAlpha(darkerColor, Math.round(darkerColor.getAlpha() * p.darkerColorAlpha)), false, true);
            }
        }
    }

    public void init(CombatEntityAPI entity) {
        super.init(entity);

        atmosphereTex = Global.getSettings().getSprite("combat", "corona_soft");
        if (p.hardTexture) {
            atmosphereTex = Global.getSettings().getSprite("combat", "corona_hard");
        }

        float perSegment = 2f;
        segments = (int) ((p.radius * 2f * 3.14f) / perSegment);
        if (segments < 8) segments = 8;
    }

    public void advance(float amount) {
        if (p.ship != null) {
            if (p.followsPlayerShip && Global.getCombatEngine().getPlayerShip() != null) {
                entity.getLocation().set(Global.getCombatEngine().getPlayerShip().getLocation().x, Global.getCombatEngine().getPlayerShip().getLocation().y);
            } else {
                entity.getLocation().set(p.ship.getLocation().x, p.ship.getLocation().y);
            }
        }
    }

    private void renderRing(float x, float y, float radius, float thickness, float alphaMult, int segments, SpriteAPI tex, Color color, boolean additive, boolean alwaysFull) {

        float startDeg = 0f;
        float totalArc = p.totalArc;
        if (alwaysFull) {
            totalArc = 1f;
        }
        float endDeg = totalArc * p.maxArc;

        float degreeOffset = p.degreeOffset;
        if (p.reverseRing) {
            degreeOffset *= -1;
        }
        if (p.followFacing) {
            degreeOffset += p.ship.getFacing() - 90f;
        }

        float startRad = (float) Math.toRadians(startDeg);
        float endRad = (float) Math.toRadians(endDeg);
        float offset = (float) Math.toRadians(degreeOffset);
        float spanRad = Misc.normalizeAngle(endRad - startRad);
        float anglePerSegment = spanRad / segments;

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(90f, 0, 0, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        tex.bindTexture();

        GL11.glEnable(GL11.GL_BLEND);
        if (additive) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        GL11.glColor4ub((byte)color.getRed(),
                (byte)color.getGreen(),
                (byte)color.getBlue(),
                (byte)((float) color.getAlpha() * alphaMult));
        float texX = 0f;
        float incr = 1f / segments;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (float i = 0; i < segments + 1; i++) {
            boolean last = i == segments;
            //if (last) i = 0;
            float actuali = i;
            if (p.reverseRing) {
                actuali = -i;
            }
            float theta = (anglePerSegment * actuali) + offset;
            float cos = (float) Math.cos(theta);
            float sin = (float) Math.sin(theta);

            float m1 = 1f;
            float m2 = 1f;

            float x1 = cos * radius * m1;
            float y1 = sin * radius * m1;
            float x2 = cos * (radius + thickness * m2);
            float y2 = sin * (radius + thickness * m2);

            GL11.glTexCoord2f(0.5f, 0.05f);
            GL11.glVertex2f(x1, y1);

            GL11.glTexCoord2f(0.5f, 0.95f);
            GL11.glVertex2f(x2, y2);

            texX += incr;
            if (last) break;
        }

        GL11.glEnd();
        GL11.glPopMatrix();
    }

    public boolean isExpired() {
        return p.ship == null;
    }

}
