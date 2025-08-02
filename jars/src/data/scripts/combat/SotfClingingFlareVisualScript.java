package data.scripts.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.EnumSet;

public class SotfClingingFlareVisualScript extends BaseCombatLayeredRenderingPlugin {

	public static class SotfClingingFlareParams implements Cloneable {
		public CombatEntityAPI attachedTo;
		public String spritePath = "graphics/fx/starburst_glow1.png";
		public Color color;
		public float flareWidth;
		public float flareHeight;
		public float baseBrightness = 0f;
		public boolean fadeSize = false;
		public float angle = 0f;
		
		public SotfClingingFlareParams() {
		}

		public SotfClingingFlareParams(CombatEntityAPI attachedTo, Color color, float flareWidth, float flareHeight) {
			this.attachedTo = attachedTo;
			this.color = color;
			this.flareWidth = flareWidth;
			this.flareHeight = flareHeight;
		}
		
		@Override
		protected SotfClingingFlareParams clone() {
			try {
				return (SotfClingingFlareParams) super.clone();
			} catch (CloneNotSupportedException e) {
				return null; // should never happen
			}
		}
		
	}
	
	public SotfClingingFlareParams p;
	protected SpriteAPI sprite;
	protected FaderUtil fader;

	public SotfClingingFlareVisualScript(SotfClingingFlareParams p) {
		this.p = p;
		//fader = new FaderUtil(1f, 0f, 2f);
		//fader.fadeOut();
		fader = new FaderUtil(0f, 1f, 2f);
		fader.setBounceDown(true);
		fader.fadeIn();
		sprite = Global.getSettings().getSprite(p.spritePath);
	}
	
	public float getRenderRadius() {
		return Math.max(p.flareWidth, p.flareHeight) + 500f;
	}
	
	
	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.ABOVE_PARTICLES);
	}

	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused()) return;
		
		fader.advance(amount);

		if (fader.isFadedOut()) {
			fader.fadeIn();
		}
		
		if (entity != null && p.attachedTo != null) {
			if (p.attachedTo instanceof ShipAPI) {
				entity.getLocation().set(((ShipAPI)p.attachedTo).getShieldCenterEvenIfNoShield());
			} else {
				entity.getLocation().set(p.attachedTo.getLocation());
			}
		}
	}

	public void init(CombatEntityAPI entity) {
		super.init(entity);
	}

	public boolean isExpired() {
		if (p.attachedTo instanceof ShipAPI) {
			return p.attachedTo == null || p.attachedTo.isExpired() || !((ShipAPI)p.attachedTo).isAlive() || (p.attachedTo.getOwner() != 0 && p.attachedTo.getOwner() != 1);
		}
		return p.attachedTo == null || p.attachedTo.isExpired() || p.attachedTo.wasRemoved();
	}

	public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		float x = entity.getLocation().x;
		float y = entity.getLocation().y;
	
		float b = fader.getBrightness();
		if (fader.isFadingIn() && b > 0.01f) {
			b = (float) Math.sqrt(b);
		} else {
			b *= b;
		}
		float alphaMult = viewport.getAlphaMult();
		
		alphaMult *= (b * 0.5f);
		alphaMult += p.baseBrightness;
		
		float f = 0.5f + 0.5f * b;
		if (!p.fadeSize) {
			f = 1f;
		}
		
		sprite.setColor(p.color);
		sprite.setSize(p.flareWidth * f, p.flareHeight * f);
		sprite.setAngle(p.angle);
		sprite.setAdditiveBlend();
		sprite.setAlphaMult(alphaMult);
		sprite.renderAtCenter(x, y);
		
		//f *= 0.75f;
		sprite.setColor(Misc.scaleAlpha(Color.white, 1f));
		sprite.setSize(p.flareWidth * f, p.flareHeight * f * 0.33f);
		sprite.setAngle(p.angle);
		sprite.setAdditiveBlend();
		sprite.setAlphaMult(alphaMult);
		sprite.renderAtCenter(x, y);
		
//		f = 0.5f + 0.5f * b;
//		sprite.setColor(p.color);
//		sprite.setSize(p.flareHeight * f, p.flareWidth * f * 0.5f);
//		sprite.setAdditiveBlend();
//		sprite.setAlphaMult(alphaMult);
//		sprite.renderAtCenter(x, y);
		
//		f *= 0.5f;
//		sprite.setColor(Misc.scaleAlpha(Color.white, b));
//		sprite.setSize(p.flareHeight * f, p.flareWidth * f * 0.5f);
//		sprite.setAdditiveBlend();
//		sprite.setAlphaMult(alphaMult);
//		sprite.renderAtCenter(x, y);
	}

	

}
