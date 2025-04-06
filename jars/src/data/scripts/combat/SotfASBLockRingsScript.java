// Visually represents a ship's ASB mark with three rings that lock in around it
package data.scripts.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicAnim;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.EnumSet;

public class SotfASBLockRingsScript extends BaseCombatLayeredRenderingPlugin {

	public SotfASBLockOnScript plugin;

	public SotfASBLockRingsScript(SotfASBLockOnScript plugin) {
		this.plugin = plugin;
	}

	public void advance(float amount) {
		entity.getLocation().set(plugin.p.target.getLocation());
	}

	public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		if (plugin.p.target == null) {
			return;
		}
		float progress = plugin.progress;

		Color markColor = Misc.setAlpha(Misc.getNegativeHighlightColor(), 115);
		float radius = plugin.p.target.getShieldRadiusEvenIfNoShield() - (50f * MagicAnim.smooth(plugin.expireProgress));
		int segments = (int) ((radius * 2f * 3.14f) / 2f);
		float alpha1 = Math.min((progress) / (0.33f),1);
		float alpha2 = Math.min((progress - 0.33f) / (0.33f),1);
		alpha2 = Math.max(alpha2, 0);
		float alpha3 = Math.min((progress - 0.67f) / (0.33f),1);
		alpha3 = Math.max(alpha3, 0);

		float expireAlpha = (1f - Math.min(plugin.expireProgress, 1));
		if (plugin.expiring) {
			radius = plugin.p.target.getShieldRadiusEvenIfNoShield() + 50f + (50f * MagicAnim.smooth(plugin.expireProgress));
		}

		if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
			if (!plugin.expiring) {

				renderRing(plugin.p.target.getShieldCenterEvenIfNoShield().x,
						plugin.p.target.getShieldCenterEvenIfNoShield().y,
						plugin.p.target.getShieldRadiusEvenIfNoShield() + 100f - (50f * alpha1),
						16f, alpha1,
						segments,
						Global.getSettings().getSprite("combat", "corona_soft"),
						markColor, true);
				renderRing(plugin.p.target.getShieldCenterEvenIfNoShield().x,
						plugin.p.target.getShieldCenterEvenIfNoShield().y,
						plugin.p.target.getShieldRadiusEvenIfNoShield() + 120f - (50f * alpha2),
						12f, alpha2,
						segments,
						Global.getSettings().getSprite("combat", "corona_soft"),
						markColor, true);
				renderRing(plugin.p.target.getShieldCenterEvenIfNoShield().x,
						plugin.p.target.getShieldCenterEvenIfNoShield().y,
						plugin.p.target.getShieldRadiusEvenIfNoShield() + 140f - (50f * alpha3),
						8f, alpha3,
						segments,
						Global.getSettings().getSprite("combat", "corona_soft"),
						markColor, true);
			} else {
				renderRing(plugin.p.target.getShieldCenterEvenIfNoShield().x,
						plugin.p.target.getShieldCenterEvenIfNoShield().y,
						radius,
						16f, expireAlpha,
						segments,
						Global.getSettings().getSprite("combat", "corona_soft"),
						markColor, true);
				renderRing(plugin.p.target.getShieldCenterEvenIfNoShield().x,
						plugin.p.target.getShieldCenterEvenIfNoShield().y,
						radius + 20f,
						12f, expireAlpha,
						segments,
						Global.getSettings().getSprite("combat", "corona_soft"),
						markColor, true);
				renderRing(plugin.p.target.getShieldCenterEvenIfNoShield().x,
						plugin.p.target.getShieldCenterEvenIfNoShield().y,
						radius + 40f,
						8f, expireAlpha,
						segments,
						Global.getSettings().getSprite("combat", "corona_soft"),
						markColor, true);
			}
		}
	}

	public float getRenderRadius() {
		return 500f;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER, CombatEngineLayers.BELOW_INDICATORS_LAYER);
	}

	public boolean isExpired() {
		return plugin == null || plugin.p.target == null || plugin.p.user == null || plugin.expireProgress >= 1;
	}

	private void renderRing(float x, float y, float radius, float thickness, float alphaMult, int segments, SpriteAPI tex, Color color, boolean additive) {
		float startRad = (float) Math.toRadians(0);
		float endRad = (float) Math.toRadians(360);
		float spanRad = Misc.normalizeAngle(endRad - startRad);
		float anglePerSegment = spanRad / segments;

		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 0);
		GL11.glRotatef(0, 0, 0, 1);
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
			if (last) i = 0;
			float theta = anglePerSegment * i;
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

}
