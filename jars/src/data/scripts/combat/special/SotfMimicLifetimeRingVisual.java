// draws a big circle around a ship, very useful for AoE auras and the like
package data.scripts.combat.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.JitterUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.SotfNaniteSynthesized;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin.SotfInvokeHerBlessingEchoScript;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin.SotfMimicLifespanListener;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.EnumSet;

public class SotfMimicLifetimeRingVisual extends BaseCombatLayeredRenderingPlugin {


    protected SpriteAPI atmosphereTex;
    protected ShipAPI ship;
    protected int segments;
    protected float fade = 1f;
    protected SpriteAPI expiringSprite = Global.getSettings().getSprite("ui", "sotf_mimic_expiring");
    protected SpriteAPI vigorSprite = Global.getSettings().getSprite("ui", "sotf_mimic_vigor");
    protected JitterUtil jitter = new JitterUtil();
    protected float prop = 1f;

    public SotfMimicLifetimeRingVisual(ShipAPI ship) {
        this.ship = ship;

        jitter.setUseCircularJitter(true);
        jitter.setSetSeedOnRender(false);
        expiringSprite.setSize(20f, 40f);
        expiringSprite.setColor(Misc.setAlpha(SotfNaniteSynthesized.COLOR_STRONGER, 155));
        vigorSprite.setSize(25f, 25f);
        vigorSprite.setColor(Misc.setAlpha(SotfNaniteSynthesized.COLOR_STRONGER, 155));
    }

    public float getRenderRadius() {
        return ship.getShieldRadiusEvenIfNoShield() + 100f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (ship == null) return;
        if (!Global.getCombatEngine().isUIShowingHUD()) return;

        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
            renderRing(ship.getLocation().x, ship.getLocation().y, ship.getShieldRadiusEvenIfNoShield() + 30f, 8f, 0.75f * ship.getExtraAlphaMult2() * fade, segments, atmosphereTex, SotfNaniteSynthesized.COLOR, false, false);
            renderRing(ship.getLocation().x, ship.getLocation().y, ship.getShieldRadiusEvenIfNoShield() + 30f, 8f, 0.25f * ship.getExtraAlphaMult2() * fade, segments, atmosphereTex, SotfNaniteSynthesized.COLOR.darker().darker(), false, true);
            if (ship.hasListenerOfClass(SotfInvokeHerBlessingPlugin.SotfMimicDecayListener.class)) {
                //expiringSprite.render(ship.getLocation().x, ship.getLocation().y + ship.getShieldRadiusEvenIfNoShield() + 20f);
                jitter.render(expiringSprite, ship.getLocation().x, ship.getLocation().y + ship.getShieldRadiusEvenIfNoShield() + 20f, 2, 2, 3);
            } else if (ship.hasListenerOfClass(SotfInvokeHerBlessingPlugin.SotfUnlivingVigorListener.class)) {
                jitter.render(vigorSprite, ship.getLocation().x, ship.getLocation().y + ship.getShieldRadiusEvenIfNoShield() + 20f, 1, 1, 3);
            }
        }
    }

    public void init(CombatEntityAPI entity) {
        super.init(entity);

        atmosphereTex = Global.getSettings().getSprite("combat", "corona_soft");

        float perSegment = 2f;
        segments = (int) ((ship.getShieldRadiusEvenIfNoShield() * 2f * 3.14f) / perSegment);
        if (segments < 8) segments = 8;
    }

    public void advance(float amount) {
        if (ship == null) return;
        if (!ship.hasListenerOfClass(SotfMimicLifespanListener.class)) {
            fade -= amount;
            if (fade < 0) fade = 0;
        }
        entity.getLocation().set(ship.getLocation().x, ship.getLocation().y);
    }

    private void renderRing(float x, float y, float radius, float thickness, float alphaMult, int segments, SpriteAPI tex, Color color, boolean additive, boolean alwaysFull) {
        if (ship.hasListenerOfClass(SotfMimicLifespanListener.class)) {
            SotfMimicLifespanListener listener = ship.getListenerManager().getListeners(SotfMimicLifespanListener.class).get(0);
            prop = listener.time / listener.lifespan;
        }

        float startDeg = 0f;
        float totalArc = 1f - Math.min(1f, prop);
        if (alwaysFull) {
            totalArc = 1f;
        }
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
        if (ship == null) return true;
        return !ship.isAlive();
    }

}
