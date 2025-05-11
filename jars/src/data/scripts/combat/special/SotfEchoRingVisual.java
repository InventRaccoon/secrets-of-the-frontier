// draws a big circle around a ship, very useful for AoE auras and the like
package data.scripts.combat.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin.SotfInvokeHerBlessingEchoScript;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.EnumSet;

public class SotfEchoRingVisual extends BaseCombatLayeredRenderingPlugin {

    protected SpriteAPI atmosphereTex;
    public SotfInvokeHerBlessingEchoScript echo;
    protected int segments;
    protected int shriekSegments;

    public SotfEchoRingVisual(SotfInvokeHerBlessingEchoScript echo) {
        this.echo = echo;
    }

    public float getRenderRadius() {
        float renderRadius = echo.shieldRadius + 100f;
        if (echo.indicatorFade > 0 && SotfInvokeHerBlessingPlugin.haveUpgrade(SotfIDs.COTL_SHRIEKOFTHEDAMNED)) {
            renderRadius += SotfInvokeHerBlessingPlugin.SHRIEK_RANGE;
        }
        return renderRadius;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (echo == null) return;
        if (echo.fade <= 0) return;
        if (!Global.getCombatEngine().isUIShowingHUD()) return;

        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
            renderRing(echo.loc.x, echo.loc.y, echo.shieldRadius, 8f, 0.75f * echo.fade, segments, atmosphereTex, echo.color, false, false);
            renderRing(echo.loc.x, echo.loc.y, echo.shieldRadius, 8f, 0.25f * echo.fade, segments, atmosphereTex, echo.color.darker().darker(), false, true);
            if (SotfInvokeHerBlessingPlugin.haveUpgrade(SotfIDs.COTL_SHRIEKOFTHEDAMNED)) {
                renderRing(echo.loc.x, echo.loc.y, SotfInvokeHerBlessingPlugin.SHRIEK_RANGE, 10f, 0.75f * echo.fade * echo.indicatorFade, shriekSegments, atmosphereTex, SotfInvokeHerBlessingPlugin.UI_COLOR, false, true);
            }
        }
    }

    public void init(CombatEntityAPI entity) {
        super.init(entity);

        atmosphereTex = Global.getSettings().getSprite("combat", "corona_soft");

        float perSegment = 2f;
        segments = (int) ((echo.shieldRadius * 2f * 3.14f) / perSegment);
        if (segments < 8) segments = 8;

        shriekSegments = (int) ((SotfInvokeHerBlessingPlugin.SHRIEK_RANGE * 2f * 3.14f) / perSegment);
        if (shriekSegments < 8) shriekSegments = 8;
    }

    public void advance(float amount) {
        if (echo == null) return;
        entity.getLocation().set(echo.loc.x, echo.loc.y);
    }

    private void renderRing(float x, float y, float radius, float thickness, float alphaMult, int segments, SpriteAPI tex, Color color, boolean additive, boolean alwaysFull) {
        float startDeg = 0f;
        float totalArc = 1f - (echo.elapsed / echo.maxLifetime);
        if (alwaysFull) {
            totalArc = 1f;
        }
        if (totalArc > 1f) totalArc = 1f;
        if (totalArc < 0f) totalArc = 0f;
        float endDeg = totalArc * 360f;

        float startRad = (float) Math.toRadians(startDeg);
        float endRad = (float) Math.toRadians(endDeg);
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
            float theta = (anglePerSegment * i);
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
        return echo == null || echo.fade <= 0;
    }

}
